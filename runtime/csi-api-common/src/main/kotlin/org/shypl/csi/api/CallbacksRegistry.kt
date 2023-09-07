package org.shypl.csi.api

import org.shypl.tool.biser.BiserReader

interface CallbacksRegistry {
	fun put(callback: BiserReader.(Int) -> Unit): Int
	
	fun take(id: Int): (BiserReader.(Int) -> Unit)?
}