package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.{Decoupled, DecoupledIO}
import org.chipsalliance.tilelink.bundle.{TLChannelA, TLChannelB, TLChannelC, TLChannelD, TLChannelE, TLLink, TLLinkParameter}

case class TLToAuroraParameters(
    masterLinkParameters: TLLinkParameter,
    slaveLinkParameters: TLLinkParameter,
    // The number of octets in a User PDU
    userPDUOctets: Int,
    laneSize: Int
                               ) extends SerializableModuleParameter {
  assert(laneSize == 1, "only support 1 lane for now.")
}

class TLToAurora(val parameter: TLToAuroraParameters) extends RawModule
  with SerializableModule[TLToAuroraParameters] {
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
  val tx = Module(new TX(TXParameters(parameter.masterLinkParameters, parameter.slaveLinkParameters)))
  val rx = Module(new RX(RXParameters(parameter.masterLinkParameters, parameter.slaveLinkParameters)))
  val retransmitQueue = Module(new RetransmissionQueue)

  // Connection
  // TileLink Master
  tx.masterAChannel :<>= masterLink.a
  rx.masterBChannel.foreach(masterLink.b :<>= _)
  tx.masterCChannel.foreach(_ :<>= masterLink.c)
  masterLink.d :<>= rx.masterDChannel
  tx.masterEChannel.foreach(_ :<>= masterLink.e)

  // TileLink Slave
  slaveLink.a <>= rx.slaveAChannel
  tx.slaveBChannel.foreach(_ :<>= slaveLink.b)
  rx.slaveCChannel.foreach(slaveLink.c <>= _)
  tx.slaveDChannel :<>= slaveLink.d
  rx.slaveEChannel.foreach(slaveLink.e <>= _)
}

/** This design borrows idea from OmniXtend-1.0.3 Chapter 4. Retransmission. */
class RetransmissionQueue extends Module
class UserFlowControlMessages extends Bundle

class UserPDUs extends Bundle