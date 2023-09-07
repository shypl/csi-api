package org.shypl.csi.api.generator.model

class Method(
	val id: Int,
	val name: String,
	suspend: Boolean,
	arguments: List<Argument>,
	result: Result?
) : Compatible() {
	
	var suspend = suspend
		set(value) {
			if (field != value) {
				field = value
				reduceCompatibility(Compatibility.PARTIAL)
			}
		}
	
	var arguments = arguments.toList()
		set(value) {
			if (field != value) {
				field = value.toList()
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
	
	var result = result
		set(value) {
			if (field != value) {
				field = value
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
	
	//
	sealed interface Argument {
		val name: String
		
		data class Value(override val name: String, val type: Type) : Argument
		
		data class Subscription(override val name: String, val parameters: List<Parameter>) : Argument {
			data class Parameter(val name: String?, val type: Type)
		}
	}
	
	sealed interface Result {
		data class Value(val type: Type) : Result
		
		data object Subscription : Result
		
		data class Service(val descriptor: ClassName) : Result
	}
}