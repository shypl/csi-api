package org.shypl.csi.api

import org.shypl.tool.biser.BiserReader

object NothingCallbacksRegistry : CallbacksRegistry {
	override fun put(callback: (BiserReader.(Int) -> Unit)): Int {
		throw UnsupportedOperationException()
	}
	
	override fun take(id: Int): (BiserReader.(Int) -> Unit)? {
		throw UnsupportedOperationException()
	}
}