package org.shypl.csi.api.generator

import org.shypl.csi.api.generator.target.Target
import org.shypl.csi.api.generator.target.kotlin.ClientKotlinTarget
import org.shypl.csi.api.generator.target.kotlin.ServerKotlinTarget
import org.shypl.csi.api.generator.target.typescript.ClientTypescriptTarget
import java.nio.file.Path

internal class GeneratorConfigurationImpl(private val sourcePackage: String) : GeneratorConfiguration {
	val targets = mutableListOf<Target>()
	
	override fun addTarget(target: Target) {
		require(targets.none { it.path == target.path }) { "Target path '${target.path}' already used" }
		
		targets.add(target)
	}
	
	override fun serverKotlin(path: String) {
		addTarget(ServerKotlinTarget(Path.of(path), sourcePackage))
	}
	
	override fun clientKotlin(path: String) {
		addTarget(ClientKotlinTarget(Path.of(path), sourcePackage))
	}
	
	override fun clientTypescript(path: String, srcPackage: String, genPackage: String, libPackage: String) {
		addTarget(ClientTypescriptTarget(Path.of(path), srcPackage, genPackage, libPackage))
	}
	
}

