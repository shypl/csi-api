package org.shypl.csi.api.generator.target.typescript

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.code.CodeDependency
import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.ClassPackage
import org.shypl.csi.api.generator.model.ConstantEntity
import org.shypl.csi.api.generator.model.EntityVisitor
import org.shypl.csi.api.generator.model.EnumEntity
import org.shypl.csi.api.generator.model.InheritableEntity
import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.model.StructureEntity
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor
import org.shypl.csi.api.generator.target.DefaultWriteCallVisitor

class TypescriptEncoderGenerator(
	private val model: Model,
	private val publicTypes: Set<Type>,
	private val encoderNames: TypeVisitor<String, DependedCode>,
	private val typeNames: TypeVisitor<String, DependedCode>,
	libPackage: ClassPackage
) : TypeVisitor<Unit, Code>, EntityVisitor<Unit, Code> {
	
	private val writeCalls = DefaultWriteCallVisitor(encoderNames)
	
	private val dependencyEncoder = libPackage.getName("tool.biser/Encoder")
	private val dependencyUnknownEntityEncoderException = CodeDependency(libPackage.getName("tool.biser/_exceptions"), "UnknownEntityEncoderException")
	
	override fun visitPrimitiveType(type: Type.Primitive, data: Code) {
	}
	
	override fun visitEntityType(type: Type.Entity, data: Code) {
		data.addDependency(type.name)
		model.getEntity(type.className).accept(this, data)
	}
	
	override fun visitListType(type: Type.List, data: Code) {
		writeDeclaration(type, data) {
			line(writeCalls.visit(type, data, "v"))
		}
	}
	
	override fun visitMapType(type: Type.Map, data: Code) {
		writeDeclaration(type, data) {
			line(writeCalls.visit(type, data, "v"))
		}
	}
	
	override fun visitNullableType(type: Type.Nullable, data: Code) {
		if (type.original == Type.Primitive.STRING
			|| type.original == Type.Primitive.DATE_TIME
			) {
			return
		}
		
		writeDeclaration(type, data) {
			identBracketsCurly("if (v === null) w.writeBoolean(false); else ") {
				line("w.writeBoolean(true)")
				line("w." + writeCalls.visit(type.original, data, "v"))
			}
		}
	}
	
	///
	
	override fun visitEnumEntity(entity: EnumEntity, data: Code) {
		val type = model.getEntityType(entity.name)
		val typeName = type.accept(typeNames, data)
		writeDeclaration(type, data) {
			identBracketsCurly("switch (v) ") {
				entity.values.forEachIndexed { i, v ->
					line("case $typeName.$v: w.writeInt($i); break")
				}
			}
		}
	}
	
	override fun visitStructureEntity(entity: StructureEntity, data: Code) {
		writeDeclaration(model.getEntityType(entity.name), data) {
			if (entity.children.isNotEmpty()) {
				
				entity.children.forEachIndexed { i, childName ->
					val child = model.getEntity(childName)
					val childType = model.getEntityType(childName)
					val childTypeName = childType.accept(typeNames, data)
					
					identBracketsCurly("${if (i != 0) "else " else ""}if (v instanceof $childTypeName)") {
						if (child is InheritableEntity && (child is ConstantEntity || (child is StructureEntity && child.children.isEmpty()))) {
							line("w.writeInt(${child.id})")
						}
						line("${childType.accept(encoderNames, data)}(w, v)")
					}
				}
				
				if (entity.abstract) {
					if (!entity.sealed) {
						data.addDependency(dependencyUnknownEntityEncoderException)
						line("else -> throw new UnknownEntityEncoderException(it)")
					}
				}
				else {
					identBracketsCurly("else -> ") {
						writeStructureEntityFields(entity, data)
					}
				}
				
			}
			else {
				writeStructureEntityFields(entity, data)
			}
		}
	}
	
	override fun visitConstantEntity(entity: ConstantEntity, data: Code) {
		writeDeclaration(model.getEntityType(entity.name), data) {}
	}
	
	///
	
	private fun Code.writeStructureEntityFields(entity: StructureEntity, code: Code) {
		if (entity.children.isNotEmpty()) {
			line("w.writeInt(${entity.id})")
		}
		
		model.getStructureEntityAllFields(entity).forEach { field ->
			line("w." + writeCalls.visit(field.type, code, "v.${field.name}"))
		}
	}
	
	
	private fun writeDeclaration(type: Type, code: Code): Code {
		code.addDependency(dependencyEncoder)
		
		val coderName = type.accept(encoderNames, code)
		val typeName = type.accept(typeNames, code)
		
		val block = code.identBracketsCurly((if (publicTypes.contains(type)) "export " else "") + "const $coderName: Encoder<$typeName> = (w, v) => ")
		
		code.line()
		
		return block
	}
	
	private inline fun writeDeclaration(type: Type, context: Code, code: Code.() -> Unit) {
		writeDeclaration(type, context).apply(code)
	}
}