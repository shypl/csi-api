package org.shypl.csi.api.generator.target

import org.shypl.csi.api.generator.model.Model
import java.nio.file.Path

abstract class Target(
	val path: Path
) {
	abstract fun generate(model: Model)
}