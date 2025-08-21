package org.shypl.csi.api.generator.model

import org.shypl.tool.utils.collections.getOrAdd
import org.shypl.tool.utils.collections.mutableKeyedSetOf
import java.util.*

@Suppress("EqualsOrHashCode")
abstract class ClassPath<P : ClassPath<P>>(
	val parent: P?,
	val value: String,
) {
	private val _children = mutableKeyedSetOf<String, P> { it.value }
	
	val children: Collection<P>
		get() = _children
	
	fun getChild(value: String): P {
		val i = value.indexOf('.')
		if (i == -1) {
			return _children.getOrAdd(value) { createChild(value) }
		}
		return getChild(value.substring(0, i)).getChild(value.substring(i + 1))
	}
	
	abstract fun createChild(value: String): P
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ClassPath<*>) return false
		if (parent != other.parent) return false
		if (value != other.value) return false
		return true
	}
	
	val fullValue: String get() = toString('_')
	
	open fun toString(separator: Char): String {
		return if (parent == null) value
		else parent.toString(separator) + separator + value
	}
	
	override fun toString(): String {
		return toString('.')
	}
}

open class ClassPackage(
	parent: ClassPackage?,
	value: String,
) : ClassPath<ClassPackage>(parent, value) {
	
	val root: ClassPackage
		get() = parent?.root ?: this
	
	val path: List<String> by lazy {
		if (parent == null) emptyList()
		else if (parent.parent == null) listOf(value)
		else parent.path + value
	}
	
	private val _names = mutableKeyedSetOf<String, ClassName> { it.value }
	
	fun getName(value: String): ClassName {
		var i = value.indexOf('/')
		if (i == -1) {
			i = value.indexOf('.')
			if (i == -1) {
				return _names.getOrAdd(value) { ClassName(null, value, this) }
			}
			return getName(value.substring(0, i)).getChild(value.substring(i + 1))
		}
		return getChild(value.substring(0, i)).getName(value.substring(i + 1))
	}
	
	override fun createChild(value: String): ClassPackage {
		return ClassPackage(this, value)
	}
	
	override fun equals(other: Any?): Boolean {
		return super.equals(other) && other is ClassPackage
	}
	
	override fun hashCode(): Int {
		return Objects.hash(parent, value)
	}
	
	override fun toString(separator: Char): String {
		return if (parent == null) value
		else if (parent.parent == null) value
		else parent.toString(separator) + separator + value
	}
}

class RootClassPackage : ClassPackage(null, "<root>") {
	override fun createChild(value: String): ClassPackage {
		return ClassPackage(this, value)
	}
}


class ClassName(
	parent: ClassName?,
	value: String,
	val pkg: ClassPackage,
) : ClassPath<ClassName>(parent, value) {
	
	val full: String
		get() = "$pkg/$this"
	
	val root: ClassName
		get() = parent?.root ?: this
	
	val rootPackage: ClassPackage
		get() = pkg.root
	
	val path: List<String> by lazy {
		if (parent == null) pkg.path + listOf(value) else parent.path + value
	}
	
	fun toStringFull(separator: Char): String {
		return path.joinToString(separator.toString())
	}
	
	override fun createChild(value: String): ClassName {
		return ClassName(this, value, pkg)
	}
	
	override fun equals(other: Any?): Boolean {
		return super.equals(other) && other is ClassName && pkg == other.pkg
	}
	
	override fun hashCode(): Int {
		return Objects.hash(parent, value, pkg)
	}
}