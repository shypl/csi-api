package org.shypl.csi.api.client

import kotlinx.coroutines.CoroutineScope
import org.shypl.csi.api.BaseApiAdapter
import org.shypl.csi.api.OuterApi
import org.shypl.csi.core.Connection
import org.shypl.csi.core.client.ConnectFailReason
import org.shypl.csi.core.client.ConnectionAcceptor
import org.shypl.csi.core.client.ConnectionHandler
import org.shypl.tool.io.ByteBuffer
import org.shypl.tool.utils.pool.ObjectPool

abstract class AbstractApiAdapter<IA : InnerApi, OA : OuterApi>(
	private val sluice: ApiSluice<IA, OA>,
	coroutineScope: CoroutineScope,
	byteBuffers: ObjectPool<ByteBuffer>
) : BaseApiAdapter<IA, OA, ConnectionHandler>(coroutineScope, byteBuffers), ConnectionAcceptor {
	
	override fun acceptConnection(connection: Connection): ConnectionHandler {
		val context = createContext(connection)
		val outerApi = createOuterApi(context)
		val innerApi = sluice.connect(outerApi)
		return createConnectionHandler(context, innerApi)
	}
	
	override fun acceptFail(reason: ConnectFailReason) {
		sluice.fail(reason)
	}
}