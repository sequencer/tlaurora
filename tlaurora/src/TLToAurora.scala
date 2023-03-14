package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.Decoupled
import org.chipsalliance.tilelink.bundle.{TLLink, TLLinkParameter}

case class TLToAuroraParameters(
  masterLinkParameters: TLLinkParameter,
  slaveLinkParameters:  TLLinkParameter,
  serializerScheme:     TLSerializerScheme,
  userPDUWidth:         Int)
    extends SerializableModuleParameter {
  require(userPDUWidth % 8 == 0, "userPDUWidth must be multiple of 8")
  require(serializerScheme == TLUHNoAtomic, "Only support TLUHNoAtomic scheme for now")
  serializerScheme match {
    case TLUHNoAtomic =>
      require(!masterLinkParameters.hasBCEChannels, "TLUHNoAtomic doesn't support BCE channel")
    case TLUH =>
    case TLC  =>
  }
}

class TLToAurora(val parameter: TLToAuroraParameters) extends RawModule with SerializableModule[TLToAuroraParameters] {
  // IOs
  // TileLink TX Clock Domain
  val txBusClock = IO(Input(Clock()))
  val txBusReset = IO(Input(Reset()))
  // TileLink Master link under txBusClock domain
  val masterLink = IO(new TLLink(parameter.masterLinkParameters))
  def txBusDomain[T](unit: => T): T = { withClockAndReset(txBusClock, txBusReset) { unit } }

  // TileLink RX Clock Domain
  val rxBusClock = IO(Input(Clock()))
  val rxBusReset = IO(Input(Reset()))
  // TileLink Slave under rxBusClock domain
  val slaveLink = IO(new TLLink(parameter.slaveLinkParameters))
  def rxBusDomain[T](unit: => T): T = { withClockAndReset(rxBusClock, rxBusReset) { unit } }

  // Aurora TX Clock Domain
  // Basically this is the clock from Serdes PLL
  val txAuroraClock = IO(Input(Clock()))
  val txAuroraReset = IO(Input(Reset()))
  // TX Aurora IO under txClock domain
  val txUserFlowControlMessages = IO(Decoupled(new UserFlowControlMessages))
  val txUserPDUs = IO(Decoupled(new UserFlowControlMessages))
  def txAuroraDomain[T](unit: => T): T = { withClockAndReset(txAuroraClock, txAuroraReset) { unit } }

  // Aurora RX Clock Domain
  // Basically this is the clock from Serdes CDR
  val rxAuroraClock = IO(Input(Clock()))
  val rxAuroraReset = IO(Input(Reset()))
  // RX Aurora IO under rxClock domain
  val rxUserFlowControlMessages = IO(Flipped(Decoupled(new UserFlowControlMessages)))
  def rxAuroraDomain[T](unit: => T): T = { withClockAndReset(rxAuroraClock, rxAuroraReset) { unit } }

  // Module
  // Master
  val sourceA = txBusDomain(
    Module(
      new SourceA(
        SourceAParameters(
          parameter.masterLinkParameters.channelAParameter,
          parameter.serializerScheme,
          parameter.userPDUWidth
        )
      )
    )
  )
  val sinkD = txBusDomain(
    Module(
      new SinkD(
        SinkDParameters(
          parameter.masterLinkParameters.channelDParameter,
          parameter.serializerScheme,
          parameter.userPDUWidth
        )
      )
    )
  )
  val sinkA = rxBusDomain(
    Module(
      new SinkA(
        SinkAParameters(
          parameter.slaveLinkParameters.channelAParameter,
          parameter.serializerScheme,
          parameter.userPDUWidth
        )
      )
    )
  )
  val sourceD = rxBusDomain(
    Module(
      new SourceD(
        SourceDParameters(
          parameter.slaveLinkParameters.channelDParameter,
          parameter.serializerScheme,
          parameter.userPDUWidth
        )
      )
    )
  )

  // Connection
  // TileLink Master
  sourceA.masterAChannel :<>= masterLink.a
  masterLink.d :<>= sinkD.masterDChannel

  // TileLink Slave
  slaveLink.a :<>= sinkA.slaveAChannel
  sourceD.slaveDChannel :<>= slaveLink.d

  // Aurora TX
}

/** This design borrows idea from OmniXtend-1.0.3 Chapter 4. Retransmission. */
class RetransmissionQueue extends Module
class UserFlowControlMessages extends Bundle
