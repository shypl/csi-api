package org.shypl.csi.api

import org.shypl.tool.biser.ByteBufferBiserReader
import org.shypl.tool.utils.pool.ObjectPool

interface ApiMessagePool {
	val readers: ObjectPool<ByteBufferBiserReader>
	val writers: ObjectPool<OutputApiMessage>
}

