package org.shypl.csi.api.server

import kotlinx.coroutines.CoroutineScope
import org.shypl.csi.api.BaseApiAdapter
import org.shypl.csi.api.OuterApi
import org.shypl.csi.core.Connection
import org.shypl.csi.core.server.ConnectionAcceptor
import org.shypl.csi.core.server.ConnectionHandler
import org.shypl.tool.io.ByteBuffer
import org.shypl.tool.utils.pool.ObjectPool

abstract class AbstractApiAdapter<I : Any, IA : InnerApi, OA : OuterApi>(
	private val sluice: ApiSluice<I, IA, OA>,
	coroutineScope: CoroutineScope,
	byteBuffers: ObjectPool<ByteBuffer>
) : BaseApiAdapter<IA, OA, ConnectionHandler>(coroutineScope, byteBuffers), ConnectionAcceptor<I> {
	
	override fun acceptConnection(identity: I, connection: Connection): ConnectionHandler {
		val context = createContext(connection)
		val outerApi = createOuterApi(context)
		val innerApi = sluice.connect(identity, outerApi)
		return createConnectionHandler(context, innerApi)
	}
}