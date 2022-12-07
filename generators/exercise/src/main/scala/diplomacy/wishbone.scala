//package diplomacy
//
//import chipsalliance.rocketchip.config.{Config, Parameters}
//import chisel3._
//import chisel3.internal.sourceinfo.SourceInfo
//import chisel3.stage.ChiselStage
//import chisel3.util.random.FibonacciLFSR
//import freechips.rocketchip.diplomacy.{SimpleNodeImp, RenderedEdge, ValName, SourceNode,
//                                       NexusNode, SinkNode, LazyModule, LazyModuleImp}
//
//case class WBBundleParameters (addrBits: Int, dataBits: Int)
//
//class WBBundle(params: WBBundleParameters) extends Bundle {
//    val address = Output(UInt(params.addrBits.W))
//    val dataOut = Output(UInt(params.addrBits.W))
//    val dataIn  = Input (UInt(params.dataBits.W))
//}
//
//case class WBMasterParameters(name: String) 
//case class WBSlaveParameters (name: String, address: Seq[AddressSet]) 
//
//case class WBMasterPortParameters(masters: Seq[WBMasterParameters]) 
//case class WBSlavePortParameters (slaves: Seq[WBSlaveParameters]) 
//
//case class WBEdgeParameters(master: WBMasterPortParameters, slave : WBSlavePortParameters, p: Parameters, sourceInfo: SourceInfo) {
//    val bundle = WBBundleParameters( /* black magic to calculate the bundle parameters */ )
//}
//
//
//object WBImp extends SimpleNodeImp {
//  // Collect downstream and upstream parameters into an edge.
//  def edge(pd: WBMasterPortParameters, pu: WBSlavePortParameters, p: Parameters, sourceInfo: SourceInfo) = 
//    WBEdgeParameters(pd, pu, p, sourceInfo)
//
//  // generate hardware bundle.
//  def bundle(e: WBEdgeParameters) = WBBundle(e.bundle)
//
//  // Tell this node that it has an additional outgoing connection
//  override def mixO(pd: WBMasterPortParameters, node: OutwardNode) = 
//    pd.copy(masters = pd.masters.map { c => c.copy(nodePath = node +: c.nodePath) })
//
//  // Tell this node that it has an additional incoming connection
//  override def mixI(pu: WBSlavePortParameters, node: InwardNode) = 
//    pu.copy(slaves = pu.slaves.map { m => m.copy(nodePath = node +: m.nodePath) })
//}
//
//
//// define master node
//case class WBMasterNode(portParams: Seq[WBMasterPortParameters])(implicit valName: ValName) 
//  extends SourceNode(WBImp)(portParams)
//
//// define slave node
//case class WBSlaveNode(portParams: Seq[WBSlavePortParameters])(implicit valName: ValName)  
//  extends SinkNode(WBImp)(portParams)
//
//// define nexus node, which will be used in CrossBar
//case class WBNexusNode(masterFn: Seq[WBMasterPortParameters] => WBMasterPortParameters,
//                       slaveFn: Seq[WBSlavePortParameters] => WBSlavePortParameters)(implicit valName: ValName) 
//  extends NexusNode(WBImp)(masterFn, slaveFn)