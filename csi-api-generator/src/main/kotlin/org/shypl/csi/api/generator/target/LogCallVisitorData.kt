package org.shypl.csi.api.generator.target

import org.shypl.csi.api.generator.code.Code

class LogCallVisitorData(
	val loggers: TypeCollector,
	val code: Code,
	val argName: String?,
	val argVal: String,
	val sep: Boolean
)