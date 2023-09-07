package org.shypl.csi.api.server

import org.shypl.csi.api.BaseApiConnection
import org.shypl.csi.api.Context
import org.shypl.csi.core.server.ConnectionHandler

abstract class AbstractApiConnection<IA : InnerApi>(
	context: Context,
	api: IA
) : BaseApiConnection<IA>(context, api), ConnectionHandler