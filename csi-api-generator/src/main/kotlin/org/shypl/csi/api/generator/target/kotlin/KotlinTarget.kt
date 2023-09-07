package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.model.ClassPackage
import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.target.Target
import java.nio.file.Path

abstract class KotlinTarget(path: Path, private val pkg: String) : Target(path) {
	override fun generate(model: Model) {
		val targetPackage = model.rootPackage.getChild(this.pkg)
		
		val codersGenerator = KotlinCodersGenerator(model, targetPackage)
		val apiGenerator = createApiGenerator(model, targetPackage, codersGenerator)
		
		val target = path.resolve(targetPackage.toString('/')).toFile()
		
		val codeSource = KotlinCodeStorage(path)
		
		apiGenerator.generate(codeSource)
		codersGenerator.generate(codeSource)
		
		val files = codeSource.saveNewFiles()
		
		target.walkBottomUp().forEach {
			if (it.isDirectory) {
				if (it.listFiles()?.size == 0) it.delete()
			}
			else if (!files.contains(it.toPath())) {
				it.delete()
			}
		}
	}
	
	protected abstract fun createApiGenerator(model: Model, targetPackage: ClassPackage, codersGenerator: KotlinCodersGenerator): KotlinApiGenerator
	
}

