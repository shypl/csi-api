package sandbox.api.server

import sandbox.api.data.SealedClass
import java.io.Closeable

interface ServerService1 {
	fun call()
	
	suspend fun callWithResult(): String
	
	
	suspend fun openService1(b: SealedClass.SubObject): ServerService2
	
	suspend fun callWithArgumentAndResult(a: Int): Int
	
	suspend fun listenOne(handler: (a: Int) -> Unit): Closeable
}

interface ServerService2 : Closeable {
	suspend fun sayHello2(): String
}