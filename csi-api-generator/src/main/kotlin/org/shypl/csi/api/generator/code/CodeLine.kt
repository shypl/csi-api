package org.shypl.csi.api.generator.code

class CodeLine(private val content: String) : CodeStatement {
	override fun write(writer: StringBuilder) {
		writer.append(content).append('\n')
	}
}