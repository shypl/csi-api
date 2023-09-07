package org.shypl.csi.api

import org.shypl.tool.io.ByteBuffer
import org.shypl.tool.io.DummyByteBuffer
import org.shypl.tool.biser.ByteBufferBiserWriter

internal class OutputApiMessageImpl(
	override var buffer: ByteBuffer
) : OutputApiMessage {
	override val writer: ByteBufferBiserWriter = ByteBufferBiserWriter(buffer)
	
	override fun dispose() {
		buffer = DummyByteBuffer
		writer.buffer = DummyByteBuffer
	}
}