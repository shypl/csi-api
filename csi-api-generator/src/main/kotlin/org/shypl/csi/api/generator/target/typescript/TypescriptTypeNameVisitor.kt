package org.shypl.csi.api.generator.target.typescript

import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.ClassPackage
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor

class TypescriptTypeNameVisitor(private val libPackage: ClassPackage) : TypeVisitor<String, DependedCode> {
	
	override fun visitPrimitiveType(type: Type.Primitive, data: DependedCode): String {
		return when (type) {
			Type.Primitive.BOOLEAN       -> "boolean"
			Type.Primitive.BYTE          -> "number"
			Type.Primitive.INT           -> "number"
			Type.Primitive.LONG          -> {
				data.addDependency(libPackage.getName("lang/Long"))
				"Long"
			}
			
			Type.Primitive.DOUBLE        -> "number"
			Type.Primitive.STRING        -> "string"
			Type.Primitive.DATE_TIME     -> "Date"
			Type.Primitive.BOOLEAN_ARRAY -> "ReadonlyArray<boolean>"
			Type.Primitive.BYTE_ARRAY    -> "Int8Array"
			Type.Primitive.INT_ARRAY     -> "ReadonlyArray<number>"
			Type.Primitive.LONG_ARRAY    -> "ReadonlyArray<Long>"
			Type.Primitive.DOUBLE_ARRAY  -> "ReadonlyArray<number>"
		}
	}
	
	override fun visitEntityType(type: Type.Entity, data: DependedCode): String {
		data.addDependency(type.className)
		return type.className.fullValue
	}
	
	override fun visitListType(type: Type.List, data: DependedCode): String {
		return "ReadonlyArray<${type.element.accept(this, data)}>"
	}
	
	override fun visitMapType(type: Type.Map, data: DependedCode): String {
		return "ReadonlyMap<${type.key.accept(this, data)}, ${type.value.accept(this, data)}>"
	}
	
	override fun visitNullableType(type: Type.Nullable, data: DependedCode): String {
		return type.original.accept(this, data) + " | null"
	}
	
}

