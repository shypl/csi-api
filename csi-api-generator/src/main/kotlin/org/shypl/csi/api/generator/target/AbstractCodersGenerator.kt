package org.shypl.csi.api.generator.target

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.code.CodeStorage
import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.ClassName
import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.model.StructureEntity
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor

@Suppress("LeakingThis")
abstract class AbstractCodersGenerator<C: CodeStorage>(
	protected val model: Model,
	private val biserEncoders: ClassName,
	private val biserDecoders: ClassName,
	private val generatedEncoders: ClassName,
	private val generatedDecoders: ClassName
) {
	protected val encoders = mutableSetOf<Type>()
	protected val decoders = mutableSetOf<Type>()
	
	private val outerWriteCallVisitor = DefaultWriteCallVisitor(DefaultCoderNameVisitor(createOuterCoderNameScopeVisitor(biserEncoders, generatedEncoders)))
	private val outerReadCallVisitor = DefaultReadCallVisitor(DefaultCoderNameVisitor(createOuterCoderNameScopeVisitor(biserDecoders, generatedDecoders)))
	
	protected abstract val typeNames: TypeVisitor<String, DependedCode>
	
	private val callRegistration = object : TypeVisitor<Unit, MutableSet<Type>> {
		override fun visitPrimitiveType(type: Type.Primitive, data: MutableSet<Type>) {}
		
		override fun visitEntityType(type: Type.Entity, data: MutableSet<Type>) {
			data.add(type)
			val entity = model.getEntity(type.className)
			if (entity is StructureEntity) {
				model.getStructureEntityAllFields(entity).forEach {
					it.type.accept(this, data)
				}
			}
		}
		
		override fun visitListType(type: Type.List, data: MutableSet<Type>) {
			data.add(type.element)
		}
		
		override fun visitMutableListType(type: Type.MutableList, data: MutableSet<Type>) {
			data.add(type.element)
		}
		
		override fun visitMapType(type: Type.Map, data: MutableSet<Type>) {
			data.add(type.key)
			data.add(type.value)
		}
		
		override fun visitMutableMapType(type: Type.MutableMap, data: MutableSet<Type>) {
			data.add(type.key)
			data.add(type.value)
		}
		
		override fun visitNullableType(type: Type.Nullable, data: MutableSet<Type>) {
			data.add(type)
		}
	}
	
	fun getTypeName(type: Type, depended: DependedCode): String {
		return type.accept(typeNames, depended)
	}
	
	fun provideWriteCall(depended: DependedCode, type: Type, value: String): String {
		type.accept(callRegistration, encoders)
		return outerWriteCallVisitor.visit(type, depended, value)
	}
	
	fun provideReadCall(depended: DependedCode, type: Type): String {
		type.accept(callRegistration, decoders)
		return type.accept(outerReadCallVisitor, depended)
	}
	
	fun registerEncoder(type: Type) {
		encoders.add(type)
	}
	
	fun registerDecoder(type: Type) {
		decoders.add(type)
	}
	
	open fun generate(source: C) {
		val encoderNames = DefaultCoderNameVisitor(createInnerCoderNameScopeVisitor(biserEncoders))
		val decoderNames = DefaultCoderNameVisitor(createInnerCoderNameScopeVisitor(biserDecoders))
		
		generate(
			source,
			generatedEncoders,
			encoders,
			createEncoderGenerator(model, encoders, encoderNames, typeNames)
		)
		
		generate(
			source,
			generatedDecoders,
			decoders,
			createDecoderGenerator(model, decoders, decoderNames, typeNames)
		)
	}
	
	protected abstract fun createOuterCoderNameScopeVisitor(biserCoders: ClassName, generatedCoders: ClassName): CoderNameScopeVisitor
	
	protected abstract fun createInnerCoderNameScopeVisitor(biserCoders: ClassName): CoderNameScopeVisitor
	
	protected abstract fun createEncoderGenerator(
		model: Model,
		encoders: MutableSet<Type>,
		encoderNames: DefaultCoderNameVisitor,
		typeNames: TypeVisitor<String, DependedCode>
	): TypeVisitor<Unit, Code>
	
	protected abstract fun createDecoderGenerator(
		model: Model,
		decoders: MutableSet<Type>,
		decoderNames: DefaultCoderNameVisitor,
		typeNames: TypeVisitor<String, DependedCode>
	): TypeVisitor<Unit, Code>
	
	protected abstract fun generate(source: C, target: ClassName, types: Set<Type>, generator: TypeVisitor<Unit, Code>)
}
