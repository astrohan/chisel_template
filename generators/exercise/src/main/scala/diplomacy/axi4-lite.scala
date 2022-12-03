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
}

// data channel
class A4LIntfData(params: A4LBundleParams)  extends A4LBundleBase(params) {
    val data = UInt(params.dataWidth.W)
    val strb = UInt((params.dataWidth/8).W)
    val id   = UInt(params.idWidth.W)
}

// bresp channel
class A4LIntfResp(params: A4LBundleParams)  extends A4LBundleBase(params) {
    val resp   = UInt(2.W)
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
    val aw = Decoupled(new A4LIntfAddr(params))
    val ar = Decoupled(new A4LIntfAddr(params))
    val w  = Decoupled(new A4LIntfData(params))
    val r  = Decoupled(new A4LIntfData(params))
    val br = Flipped(Decoupled(new A4LIntfResp(params)))
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


class A4LMaster(numPorts: Int, addrWidth: Int, dataWidth: Int, idWidth: Int)(implicit p: Parameters) extends LazyModule {
    val node = new A4LMasterNode(Seq.fill(numPorts)(A4LMasterParams(addrWidth, dataWidth, idWidth)))

    lazy val module = new LazyModuleImp(this) {
    }

    override lazy val desireName = "DMA_AXI4Lite"
}



