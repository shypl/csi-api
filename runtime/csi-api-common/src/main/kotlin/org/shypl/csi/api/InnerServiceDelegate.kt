package org.shypl.csi.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.shypl.tool.biser.BiserReader
import org.shypl.tool.biser.BiserWriter
import org.shypl.tool.io.InputByteBuffer
import org.shypl.tool.logging.debug
import org.shypl.tool.utils.pool.use
import java.io.Closeable

abstract class InnerServiceDelegate<S>(
	protected val context: Context,
	protected val service: S,
	var name: String
) {
	abstract fun callMethod(methodId: Int, message: BiserReader): Boolean
	
	fun setup(id: Int) {
		name += "[+$id]"
	}
	
	fun closeConnectionDueError(e: Throwable) {
		context.logger.error("Uncaught exception", e)
		context.connection.closeDueError()
	}
	
	protected fun <S : Closeable> registerInstanceService(delegate: InnerInstanceServiceDelegate<S>): Int {
		return context.innerInstanceServices.add(delegate)
	}
	
	protected fun registerSubscription(subscription: OuterSubscription, closeable: Closeable): Int {
		return context.outerSubscriptions.add(subscription, closeable)
	}
	
	protected inline fun launchCoroutine(crossinline block: suspend CoroutineScope.() -> Unit) {
		context.coroutineScope.launch {
			try {
				block()
			}
			catch (e: Throwable) {
				closeConnectionDueError(e)
			}
		}
	}
	
	protected inline fun sendMethodResponse(callback: Int, data: BiserWriter.() -> Unit) {
		context.messagePool.writers.use { message ->
			val writer = message.writer
			prepareMethodResponseMessage(callback, writer)
			writer.data()
			sendResponseMessage(message.buffer)
		}
	}
	
	protected fun sendInstanceServiceResponse(callback: Int, serviceId: Int) {
		sendMethodResponse(callback) {
			writeInt(serviceId)
		}
	}
	
	protected fun sendInstanceServiceResponse(callback: Int, serviceId: Int, subscriptionId: Int) {
		sendMethodResponse(callback) {
			writeInt(serviceId)
			writeInt(subscriptionId)
		}
	}
	
	
	protected fun sendSubscriptionResponse(callback: Int, subscriptionId: Int) {
		sendMethodResponse(callback) {
			writeInt(subscriptionId)
		}
	}
	
	
	protected fun prepareMethodResponseMessage(callback: Int, message: BiserWriter) {
		message.writeByte(ApiMessageType.METHOD_RESPONSE.code)
		message.writeInt(callback)
	}
	
	protected fun sendResponseMessage(message: InputByteBuffer) {
		context.connection.sendMessage(message)
	}
	
	protected fun logInstanceServiceResponse(method: String, callback: Int, serviceId: Int) {
		logMethodResponse(method, callback) {
			append("+").append(serviceId)
		}
	}
	
	protected fun logInstanceServiceResponse(method: String, callback: Int, serviceId: Int, subscriptionId: Int) {
		logMethodResponse(method, callback) {
			append("+").append(serviceId).append('~').append(subscriptionId)
		}
	}
	
	protected fun logSubscriptionResponse(method: String, callback: Int, subscriptionId: Int) {
		logMethodResponse(method, callback) {
			append("~").append(subscriptionId)
		}
	}
	
	protected inline fun logMethodCall(method: String, data: StringBuilder.() -> Unit) {
		context.logger.debug {
			prepareLogMethodCall(method)
				.append('(')
				.apply(data)
				.append(')')
				.toString()
		}
	}
	
	protected inline fun logMethodCall(method: String, callback: Int, data: StringBuilder.() -> Unit) {
		context.logger.debug {
			prepareLogMethodCall(method, callback)
				.append('(')
				.apply(data)
				.append(')')
				.toString()
		}
	}
	
	protected inline fun logMethodResponse(method: String, callback: Int, data: StringBuilder.() -> Unit) {
		context.logger.debug {
			prepareLogMethodResponse(method, callback)
				.append(": ")
				.apply(data)
				.toString()
		}
	}
	
	protected fun prepareLogMethodCall(method: String): StringBuilder {
		return StringBuilder()
			.append("<- ")
			.append(name)
			.append('.')
			.append(method)
	}
	
	protected fun prepareLogMethodCall(method: String, callback: Int): StringBuilder {
		return StringBuilder()
			.append("<- [").append(callback).append("] ")
			.append(name)
			.append('.')
			.append(method)
	}
	
	protected fun prepareLogMethodResponse(method: String, callback: Int): StringBuilder {
		return StringBuilder()
			.append("~> [").append(callback).append("] ")
			.append(name)
			.append('.')
			.append(method)
	}
	
	internal open fun close() {
		context.logger.debug {
			"<~ $name [close]"
		}
	}
}