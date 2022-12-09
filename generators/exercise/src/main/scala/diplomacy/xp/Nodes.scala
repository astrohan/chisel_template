package diplomacy.xp

import freechips.rocketchip.config.{Config, Parameters}
import chisel3._
import chisel3.util._
import chisel3.internal.sourceinfo.SourceInfo
import chisel3.stage.ChiselStage
import chisel3.util.random.FibonacciLFSR
import freechips.rocketchip.diplomacy.{NodeImp, SimpleNodeImp, RenderedEdge, ValName, SourceNode,
                                       NexusNode, SinkNode, LazyModule, LazyModuleImp}

object XPNodeImp extends SimpleNodeImp[XPMasterNodeParams, XPSlaveNodeParams, XPEdgeParams, XPBundle] {
  def edge(pd: XPMasterNodeParams, pu: XPSlaveNodeParams, p: Parameters, sourceInfo: SourceInfo) = {
    require(pd.addrWidth == pu.addrWidth, "Both of address widths must be equivalent")
    require(pd.dataWidth == pu.dataWidth, "Both of data widths must be equivalent")
    XPEdgeParams(pd, pu)
  }

  def bundle(e: XPEdgeParams) = XPBundle(e.bundle)
  def render(e: XPEdgeParams) = RenderedEdge("blue", s"hello") 
}

class XPMasterNode(master: Seq[XPMasterNodeParams])(implicit valName: ValName) extends SourceNode(XPNodeImp)(master)
class XPSlaveNode (slave : Seq[XPSlaveNodeParams ])(implicit valName: ValName) extends SinkNode  (XPNodeImp)(slave )
class XPNexusNode (
    master: Seq[XPMasterNodeParams] => XPMasterNodeParams, 
    slave : Seq[XPSlaveNodeParams ] => XPSlaveNodeParams)
    (implicit valName: ValName) extends NexusNode(XPNodeImp)(master, slave)
