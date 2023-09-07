plugins {
	kotlin("jvm")
	id("java-library")
	id("maven-publish")
}

dependencies {
	api(project(":runtime:csi-api-common"))
	api(libs.shypl.csi.core.server)
}