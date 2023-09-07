package sandbox.api.data

sealed class SealedClass {
	
	object SubObject: SealedClass()
	
	sealed class SubSealedClass: SealedClass() {
		class SubSubClass(val a: Int): SubSealedClass()
		
		object SubSubObject: SubSealedClass()
	}
}

enum class EnumClass {
	A, B
}

open class Simple(val a: Int)
