package org.shypl.csi.api

import kotlinx.coroutines.CoroutineScope
import org.shypl.csi.core.Connection
import org.shypl.tool.logging.Logger

class Context(
	val logger: Logger,
	val coroutineScope: CoroutineScope,
	val messagePool: ApiMessagePool,
	val connection: Connection,
	val callbacks: CallbacksRegistry,
) {
	val innerInstanceServices = InnerServiceHolder()
	val innerSubscriptions = InnerSubscriptionHolder()
	val outerSubscriptions = OuterSubscriptionHolder(logger)
}