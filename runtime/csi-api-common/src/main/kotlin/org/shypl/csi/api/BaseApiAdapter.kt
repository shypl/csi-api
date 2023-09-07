package org.shypl.csi.api

import kotlinx.coroutines.CoroutineScope
import org.shypl.csi.core.BaseConnectionHandler
import org.shypl.csi.core.Connection
import org.shypl.tool.io.ByteBuffer
import org.shypl.tool.logging.Logging
import org.shypl.tool.logging.wrap
import org.shypl.tool.utils.pool.ObjectPool

abstract class BaseApiAdapter<IA : BaseInnerApi, OA : OuterApi, CH : BaseConnectionHandler>(
	protected val coroutineScope: CoroutineScope,
	byteBuffers: ObjectPool<ByteBuffer>
) {
	protected val messagePool: ApiMessagePool = ApiMessagePoolImp(byteBuffers)
	
	protected abstract fun getLoggerName(): String
	
	protected abstract fun createOuterApi(context: Context): OA
	
	protected abstract fun createConnectionHandler(context: Context, api: IA): CH
	
	protected abstract fun provideCallbacksRegistry(): CallbacksRegistry
	
	protected fun createContext(connection: Connection): Context {
		return Context(
			Logging.getLogger(getLoggerName()).wrap("[${connection.loggingName}] "),
			coroutineScope,
			messagePool,
			connection,
			provideCallbacksRegistry()
		)
	}
}


