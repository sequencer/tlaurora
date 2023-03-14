package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.DecoupledIO
import org.chipsalliance.tilelink.bundle.{TLChannelD, TileLinkChannelDParameter}

case class SourceDParameters(
  tileLinkChannelDParameter: TileLinkChannelDParameter,
  scheme:                    TLSerializerScheme)
    extends SerializableModuleParameter

class SourceD(val parameter: SourceDParameters) extends Module with SerializableModule[SourceDParameters] {
  val slaveDChannel = IO(DecoupledIO(new TLChannelD(parameter.tileLinkChannelDParameter)))
  val mshrIO = IO(DecoupledIO(new MSHRSourceD))
}
