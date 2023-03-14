package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.DecoupledIO
import org.chipsalliance.tilelink.bundle._

case class TXParameters(
  masterLinkParameters: TLLinkParameter,
  slaveLinkParameters:  TLLinkParameter,
  scheme:               TLSerializerScheme)
    extends SerializableModuleParameter

/** Take TileLink message in master ACE Channel and message in slave BD Channel.
  * Convert them to Aurora [[UserPDUs]].
  */
class TX(val parameter: TXParameters) extends Module with SerializableModule[TXParameters] {
  // IO
}
