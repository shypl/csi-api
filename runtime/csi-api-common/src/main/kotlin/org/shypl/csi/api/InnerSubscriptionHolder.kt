package org.shypl.csi.api

import java.util.concurrent.ConcurrentHashMap

class InnerSubscriptionHolder {
	private val map = ConcurrentHashMap<Int, InnerSubscription>()
	
	fun add(subscription: InnerSubscription) {
		map[subscription.id] = subscription
	}
	
	fun get(id: Int): InnerSubscription? {
		return map[id]
	}
	
	fun remove(id: Int) {
		map.remove(id)
	}
	
	fun closeAll() {
		map.values.toList().forEach(InnerSubscription::close)
	}
}

