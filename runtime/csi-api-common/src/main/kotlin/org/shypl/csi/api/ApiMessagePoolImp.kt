package org.shypl.csi.api

import org.shypl.tool.biser.ByteBufferBiserReader
import org.shypl.tool.io.ByteBuffer
import org.shypl.tool.io.DummyInputByteBuffer
import org.shypl.tool.utils.pool.ArrayObjectPool
import org.shypl.tool.utils.pool.ObjectAllocator
import org.shypl.tool.utils.pool.ObjectPool

internal class ApiMessagePoolImp(byteBuffers: ObjectPool<ByteBuffer>) : ApiMessagePool {
	override val readers: ObjectPool<ByteBufferBiserReader> = ArrayObjectPool(64, ReaderAllocator())
	override val writers: ObjectPool<OutputApiMessage> = ArrayObjectPool(64, WriterAllocator(byteBuffers))
	
	private class ReaderAllocator : ObjectAllocator<ByteBufferBiserReader> {
		override fun produceInstance(): ByteBufferBiserReader {
			return ByteBufferBiserReader(DummyInputByteBuffer)
		}
		
		override fun clearInstance(instance: ByteBufferBiserReader) {
			instance.buffer = DummyInputByteBuffer
		}
		
		override fun disposeInstance(instance: ByteBufferBiserReader) {
			clearInstance(instance)
		}
	}
	
	private class WriterAllocator(private val buffers: ObjectPool<ByteBuffer>) : ObjectAllocator<OutputApiMessage> {
		override fun produceInstance(): OutputApiMessage {
			return OutputApiMessageImpl(buffers.take())
		}
		
		override fun clearInstance(instance: OutputApiMessage) {
			instance.buffer.clear()
		}
		
		override fun disposeInstance(instance: OutputApiMessage) {
			val buffer = instance.buffer
			instance.dispose()
			buffers.back(buffer)
		}
	}
}