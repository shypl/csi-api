plugins {
	kotlin("jvm")
}

dependencies {
	implementation(project(":runtime:csi-api-server"))
	implementation(project(":sandbox:api"))
}