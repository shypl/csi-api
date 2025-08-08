package org.shypl.csi.api.generator.model

class Service(
	val id: Int,
	val name: String,
	className: ClassName
) : Compatible() {
	
	var className = className
		set(value) {
			if (field != value) {
				field = value
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
}