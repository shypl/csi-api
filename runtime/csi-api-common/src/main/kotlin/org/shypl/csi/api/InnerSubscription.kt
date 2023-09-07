package org.shypl.csi.api

import org.shypl.tool.biser.BiserReader
import org.shypl.tool.logging.debug
import org.shypl.tool.utils.pool.use
import java.io.Closeable
import kotlin.jvm.Volatile

abstract class InnerSubscription(
	protected val context: Context,
	protected val service: OuterService,
	protected val name: String,
	val id: Int
) : Closeable {
	
	@Volatile
	private var closed = false
	
	override fun close() {
		if (!closed) {
			closed = true
			
			service._removeSubscription(id)
			
			context.logger.debug {
				"-> ${service._name}.$name[~$id] [close]"
			}
			
			context.messagePool.writers.use { message ->
				message.writer.apply {
					writeByte(ApiMessageType.SUBSCRIPTION_CLOSE.code)
					writeInt(id)
				}
				context.connection.sendMessage(message.buffer)
			}
			
		}
	}
	
	abstract fun call(argumentId: Int, message: BiserReader): Boolean
	
	protected inline fun logCall(method: String, data: StringBuilder.() -> Unit) {
		context.logger.debug {
			StringBuilder()
				.append("<~ ").append(service._name).append('.').append(name).append("[~").append(id).append("].").append(method)
				.append('(')
				.apply(data)
				.append(')')
				.toString()
		}
	}
}
