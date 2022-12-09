//package diplomacy.lazytest
//
//import chipsalliance.rocketchip.config.{Config, Parameters}
//import chisel3._
//import chisel3.internal.sourceinfo.SourceInfo
//import chisel3.stage.ChiselStage
//import chisel3.util.random.FibonacciLFSR
//import freechips.rocketchip.diplomacy.{SimpleNodeImp, RenderedEdge, ValName, SourceNode,
//                                       NexusNode, SinkNode, LazyModule, LazyModuleImp}
//import freechips.rocketchip.amba.axi4._
//
//case class MasterNodeParams(width: Int)
//case class SlaveNodeParams(width: Int)
//case class EdgeParams(width: Int)
//
//object TestNodeImp extends SimpleNodeImp[MasterNodeParams, SlaveNodeParams, EdgeParams, UInt] {
//    def edge(pd: MasterNodeParams, pu: SlaveNodeParams, p: Parameters, sourceInfo: SourceInfo) = {
//        if (pd.width < pu.width) EdgeParams(pd.width) else EdgeParams(pu.width)
//    }
//
//    def bundle(e: EdgeParams) = UInt(e.width.W)
//    def render(e: EdgeParams) = RenderedEdge("blue", s"hello")
//}
//
//class DMasterSourceNode(widths: Seq[MasterNodeParams])(implicit valName: ValName) extends SourceNode(TestNodeImp)(widths)
//class DMasterSinkNode(widths: Seq[SlaveNodeParams])(implicit valName: ValName) extends SinkNode(TestNodeImp)(widths)
//
//class DMaster()(implicit p: Parameters) extends LazyModule {
//    val
//    lazy val module = new DMasterModule(this)
//    override lazy val desiredName = "DMaster"
//}
//
//class DMasterModule(outer: DMaster) extends LazyModuleImp(outer) {
//    val io = IO(new Bundle{
//        val enable = Input(Bool())
//        val x_pos = Output(UInt(4.W))
//        val y_pos = Output(UInt(4.W))
//    })
//
//    val x_pos = RegInit(0.U(4.W))
//    val y_pos = RegInit(0.U(4.W))
//
//    io.x_pos := x_pos
//    io.y_pos := y_pos
//    when(io.enable) {
//        x_pos := x_pos + 1.U
//        y_pos := y_pos - 1.U
//    }
//}
//
//class DSlave()(implicit p: Parameters) extends LazyModule {
//    lazy val module = new DSlaveModule(this)
//    override lazy val desiredName = "DSlave"
//}
//
//class DSlaveModule(outer: DSlave) extends LazyModuleImp(outer) {
//
//}
//
//class TOP1()(implicit p: Parameters) extends LazyModule {
//    val dut = LazyModule(new DMaster())
//    lazy val module = new TOP1_MODULE(this)
//    override lazy val desiredName = "TOP"
//}
//
//class TOP1_MODULE(outer: TOP1) extends LazyModuleImp(outer) {
//    val io = IO(new Bundle{
//        val addr = Output(UInt(8.W))
//    })
//
//    val dut = outer.dut.module
//    val addr = RegInit(0.U(8.W))
//
//    io.addr := addr
//    dut.io.enable := true.B
//    when(reset.asBool) {
//        dut.io.enable := false.B
//    }
//    when(!reset.asBool) {
//        addr := dut.io.x_pos * dut.io.y_pos
//    }
//}
//
//object GenDMaster extends App {
//    println("Here, GenDMaster")
//
//    new chisel3.stage.ChiselStage().emitSystemVerilog(
//        LazyModule(new DMaster()(Parameters.empty)).module,
//        Array("--target-dir", "generated")
//    )
//}
//
//object GenTOP1 extends App {
//    println("Here, GenTOP1")
//
//    new chisel3.stage.ChiselStage().emitSystemVerilog(
//        LazyModule(new TOP1()(Parameters.empty)).module,
//        Array("--target-dir", "generated")
//    )
//}