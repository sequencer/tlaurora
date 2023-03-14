package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.DecoupledIO
import org.chipsalliance.tilelink.bundle.{TLChannelA, TileLinkChannelAParameter}

case class SourceAParameters(
  tileLinkChannelAParameter: TileLinkChannelAParameter,
  scheme:                    TLSerializerScheme)
    extends SerializableModuleParameter

class SourceA(val parameter: SourceAParameters) extends Module with SerializableModule[SourceAParameters] {
  val masterAChannel = IO(DecoupledIO(new TLChannelA(parameter.tileLinkChannelAParameter)))
  val mshrIO = IO(DecoupledIO(new MSHRSourceA))
}
