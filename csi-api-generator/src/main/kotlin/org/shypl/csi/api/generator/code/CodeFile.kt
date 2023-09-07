package org.shypl.csi.api.generator.code

import org.shypl.csi.api.generator.model.ClassPackage
import org.shypl.tool.utils.collections.mutableKeyedSetOf

@Suppress("LeakingThis")
abstract class CodeFile(private val rootClassPackage: ClassPackage) : DependedCode {
	private val _dependencies = mutableKeyedSetOf(CodeDependency::name)
	
	val header = Code(0, this)
	val body = Code(0, this)
	
	val dependencies: Collection<CodeDependency>
		get() = _dependencies
	
	
	override fun addDependency(dependency: CodeDependency) {
		val d = _dependencies[dependency.name]
		if (d == null) {
			_dependencies.add(dependency)
		}
		else {
			d.aliases.addAll(dependency.aliases)
		}
	}
	
	override fun addDependency(name: String, vararg aliases: String) {
		addDependency(CodeDependency(rootClassPackage.getName(name), *aliases))
	}
}