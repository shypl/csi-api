package org.shypl.csi.api.client

import org.shypl.csi.api.OuterApi
import org.shypl.csi.core.client.ConnectFailReason

interface ApiSluice<IA : InnerApi, OA : OuterApi> {
	fun connect(server: OA): IA
	
	fun fail(reason: ConnectFailReason)
}