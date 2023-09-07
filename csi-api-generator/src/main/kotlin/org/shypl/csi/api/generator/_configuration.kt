package org.shypl.csi.api.generator

import org.shypl.csi.api.generator.target.Target

interface GeneratorConfiguration {
	fun addTarget(target: Target)
	
	fun serverKotlin(path: String)
	
	fun clientKotlin(path: String)
	
	fun clientTypescript(path: String, srcPackage: String, genPackage: String, libPackage: String)
}