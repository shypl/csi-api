@file:Suppress("unused")

package org.shypl.csi.api

import org.shypl.tool.lang.toHexString

fun StringBuilder.log(value: Boolean): StringBuilder = append(value)
fun StringBuilder.log(value: Byte): StringBuilder = append(value)
fun StringBuilder.log(value: Int): StringBuilder = append(value)
fun StringBuilder.log(value: Double): StringBuilder = append(value)
fun StringBuilder.log(value: Long): StringBuilder = append(value)
fun StringBuilder.log(value: String): StringBuilder = append(value)
fun StringBuilder.log(value: BooleanArray) = value.joinTo(this, SEP, "[", "]")
fun StringBuilder.log(value: ByteArray) = value.toHexString(this)
fun StringBuilder.log(value: IntArray) = value.joinTo(this, SEP, "[", "]")
fun StringBuilder.log(value: DoubleArray) = value.joinTo(this, SEP, "[", "]")
fun StringBuilder.log(value: LongArray) = value.joinTo(this, SEP, "[", "]")

fun StringBuilder.log(arg: String, value: Boolean) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: Byte) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: Int) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: Double) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: Long) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: String) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: BooleanArray) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: ByteArray) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: IntArray) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: DoubleArray) = logPrefix(arg).log(value)
fun StringBuilder.log(arg: String, value: LongArray) = logPrefix(arg).log(value)

fun StringBuilder.logS(value: Boolean): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: Byte): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: Int): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: Double): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: Long): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: String): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: BooleanArray): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: ByteArray): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: IntArray): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: DoubleArray): StringBuilder = log(value).append(SEP)
fun StringBuilder.logS(value: LongArray): StringBuilder = log(value).append(SEP)

fun StringBuilder.logS(arg: String, value: Boolean): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: Byte): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: Int): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: Double): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: Long): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: String): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: BooleanArray): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: ByteArray): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: IntArray): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: DoubleArray): StringBuilder = logPrefix(arg).log(value).append(SEP)
fun StringBuilder.logS(arg: String, value: LongArray): StringBuilder = logPrefix(arg).log(value).append(SEP)

fun <T> StringBuilder.log(arg: String, value: Array<T>, log: StringBuilder.(T) -> Unit) = logPrefix(arg).log(value, log)
fun <T> StringBuilder.logS(arg: String, value: Array<T>, log: StringBuilder.(T) -> Unit): StringBuilder = log(arg, value, log).append(SEP)

fun <T> StringBuilder.log(arg: String, value: List<T>, log: StringBuilder.(T) -> Unit) = logPrefix(arg).log(value, log)
fun <T> StringBuilder.logS(arg: String, value: List<T>, log: StringBuilder.(T) -> Unit): StringBuilder = log(arg, value, log).append(SEP)

fun <T> StringBuilder.log(value: Array<T>, log: StringBuilder.(T) -> Unit): StringBuilder {
	append('[')
	var s = false
	value.forEach {
		if (s) append(SEP) else s = true
		log(it)
	}
	append(']')
	return this
}

fun <T> StringBuilder.log(value: List<T>, log: StringBuilder.(T) -> Unit): StringBuilder {
	append('[')
	var s = false
	value.forEach {
		if (s) append(SEP) else s = true
		log(it)
	}
	append(']')
	return this
}

fun <K, V> StringBuilder.log(value: Map<K, V>, logK: StringBuilder.(K) -> Unit, logV: StringBuilder.(V) -> Unit): StringBuilder {
	append('[')
	var s = false
	value.forEach {
		if (s) append(SEP) else s = true
		logK(it.key)
		append(COM)
		logV(it.value)
	}
	append(']')
	return this
}

fun <T> StringBuilder.logS(value: List<T>, log: StringBuilder.(T) -> Unit): StringBuilder = log(value, log).append(SEP)

fun <T> StringBuilder.log(arg: String, value: T, log: StringBuilder.(T) -> Unit) = logPrefix(arg).log(value, log)
fun <T> StringBuilder.logS(arg: String, value: T, log: StringBuilder.(T) -> Unit): StringBuilder = log(arg, value, log).append(SEP)

fun <T> StringBuilder.log(value: T, log: StringBuilder.(T) -> Unit) = apply { log(value) }
fun <T> StringBuilder.logS(value: T, log: StringBuilder.(T) -> Unit): StringBuilder = log(value, log).append(SEP)


private const val SEP = ", "
private const val COM = ": "
private fun StringBuilder.logPrefix(arg: String) = append(arg).append(COM)