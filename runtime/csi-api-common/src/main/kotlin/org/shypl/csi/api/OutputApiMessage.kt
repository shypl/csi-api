package org.shypl.csi.api

import org.shypl.tool.io.ByteBuffer
import org.shypl.tool.biser.BiserWriter

interface OutputApiMessage {
	val buffer: ByteBuffer
	val writer: BiserWriter
	
	fun dispose()
}