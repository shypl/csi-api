package org.shypl.csi.api

import java.io.Closeable

abstract class InnerInstanceServiceDelegate<S : Closeable>(
	context: Context,
	service: S,
	name: String
) : InnerServiceDelegate<S>(context, service, name) {
	override fun close() {
		super.close()
		service.close()
	}
}