package org.shypl.csi.api.generator.target

import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor

class DefaultWriteCallVisitor(private val encoderNaming: TypeVisitor<String, DependedCode>) : TypeVisitor<String, DefaultWriteCallVisitor.Data> {
	
	override fun visitPrimitiveType(type: Type.Primitive, data: Data): String {
		return when (type) {
			Type.Primitive.BOOLEAN       -> "writeBoolean("
			Type.Primitive.BYTE          -> "writeByte("
			Type.Primitive.INT           -> "writeInt("
			Type.Primitive.LONG          -> "writeLong("
			Type.Primitive.DOUBLE        -> "writeDouble("
			Type.Primitive.STRING        -> "writeString("
			Type.Primitive.BOOLEAN_ARRAY -> "writeBooleanArray("
			Type.Primitive.BYTE_ARRAY    -> "writeByteArray("
			Type.Primitive.INT_ARRAY     -> "writeIntArray("
			Type.Primitive.LONG_ARRAY    -> "writeLongArray("
			Type.Primitive.DOUBLE_ARRAY  -> "writeDoubleArray("
		} + "${data.value})"
	}
	
	override fun visitListType(type: Type.List, data: Data): String {
		return "writeList(${data.value}, ${type.element.accept(encoderNaming, data.code)})"
	}
	
	override fun visitMapType(type: Type.Map, data: Data): String {
		return "writeMap(${data.value}, ${type.key.accept(encoderNaming, data.code)}, ${type.value.accept(encoderNaming, data.code)})"
	}
	
	override fun visitEntityType(type: Type.Entity, data: Data): String {
		return "write(${data.value}, ${type.accept(encoderNaming, data.code)})"
	}
	
	override fun visitNullableType(type: Type.Nullable, data: Data): String {
		if (type.original == Type.Primitive.STRING) {
			return "writeStringNullable(${data.value})"
		}
		return "write(${data.value}, ${type.accept(encoderNaming, data.code)})"
	}
	
	fun visit(type: Type, code: DependedCode, value: String): String {
		return type.accept(this, Data(code, value))
	}
	
	
	class Data(
		val code: DependedCode,
		val value: String
	)
}