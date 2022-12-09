package diplomacy.xp

import freechips.rocketchip.config.{Config, Parameters}
import chisel3._
import chisel3.util._
import chisel3.internal.sourceinfo.SourceInfo
import chisel3.stage.ChiselStage
import chisel3.util.random.FibonacciLFSR
import freechips.rocketchip.diplomacy.{NodeImp, SimpleNodeImp, RenderedEdge, ValName, SourceNode,
                                       NexusNode, SinkNode, LazyModule, LazyModuleImp}
import freechips.rocketchip.util.GenericParameterizedBundle

abstract class XPBundleBase(params: XPBundleParams) extends GenericParameterizedBundle(params)

class XPBundleReq(params: XPBundleParams) extends XPBundleBase(params) {
    val addr = UInt(params.addrWidth.W)
    val data = UInt(params.dataWidth.W)
    val id   = UInt(params.idWidth.W)
}

class XPBundleResp(params: XPBundleParams) extends XPBundleBase(params) {
    val resp = UInt(2.W)
    val data = UInt(params.dataWidth.W)
    val id   = UInt(params.idWidth.W)
}

class XPBundle(params: XPBundleParams) extends XPBundleBase(params) {
    val req = Irrevocable(new XPBundleReq(params))
    val resp = Flipped(Irrevocable(new XPBundleResp(params)))
}

object XPBundle {
    def apply(params: XPBundleParams) = new XPBundle(params)
}