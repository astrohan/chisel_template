package diplomacy

import chipsalliance.rocketchip.config.{Config, Parameters}
import chisel3._
import chisel3.util._
import chisel3.internal.sourceinfo.SourceInfo
import chisel3.stage.ChiselStage
import chisel3.util.random.FibonacciLFSR
import freechips.rocketchip.diplomacy.{NodeImp, SimpleNodeImp, RenderedEdge, ValName, SourceNode,
                                       NexusNode, SinkNode, LazyModule, LazyModuleImp}
import freechips.rocketchip.util.GenericParameterizedBundle
import scala.math.max

// ----------------------------------------------
// AXI4-Lite bundle
// ----------------------------------------------
// address channel
class A4LIntfAddr(params: A4LBundleParams)  extends A4LBundleBase(params) {
    val addr = UInt(params.addrWidth.W)
    val prot = UInt(3.W)
    val id   = UInt(params.idWidth.W)
}

// data channel
class A4LIntfData(params: A4LBundleParams)  extends A4LBundleBase(params) {
    val data = UInt(params.dataWidth.W)
    val strb = UInt((params.dataWidth/8).W)
    val id   = UInt(params.idWidth.W)
}

// bresp channel
class A4LIntfResp(params: A4LBundleParams)  extends A4LBundleBase(params) {
    val resp = UInt(2.W)
    val id   = UInt(params.idWidth.W)
}
// ----------------------------------------------
// ~AXI4-Lite bundle
// ----------------------------------------------


// ----------------------------------------------
// AXI4-Lite diplomacy parameters & bundle
// ----------------------------------------------
case class A4LMasterParams(addrWidth: Int, dataWidth: Int, idWidth: Int)
case class A4LSlaveParams (addrWidth: Int, dataWidth: Int, idWidth: Int)
case class A4LBundleParams(addrWidth: Int, dataWidth: Int, idWidth: Int) {
    require(addrWidth > 0)
    require(dataWidth > 8)
    require(idWidth   > 0)
} 
object A4LBundleParams {
    def apply(master: A4LMasterParams, slave: A4LSlaveParams) = new A4LBundleParams (
        addrWidth = master.addrWidth,
        dataWidth = master.dataWidth,
        idWidth   = max(master.idWidth, slave.idWidth)
    )
}

abstract class A4LBundleBase(params: A4LBundleParams) extends GenericParameterizedBundle(params)
case class A4LBundle(params: A4LBundleParams) extends A4LBundleBase(params) {
    // this is for master interface. use Flipped for slave interface
    val aw = Irrevocable(new A4LIntfAddr(params))
    val ar = Irrevocable(new A4LIntfAddr(params))
    val w  = Irrevocable(new A4LIntfData(params))
    val r  = Flipped(Irrevocable(new A4LIntfData(params)))
    val br = Flipped(Irrevocable(new A4LIntfResp(params)))
}

case class A4LEdgeOParams (master: A4LMasterParams, slave: A4LSlaveParams) {
    val bundle = A4LBundleParams(master, slave)
}
case class A4LEdgeIParams (master: A4LMasterParams, slave: A4LSlaveParams) {
    val bundle = A4LBundleParams(master, slave)
}
// ----------------------------------------------
// ~AXI4-Lite diplomacy parameters & bundle
// ----------------------------------------------



// ----------------------------------------------
// AXI4-Lite node 
// ----------------------------------------------
object A4LNodeImp extends NodeImp[A4LMasterParams, A4LSlaveParams, A4LEdgeOParams, A4LEdgeIParams, A4LBundle] {
  def edgeO(pd: A4LMasterParams, pu: A4LSlaveParams, p: Parameters, sourceInfo: SourceInfo) = {
    require(pd.addrWidth == pu.addrWidth, "Both of address widths must be equivalent")
    require(pd.dataWidth == pu.dataWidth, "Both of data widths must be equivalent")
    A4LEdgeOParams(pd, pu)
  }

  def edgeI(pd: A4LMasterParams, pu: A4LSlaveParams, p: Parameters, sourceInfo: SourceInfo) = {
    require(pd.addrWidth == pu.addrWidth, "Both of address widths must be equivalent")
    require(pd.dataWidth == pu.dataWidth, "Both of data widths must be equivalent")
    A4LEdgeIParams(pd, pu)
  }

  def bundleO(eo: A4LEdgeOParams) = A4LBundle(eo.bundle)
  def bundleI(ei: A4LEdgeIParams) = Flipped(A4LBundle(ei.bundle))
}

class A4LMasterNode(master: Seq[A4LMasterParams])(implicit valName: ValName) extends SourceNode(A4LNodeImp)(master)
class A4LSlaveNode (slave : Seq[A4LSlaveParams ])(implicit valName: ValName) extends SinkNode  (A4LNodeImp)(slave )
class A4LNexusNode (
    master: Seq[A4LMasterParams] => A4LMasterParams, 
    slave : Seq[A4LSlaveParams ] => A4LSlaveParams)
    (implicit valName: ValName) extends NexusNode (A4LNodeImp)(master, slave )
// ----------------------------------------------
// ~AXI4-Lite node 
// ----------------------------------------------


class A4LMaster(numPorts: Int, addrWidth: Int, dataWidth: Int, idWidth: Int, moCntWidth: Int)(implicit p: Parameters) extends LazyModule {
    val node = new A4LMasterNode(Seq.fill(numPorts)(A4LMasterParams(addrWidth, dataWidth, idWidth)))

