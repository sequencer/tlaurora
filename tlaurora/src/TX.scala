package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.DecoupledIO
import org.chipsalliance.tilelink.bundle._

case class TXParameters(
                         masterLinkParameters: TLLinkParameter,
                         slaveLinkParameters: TLLinkParameter,
                       ) extends SerializableModuleParameter


/** Take TileLink message in master ACE Channel and message in slave BD Channel.
 * Convert them to Aurora [[UserPDUs]].
 */
class TX(val parameter: TXParameters) extends Module
  with SerializableModule[TXParameters] {
  // IO
  // TileLink Master Channels
  val masterAChannel = IO(DecoupledIO(new TLChannelA(parameter.masterLinkParameters.channelAParameter)))
  val masterCChannel = parameter.masterLinkParameters.channelCParameter.map(p => IO(DecoupledIO(new TLChannelC(p))))
  val masterEChannel = parameter.masterLinkParameters.channelEParameter.map(p => IO(DecoupledIO(new TLChannelE(p))))
  // TileLink Slave Channels
  val slaveBChannel = parameter.slaveLinkParameters.channelBParameter.map(p => IO(DecoupledIO(new TLChannelB(p))))
  val slaveDChannel = IO(DecoupledIO(new TLChannelD(parameter.slaveLinkParameters.channelDParameter)))
}
