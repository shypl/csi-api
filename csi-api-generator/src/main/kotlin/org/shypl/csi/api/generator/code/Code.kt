package org.shypl.csi.api.generator.code

import java.util.*

class Code(private val ident: Int, private val file: CodeFile) : CodeStatement, DependedCode {
	private val statements = LinkedList<CodeStatement>()
	
	override fun addDependency(dependency: CodeDependency) {
		file.addDependency(dependency)
	}
	
	override fun addDependency(name: String, vararg aliases: String) {
		file.addDependency(name, *aliases)
	}
	
	fun line(v: String) {
		append(CodeLine("\t".repeat(ident) + v))
	}
	
	fun line() {
		line("")
	}
	
	inline fun line(v: StringBuilder.() -> Unit) {
		line(StringBuilder().apply(v).toString())
	}
	
	fun <T : CodeStatement> prepend(statement: T): T {
		statements.addFirst(statement)
		return statement
	}
	
	fun <T : CodeStatement> append(statement: T): T {
		statements.addLast(statement)
		return statement
	}
	
	fun ident(): Code {
		return append(Code(ident + 1, file))
	}
	
	inline fun ident(block: Code.() -> Unit) {
		ident().block()
	}
	
	fun identLine(v: String) {
		append(CodeLine("\t".repeat(ident + 1) + v))
	}
	
	fun identBracketsCurly(line: String): Code {
		line("$line{")
		val block = ident()
		line("}")
		return block
	}
	
	inline fun identBracketsCurly(line: String, block: Code.() -> Unit) {
		identBracketsCurly(line).block()
	}
	
	fun identBracketsRound(line: String): Code {
		line("$line(")
		val block = ident()
		line(")")
		return block
	}
	
	inline fun identBracketsRound(line: String, block: Code.() -> Unit) {
		identBracketsRound(line).block()
	}
	
	override fun write(writer: StringBuilder) {
		statements.forEach { it.write(writer) }
	}
}