package org.shypl.csi.api.generator.target.typescript

import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.target.Target
import java.nio.file.Path

class ClientTypescriptTarget(
	path: Path,
	private val srcPackage: String,
	private val genPackage: String,
	private val libPackage: String,
) : Target(path) {
	override fun generate(model: Model) {
		val libPackage = model.rootPackage.getChild(this.libPackage)
		val genPackage = model.rootPackage.getChild(this.genPackage)
		
		val codersGenerator = TypescriptCodersGenerator(model, libPackage, genPackage)
		val apiGenerator = ClientTypescriptApiGenerator(model, codersGenerator, libPackage, genPackage)
		
		val codeSource = TypescriptCodeSource(path, genPackage, srcPackage)
		
		apiGenerator.generate(codeSource)
		codersGenerator.generate(codeSource)
		
		val files = codeSource.saveNewFiles()
		
		codeSource.genPath.toFile().walkBottomUp().forEach {
			if (it.isDirectory) {
				if (it.listFiles()?.size == 0) it.delete()
			} else if (!files.contains(it.toPath())) {
				if (it.extension == "meta") {
					if (!it.resolveSibling(it.nameWithoutExtension).exists()) it.delete()
				} else {
					it.delete()
				}
			}
		}
	}
}