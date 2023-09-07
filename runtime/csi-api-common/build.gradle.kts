plugins {
	kotlin("jvm")
	id("java-library")
	id("maven-publish")
}

dependencies {
	implementation(libs.shypl.tool.lang)
	implementation(libs.shypl.tool.logging)
	implementation(libs.shypl.csi.core.common)
	
	api(libs.shypl.tool.biser)
	api(libs.shypl.tool.utils)
	api(libs.shypl.tool.io)
	api(libs.coroutines)
}