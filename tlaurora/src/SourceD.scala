package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.{DecoupledIO, MuxLookup, log2Ceil}
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

class SourceD(val parameter: SourceDParameters) extends Module with SerializableModule[SourceDParameters] {
  val slaveDChannel = IO(DecoupledIO(Flipped(new TLChannelD(parameter.tileLinkChannelDParameter))))
  val pdu = IO(Output(DecoupledIO(UInt(parameter.userPDUWidth.W))))

  /** counter to log how many octet has been sent out to PDU Queue. */
  val counter = RegInit(0.U(log2Ceil(parameter.maxSize / parameter.userPDUWidth).W))

  /** catch the posedge signal of [[slaveDChannel.valid]]. */
  val clearCounter: Bool = !RegNext(slaveDChannel.valid) && slaveDChannel.valid || !slaveDChannel.valid

  /** signals on wire */
  val octets: Vec[UInt] = Wire(VecInit.fill(parameter.maxSize / 8)(UInt(8.W)))

  // clear counter when masterAChannel valid has a posedge.
  // increase counter when pdu is ready.
  counter := Mux(clearCounter, 0.U, counter + pdu.fire.asUInt)

  // dynamic selection, add a register for [[octets]] if necessary
  pdu.bits := octets(counter)
  // couple [[masterAChannel]] to pdu queue.
  pdu.valid := slaveDChannel.valid

  // pull up the ready of [[slaveDChannel]] indicate the [[slaveDChannel]] is fired.
  slaveDChannel.ready := counter === MuxLookup(
    slaveDChannel.bits.opcode,
    0.U(2.W),
    Seq(
      OpCode.AccessAck -> (parameter.accessAckSize / parameter.userPDUWidth).U,
      OpCode.AccessAckData -> (parameter.accessAckDataSize / parameter.userPDUWidth).U,
    )
  )

  // package transaction into octets
  octets(0) := 0.U(1.W) ## MuxLookup(
    slaveDChannel.bits.opcode,
    0.U(2.W),
    Seq(
      OpCode.AccessAck -> "b0".U(1.W),
      OpCode.AccessAckData -> "b1".U(1.W),
    ) // Also hasData
  ) ##
    slaveDChannel.bits.denied.asBool ## //
    slaveDChannel.bits.corrupt.asBool ## //
    slaveDChannel.bits.size.asTypeOf(UInt(4.W))
  slaveDChannel.bits.source.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          index // source
      ) := VecInit(bit).asUInt
  }
  slaveDChannel.bits.data.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          parameter.tileLinkChannelDParameter.sourceWidth / 8 + // source
          index // data
      ) := VecInit(bit).asUInt
  }

}
