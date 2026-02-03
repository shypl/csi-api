package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.ConstantEntity
import org.shypl.csi.api.generator.model.EntityVisitor
import org.shypl.csi.api.generator.model.EnumEntity
import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.model.StructureEntity
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor
import org.shypl.csi.api.generator.target.DefaultReadCallVisitor

class KotlinDecoderGenerator(
	private val model: Model,
	private val publicTypes: Set<Type>,
	private val decoderNames: TypeVisitor<String, DependedCode>,
	private val typeNames: TypeVisitor<String, DependedCode>,
) : TypeVisitor<Unit, Code>, EntityVisitor<Unit, Code> {
	
	private val readCalls = DefaultReadCallVisitor(decoderNames)
	
	private val dependencyDecoder = model.getClassName("org.shypl.tool.biser/Decoder")
	private val dependencyUnknownIdDecoderException = model.getClassName("org.shypl.tool.biser/UnknownIdDecoderException")
	
	override fun visitPrimitiveType(type: Type.Primitive, data: Code) {
	}
	
	override fun visitEntityType(type: Type.Entity, data: Code) {
		data.addDependency(type.className)
		model.getEntity(type.className).accept(this, data)
	}
	
	override fun visitListType(type: Type.List, data: Code) {
		writeDeclaration(type, data) {
			line(type.accept(readCalls, data))
		}
	}
	
	override fun visitMutableListType(type: Type.MutableList, data: Code) {
		writeDeclaration(type, data) {
			line(type.accept(readCalls, data))
		}
	}
	
	override fun visitMapType(type: Type.Map, data: Code) {
		writeDeclaration(type, data) {
			line(type.accept(readCalls, data))
		}
	}
	
	override fun visitMutableMapType(type: Type.MutableMap, data: Code) {
		writeDeclaration(type, data) {
			line(type.accept(readCalls, data))
		}
	}
	
	override fun visitNullableType(type: Type.Nullable, data: Code) {
		if (type.original == Type.Primitive.STRING
			|| type.original == Type.Primitive.DATE_TIME
		) {
			return
		}
		writeDeclaration(type, data) {
			line {
				append("if (readBoolean()) ")
				append(type.original.accept(readCalls, data))
				append(" else null")
			}
		}
	}
	
	///
	
	override fun visitEnumEntity(entity: EnumEntity, data: Code) {
		val type = model.getEntityType(entity.name)
		val typeName = type.accept(typeNames, data)
		writeDeclaration(type, data) {
			identBracketsCurly("when (val id = readInt()) ") {
				entity.values.forEachIndexed { i, v ->
					line("$i -> $typeName.$v")
				}
				data.addDependency(dependencyUnknownIdDecoderException)
				line("else -> throw UnknownIdDecoderException(id, $typeName::class)")
			}
		}
	}
	
	override fun visitStructureEntity(entity: StructureEntity, data: Code) {
		val type = model.getEntityType(entity.name)
		
		if (entity.children.isNotEmpty()) {
			
			if (!entity.abstract) {
				writeDeclaration(type, data, true).apply {
					writeStructureEntityDecode(entity, data)
				}
			}
			
			writeDeclaration(type, data) {
				identBracketsCurly("when (val id = readInt()) ") {
					model.getStructureEntityAllChildren(entity).forEach { child ->
						val name = model.getEntityType(child.name).accept(decoderNames, this)
						if (child is StructureEntity && !child.abstract && child.children.isNotEmpty())
							line("${child.id} -> ${name}_RAW()")
						else
							line("${child.id} -> $name()")
					}
					
					if (!entity.abstract) {
						val name = type.accept(decoderNames, this)
						line("${entity.id} -> ${name}_RAW()")
					}
					
					val typeName = type.accept(typeNames, data)
					data.addDependency(dependencyUnknownIdDecoderException)
					line("else -> throw UnknownIdDecoderException(id, $typeName::class)")
				}
			}
		}
		else {
			writeDeclaration(type, data) {
				writeStructureEntityDecode(entity, data)
			}
		}
	}
	
	override fun visitConstantEntity(entity: ConstantEntity, data: Code) {
		val type = model.getEntityType(entity.name)
		writeDeclaration(type, data) {
			line(type.accept(typeNames, data))
		}
	}
	
	///
	
	private fun Code.writeStructureEntityDecode(entity: StructureEntity, code: Code) {
		val type = model.getEntityType(entity.name)
		identBracketsRound(type.accept(typeNames, code)) {
			val fields = model.getStructureEntityAllFields(entity)
			val last = fields.size - 1
			fields.forEachIndexed { i, field ->
				line(field.type.accept(readCalls, code) + (if (i == last) "" else ","))
			}
		}
	}
	
	private fun writeDeclaration(type: Type, code: Code, raw: Boolean = false): Code {
		code.addDependency(dependencyDecoder)
		
		var coderName = type.accept(decoderNames, code)
		val typeName = type.accept(typeNames, code)
		
		if (raw) {
			coderName += "_RAW"
		}
		
		val block = code.identBracketsCurly((if (raw || !publicTypes.contains(type)) "private " else "") + "val $coderName: Decoder<$typeName> = ")
		
		code.line()
		
		return block
	}
	
	private inline fun writeDeclaration(type: Type, context: Code, code: Code.() -> Unit) {
		writeDeclaration(type, context).apply(code)
	}
}