    lazy val module = new LazyModuleImp(this) {
        val io = IO(new Bundle {
            val dmaEnable = Input(Bool())    // AXI4Lite traffic is generated when dmaEnable is true
            val maxMO     = Input(UInt(moCntWidth.W)) // maximum MO = maxMO + 1
        })

        val awvalid = Seq.fill(numPorts)(Bool())
        val arvalid = Seq.fill(numPorts)(Bool())
        val wvalid  = Seq.fill(numPorts)(Bool())
        val rready  = Seq.fill(numPorts)(true.B)
        val bready  = Seq.fill(numPorts)(true.B)

        val writeDataCount = Seq.fill(numPorts)(moCntWidth.W)
        val writeRespCount = Seq.fill(numPorts)(moCntWidth.W)
        val readCount  = Seq.fill(numPorts)(moCntWidth.W)

        //(node.in zip node.out) foreach {
        //    case ((in, edgeIn), (out, edgeOut)) => out.aw.valid := awvalid
        //}
        for (i <- 0 until numPorts) {
            node.out(i)._1.aw.valid := awvalid(i)
            mode.out(i)._1.aw.addr  := 0.U // TODO
            mode.out(i)._1.aw.prot  := 0.U // TODO
            mode.out(i)._1.aw.it    := 0.U // TODO

            node.out(i)._1.ar.valid := arvalid(i)
            mode.out(i)._1.ar.addr  := 0.U // TODO
            mode.out(i)._1.ar.prot  := 0.U // TODO
            mode.out(i)._1.ar.it    := 0.U // TODO

            node.out(i)._1.w.valid  := wvalid(i)
            node.out(i)._1.w.data   := 0.U // TODO
            node.out(i)._1.w.strb   := UIntToOH(dataWidth) - 1

            // write data MO counter
            when(reset.asBool) {
                writeDataCount(i) := 0.U
            } .elsewhen(node.out(i)._1.aw.fire() && !node.out(i)._1.w.fire()) {
                writeDataCount(i) := writeDataCount(i) + 1.U
            } .elsewhen(!node.out(i)._1.aw.fire() && node.out(i)._1.w.fire()) {
                writeDataCount(i) := writeDataCount(i) - 1.U
            }

            // write resp MO counter
            when(reset.asBool) {
                writeRespCount(i) := 0.U
            } .elsewhen(node.out(i)._1.aw.fire() && !node.out(i)._1.br.fire()) {
                writeRespCount(i) := writeRespCount(i) + 1.U
            } .elsewhen(!node.out(i)._1.aw.fire() && node.out(i)._1.br.fire()) {
                writeRespCount(i) := writeRespCount(i) - 1.U
            }

            // read MO counter
            when(reset.asBool) {
                readCount(i) := 0.U
            } .elsewhen(node.out(i)._1.ar.fire() && !node.out(i)._1.r.fire()) {
                readCount(i) := readCount(i) + 1.U
            } .elsewhen(!node.out(i)._1.ar.fire() && node.out(i)._1.r.fire()) {
                readCount(i) := readCount(i) - 1.U
            }

            // awvalid control
            when(reset.asBool) {
                awvalid(i) := false.B
            } .elsewhen(!io.dmaEnable && awvalid(i)) { // disabled DMA
                awvalid(i) := false.B
            } .elsewhen(io.maxMO === writeDataCount(i)) { // reached max MO
                awvalid(i) := false.B
            } .elsewhen(io.dmaEnable && !awvalid(i)) { // generate aw-transaction
                awvalid(i) := true.B
            }

            // wvalid control
            when(reset.asBool) {
                wvalid(i) := false.B
            } .elsewhen(!io.dmaEnable && wvalid(i)) { // disabled DMA
                wvalid(i) := false.B
            } .elsewhen(io.dmaEnable && writeDataCount.asBool) { // generate w-transaction
                wvalid(i) := true.B
            }

            // arvalid control
            when(reset.asBool) {
                arvalid(i) := false.B
            } .elsewhen(!io.dmaEnable && arvalid(i)) { // disabled DMA
                arvalid(i) := false.B
            } .elsewhen(io.dmaEnable && readCount.asBool) { // generate w-transaction
                arvalid(i) := true.B
            }
        }
    }

    override lazy val desireName = "DMA_AXI4Lite"
}


class A4LSlave(moCntWidth: Int)(implicit p: Parameters) extends LazyModule {
    val node = new A4LSalveNode()

    lazy val module = new LazyModuleImp(this) {
        val writeDataCount = UInt(moCntWidth.W)
        
        when(reset.asBool) {
            writeDataCount := 0.U
        } .elsewhen(node.in.w.fire() && !node.out.br.fire()) {
            writeDataCount := writeDataCount + 1.U
        } .elsewhen(!node.in.w.fire() && node.out.br.fire()) {
            writeDataCount := writeDataCount - 1.U
        }
    }

    override lazy val desireName = "MEM_AXI4Lite"
}


class A4LTestHarness()(implicit p: Parameters) extends LazyModule {
    val master = LazyModule(new A4LMaster(numPorts = 6, addrWidth = 32, dataWidth = 64, idWidth = 4, moCntWidth = 4))
    val slave  = LazyModule(new A4LSlave (moCntWidth = 4))

    slave.node = := master.node

    override lazy val desireName = "AXI4LiteTestHarness"
}