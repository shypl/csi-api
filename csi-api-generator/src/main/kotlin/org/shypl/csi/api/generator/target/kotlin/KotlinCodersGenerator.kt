package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.ClassName
import org.shypl.csi.api.generator.model.ClassPackage
import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.model.Type
import org.shypl.csi.api.generator.model.TypeVisitor
import org.shypl.csi.api.generator.target.AbstractCodersGenerator
import org.shypl.csi.api.generator.target.CoderNameScopeVisitor
import org.shypl.csi.api.generator.target.CodersTypeAggregator
import org.shypl.csi.api.generator.target.DefaultCoderNameVisitor

@Suppress("DuplicatedCode")
class KotlinCodersGenerator(
	model: Model,
	targetPackage: ClassPackage,
	private val internal: Boolean = true,
) : AbstractCodersGenerator<KotlinCodeStorage>(
	model,
	model.getClassName("org.shypl.tool.biser/Encoders"),
	model.getClassName("org.shypl.tool.biser/Decoders"),
	targetPackage.getName("DataEncoders"),
	targetPackage.getName("DataDecoders"),
) {
	override val typeNames = KotlinTypeNameVisitor()
	
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
		typeNames: TypeVisitor<String, DependedCode>,
	): TypeVisitor<Unit, Code> {
		return KotlinEncoderGenerator(model, encoders, encoderNames, typeNames)
	}
	
	override fun createDecoderGenerator(
		model: Model,
		decoders: MutableSet<Type>,
		decoderNames: DefaultCoderNameVisitor,
		typeNames: TypeVisitor<String, DependedCode>,
	): TypeVisitor<Unit, Code> {
		return KotlinDecoderGenerator(model, decoders, decoderNames, typeNames)
	}
	
	override fun generate(source: KotlinCodeStorage, target: ClassName, types: Set<Type>, generator: TypeVisitor<Unit, Code>) {
		if (types.isEmpty()) {
			return
		}
		
		val allTypes = mutableSetOf<Type>()
		val aggregator = CodersTypeAggregator(model)
		types.forEach { it.accept(aggregator, allTypes) }
		
		val codeFile = source.newFile(target)
		val code = codeFile.body.identBracketsCurly((if (internal) "internal " else "") + "object " + target.value + " ")
		
		allTypes.forEach { it.accept(generator, code) }
	}
	
	///
	
	private class OuterCoderNameScopeVisitor(private val biserCodersName: ClassName, private val generatedCodersName: ClassName) : CoderNameScopeVisitor {
		override fun visitPrimitiveScope(name: String, depended: DependedCode): String {
			depended.addDependency(biserCodersName)
			return "${biserCodersName.value}.$name"
		}
		
		override fun visitGeneratedScope(name: String, depended: DependedCode): String {
			depended.addDependency(generatedCodersName)
			return generatedCodersName.value + '.' + name
		}
	}
	
	private class InnerCoderNameScopeVisitor(private val biserCodersName: ClassName) : CoderNameScopeVisitor {
		override fun visitPrimitiveScope(name: String, depended: DependedCode): String {
			depended.addDependency(biserCodersName)
			return "${biserCodersName.value}.$name"
		}
		
		override fun visitGeneratedScope(name: String, depended: DependedCode): String {
			return name
		}
	}
}