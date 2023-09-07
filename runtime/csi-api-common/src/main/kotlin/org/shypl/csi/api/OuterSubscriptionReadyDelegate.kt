package org.shypl.csi.api

import java.util.concurrent.ConcurrentLinkedQueue

class OuterSubscriptionReadyDelegate(private val subscription: OuterSubscription) {
	private val queue = ConcurrentLinkedQueue<() -> Unit>()
	
	fun delay(fn: () -> Unit) {
		synchronized(this) {
			if (!subscription._ready) {
				queue.add(fn)
				return
			}
		}
		fn()
	}
	
	fun ready() {
		synchronized(this) {
			check(subscription._ready)
		}
		queue.forEach { it() }
		queue.clear()
	}
}