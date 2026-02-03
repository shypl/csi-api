package org.shypl.csi.api.generator.model

import org.shypl.tool.lang.cast
import org.shypl.tool.utils.collections.MutableKeyedSet
import org.shypl.tool.utils.collections.getOrAdd
import org.shypl.tool.utils.collections.mutableKeyedSetOf

class Model : Compatible() {
	val version = Version()
	val client = Api()
	val server = Api()
	
	var lastEntityId = 0
		private set
	
	val rootPackage = RootClassPackage()
	
	private val _entities = mutableKeyedSetOf(Entity::name)
	private val _services = mutableKeyedSetOf(ServiceDescriptor::name)
	
	private val cacheNullableTypes = mutableKeyedSetOf(Type.Nullable::original)
	private val cacheEntityTypes = mutableKeyedSetOf(Type.Entity::className)
	private val cacheListTypes = mutableKeyedSetOf(Type.List::element)
	private val cacheMutableListTypes = mutableKeyedSetOf(Type.MutableList::element)
	private val cacheMapTypes = hashMapOf<Type, MutableKeyedSet<Type, Type.Map>>()
	private val cacheMutableMapTypes = hashMapOf<Type, MutableKeyedSet<Type, Type.MutableMap>>()
	
	val entities: Collection<Entity>
		get() = _entities
	
	val services: Collection<ServiceDescriptor>
		get() = _services
	
	fun getNullableType(original: Type): Type.Nullable {
		return cacheNullableTypes.getOrAdd(original, Type::Nullable)
	}
	
	fun getEntityType(name: ClassName): Type.Entity {
		return cacheEntityTypes.getOrAdd(name) { Type.Entity(name) }
	}
	
	fun getListType(element: Type): Type.List {
		return cacheListTypes.getOrAdd(element, Type::List)
	}
	
	fun getMutableListType(element: Type): Type.MutableList {
		return cacheMutableListTypes.getOrAdd(element, Type::MutableList)
	}
	
	fun getMapType(key: Type, value: Type): Type.Map {
		return cacheMapTypes
			.getOrPut(key) { mutableKeyedSetOf(Type.Map::value) }
			.getOrAdd(value) { Type.Map(key, value) }
	}
	
	fun getMutableMapType(key: Type, value: Type): Type.MutableMap {
		return cacheMutableMapTypes
			.getOrPut(key) { mutableKeyedSetOf(Type.MutableMap::value) }
			.getOrAdd(value) { Type.MutableMap(key, value) }
	}
	
	fun getClassName(name: String): ClassName {
		return rootPackage.getChild(name.substringBefore('/')).getName(name.substringAfter('/'))
	}
	
	fun getClassName(pkg: String, name: String): ClassName {
		return rootPackage.getChild(pkg).getName(name)
	}
	
	fun getEntity(name: ClassName): Entity {
		return requireNotNull(_entities[name]) { "Entity by name $name is not registered" }
	}
	
	private fun getInheritableEntity(name: ClassName): InheritableEntity {
		return getEntity(name).cast { "Name $name registered not as InheritableEntity" }
	}
	
	fun getStructureEntity(name: ClassName): StructureEntity {
		return getEntity(name).cast { "Name $name registered not as StructureEntity" }
	}
	
	fun getStructureEntityAllChildren(entity: StructureEntity): List<InheritableEntity> {
		val children = entity.children.mapTo(HashSet()) { getInheritableEntity(it) }
		children.addAll(children.flatMap { (it as? StructureEntity)?.let(::getStructureEntityAllChildren).orEmpty() })
		return children.sortedBy { it.id }
	}
	
	fun getStructureEntityAllFields(entity: StructureEntity): List<StructureEntity.Field> {
		return entity.parent?.let { getStructureEntityAllFields(getStructureEntity(it)) }.orEmpty() + entity.fields
	}
	
	fun getServiceDescriptor(name: ClassName): ServiceDescriptor {
		return requireNotNull(_services[name]) { "ServiceDescriptor by name $name is not registered" }
	}
	
	fun versioning() {
		val compatibility = getCompatibility()
		
		if (compatibility == Compatibility.PARTIAL) {
			++version.current
			if (version.compatible == 0) version.compatible = version.current
		}
		else if (compatibility == Compatibility.ABSENT) {
			++version.current
			version.compatible = version.current
		}
		
		resetCompatibility()
	}
	
	fun removeUnusedClasses(classes: Collection<ClassName>) {
		if (_entities.removeAll { !classes.contains(it.name) }) {
			reduceCompatibility(Compatibility.ABSENT)
		}
		if (_services.removeAll { !classes.contains(it.name) }) {
			reduceCompatibility(Compatibility.ABSENT)
		}
	}
	
