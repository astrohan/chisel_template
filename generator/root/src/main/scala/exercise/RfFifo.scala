package exercise

import chisel3._
import chisel3.util._

class dataSet(val dataWidth:Int, val idWidth:Int) extends Bundle {
  val data = UInt(dataWidth.W)
  val id   = UInt(idWidth.W)
}

class RfFifo(val dataWidth:Int, val idWidth:Int, val fifoDepth:Int) extends Module {
  val io = IO(new Bundle {
    val i_pvalid        = Input(Bool())
    val i_pdata         = Input(new dataSet(dataWidth, idWidth))
    val o_pstall        = Output(Bool())
    val o_cvalid        = Output(Bool())
    val o_cdata         = Output(new dataSet(dataWidth, idWidth))
    val i_cstall        = Input(Bool())
  })

  //val rf = SyncReadMem(fifoDepth, new dataSet(dataWidth, idWidth))
  val rf = Mem(fifoDepth, new dataSet(dataWidth, idWidth))
  //val rf = Reg(Vec(fifoDepth, new dataSet(dataWidth, idWidth)))
  //val initRfValue = Wire(new dataSet(dataWidth, idWidth))
  //initRfValue.data := 0.U
  //initRfValue.id := 0.U
  //val rf = RegInit(VecInit(Vector.fill(fifoDepth)(initRfValue)))

  val wptr = Reg(UInt(log2Ceil(fifoDepth).W))
  val rptr = Reg(UInt(log2Ceil(fifoDepth).W))

  io.o_pstall     := io.o_cvalid && io.i_cstall
  io.o_cvalid     := wptr =/= rptr
  //io.o_cdata.data := rf(rptr).data
  //io.o_cdata.id   := rf(rptr).id
  io.o_cdata <> rf(rptr)

  when(reset.asBool) { // interpret reset as bool
    wptr := 0.U
  } .elsewhen(io.i_pvalid && io.o_pstall) {
    //rf(wptr).data := io.i_pdata.data
    //rf(wptr).id := io.i_pdata.id
    rf(wptr) <> io.i_pdata
    wptr := wptr + 1.U
  }

  when(reset.asBool) {
    rptr := 0.U
  } .elsewhen(io.o_cvalid && io.i_cstall) {
    rptr := rptr + 1.U
  }
}

object RfFifo extends App {
  //emitVerilog(new RfFifo(dataWidth=4, idWidth=1, fifoDepth=8))
  emitVerilog(new RfFifo(dataWidth=4, idWidth=1, fifoDepth=8), Array("--target-dir", "generated"))
  //val s = getVerilogString(new RfFifo(dataWidth=4, idWidth=1, fifoDepth=8))
  //println(s)
}
