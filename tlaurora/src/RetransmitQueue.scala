package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.{DecoupledIO, Queue}

case class RetransmitQueueParameters(
  userPDUWidth: Int,
  ufcPDUWidth:  Int,
  queueSize:    Int)
    extends SerializableModuleParameter

/** This design borrows idea from OmniXtend-1.0.3 Chapter 4. Retransmission. */
class RetransmitQueue(val parameter: RetransmitQueueParameters)
    extends RawModule
    with SerializableModule[RetransmitQueueParameters] {
  // TileLink TX Clock Domain
  val txBusClock = IO(Input(Clock()))
  val txBusReset = IO(Input(Reset()))

  def txBusDomain[T](unit: => T): T = withClockAndReset(txBusClock, txBusReset)(unit)

  // TileLink TX Clock Domain
  val rxBusClock = IO(Input(Clock()))
  val rxBusReset = IO(Input(Reset()))

  def rxBusDomain[T](unit: => T): T = withClockAndReset(rxBusClock, rxBusReset)(unit)

  /** From PDUMux */
  val userPduIn: DecoupledIO[UInt] = IO(Flipped(DecoupledIO(UInt(parameter.userPDUWidth.W))))

  /** To TX */
  val userPduOut: DecoupledIO[UInt] = IO(DecoupledIO(UInt(parameter.userPDUWidth.W)))

  /** From RX in RX Domain, need catch with RX Clock. */
  val ufcPduIn: DecoupledIO[UInt] = IO(Flipped(DecoupledIO(UInt(parameter.ufcPDUWidth.W))))

  /** Memory of Queue. */
  val queueMem = SyncReadMem(parameter.queueSize, UInt(parameter.userPDUWidth.W))

  /** Queue Pointer.
    * Maintain a stack of pointers to the queue memory.
    */
}
