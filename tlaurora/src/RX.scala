package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.DecoupledIO
import org.chipsalliance.tilelink.bundle.{TLChannelA, TLChannelB, TLChannelC, TLChannelD, TLChannelE, TLLinkParameter}
import org.chipsalliance.tilelink.tlaurora.UserPDUs

case class RXParameters(
                         masterLinkParameters: TLLinkParameter,
                         slaveLinkParameters: TLLinkParameter,
                       ) extends SerializableModuleParameter

/** Take Aurora RX [[UserPDUs]].
 * Convert them to TileLink message in master BD Channel and slave ACE Channel.
 */
class RX(val parameter: RXParameters) extends Module
  with SerializableModule[RXParameters] {
  // IO
  // TileLink Master Channels
  val masterBChannel = parameter.slaveLinkParameters.channelBParameter.map(p => IO(Flipped(DecoupledIO(new TLChannelB(p)))))
  val masterDChannel = IO(Flipped(DecoupledIO(new TLChannelD(parameter.slaveLinkParameters.channelDParameter))))
  // TileLink Slave Channels
  val slaveAChannel = IO(Flipped(DecoupledIO(new TLChannelA(parameter.masterLinkParameters.channelAParameter))))
  val slaveCChannel = parameter.masterLinkParameters.channelCParameter.map(p => IO(Flipped(DecoupledIO(new TLChannelC(p)))))
  val slaveEChannel = parameter.masterLinkParameters.channelEParameter.map(p => IO(Flipped(DecoupledIO(new TLChannelE(p)))))
}
