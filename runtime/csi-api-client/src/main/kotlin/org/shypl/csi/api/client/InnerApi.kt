package org.shypl.csi.api.client

import org.shypl.csi.api.BaseInnerApi
import org.shypl.csi.core.client.ConnectionRecoveryHandler

interface InnerApi : BaseInnerApi {
	fun handleConnectionCloseTimeout(seconds: Int)
	
	fun handleConnectionLost(): ConnectionRecoveryHandler
}