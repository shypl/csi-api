package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor

class KotlinTypeNameVisitor : TypeVisitor<String, DependedCode> {
	override fun visitPrimitiveType(type: Type.Primitive, data: DependedCode): String {
		return when (type) {
			Type.Primitive.BOOLEAN       -> "Boolean"
			Type.Primitive.BYTE          -> "Byte"
			Type.Primitive.INT           -> "Int"
			Type.Primitive.LONG          -> "Long"
			Type.Primitive.DOUBLE        -> "Double"
			Type.Primitive.STRING        -> "String"
			Type.Primitive.BOOLEAN_ARRAY -> "BooleanArray"
			Type.Primitive.BYTE_ARRAY    -> "ByteArray"
			Type.Primitive.INT_ARRAY     -> "IntArray"
			Type.Primitive.LONG_ARRAY    -> "LongArray"
			Type.Primitive.DOUBLE_ARRAY  -> "DoubleArray"
		}
	}
	
	override fun visitEntityType(type: Type.Entity, data: DependedCode): String {
		data.addDependency(type.name)
		return type.className.toString('.')
	}
	
	override fun visitListType(type: Type.List, data: DependedCode): String {
		return "List<${type.element.accept(this, data)}>"
	}
	
	override fun visitMapType(type: Type.Map, data: DependedCode): String {
		return "Map<${type.key.accept(this, data)}, ${type.value.accept(this, data)}>"
	}
	
	override fun visitNullableType(type: Type.Nullable, data: DependedCode): String {
		return type.original.accept(this, data) + '?'
	}
	
}