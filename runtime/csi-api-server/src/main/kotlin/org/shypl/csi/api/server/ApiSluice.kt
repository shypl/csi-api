package org.shypl.csi.api.server

import org.shypl.csi.api.OuterApi

interface ApiSluice<I : Any, IA : InnerApi, OA : OuterApi> {
	fun connect(identity: I, client: OA): IA
}