	override fun resetCompatibility() {
		super.resetCompatibility()
		client.resetCompatibility()
		server.resetCompatibility()
		_entities.forEach(Compatible::resetCompatibility)
	}
	
	override fun clarifyCompatibility(): Compatibility {
		var compatibility = super.clarifyCompatibility()
		compatibility = compatibility.clarify(client::getCompatibility)
		compatibility = compatibility.clarify(server::getCompatibility)
		compatibility = compatibility.clarify { _services.fold(compatibility) { c, e -> c.reduce(e.getCompatibility()) } }
		compatibility = compatibility.clarify { _entities.fold(compatibility) { c, e -> c.reduce(e.getCompatibility()) } }
		return compatibility
	}
	
	private inline fun Compatibility.clarify(clarify: () -> Compatibility): Compatibility {
		if (this == Compatibility.ABSENT) return this
		return reduce(clarify())
	}
	
	fun commitRecover(lastEntityId: Int) {
		require(lastEntityId >= this.lastEntityId)
		resetCompatibility()
		this.lastEntityId = lastEntityId
	}
	
	fun provideServiceDescriptor(name: ClassName, closable: Boolean): ServiceDescriptor {
		var service = _services[name]
		if (service == null) {
			service = ServiceDescriptor(name, closable)
			_services.add(service)
			reduceCompatibility(Compatibility.PARTIAL)
		}
		else {
			service.closeable = closable
		}
		return service
	}
	
	fun recoverServiceDescriptor(name: ClassName, closable: Boolean): ServiceDescriptor {
		var service = _services[name]
		if (service == null) {
			service = ServiceDescriptor(name, closable)
			_services.add(service)
		}
		else {
			service.closeable = closable
		}
		return service
	}
	
	fun provideEnumEntity(name: ClassName, values: List<String>): EnumEntity {
		return provideEntity(name, { EnumEntity(name, values) }) {
			it.values = values
		}
	}
	
	fun recoverEnumEntity(name: ClassName, values: List<String>): EnumEntity {
		return provideEnumEntity(name, values)
	}
	
	fun provideConstantEntity(name: ClassName, parent: ClassName?): ConstantEntity {
		return provideEntity(name, { ConstantEntity(name, ++lastEntityId, parent) }) {
			it.parent = parent
		}
	}
	
	fun recoverConstantEntity(id: Int, name: ClassName, parent: ClassName?): ConstantEntity {
		return recoverInheritableEntity(id, name, { ConstantEntity(name, id, parent) }) {
			it.parent = parent
		}
	}
	
	fun provideStructureEntity(
		name: ClassName,
		parent: ClassName?,
		abstract: Boolean,
		sealed: Boolean,
		fields: List<StructureEntity.Field>,
		children: List<ClassName>,
	): StructureEntity {
		return provideEntity(name, { StructureEntity(name, ++lastEntityId, parent, abstract, sealed, fields, children) }) {
			it.parent = parent
			it.abstract = abstract
			it.sealed = sealed
			it.fields = fields
			it.children = children.toSet()
		}
	}
	
	fun recoverStructureEntity(
		id: Int,
		name: ClassName,
		parent: ClassName?,
		abstract: Boolean,
		sealed: Boolean,
		fields: List<StructureEntity.Field>,
		children: List<ClassName>,
	): StructureEntity {
		return recoverInheritableEntity(id, name, { StructureEntity(name, id, parent, abstract, sealed, fields, children) }) {
			it.parent = parent
			it.abstract = abstract
			it.sealed = sealed
			it.fields = fields
			it.children = children.toSet()
		}
	}
	
	private inline fun <reified E : Entity> provideEntity(name: ClassName, create: () -> E, update: (E) -> Unit): E {
		var entity = _entities[name]
		if (entity != null) {
			require(entity is E) { "Entity id $name already registered as ${entity::class.simpleName}" }
			update(entity)
		}
		else {
			entity = create()
			_entities.add(entity)
			reduceCompatibility(Compatibility.PARTIAL)
		}
		return entity
	}
	
	private inline fun <reified E : InheritableEntity> recoverInheritableEntity(id: Int, name: ClassName, create: () -> E, update: (E) -> Unit): E {
		var entity = _entities.find { it is InheritableEntity && it.id == id } ?: _entities.find { it.name == name }
		
		if (entity != null) {
			if (entity !is E || entity.id != id || entity.name != name) {
				_entities.remove(entity)
				entity = create()
				_entities.add(entity)
				reduceCompatibility(Compatibility.ABSENT)
			}
			else {
				update(entity)
			}
		}
		else {
			entity = create()
			_entities.add(entity)
		}
		
		return entity
	}
}
