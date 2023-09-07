package org.shypl.csi.api

import org.shypl.csi.core.BaseConnectionHandler
import org.shypl.csi.core.ProtocolBrokenException
import org.shypl.tool.io.InputByteBuffer
import org.shypl.tool.lang.alsoOnFalse
import org.shypl.tool.logging.debug
import org.shypl.tool.utils.pool.use

abstract class BaseApiConnection<IA : BaseInnerApi>(
	protected val context: Context,
	protected val api: IA,
) : BaseConnectionHandler {
	
	init {
		context.logger.debug { "Open" }
	}
	
	override fun handleConnectionMessage(message: InputByteBuffer) {
		context.messagePool.readers.use { reader ->
			reader.buffer = message
			
			val messageType = when (val messageTypeCode = reader.readByte()) {
				ApiMessageType.METHOD_CALL.code          -> ApiMessageType.METHOD_CALL
				ApiMessageType.METHOD_RESPONSE.code      -> ApiMessageType.METHOD_RESPONSE
				ApiMessageType.SUBSCRIPTION_CALL.code    -> ApiMessageType.SUBSCRIPTION_CALL
				ApiMessageType.SUBSCRIPTION_CLOSE.code   -> ApiMessageType.SUBSCRIPTION_CLOSE
				ApiMessageType.INSTANCE_METHOD_CALL.code -> ApiMessageType.INSTANCE_METHOD_CALL
				ApiMessageType.INSTANCE_CLOSE.code       -> ApiMessageType.INSTANCE_CLOSE
				else                                     -> throw ProtocolBrokenException("Bad api message type code $messageTypeCode")
			}
			
			when (messageType) {
				ApiMessageType.METHOD_CALL          -> {
					val serviceId = reader.readInt()
					val methodId = reader.readInt()
					(findService(serviceId)?.callMethod(methodId, reader) ?: false).alsoOnFalse {
						throw ProtocolBrokenException("Calling an unknown service $serviceId.$methodId")
					}
				}
				
				ApiMessageType.INSTANCE_METHOD_CALL -> {
					val serviceId = reader.readInt()
					val methodId = reader.readInt()
					(context.innerInstanceServices.get(serviceId)?.callMethod(methodId, reader) ?: false).alsoOnFalse {
						throw ProtocolBrokenException("Calling an unknown service $serviceId.$methodId")
					}
				}
				
				ApiMessageType.METHOD_RESPONSE      -> {
					val responseId = reader.readInt()
					context.callbacks.take(responseId)?.invoke(reader, responseId)
						?: throw ProtocolBrokenException("Response an unknown callback $responseId")
				}
				
				ApiMessageType.SUBSCRIPTION_CALL    -> {
					val subscriptionId = reader.readInt()
					val argumentId = reader.readInt()
					val subscription = context.innerSubscriptions.get(subscriptionId)
					if (subscription == null) {
						context.logger.warn("Calling an unknown subscription $subscriptionId")
						message.skipRead()
					}
					else {
						subscription.call(argumentId, reader).alsoOnFalse {
							throw ProtocolBrokenException("Calling an unknown subscription $subscriptionId.$argumentId")
						}
					}
				}
				
				ApiMessageType.SUBSCRIPTION_CLOSE -> {
					val subscriptionId = reader.readInt()
					context.outerSubscriptions.close(subscriptionId).alsoOnFalse {
						throw ProtocolBrokenException("Calling an unknown subscription $subscriptionId")
					}
				}
				
				ApiMessageType.INSTANCE_CLOSE       -> {
					val serviceId = reader.readInt()
					context.innerInstanceServices.close(serviceId).alsoOnFalse {
						throw ProtocolBrokenException("Sub service $serviceId is not exists")
					}
				}
			}
			
		}
	}
	
	override fun handleConnectionClose() {
		context.logger.debug { "Close" }
		
		context.outerSubscriptions.closeAll()
		context.innerSubscriptions.closeAll()
		context.innerInstanceServices.closeAll()
		
		try {
			api.handleConnectionClose()
		}
		catch (e: Throwable) {
			context.logger.error("Error on close", e)
		}
	}
	
	protected abstract fun findService(serviceId: Int): InnerServiceDelegate<*>?
}

