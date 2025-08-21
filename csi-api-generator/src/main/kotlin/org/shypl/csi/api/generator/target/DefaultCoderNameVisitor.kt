package org.shypl.csi.api.generator.target

import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor

class DefaultCoderNameVisitor(private val scope: CoderNameScopeVisitor) : TypeVisitor<String, DependedCode> {
	private var deep = 0
	
	override fun visitPrimitiveType(type: Type.Primitive, data: DependedCode): String {
		val name = when (type) {
			Type.Primitive.BOOLEAN       -> "BOOLEAN"
			Type.Primitive.BYTE          -> "BYTE"
			Type.Primitive.INT           -> "INT"
			Type.Primitive.LONG          -> "LONG"
			Type.Primitive.DOUBLE        -> "DOUBLE"
			Type.Primitive.STRING        -> "STRING"
			Type.Primitive.DATE_TIME     -> "DATE_TIME"
			Type.Primitive.BOOLEAN_ARRAY -> "BOOLEAN_ARRAY"
			Type.Primitive.BYTE_ARRAY    -> "BYTE_ARRAY"
			Type.Primitive.INT_ARRAY     -> "INT_ARRAY"
			Type.Primitive.LONG_ARRAY    -> "LONG_ARRAY"
			Type.Primitive.DOUBLE_ARRAY  -> "DOUBLE_ARRAY"
		}
		return if (deep == 0) scope.visitPrimitiveScope(name, data) else name
	}
	
	override fun visitListType(type: Type.List, data: DependedCode): String {
		++deep
		val name = "LIST_" + type.element.accept(this, data)
		--deep
		return if (deep == 0) scope.visitGeneratedScope(name, data) else name
	}
	
	override fun visitMapType(type: Type.Map, data: DependedCode): String {
		++deep
		val name = "MAP_" + type.key.accept(this, data) + "__" + type.value.accept(this, data)
		--deep
		return if (deep == 0) scope.visitGeneratedScope(name, data) else name
	}
	
	override fun visitEntityType(type: Type.Entity, data: DependedCode): String {
		val name = "ENTITY_" + type.className.toStringFull('_')
		return if (deep == 0) scope.visitGeneratedScope(name, data) else name
	}
	
	override fun visitNullableType(type: Type.Nullable, data: DependedCode): String {
		++deep
		val name = "NULLABLE_" + type.original.accept(this, data)
		--deep
		return if (deep == 0) scope.visitGeneratedScope(name, data) else name
	}
}