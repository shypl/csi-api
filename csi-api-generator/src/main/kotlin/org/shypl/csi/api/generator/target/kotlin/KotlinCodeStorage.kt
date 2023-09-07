package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.code.ClassCodeFile
import org.shypl.csi.api.generator.code.CodeStorage
import org.shypl.csi.api.generator.model.ClassName
import java.nio.file.Path

class KotlinCodeStorage(private val root: Path) : CodeStorage() {
	override fun defineClassFilePath(name: ClassName): Path {
		return root.resolve("${name.path.joinToString("/")}.kt")
	}
	
	override fun writeClassFileHeader(path: Path, file: ClassCodeFile, builder: StringBuilder) {
		super.writeClassFileHeader(path, file, builder)
		
		file.name.pkg.toString('.').also {
			if (it.isNotEmpty()) builder.append("package ${it}\n\n")
		}
		
		if (file.dependencies.isNotEmpty()) {
			file.dependencies
				.filter { file.name.pkg != it.name.pkg }
				.distinctBy { it.name.root }
				.sortedBy { it.name.root.path.joinToString(".") }
				.forEach {
					builder.append("import ${it.name.root.path.joinToString(".")} ")
					if (it.aliases.isNotEmpty()) {
						builder.append(" as ${it.aliases.first()}")
					}
					builder.append("\n")
				}
			builder.append("\n")
		}
	}
}