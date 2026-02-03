package org.shypl.csi.api.generator.target.typescript

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.*
import org.shypl.csi.api.generator.target.AbstractCodersGenerator
import org.shypl.csi.api.generator.target.CoderNameScopeVisitor
import org.shypl.csi.api.generator.target.CodersTypeAggregator
import org.shypl.csi.api.generator.target.DefaultCoderNameVisitor

@Suppress("DuplicatedCode")
class TypescriptCodersGenerator(
	model: Model,
	private val libPackage: ClassPackage,
	genPackage: ClassPackage,
) : AbstractCodersGenerator<TypescriptCodeSource>(
	model,
	libPackage.getName("tool.biser/Encoders"),
	libPackage.getName("tool.biser/Decoders"),
	genPackage.getName("_internal/DataEncoders"),
	genPackage.getName("_internal/DataDecoders"),
) {
	override val typeNames: TypeVisitor<String, DependedCode> = TypescriptTypeNameVisitor(libPackage)
	
	override fun createOuterCoderNameScopeVisitor(biserCoders: ClassName, generatedCoders: ClassName): CoderNameScopeVisitor {
		return OuterCoderNameScopeVisitor(biserCoders, generatedCoders)
	}
	
	override fun createInnerCoderNameScopeVisitor(biserCoders: ClassName): CoderNameScopeVisitor {
		return InnerCoderNameScopeVisitor(biserCoders)
	}
	
	override fun createEncoderGenerator(
		model: Model,
		encoders: MutableSet<Type>,
		encoderNames: DefaultCoderNameVisitor,
		typeNames: TypeVisitor<String, DependedCode>
	): TypeVisitor<Unit, Code> {
		return TypescriptEncoderGenerator(model, encoders, encoderNames, typeNames, libPackage)
	}
	
	override fun createDecoderGenerator(
		model: Model,
		decoders: MutableSet<Type>,
		decoderNames: DefaultCoderNameVisitor,
		typeNames: TypeVisitor<String, DependedCode>
	): TypeVisitor<Unit, Code> {
		return TypescriptDecoderGenerator(model, decoders, decoderNames, typeNames, libPackage)
	}
	
	override fun generate(source: TypescriptCodeSource) {
		super.generate(source)
		
		val entities = mutableSetOf<Entity>()
		val aggregator = object : TypeVisitor<Unit, MutableSet<Entity>>, EntityVisitor<Unit, MutableSet<Entity>> {
			override fun visitPrimitiveType(type: Type.Primitive, data: MutableSet<Entity>) {}
			
			override fun visitEntityType(type: Type.Entity, data: MutableSet<Entity>) {
				val entity = model.getEntity(type.className)
				entity.accept(this, data)
			}
			
			override fun visitListType(type: Type.List, data: MutableSet<Entity>) {
				type.element.accept(this, data)
			}
			
			override fun visitMutableListType(type: Type.MutableList, data: MutableSet<Entity>) {
				type.element.accept(this, data)
			}
			
			override fun visitMapType(type: Type.Map, data: MutableSet<Entity>) {
				type.key.accept(this, data)
				type.value.accept(this, data)
			}
			
			override fun visitMutableMapType(type: Type.MutableMap, data: MutableSet<Entity>) {
				type.key.accept(this, data)
				type.value.accept(this, data)
			}
			
			override fun visitNullableType(type: Type.Nullable, data: MutableSet<Entity>) {
				type.original.accept(this, data)
			}
			
			override fun visitEnumEntity(entity: EnumEntity, data: MutableSet<Entity>) {
				data.add(entity)
			}
			
			override fun visitStructureEntity(entity: StructureEntity, data: MutableSet<Entity>) {
				if (data.add(entity)) {
					entity.parent?.also { model.getEntity(it).accept(this, data) }
					entity.fields.forEach { it.type.accept(this, data) }
					model.getStructureEntityAllChildren(entity).forEach { it.accept(this, data) }
				}
			}
			
			override fun visitConstantEntity(entity: ConstantEntity, data: MutableSet<Entity>) {
				if (data.add(entity)) {
					entity.parent?.also { model.getEntity(it).accept(this, data) }
				}
			}
		}
		
		encoders.forEach { it.accept(aggregator, entities) }
		decoders.forEach { it.accept(aggregator, entities) }
		
		val generator = TypescriptSourceGenerator(model, typeNames)
		
		entities.forEach {
			val file = source.newFile(it.name)
			it.accept(generator, file.body)
		}
	}
	
	override fun generate(source: TypescriptCodeSource, target: ClassName, types: Set<Type>, generator: TypeVisitor<Unit, Code>) {
		if (types.isEmpty()) {
			return
		}
		
		val allTypes = mutableSetOf<Type>()
		val aggregator = CodersTypeAggregator(model)
		types.forEach { it.accept(aggregator, allTypes) }
		
		val file = source.newFile(target)
		file.header.line("// noinspection DuplicatedCode,JSUnusedLocalSymbols")
		file.header.line()
		val code = file.body.identBracketsCurly("export namespace " + target.value + " ")
		
		allTypes.forEach { it.accept(generator, code) }
	}
	
	///
	
	private class OuterCoderNameScopeVisitor(private val biserCoders: ClassName, private val generatedCoders: ClassName) : CoderNameScopeVisitor {
		override fun visitPrimitiveScope(name: String, depended: DependedCode): String {
			depended.addDependency(biserCoders)
			return "${biserCoders.value}.$name"
		}
		
		override fun visitGeneratedScope(name: String, depended: DependedCode): String {
			depended.addDependency(generatedCoders)
			return generatedCoders.value + '.' + name
		}
	}
	
	private class InnerCoderNameScopeVisitor(private val biserCoders: ClassName) : CoderNameScopeVisitor {
		override fun visitPrimitiveScope(name: String, depended: DependedCode): String {
			depended.addDependency(biserCoders)
			return "${biserCoders.value}.$name"
		}
		
		override fun visitGeneratedScope(name: String, depended: DependedCode): String {
			return name
		}
	}
}