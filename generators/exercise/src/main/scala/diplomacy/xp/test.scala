package diplomacy.xp

import freechips.rocketchip.config.{Config, Parameters}
import chisel3._
import chisel3.util._
import chisel3.internal.sourceinfo.SourceInfo
import chisel3.stage.ChiselStage
import chisel3.util.random.FibonacciLFSR
import freechips.rocketchip.diplomacy.{NodeImp, SimpleNodeImp, RenderedEdge, ValName, SourceNode,
                                       NexusNode, SinkNode, LazyModule, LazyModuleImp}
import scala.math.max
import freechips.rocketchip.tile.HasNonDiplomaticTileParameters
import freechips.rocketchip.util.GenericParameterizedBundle

class XPMaster(addrWidth: Int, dataWidth: Int, idWidth: Int)(implicit p: Parameters) extends LazyModule {
    val node = new XPMasterNode(Seq.fill(2)(XPMasterNodeParams(addrWidth, dataWidth, idWidth)))

    lazy val module = new XPMasterModule(this)(addrWidth, dataWidth, idWidth)
    override lazy val desiredName = "XPMaster"
}

class XPMasterModule(outer: XPMaster)(addrWidth: Int, dataWidth: Int, idWidth: Int) extends LazyModuleImp(outer) {
    outer.node.out.foreach { case (intf, _) => {
        val req = intf.req
        val resp = intf.resp

        val addr = RegInit(0.U(addrWidth.W))
        val data = Reg(UInt(dataWidth.W))
        val id   = RegInit(0.U(idWidth.W))

        req.valid  := FibonacciLFSR.maxPeriod(16)
        req.bits.addr := addr
        req.bits.data := data
        req.bits.id := id

        resp.ready := true.B

        when(req.fire) {
            addr := addr + 4.U
            data := data + 10.U
            id   := id + 1.U
        }
    }}
}

class XPSlave(addrWidth: Int, dataWidth: Int, idWidth: Int)(implicit p: Parameters) extends LazyModule {
    val node = new XPSlaveNode(Seq.fill(2)(XPSlaveNodeParams(addrWidth, dataWidth, idWidth)))

    lazy val module = new XPSlaveModule(this)

    override lazy val desiredName = "XPSlave"
}

class XPSlaveModule(outer: XPSlave) extends LazyModuleImp(outer) {
    outer.node.in.foreach{ case(intf, _) => {
        val req = intf.req
        val resp = intf.resp

        req.ready       := true.B
        resp.valid      := req.fire
        resp.bits.id    := req.bits.id
        resp.bits.data  := req.bits.data
        resp.bits.resp  := 0.U
    } }
}

class XPNexus(implicit p: Parameters) extends LazyModule {
    val node = new XPNexusNode(
        {case master: Seq[XPMasterNodeParams] => master.head},
        {case slave : Seq[XPSlaveNodeParams] => slave.head}
    )
    lazy val module = new XPNexusModule(this)
    override lazy val desiredName = "XPNexus"
}

class XPNexusModule(outer: XPNexus) extends LazyModuleImp(outer) {
    val node = outer.node
    node.out.head._1 := node.in.head._1
}

class XPTestHarness()(implicit p: Parameters) extends LazyModule {
    val xpmaster = LazyModule(new XPMaster(32,64,4))
    val xpnexus  = LazyModule(new XPNexus ())
    val xpslave  = LazyModule(new XPSlave (32,64,4))

    xpnexus.node := xpmaster.node
    xpslave.node := xpmaster.node
    xpslave.node := xpnexus.node

    lazy val module = new LazyModuleImp(this)

    override lazy val desiredName = "XPTestHarness"
}

object GenXPTestHarness extends App {
  println("\n") ; println("\n") ; println("\n") ; println("\n")
  println("\n") ; println("\n") ; println("\n") ; println("\n")
  println("\n") ; println("\n") ; println("\n") ; println("\n")
  println("Here, GenXPTestHarness")
  new chisel3.stage.ChiselStage().emitSystemVerilog(
    LazyModule(new XPTestHarness()(Parameters.empty)).module,
    Array("--target-dir", "generated")
  ) // generate systemverilog rtl
}