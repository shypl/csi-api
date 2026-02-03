package org.shypl.csi.api.generator.target

import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor

class DefaultReadCallVisitor(private val encoderNaming: TypeVisitor<String, DependedCode>) : TypeVisitor<String, DependedCode> {
	
	override fun visitPrimitiveType(type: Type.Primitive, data: DependedCode): String {
		return when (type) {
			Type.Primitive.BOOLEAN       -> "readBoolean()"
			Type.Primitive.BYTE          -> "readByte()"
			Type.Primitive.INT           -> "readInt()"
			Type.Primitive.LONG          -> "readLong()"
			Type.Primitive.DOUBLE        -> "readDouble()"
			Type.Primitive.STRING        -> "readString()"
			Type.Primitive.DATE_TIME     -> "readDateTime()"
			Type.Primitive.BOOLEAN_ARRAY -> "readBooleanArray()"
			Type.Primitive.BYTE_ARRAY    -> "readByteArray()"
			Type.Primitive.INT_ARRAY     -> "readIntArray()"
			Type.Primitive.LONG_ARRAY    -> "readLongArray()"
			Type.Primitive.DOUBLE_ARRAY  -> "readDoubleArray()"
		}
	}
	
	override fun visitListType(type: Type.List, data: DependedCode): String {
		return "readList(${type.element.accept(encoderNaming, data)})"
	}
	
	override fun visitMutableListType(type: Type.MutableList, data: DependedCode): String {
		return "readList(${type.element.accept(encoderNaming, data)})"
	}
	
	override fun visitMutableMapType(type: Type.MutableMap, data: DependedCode): String {
		return "readMap(${type.key.accept(encoderNaming, data)}, ${type.value.accept(encoderNaming, data)})"
	}
	
	override fun visitMapType(type: Type.Map, data: DependedCode): String {
		return "readMap(${type.key.accept(encoderNaming, data)}, ${type.value.accept(encoderNaming, data)})"
	}
	
	override fun visitEntityType(type: Type.Entity, data: DependedCode): String {
		return "read(${type.accept(encoderNaming, data)})"
	}
	
	override fun visitNullableType(type: Type.Nullable, data: DependedCode): String {
		return when (type.original) {
			Type.Primitive.STRING    -> "readStringNullable()"
			Type.Primitive.DATE_TIME -> "readDateTimeNullable()"
			
			else                     -> "read(${type.accept(encoderNaming, data)})"
		}
	}
	
}