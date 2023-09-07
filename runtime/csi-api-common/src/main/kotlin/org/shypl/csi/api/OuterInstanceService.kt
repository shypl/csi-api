package org.shypl.csi.api

import java.io.Closeable

abstract class OuterInstanceService(context: Context, instance: Boolean, id: Int, name: String) : OuterService(context, instance, id, name), Closeable {
	override fun close() {
		_close()
	}
}