package org.shypl.csi.api.generator.code

import org.shypl.csi.api.generator.model.ClassName

class CodeDependency(val name: ClassName) {
	val aliases = mutableSetOf<String>()
	
	constructor(entity: ClassName, vararg aliases: String) : this(entity) {
		this.aliases.addAll(aliases)
	}
}