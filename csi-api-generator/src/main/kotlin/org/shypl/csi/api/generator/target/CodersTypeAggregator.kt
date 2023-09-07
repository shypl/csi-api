package org.shypl.csi.api.generator.target

import org.shypl.csi.api.generator.model.ConstantEntity
import org.shypl.csi.api.generator.model.EntityVisitor
import org.shypl.csi.api.generator.model.EnumEntity
import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.model.StructureEntity
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor

class CodersTypeAggregator(private val model: Model) : TypeVisitor<Unit, MutableSet<Type>>, EntityVisitor<Unit, MutableSet<Type>> {
	
	private val fieldsAggregator = object : TypeVisitor<Unit, MutableSet<Type>> {
		override fun visitPrimitiveType(type: Type.Primitive, data: MutableSet<Type>) {
		}
		
		override fun visitEntityType(type: Type.Entity, data: MutableSet<Type>) {
			if (data.add(type)) {
				model.getEntity(type.className).accept(this@CodersTypeAggregator, data)
			}
		}
		
		override fun visitListType(type: Type.List, data: MutableSet<Type>) {
			type.element.accept(this@CodersTypeAggregator, data)
		}
		
		override fun visitMapType(type: Type.Map, data: MutableSet<Type>) {
			type.key.accept(this@CodersTypeAggregator, data)
			type.value.accept(this@CodersTypeAggregator, data)
		}
		
		override fun visitNullableType(type: Type.Nullable, data: MutableSet<Type>) {
			if (data.add(type)) {
				type.original.accept(this@CodersTypeAggregator, data)
			}
		}
	}
	
	override fun visitPrimitiveType(type: Type.Primitive, data: MutableSet<Type>) {}
	
	override fun visitEntityType(type: Type.Entity, data: MutableSet<Type>) {
		if (data.add(type)) {
			model.getEntity(type.className).accept(this, data)
		}
	}
	
	override fun visitListType(type: Type.List, data: MutableSet<Type>) {
		if (data.add(type)) {
			type.element.accept(this, data)
		}
	}
	
	override fun visitMapType(type: Type.Map, data: MutableSet<Type>) {
		if (data.add(type)) {
			type.key.accept(this, data)
			type.value.accept(this, data)
		}
	}
	
	override fun visitNullableType(type: Type.Nullable, data: MutableSet<Type>) {
		if (data.add(type)) {
			type.original.accept(this, data)
		}
	}
	
	///
	
	override fun visitEnumEntity(entity: EnumEntity, data: MutableSet<Type>) {
	}
	
	override fun visitStructureEntity(entity: StructureEntity, data: MutableSet<Type>) {
		entity.children.forEach {
			model.getEntityType(it).accept(this, data)
		}
		entity.fields.forEach {
			it.type.accept(fieldsAggregator, data)
		}
	}
	
	override fun visitConstantEntity(entity: ConstantEntity, data: MutableSet<Type>) {
	}
}