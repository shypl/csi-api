package org.shypl.csi.api.generator.model

abstract class Entity(
	val name: ClassName,
) : Compatible() {
	
	fun <R> accept(visitor: EntityVisitor<R, Unit>): R = accept(visitor, Unit)
	
	abstract fun <R, D> accept(visitor: EntityVisitor<R, D>, data: D): R
	
	override fun toString(): String {
		return name.full
	}
}

class EnumEntity(
	name: ClassName,
	values: List<String>
) : Entity(name) {
	var values = values
		set(value) {
			if (field != value) {
				field = value.toList()
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
	
	override fun <R, D> accept(visitor: EntityVisitor<R, D>, data: D): R {
		return visitor.visitEnumEntity(this, data)
	}
}

abstract class InheritableEntity(
	name: ClassName,
	val id: Int,
	parent: ClassName?
) : Entity(name) {
	var parent = parent
		set(value) {
			if (field != value) {
				field = value
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
}

class StructureEntity(
	name: ClassName,
	id: Int,
	parent: ClassName?,
	abstract: Boolean,
	sealed: Boolean,
	fields: List<Field>,
	children: List<ClassName>
) : InheritableEntity(name, id, parent) {
	
	var abstract = abstract
		set(value) {
			if (field != value) {
				field = value
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
	
	var sealed = sealed
		set(value) {
			if (field != value) {
				field = value
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
	
	
	var fields = fields
		set(value) {
			if (field != value) {
				field = value
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
	
	
	var children: Set<ClassName> = HashSet(children)
		set(value) {
			if (field != value) {
				field = value.toSet()
				reduceCompatibility(Compatibility.ABSENT)
			}
		}
	
	fun isFieldOwner(name: String): Boolean {
		return fields.any { it.name == name }
	}
	
	override fun <R, D> accept(visitor: EntityVisitor<R, D>, data: D): R {
		return visitor.visitStructureEntity(this, data)
	}
	
	data class Field(
		val name: String,
		val type: Type,
	)
}

class ConstantEntity(
	name: ClassName,
	id: Int,
	parent: ClassName?
) : InheritableEntity(name, id, parent) {
	override fun <R, D> accept(visitor: EntityVisitor<R, D>, data: D): R {
		return visitor.visitConstantEntity(this, data)
	}
}


interface EntityVisitor<R, D> {
	fun visitEnumEntity(entity: EnumEntity, data: D): R
	fun visitStructureEntity(entity: StructureEntity, data: D): R
	fun visitConstantEntity(entity: ConstantEntity, data: D): R
}
