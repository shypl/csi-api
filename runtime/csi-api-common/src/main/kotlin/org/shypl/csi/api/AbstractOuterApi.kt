package org.shypl.csi.api

import org.shypl.csi.core.Connection

abstract class AbstractOuterApi(protected val connection: Connection) : OuterApi {
	override fun closeConnection() {
		connection.closeDueError()
	}
	
	override fun closeConnectionDueError() {
		connection.closeDueError()
	}
}