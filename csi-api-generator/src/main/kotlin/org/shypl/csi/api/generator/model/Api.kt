package org.shypl.csi.api.generator.model

class Api : Compatible() {
	lateinit var name: ClassName
	
	private val _services = mutableListOf<Service>()
	
	var lastServiceId = 0
		private set
	
	val services: Collection<Service>
		get() = _services
	
	fun provideService(name: String, descriptor: ClassName): Service {
		var service = _services.find { it.name == name }
		
		if (service == null) {
			service = Service(++lastServiceId, name, descriptor)
			_services.add(service)
			reduceCompatibility(Compatibility.PARTIAL)
		}
		
		return service
	}
	
	fun recoverService(id: Int, name: String, descriptor: ClassName): Service {
		var service = _services.find { it.id == id } ?: _services.find { it.name == name }
		
		if (service != null) {
			if (service.id != id || service.name != name) {
				_services.remove(service)
				service = Service(id, name, descriptor)
				_services.add(service)
				reduceCompatibility(Compatibility.ABSENT)
			}
			else {
				service.className = descriptor
			}
		}
		else {
			service = Service(id, name, descriptor)
			_services.add(service)
		}
		
		return service
	}
	
	fun removeUnusedServices(services: MutableList<String>) {
		if (_services.removeAll { !services.contains(it.name) }) {
			reduceCompatibility(Compatibility.ABSENT)
		}
	}
	
	override fun resetCompatibility() {
		super.resetCompatibility()
		_services.forEach(Compatible::resetCompatibility)
	}
	
	override fun clarifyCompatibility(): Compatibility {
		return _services.fold(super.clarifyCompatibility()) { c, e -> c.reduce(e.getCompatibility()) }
	}
	
	
	fun commitRecover(lastServiceId: Int) {
		require(lastServiceId >= this.lastServiceId)
		resetCompatibility()
		this.lastServiceId = lastServiceId
	}
}


