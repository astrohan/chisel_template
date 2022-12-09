package diplomacy.xp

import chisel3.internal.sourceinfo.SourceInfo
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import scala.math.max

case class XPMasterNodeParams(addrWidth: Int, dataWidth: Int, idWidth: Int)
case class XPSlaveNodeParams (addrWidth: Int, dataWidth: Int, idWidth: Int)

case class XPBundleParams(addrWidth: Int, dataWidth: Int, idWidth: Int) {
    require(addrWidth > 0)
    require(dataWidth > 7)
    require(idWidth   > 0)
} 
object XPBundleParams {
    def apply(master: XPMasterNodeParams, slave: XPSlaveNodeParams) = new XPBundleParams (
        addrWidth = master.addrWidth,
        dataWidth = master.dataWidth,
        idWidth   = max(master.idWidth, slave.idWidth)
    )
}

case class XPEdgeParams (master: XPMasterNodeParams, slave: XPSlaveNodeParams) {
    val bundle = XPBundleParams(master, slave)
}
