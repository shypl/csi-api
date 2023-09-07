package org.shypl.csi.api.generator.target

import org.shypl.csi.api.generator.code.DependedCode

interface CoderNameScopeVisitor {
	fun visitPrimitiveScope(name: String, depended: DependedCode): String
	
	fun visitGeneratedScope(name: String, depended: DependedCode): String
}