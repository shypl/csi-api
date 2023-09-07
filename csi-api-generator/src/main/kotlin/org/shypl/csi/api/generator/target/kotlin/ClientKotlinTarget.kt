package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.model.ClassPackage
import org.shypl.csi.api.generator.model.Model
import java.nio.file.Path

class ClientKotlinTarget(path: Path, pkg: String) : KotlinTarget(path, pkg) {
	override fun createApiGenerator(model: Model, targetPackage: ClassPackage, codersGenerator: KotlinCodersGenerator): KotlinApiGenerator {
		return ClientKotlinApiGenerator(model, codersGenerator, targetPackage)
	}
}