package org.shypl.csi.api

internal enum class ApiMessageType(val code: Byte) {
	METHOD_CALL(1),
	METHOD_RESPONSE(2),
	INSTANCE_METHOD_CALL(3),
	INSTANCE_CLOSE(4),
	SUBSCRIPTION_CALL(5),
	SUBSCRIPTION_CLOSE(6)
}