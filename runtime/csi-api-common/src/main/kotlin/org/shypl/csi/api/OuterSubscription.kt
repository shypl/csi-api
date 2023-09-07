package org.shypl.csi.api

import org.shypl.tool.biser.BiserWriter
import org.shypl.tool.logging.debug
import org.shypl.tool.utils.DummyCloseable
import org.shypl.tool.utils.pool.use
import java.io.Closeable

@Suppress("PropertyName", "LeakingThis")
abstract class OuterSubscription(
	protected val _context: Context,
	private val _service: InnerServiceDelegate<*>,
	private val _name: String,
) {
	@Volatile
	private var _id: Int = 0
	
	@Volatile
	private var _closeable: Closeable = DummyCloseable
	
	@Volatile
	var _ready: Boolean = false
	
	@Volatile
	private var _readyDelegate: OuterSubscriptionReadyDelegate? = OuterSubscriptionReadyDelegate(this)
	
	fun ready() {
		check(!_ready)
		_ready = true
		_readyDelegate!!.ready()
		_readyDelegate = null
	}
	
	internal fun setup(id: Int, closeable: Closeable) {
		_id = id
		_closeable = closeable
	}
	
	internal fun close() {
		_context.logger.debug {
			"<~ ${_service.name}.$_name[~$_id] [close]"
		}
		
		_closeable.close()
		_closeable = DummyCloseable
	}
	
	protected fun delayCall(fn: () -> Unit) {
		val delegate = _readyDelegate
		if (delegate == null) fn() else delegate.delay(fn)
	}
	
	protected fun call(methodId: Int) {
		call(methodId) {}
	}
	
	protected inline fun logCall(method: String, data: StringBuilder.() -> Unit) {
		_context.logger.debug {
			prepareLogSend(method)
				.append('(').apply(data).append(')')
				.toString()
		}
	}
	
	protected fun prepareLogSend(method: String): StringBuilder {
		return StringBuilder().append("-> ").append(_service.name).append('.').append(_name).append("[~").append(_id).append("].").append(method)
	}
	
	protected inline fun call(methodId: Int, data: BiserWriter.() -> Unit) {
		_context.messagePool.writers.use { message ->
			val writer = message.writer
			prepareCallMessage(methodId, writer)
			writer.data()
			_context.connection.sendMessage(message.buffer)
		}
	}
	
	protected fun prepareCallMessage(methodId: Int, message: BiserWriter) {
		message.writeByte(ApiMessageType.SUBSCRIPTION_CALL.code)
		message.writeInt(_id)
		message.writeInt(methodId)
	}
	
}
