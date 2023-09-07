package org.shypl.csi.api

import org.shypl.tool.biser.BiserReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RealCallbacksRegistry : CallbacksRegistry {
	private val nextId = AtomicInteger()
	private val map = ConcurrentHashMap<Int, BiserReader.(Int) -> Unit>()
	
	override fun put(callback: BiserReader.(Int) -> Unit): Int {
		val id = nextId.getAndIncrement()
		if (null == map.putIfAbsent(id, callback)) return id
		
		throw IllegalStateException("Unprocessed callback $id")
	}
	
	override fun take(id: Int): (BiserReader.(Int) -> Unit)? {
		return map.remove(id)
	}
}
