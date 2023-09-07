package org.shypl.csi.api.generator.code

import org.shypl.csi.api.generator.model.ClassName

interface DependedCode {
	fun addDependency(dependency: CodeDependency)
	
	fun addDependency(name: String, vararg aliases: String)
	
	fun addDependency(name: ClassName) {
		addDependency(CodeDependency(name))
	}
	
	fun addDependency(name: ClassName, vararg aliases: String) {
		addDependency(CodeDependency(name, *aliases))
	}
}