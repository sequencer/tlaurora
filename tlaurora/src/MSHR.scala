package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.Decoupled

case class MSHRParameters(
  slots:  Int,
  scheme: TLSerializerScheme)
    extends SerializableModuleParameter

class MSHRSourceA extends Bundle
class MSHRSinkD extends Bundle
class MSHRSinkA extends Bundle
class MSHRSourceD extends Bundle

/** [[MSHR]] for recording outstanding transactions.
  * For each transaction it will allocate a slot in the MSHR, return the slot id to the source.
  */
class MSHR(val parameter: MSHRParameters) extends Module with SerializableModule[MSHRParameters] {
  // Master
  val sourceA = IO(Flipped(Decoupled(new MSHRSourceA)))
  val sinkD = IO(Flipped(Decoupled(new MSHRSinkD)))
  // Slave
  val sinkA = IO(Flipped(Decoupled(new MSHRSinkA)))
  val sourceD = IO(Flipped(Decoupled(new MSHRSourceD)))
}
