package org.chipsalliance.tilelink.tlaurora

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util.{Decoupled, DecoupledIO, PriorityMux, RegEnable}
import org.chipsalliance.tilelink.bundle.{TLLink, TLLinkParameter}

case class PDUMuxParameters(
  userPDUWidth:     Int,
  sourceASizeWidth: Int,
  sourceDSizeWidth: Int)
    extends SerializableModuleParameter

/** Mux among multiple sources.
  * The priority of source is defined by TileLink spec.
  * messages cannot interleave.
  */
class PDUMux(val parameter: PDUMuxParameters) extends Module with SerializableModule[PDUMuxParameters] {
  val sourceA: DecoupledIO[SourceToPDUMux] = IO(
    Flipped(DecoupledIO(new SourceToPDUMux(parameter.userPDUWidth, parameter.sourceASizeWidth)))
  )
  val sourceD: DecoupledIO[SourceToPDUMux] = IO(
    Flipped(DecoupledIO(new SourceToPDUMux(parameter.userPDUWidth, parameter.sourceDSizeWidth)))
  )
  val userPduOut: DecoupledIO[UInt] = IO(DecoupledIO(UInt(parameter.userPDUWidth.W)))

  val remainSize: UInt = RegEnable(
    Mux(
      sourceD.fire,
      sourceD.bits.size,
      Mux(
        sourceA.fire,
        sourceA.bits.size,
        0.U
      )
    ),
    RegInit(0.U(parameter.sourceASizeWidth.W)),
    sourceA.fire || sourceD.fire
  )

  val selector: Vec[Bool] = RegEnable(
    VecInit(Seq(sourceD.valid, sourceA.valid)),
    VecInit(0.U(2.W).asBools),
    remainSize === 0.U
  )

  userPduOut.bits := PriorityMux(
    selector,
    Seq(
      sourceD.bits.userPDU,
      sourceA.bits.userPDU
    )
  )

  userPduOut.valid := sourceA.valid || sourceD.valid

}

class SourceToPDUMux(userPDUWidth: Int, sizeWidth: Int) extends Bundle {
  val userPDU = Output(UInt(userPDUWidth.W))
  val size = Output(UInt(sizeWidth.W))
}
