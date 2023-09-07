package org.shypl.csi.api.generator.model

class ServiceDescriptor(
	val name: ClassName,
	closeable: Boolean
) : Compatible() {
	private val _methods = mutableListOf<Method>()
	
	var lastMethodId = 0
		private set
	
	var closeable = closeable
		set(value) {
			if (field != value) {
				field = value
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
	
	val methods: Collection<Method>
		get() = _methods
	
	fun provideMethod(name: String, suspend: Boolean, arguments: List<Method.Argument>, result: Method.Result?): Method {
		var method = _methods.find { it.name == name }
		
		if (method == null) {
			method = Method(++lastMethodId, name, suspend, arguments, result)
			_methods.add(method)
			reduceCompatibility(Compatibility.PARTIAL)
		}
		else {
			method.suspend = suspend
			method.arguments = arguments
			method.result = result
		}
		
		return method
	}
	
	fun recoverMethod(id: Int, name: String, suspend: Boolean, arguments: List<Method.Argument>, result: Method.Result?): Method {
		var method = _methods.find { it.id == id } ?: _methods.find { it.name == name }
		
		if (method != null) {
			if (method.id != id || method.name != name) {
				_methods.remove(method)
				method = Method(id, name, suspend, arguments, result)
				_methods.add(method)
				reduceCompatibility(Compatibility.ABSENT)
			}
			else {
				method.suspend = suspend
				method.arguments = arguments
				method.result = result
			}
		}
		else {
			method = Method(id, name, suspend, arguments, result)
			_methods.add(method)
		}
		
		return method
	}
	
	override fun resetCompatibility() {
		super.resetCompatibility()
		_methods.forEach(Compatible::resetCompatibility)
	}
	
	override fun clarifyCompatibility(): Compatibility {
		return _methods.fold(super.clarifyCompatibility()) { c, e -> c.reduce(e.getCompatibility()) }
	}
	
	fun commitRecover(lastMethodId: Int) {
		require(lastMethodId >= this.lastMethodId)
		resetCompatibility()
		this.lastMethodId = lastMethodId
	}
	
	fun removeUnusedMethods(methods: MutableList<String>) {
		if (_methods.removeAll { !methods.contains(it.name) }) {
			reduceCompatibility(Compatibility.ABSENT)
		}
	}
}