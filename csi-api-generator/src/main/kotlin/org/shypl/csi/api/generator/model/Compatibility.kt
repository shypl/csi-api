package org.shypl.csi.api.generator.model

enum class Compatibility {
	FULL,
	PARTIAL,
	ABSENT;
	
	fun reduce(compatibility: Compatibility): Compatibility {
		return if (compatibility.ordinal > ordinal) compatibility else this
	}
}