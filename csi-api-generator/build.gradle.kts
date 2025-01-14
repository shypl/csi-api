plugins {
	kotlin("jvm")
	id("java-library")
	id("maven-publish")
}

dependencies {
	implementation(libs.shypl.tool.lang)
	implementation(libs.shypl.tool.utils)
	implementation(kotlin("reflect"))
	implementation("org.reflections:reflections:0.10.2")
	implementation("ch.qos.logback:logback-classic:1.5.16")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
}