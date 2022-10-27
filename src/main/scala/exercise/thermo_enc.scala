package exercise

import chisel3._
import chisel3.util._

class thermo_enc (val dwidth:Int) extends Module {
  val out_width = 1<<in_width
  val io = IO(new Bundle {
    val binary = Input (UInt(dwidth.W ))
    val thermo = Output(UInt(dwidth.W))
  })

  io.thermo := io.binary
}

object thermo_enc extends App {
  emitVerilog(new thermo_enc(dwidth=8), Array("--target-dir", "generated"))
}