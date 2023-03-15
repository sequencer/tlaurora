package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.{log2Ceil, DecoupledIO, MuxLookup, RegEnable}
import org.chipsalliance.tilelink.bundle.{OpCode, TLChannelA, TileLinkChannelAParameter}

case class SourceAParameters(
  tileLinkChannelAParameter: TileLinkChannelAParameter,
  scheme:                    TLSerializerScheme,
  userPDUWidth:              Int)
    extends SerializableModuleParameter {
  require(tileLinkChannelAParameter.sizeWidth <= 4, "Size width should be less than 4")

  def getSize = (8 + // metadata
    tileLinkChannelAParameter.sourceWidth + // source
    tileLinkChannelAParameter.addressWidth // address
  ) / userPDUWidth

  def putFullDataSize = (8 + // metadata
    tileLinkChannelAParameter.sourceWidth + // source
    tileLinkChannelAParameter.addressWidth + // address
    tileLinkChannelAParameter.dataWidth // data
  ) / userPDUWidth

  def putPartialDataSize = (8 + // metadata
    tileLinkChannelAParameter.sourceWidth + // source
    tileLinkChannelAParameter.addressWidth + // address
    tileLinkChannelAParameter.dataWidth + // data
    tileLinkChannelAParameter.dataWidth / 8 // mask
  ) / userPDUWidth

  def maxSize = Seq(getSize, putFullDataSize, putPartialDataSize).max
}

/** This module should under the txBus clock domain.
  *
  * The behavior of this [[SourceA]]:
  * [[SourceA]] maintains a [[latchMessage]] to latch the [[latchMessage]] when [[masterAChannel.fire]],
  * after [[masterAChannel.fire]]:
  *  - [[counter]] set the size of serializing [[octets]] and start to count down;
  *  - [[pdu.bits]] will be dynamic selected from [[latchMessage]] based on [[counter]];
  *  - after [[counter]] turns back to 0, [[pdu.valid]] should be deasserted, and [[masterAChannel.ready]] should be asserted;
  * [[masterAChannel.ready]] is asserted when [[counter]] turns back to 0.
  * [[pdu.valid]] is asserted when [[counter]] is not 0.
  */
class SourceA(val parameter: SourceAParameters) extends Module with SerializableModule[SourceAParameters] {
  val masterAChannel: DecoupledIO[TLChannelA] = IO(
    Flipped(DecoupledIO(new TLChannelA(parameter.tileLinkChannelAParameter)))
  )
  val pdu: DecoupledIO[SourceToPDUMux] = IO(
    DecoupledIO(new SourceToPDUMux(parameter.userPDUWidth, log2Ceil(parameter.maxSize / parameter.userPDUWidth)))
  )

  /** latch from TileLink Message. */
  val latchMessage: TLChannelA = RegEnable(masterAChannel.bits, masterAChannel.fire)

  /** counter to log how many octets has been sent out to PDU Queue. */
  val counter: UInt = RegInit(0.U(log2Ceil(parameter.maxSize / parameter.userPDUWidth).W))

  /** signals on wire */
  val octets: Vec[UInt] = Wire(VecInit.fill(parameter.maxSize / 8)(UInt(8.W)))

  /** how many PDU will be sent for this TileLink message. */
  val messagePDUCounts: UInt = MuxLookup(
    latchMessage.opcode,
    0.U(2.W),
    Seq(
      OpCode.Get -> (parameter.getSize / parameter.userPDUWidth).U,
      OpCode.PutFullData -> (parameter.putFullDataSize / parameter.userPDUWidth).U,
      OpCode.PutPartialData -> (parameter.putPartialDataSize / parameter.userPDUWidth).U
    )
  )

  counter := Mux(
    masterAChannel.fire,
    // set counter to corresponding size of TLMessage.
    messagePDUCounts,
    // count down counter when [[pdu.fire]].
    Mux(counter === 0.U, 0.U, counter - pdu.fire.asUInt)
  )

  // dynamic selection
  pdu.bits.userPDU := octets(counter)
  pdu.bits.size := messagePDUCounts

  // package transaction into octets
  octets(0) := 0.U(1.W) ## MuxLookup(
    latchMessage.opcode,
    0.U(2.W),
    Seq(
      OpCode.Get -> "b00".U(2.W),
      OpCode.PutFullData -> "b10".U(2.W),
      OpCode.PutPartialData -> "b11".U(2.W)
    )
  ) ##
    latchMessage.corrupt.asBool ##
    latchMessage.size.asTypeOf(UInt(4.W))
  latchMessage.source.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          index // source
      ) := VecInit(bit).asUInt
  }
  latchMessage.address.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          parameter.tileLinkChannelAParameter.sourceWidth / 8 + // source
          index // address
      ) := VecInit(bit).asUInt
  }
  latchMessage.data.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          parameter.tileLinkChannelAParameter.sourceWidth / 8 + // source
          parameter.tileLinkChannelAParameter.addressWidth / 8 + // address
          index // data
      ) := VecInit(bit).asUInt
  }
  latchMessage.mask.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          parameter.tileLinkChannelAParameter.sourceWidth / 8 + // source
          parameter.tileLinkChannelAParameter.addressWidth / 8 + // address
          parameter.tileLinkChannelAParameter.dataWidth / 8 + // data
          index // mask
      ) := VecInit(bit).asUInt
  }
}
