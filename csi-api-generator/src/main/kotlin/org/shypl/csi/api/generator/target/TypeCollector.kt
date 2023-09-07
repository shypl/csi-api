package org.shypl.csi.api.generator.target

import org.shypl.csi.api.generator.model.Type

interface TypeCollector {
	fun add(type: Type)
}