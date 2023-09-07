package org.shypl.csi.api.generator.target.typescript

import org.shypl.csi.api.generator.code.ClassCodeFile
import org.shypl.csi.api.generator.code.CodeStorage
import org.shypl.csi.api.generator.model.ClassName
import org.shypl.csi.api.generator.model.ClassPackage
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class TypescriptCodeSource(
	private val root: Path,
	genPackage: ClassPackage,
	srcPackage: String
) : CodeStorage() {
	val genPath: Path = root.resolve(genPackage.toString('/'))
	
	private val srcPackageCut = srcPackage.replace('.', '/') + '/'
	
	override fun defineClassFilePath(name: ClassName): Path {
		val path = "${name.pkg.toString('/')}/${name.toString('_')}.ts"
		
		if (path.startsWith(srcPackageCut)) {
			return genPath.resolve(path.substring(srcPackageCut.length))
		}
		
		return root.resolve(path)
	}
	
	override fun writeClassFileHeader(path: Path, file: ClassCodeFile, builder: StringBuilder) {
		super.writeClassFileHeader(path, file, builder)
		
		if (file.dependencies.isNotEmpty()) {
			
			file.dependencies
				.filter { it.name != file.name }
				.sortedBy { it.name.full }
				.forEach {
					val aliases = it.aliases.toMutableList()
					val dependencyName = it.name
					if (aliases.isEmpty()) {
						aliases.add(dependencyName.toString('_'))
					}
					aliases.sort()
					
					val dir = path.parent
					val dependencyPath = defineClassFilePath(it.name)
					val dependencyDir = dependencyPath.parent
					var relativePath = dir.relativize(dependencyDir).toString()
					
					val name = dependencyPath.nameWithoutExtension
					
					relativePath = when {
						relativePath.isEmpty()      -> "./$name"
						relativePath.first() != '.' -> "./$relativePath/$name"
						relativePath == ".."        -> "../$name"
						else                        -> "$relativePath/$name"
					}
					
					builder.append("import {${aliases.joinToString(", ")}} from '$relativePath'").append('\n')
				}
			builder.append("\n")
		}
	}
}