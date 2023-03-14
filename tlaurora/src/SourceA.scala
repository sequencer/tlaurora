package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.{log2Ceil, DecoupledIO, MuxLookup}
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

class SourceA(val parameter: SourceAParameters) extends Module with SerializableModule[SourceAParameters] {
  val masterAChannel: DecoupledIO[TLChannelA] = IO(DecoupledIO(new TLChannelA(parameter.tileLinkChannelAParameter)))
  val pdu:            DecoupledIO[UInt] = IO(DecoupledIO(UInt(parameter.userPDUWidth.W)))

  /** catch the posedge signal of [[masterAChannel.valid]]. */
  val setCounter: Bool = !RegNext(masterAChannel.valid) && masterAChannel.valid || !masterAChannel.valid

  /** counter to log how many octet has been sent out to PDU Queue. */
  val counter = RegInit(0.U(log2Ceil(parameter.maxSize / parameter.userPDUWidth).W))

  /** signals on wire */
  val octets: Vec[UInt] = Wire(VecInit.fill(parameter.maxSize / 8)(UInt(8.W)))

  when(setCounter) {
    // clear counter when masterAChannel valid has a posedge.
    counter := 0.U
  }.elsewhen(pdu.fire) {
    // increase counter when pdu is ready.
    counter := counter + 1.U
  }

  pdu := octets(counter)
  pdu.valid := masterAChannel.valid

  // pull up the ready of masterAChannel indicate the masterAChannel is fired.
  masterAChannel.ready := counter === MuxLookup(
    masterAChannel.bits.opcode,
    0.U(2.W),
    Seq(
      OpCode.Get -> (parameter.getSize / parameter.userPDUWidth).U,
      OpCode.PutFullData -> (parameter.putFullDataSize / parameter.userPDUWidth).U,
      OpCode.PutPartialData -> (parameter.putPartialDataSize / parameter.userPDUWidth).U
    )
  )
  when(masterAChannel.ready) {
    assert(masterAChannel.valid)
  }

  val metadata: UInt =
    0.U(1.W) ##
      MuxLookup(
        masterAChannel.bits.opcode,
        0.U(2.W),
        Seq(
          OpCode.Get -> "b00".U(2.W),
          OpCode.PutFullData -> "b10".U(2.W),
          OpCode.PutPartialData -> "b11".U(2.W)
        )
      ) ##
      masterAChannel.bits.corrupt.asBool ##
      masterAChannel.bits.size.asTypeOf(UInt(4.W))

  // package transaction into octets
  octets(0) := metadata
  masterAChannel.bits.source.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          index // source
      ) := VecInit(bit).asUInt
  }

  masterAChannel.bits.address.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          parameter.tileLinkChannelAParameter.sourceWidth / 8 + // source
          index // address
      ) := VecInit(bit).asUInt
  }

  masterAChannel.bits.data.asBools.grouped(8).zipWithIndex.foreach {
    case (bit, index) =>
      octets(
        1 + // metadata
          parameter.tileLinkChannelAParameter.sourceWidth / 8 + // source
          parameter.tileLinkChannelAParameter.addressWidth / 8 + // address
          index // data
      ) := VecInit(bit).asUInt
  }

  masterAChannel.bits.mask.asBools.grouped(8).zipWithIndex.foreach {
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
