package test

import chisel3._
import chisel3.util._


class fir(length: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))
    val valid = Input(Bool())
    val out = Output(UInt(8.W))
    val consts = Input(Vec(length, UInt(8.W)))
  })

  //// verilog style
  //val taps = Reg(Vec(length, UInt(8.W)));
  //for (i <- 0 until length) {
  //  if (i == 0) { when(io.valid) { taps(i) := io.in     } }
  //  else        { when(io.valid) { taps(i) := taps(i-1) } }
  //}
  //val w_fir_res = Wire(Vec(length, UInt(8.W)))
  //for (i <- 0 until length) {
  //  val w_tap_mul = Wire(UInt(8.W))
  //  w_tap_mul := taps(i) * io.consts(i)
  //  if (i == 0) { w_fir_res(i) := 0.U }
  //  else        { w_fir_res(i) := w_tap_mul + w_fir_res(i-1) }
  //}
  //io.out := w_fir_res(length-1)

  //// from bootcamp
  //val taps = Seq(io.in) ++ Seq.fill(io.consts.length - 1)(RegInit(0.U(8.W)))
  //taps.zip(taps.tail).foreach { case (a, b) => when (io.valid) { b := a } }
  //io.out := taps.zip(io.consts).map { case (a, b) => a * b }.reduce(_ + _)

  //// my exercise using List
  //val taps = List(io.in) ::: List.fill(io.consts.length - 1)(RegInit(0.U(8.W))) // declare taps structure
  ////(taps zip taps.tail).foreach{case (a, b) => b := a}   // shift taps
  //taps.zip(taps.tail).foreach{case (a, b) => b := a}   // shift taps
  //io.out := taps.zip(io.consts).map{case (a, b) => a * b}.reduce(_ + _) // mac

  // my exercise using Vector
  val taps = Vector(io.in) ++ Vector.fill(io.consts.length - 1)(RegInit(0.U(8.W))) // declare taps structure
  //(taps zip taps.tail).foreach{case (a, b) => b := a}   // shift taps
  taps.zip(taps.tail).foreach{case (a, b) => b := a}   // shift taps
  io.out := taps.zip(io.consts).map{case (a, b) => a * b}.reduce(_ + _) // mac
}

object fir extends App {
  emitVerilog(new fir(8), Array("--target-dir", "generated"))
}
