package org.shypl.csi.api.client

import org.shypl.csi.api.BaseApiConnection
import org.shypl.csi.api.Context
import org.shypl.csi.core.client.ConnectionHandler
import org.shypl.csi.core.client.ConnectionRecoveryHandler

abstract class AbstractApiConnection<IA : InnerApi>(
	context: Context,
	api: IA
) : BaseApiConnection<IA>(context, api), ConnectionHandler {
	override fun handleConnectionCloseTimeout(seconds: Int) {
		api.handleConnectionCloseTimeout(seconds)
	}
	
	override fun handleConnectionLost(): ConnectionRecoveryHandler {
		return api.handleConnectionLost()
	}
}