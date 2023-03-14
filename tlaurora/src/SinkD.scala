package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.DecoupledIO
import org.chipsalliance.tilelink.bundle.{TLChannelA, TLChannelD, TileLinkChannelAParameter, TileLinkChannelDParameter}

case class SinkDParameters(
  tileLinkChannelDParameter: TileLinkChannelDParameter,
  scheme:                    TLSerializerScheme)
    extends SerializableModuleParameter

class SinkD(val parameter: SinkDParameters) extends Module with SerializableModule[SinkDParameters] {
  val masterDChannel = IO(Flipped(DecoupledIO(new TLChannelD(parameter.tileLinkChannelDParameter))))
  val mshrIO = IO(DecoupledIO(new MSHRSinkD))
}
