package org.shypl.csi.api.generator.model

abstract class Compatible {
	private var compatibility = Compatibility.FULL
	
	fun getCompatibility(): Compatibility {
		return if (compatibility == Compatibility.ABSENT) compatibility
		else clarifyCompatibility().also { compatibility = it }
	}
	
	open fun resetCompatibility() {
		compatibility = Compatibility.FULL
	}
	
	protected open fun clarifyCompatibility(): Compatibility {
		return compatibility
	}
	
	protected fun reduceCompatibility(compatibility: Compatibility) {
		this.compatibility = this.compatibility.reduce(compatibility)
	}
}