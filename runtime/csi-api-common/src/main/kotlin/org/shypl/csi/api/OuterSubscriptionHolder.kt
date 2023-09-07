package org.shypl.csi.api

import org.shypl.tool.logging.Logger
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class OuterSubscriptionHolder(private val logger: Logger) {
	private val map = ConcurrentHashMap<Int, OuterSubscription>()
	private val nextId = AtomicInteger()
	
	fun add(subscription: OuterSubscription, closeable: Closeable): Int {
		while (true) {
			val id = nextId.getAndIncrement()
			if (map.putIfAbsent(id, subscription) == null) {
				subscription.setup(id, closeable)
				return id
			}
		}
	}
	
	fun close(id: Int): Boolean {
		return null != map.remove(id)?.also {
			try {
				it.close()
			}
			catch (e: Throwable) {
				logger.error("Uncaught exception", e)
			}
		}
	}
	
	fun closeAll() {
		map.keys.toList().forEach(::close)
	}
}