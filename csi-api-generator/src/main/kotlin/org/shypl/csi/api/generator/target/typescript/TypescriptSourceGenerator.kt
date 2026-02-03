package org.shypl.csi.api.generator.target.typescript

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.ClassName
import org.shypl.csi.api.generator.model.ConstantEntity
import org.shypl.csi.api.generator.model.EntityVisitor
import org.shypl.csi.api.generator.model.EnumEntity
import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.model.StructureEntity
import org.shypl.csi.api.generator.model.TypeVisitor

class TypescriptSourceGenerator(
	private val model: Model,
	private val typeNames: TypeVisitor<String, DependedCode>
) : EntityVisitor<Unit, Code> {
	
	override fun visitEnumEntity(entity: EnumEntity, data: Code) {
		data.identBracketsCurly("export enum ${entity.name.tsName} ") {
			entity.values.forEach { line("$it,") }
		}
	}
	
	override fun visitStructureEntity(entity: StructureEntity, data: Code) {
		val parent = entity.parent?.let {
			data.addDependency(it)
			" extends " + it.tsName
		} ?: ""
		
		val abstract = if (entity.abstract) "abstract " else ""
		
		data.identBracketsCurly("export ${abstract}class ${entity.name.tsName}$parent ") {
			val protected = if (entity.abstract) "protected " else ""
			
			val fields = model.getStructureEntityAllFields(entity)
			if (fields.isEmpty()) {
				line("${protected}constructor() {")
			}
			else {
				line("${protected}constructor(")
				ident {
					fields.forEach {
						line {
							if (entity.isFieldOwner(it.name)) {
								append("public ")
								if (!it.mutable) {
									append("readonly ")
								}
							}
							append(it.name).append(": ").append(it.type.accept(typeNames, data)).append(",")
						}
					}
				}
				line(") {")
			}
			ident {
				entity.parent?.also { parent ->
					line {
						append("super(")
						model.getStructureEntityAllFields(model.getStructureEntity(parent)).joinTo(this) { it.name }
						append(")")
					}
				}
			}
			line("}")
		}
	}
	
	override fun visitConstantEntity(entity: ConstantEntity, data: Code) {
		val parent = entity.parent?.let {
			data.addDependency(it)
			" extends " + it.tsName
		} ?: ""
		
		data.identBracketsCurly("export class ${entity.name.tsName}$parent ") {
			line("static readonly INSTANCE = new ${entity.name.tsName}()")
			line()
			
			identBracketsCurly("private constructor() ") {
				line("super()")
			}
		}
	}
	
	private val ClassName.tsName: String
		get() = toString('_')
}
