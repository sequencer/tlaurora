package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.DecoupledIO
import org.chipsalliance.tilelink.bundle.{TLChannelA, TileLinkChannelAParameter}

case class SinkAParameters(
  tileLinkChannelAParameter: TileLinkChannelAParameter,
  scheme:                    TLSerializerScheme)
    extends SerializableModuleParameter

class SinkA(val parameter: SinkAParameters) extends Module with SerializableModule[SinkAParameters] {
  val slaveAChannel = IO(Flipped(DecoupledIO(new TLChannelA(parameter.tileLinkChannelAParameter))))
  val mshrIO = IO(DecoupledIO(new MSHRSinkA))
}
