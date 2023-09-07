package org.shypl.csi.api

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class InnerServiceHolder {
	private val map = ConcurrentHashMap<Int, InnerServiceDelegate<*>>()
	private val nextId = AtomicInteger()
	
	fun get(id: Int): InnerServiceDelegate<*>? {
		return map[id]
	}
	
	fun close(id: Int): Boolean {
		return null != map.remove(id)?.also {
			it.close()
		}
	}
	
	fun add(item: InnerServiceDelegate<*>): Int {
		while (true) {
			val id = nextId.getAndIncrement()
			if (map.putIfAbsent(id, item) == null) {
				item.setup(id)
				return id
			}
		}
	}
	
	fun closeAll() {
		map.keys.toList().forEach(::close)
	}
}

