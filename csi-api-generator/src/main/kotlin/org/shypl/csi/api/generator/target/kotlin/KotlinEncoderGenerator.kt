package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.ConstantEntity
import org.shypl.csi.api.generator.model.EntityVisitor
import org.shypl.csi.api.generator.model.EnumEntity
import org.shypl.csi.api.generator.model.InheritableEntity
import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.model.StructureEntity
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor
import org.shypl.csi.api.generator.target.DefaultWriteCallVisitor

class KotlinEncoderGenerator(
	private val model: Model,
	private val publicTypes: Set<Type>,
	private val encoderNames: TypeVisitor<String, DependedCode>,
	private val typeNames: TypeVisitor<String, DependedCode>,
) : TypeVisitor<Unit, Code>, EntityVisitor<Unit, Code> {
	
	private val writeCalls = DefaultWriteCallVisitor(encoderNames)
	
	private val dependencyEncoder = model.getClassName("org.shypl.tool.biser/Encoder")
	private val dependencyUnknownEntityEncoderException = model.getClassName("org.shypl.tool.biser/UnknownEntityEncoderException")
	
	override fun visitPrimitiveType(type: Type.Primitive, data: Code) {
	}
	
	override fun visitEntityType(type: Type.Entity, data: Code) {
		data.addDependency(type.className)
		model.getEntity(type.className).accept(this, data)
	}
	
	override fun visitListType(type: Type.List, data: Code) {
		writeDeclaration(type, data) {
			line(writeCalls.visit(type, data, "it"))
		}
	}
	
	override fun visitMutableListType(type: Type.MutableList, data: Code) {
		writeDeclaration(type, data) {
			line(writeCalls.visit(type, data, "it"))
		}
	}
	
	override fun visitMapType(type: Type.Map, data: Code) {
		writeDeclaration(type, data) {
			line(writeCalls.visit(type, data, "it"))
		}
	}
	
	override fun visitMutableMapType(type: Type.MutableMap, data: Code) {
		writeDeclaration(type, data) {
			line(writeCalls.visit(type, data, "it"))
		}
	}
	
	override fun visitNullableType(type: Type.Nullable, data: Code) {
		if (type.original == Type.Primitive.STRING
			|| type.original == Type.Primitive.DATE_TIME
		) {
			return
		}
		
		writeDeclaration(type, data) {
			identBracketsCurly("if (it == null) writeBoolean(false) else ") {
				line("writeBoolean(true)")
				line(writeCalls.visit(type.original, data, "it"))
			}
		}
	}
	
	///
	
	override fun visitEnumEntity(entity: EnumEntity, data: Code) {
		val type = model.getEntityType(entity.name)
		val typeName = type.accept(typeNames, data)
		writeDeclaration(type, data) {
			line("writeInt(when (it) {")
			ident {
				entity.values.forEachIndexed { i, v ->
					line("$typeName.$v -> $i")
				}
			}
			line("})")
		}
	}
	
	override fun visitStructureEntity(entity: StructureEntity, data: Code) {
		writeDeclaration(model.getEntityType(entity.name), data) {
			if (entity.children.isNotEmpty()) {
				
				identBracketsCurly("when (it) ") {
					entity.children.forEach { childName ->
						val child = model.getEntity(childName)
						val childType = model.getEntityType(childName)
						val childTypeName = childType.accept(typeNames, data)
						
						identBracketsCurly("is $childTypeName -> ") {
							if (child is InheritableEntity && (child is ConstantEntity || (child is StructureEntity && child.children.isEmpty()))) {
								line("writeInt(${child.id})")
							}
							line("${childType.accept(encoderNames, data)}(it)")
						}
					}
					
					if (entity.abstract) {
						if (!entity.sealed) {
							data.addDependency(dependencyUnknownEntityEncoderException)
							line("else -> throw UnknownEntityEncoderException(it)")
						}
					}
					else {
						identBracketsCurly("else -> ") {
							writeStructureEntityFields(entity, data)
						}
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
			line("writeInt(${entity.id})")
		}
		
		model.getStructureEntityAllFields(entity).forEach { field ->
			line(writeCalls.visit(field.type, code, "it.${field.name}"))
		}
	}
	
	
	private fun writeDeclaration(type: Type, code: Code): Code {
		code.addDependency(dependencyEncoder)
		
		val coderName = type.accept(encoderNames, code)
		val typeName = type.accept(typeNames, code)
		
		val block = code.identBracketsCurly((if (publicTypes.contains(type)) "" else "private ") + "val $coderName: Encoder<$typeName> = ")
		
		code.line()
		
		return block
	}
	
	private inline fun writeDeclaration(type: Type, context: Code, code: Code.() -> Unit) {
		writeDeclaration(type, context).apply(code)
	}
}
