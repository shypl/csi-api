package org.shypl.csi.api

import org.shypl.tool.biser.BiserReader
import org.shypl.tool.biser.BiserWriter
import org.shypl.tool.io.InputByteBuffer
import org.shypl.tool.logging.debug
import org.shypl.tool.utils.pool.use

@Suppress("FunctionName", "PropertyName")
abstract class OuterService(
	protected val _context: Context,
	private val _instance: Boolean,
	private val _id: Int,
	val _name: String
) {
	
	@Volatile
	private var _closed = false
	private val _subscriptions = InnerSubscriptionHolder()
	
	protected fun _checkClosed() {
		if (_closed) throw IllegalStateException("Service is closed")
	}
	
	protected fun _callMethod(methodId: Int, callback: Int) {
		_callMethod(methodId, callback) {}
	}
	
	protected fun _callMethod(methodId: Int) {
		_callMethod(methodId) {}
	}
	
	protected inline fun _callMethod(methodId: Int, callback: Int, data: BiserWriter.() -> Unit) {
		_callMethod(methodId) {
			writeInt(callback)
			data()
		}
	}
	
	protected inline fun _callMethod(methodId: Int, data: BiserWriter.() -> Unit) {
		_context.messagePool.writers.use { message ->
			val writer = message.writer
			_prepareMethodCallMessage(methodId, writer)
			writer.data()
			_sendMessage(message.buffer)
		}
	}
	
	protected fun _registerCallback(callback: BiserReader.(Int) -> Unit): Int {
		return _context.callbacks.put(callback)
	}
	
	fun _registerSubscription(subscription: InnerSubscription) {
		_context.innerSubscriptions.add(subscription)
		_subscriptions.add(subscription)
		if (_closed) {
			subscription.close()
		}
	}
	
	
	protected fun _prepareMethodCallMessage(methodId: Int, message: BiserWriter) {
		message.writeByte(if (_instance) ApiMessageType.INSTANCE_METHOD_CALL.code else ApiMessageType.METHOD_CALL.code)
		message.writeInt(_id)
		message.writeInt(methodId)
	}
	
	protected fun _sendMessage(message: InputByteBuffer) {
		_context.connection.sendMessage(message)
	}
	
	
	protected inline fun _logMethodResponse(method: String, callback: Int, data: StringBuilder.() -> Unit) {
		_context.logger.debug {
			_prepareLogCallback(method, callback)
				.append(": ").apply(data)
				.toString()
		}
	}
	
	protected inline fun _logMethodCall(method: String, data: StringBuilder.() -> Unit) {
		_context.logger.debug {
			_prepareLogSend(method)
				.append('(').apply(data).append(')')
				.toString()
		}
	}
	
	protected inline fun _logMethodCall(method: String, callback: Int, data: StringBuilder.() -> Unit) {
		_context.logger.debug {
			_prepareLogSend(method, callback)
				.append('(').apply(data).append(')')
				.toString()
		}
	}
	
	protected fun _logInstanceOpen(method: String, callback: Int, serviceId: Int) {
		_context.logger.debug {
			_prepareLogCallback(method, callback)
				.append(": +").append(serviceId)
				.toString()
		}
	}
	
	protected fun _logInstanceOpen(method: String, callback: Int, serviceId: Int, subscriptionId: Int) {
		_context.logger.debug {
			_prepareLogCallback(method, callback)
				.append(": +").append(serviceId).append('~').append(subscriptionId)
				.toString()
		}
	}
	
	protected fun _logSubscriptionBegin(method: String, callback: Int, subscriptionId: Int) {
		_context.logger.debug {
			_prepareLogCallback(method, callback)
				.append(": ~").append(subscriptionId)
				.toString()
		}
	}
	
	protected fun _prepareLogSend(method: String): StringBuilder {
		return StringBuilder()
			.append("-> ")
			.append(_name).append('.').append(method)
	}
	
	protected fun _prepareLogSend(method: String, callback: Int): StringBuilder {
		return StringBuilder()
			.append("-> [").append(callback).append("] ")
			.append(_name).append('.').append(method)
	}
	
	protected fun _prepareLogCallback(method: String, callback: Int): StringBuilder {
		return StringBuilder()
			.append("<~ [").append(callback).append("] ")
			.append(_name).append('.').append(method)
	}
	
	
	internal fun _removeSubscription(id: Int) {
		_context.innerSubscriptions.remove(id)
		_subscriptions.remove(id)
	}
	
	internal fun _close() {
		if (!_closed) {
			_closed = true
			
			_subscriptions.closeAll()
			
			_context.logger.debug {
				"-> $_name [close]"
			}
			
			_context.messagePool.writers.use { message ->
				message.writer.apply {
					writeByte(ApiMessageType.INSTANCE_CLOSE.code)
					writeInt(_id)
				}
				_context.connection.sendMessage(message.buffer)
			}
		}
	}
}