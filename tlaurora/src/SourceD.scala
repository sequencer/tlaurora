package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.{log2Ceil, DecoupledIO, MuxLookup, RegEnable}
import org.chipsalliance.tilelink.bundle.{OpCode, TLChannelD, TileLinkChannelDParameter}

case class SourceDParameters(
  tileLinkChannelDParameter: TileLinkChannelDParameter,
  scheme:                    TLSerializerScheme,
  userPDUWidth:              Int)
    extends SerializableModuleParameter {
  def accessAckSize = (8 + // metadata
    tileLinkChannelDParameter.sourceWidth // source
  ) / userPDUWidth

  def accessAckDataSize = (8 + // metadata
    tileLinkChannelDParameter.sourceWidth + // source
    tileLinkChannelDParameter.dataWidth // data
  ) / userPDUWidth

  def maxSize = Seq(accessAckSize, accessAckDataSize).max
}

/** This module should under the txBus clock domain.
  *
  * The behavior of this [[SourceD]]:
  * [[SourceD]] maintains a [[latchMessage]] to latch the [[slaveDChannel.bits]] when [[slaveDChannel.fire]],
  * after [[slaveDChannel.fire]]:
  *  - [[counter]] set the size of serializing [[octets]] and start to count down;
  *  - [[pdu.bits]] will be dynamic selected from [[latchMessage]] based on [[counter]];
  *  - after [[counter]] turns back to 0, [[pdu.valid]] should be deasserted, and [[slaveDChannel.ready]] should be asserted;
  * [[slaveDChannel.ready]] is asserted when [[counter]] turns back to 0.
  * [[pdu.valid]] is asserted when [[counter]] is not 0.
  */
class SourceD(val parameter: SourceDParameters) extends Module with SerializableModule[SourceDParameters] {
  val slaveDChannel: DecoupledIO[TLChannelD] = IO(
    DecoupledIO(Flipped(new TLChannelD(parameter.tileLinkChannelDParameter)))
  )
  val pdu: DecoupledIO[SourceToPDUMux] = IO(
    DecoupledIO(new SourceToPDUMux(parameter.userPDUWidth, log2Ceil(parameter.maxSize / parameter.userPDUWidth)))
  )

  /** latch from TileLink Message. */
  val latchMessage: TLChannelD = RegEnable(slaveDChannel.bits, slaveDChannel.fire)

  /** counter to log how many octets has been sent out to PDU Queue. */
  val counter: UInt = RegInit(0.U(log2Ceil(parameter.maxSize / parameter.userPDUWidth).W))

  /** signals on wire */
  val octets: Vec[UInt] = Wire(VecInit.fill(parameter.maxSize / 8)(UInt(8.W)))

  /** how many PDU will be sent for this TileLink message. */
  val messagePDUCounts: UInt = MuxLookup(
    latchMessage.opcode,
    0.U(2.W),
    Seq(
      OpCode.AccessAck -> (parameter.accessAckSize / parameter.userPDUWidth).U,
      OpCode.AccessAckData -> (parameter.accessAckDataSize / parameter.userPDUWidth).U
    )
  )
  counter := Mux(
    slaveDChannel.fire,
    // set counter to corresponding size of TLMessage.
    messagePDUCounts,
    // count down counter when [[pdu.fire]].
    Mux(counter === 0.U, 0.U, counter - pdu.fire.asUInt)
  )

  // dynamic selection
  pdu.bits := octets(counter)
  pdu.bits.size := counter

  // package transaction into octets
  octets(0) := 0.U(1.W) ## MuxLookup(
    latchMessage.opcode,
    0.U(2.W),
    Seq(
      OpCode.AccessAck -> "b0".U(1.W),
      OpCode.AccessAckData -> "b1".U(1.W)
    ) // Also hasData
  ) ##
    latchMessage.denied.asBool ## //
    latchMessage.corrupt.asBool ## //
    latchMessage.size.asTypeOf(UInt(4.W))
  latchMessage.source.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          index // source
      ) := VecInit(bit).asUInt
  }
  latchMessage.data.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          parameter.tileLinkChannelDParameter.sourceWidth / 8 + // source
          index // data
      ) := VecInit(bit).asUInt
  }
}
