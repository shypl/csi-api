plugins {
	kotlin("jvm")
	id("java-library")
	id("maven-publish")
}

dependencies {
	implementation(libs.shypl.tool.lang)
	implementation(libs.shypl.tool.utils)
	implementation(kotlin("reflect"))
	implementation(libs.reflections)
	implementation(libs.logback)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.jackson.dataformat.yaml)
}