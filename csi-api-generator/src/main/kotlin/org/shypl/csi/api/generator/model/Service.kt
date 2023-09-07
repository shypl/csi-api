package org.shypl.csi.api.generator.model

class Service(
	val id: Int,
	val name: String,
	descriptor: ClassName
) : Compatible() {
	
	var descriptor = descriptor
		set(value) {
			if (field != value) {
				field = value
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
}