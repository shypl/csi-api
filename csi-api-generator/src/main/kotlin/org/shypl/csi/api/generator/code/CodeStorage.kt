package org.shypl.csi.api.generator.code

import org.shypl.csi.api.generator.model.ClassName
import org.shypl.tool.utils.collections.mutableKeyedSetOf
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

abstract class CodeStorage {
	
	private val files = mutableKeyedSetOf(ClassCodeFile::name)
	
	fun newFile(name: ClassName): ClassCodeFile {
		require(!files.containsKey(name))
		return ClassCodeFile(name).also(files::add)
	}
	
	fun saveNewFiles(): Collection<Path> {
		val list = files.map(::saveFile)
		files.clear()
		return list
	}
	
	private fun saveFile(file: ClassCodeFile): Path {
		val path = defineClassFilePath(file.name)
		val builder = StringBuilder()
		
		writeClassFileHeader(path, file, builder)
		writeClassFileBody(path, file, builder)
		
		path.parent.createDirectories()
		path.writeText(builder.toString())
		
		return path
	}
	
	protected abstract fun defineClassFilePath(name: ClassName): Path
	
	protected open fun writeClassFileHeader(path: Path, file: ClassCodeFile, builder: StringBuilder) {
		file.header.write(builder)
	}
	
	protected open fun writeClassFileBody(path: Path, file: ClassCodeFile, builder: StringBuilder) {
		file.body.write(builder)
	}
}