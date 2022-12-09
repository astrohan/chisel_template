package diplomacy.lazytest

import chipsalliance.rocketchip.config.{Config, Parameters}
import chisel3._
import chisel3.internal.sourceinfo.SourceInfo
import chisel3.stage.ChiselStage
import chisel3.util.random.FibonacciLFSR
import freechips.rocketchip.diplomacy.{SimpleNodeImp, RenderedEdge, ValName, SourceNode,
                                       NexusNode, SinkNode, LazyModule, LazyModuleImp}
import freechips.rocketchip.amba.axi4._

class DUT()(implicit p: Parameters) extends LazyModule {
    lazy val module = new DUT_MODULE(this)
    override lazy val desiredName = "DUT"
}

class DUT_MODULE(outer: DUT) extends LazyModuleImp(outer) {
    val io = IO(new Bundle{
        val enable = Input(Bool())
        val x_pos = Output(UInt(4.W))
        val y_pos = Output(UInt(4.W))
    })

    val x_pos = RegInit(0.U(4.W))
    val y_pos = RegInit(0.U(4.W))

    io.x_pos := x_pos
    io.y_pos := y_pos
    when(io.enable) {
        x_pos := x_pos + 1.U
        y_pos := y_pos - 1.U
    }
}

class TOP()(implicit p: Parameters) extends LazyModule {
    val dut = LazyModule(new DUT())
    lazy val module = new TOP_MODULE(this)
    override lazy val desiredName = "TOP"
}

class TOP_MODULE(outer: TOP) extends LazyModuleImp(outer) {
    //val io = IO(new Bundle{
    //    val addr = Output(UInt(8.W))
    //})

    val dut = outer.dut.module
    val addr = RegInit(0.U(8.W))

    //io.addr := addr
    dut.io.enable := true.B
    when(reset.asBool) {
        dut.io.enable := false.B
    }
    when(!reset.asBool) {
        addr := dut.io.x_pos * dut.io.y_pos
    }
}

object GenDUT extends App {
    println("Here, GenDUT")

    new chisel3.stage.ChiselStage().emitSystemVerilog(
        LazyModule(new DUT()(Parameters.empty)).module,
        Array("--target-dir", "generated")
    )
}

object GenTOP extends App {
    println("Here, GenTOP")

    new chisel3.stage.ChiselStage().emitSystemVerilog(
        LazyModule(new TOP()(Parameters.empty)).module,
        Array("--target-dir", "generated")
    )
}