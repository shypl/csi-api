plugins {
	kotlin("jvm")
}

dependencies {
	implementation(project(":runtime:csi-api-client"))
	implementation(project(":sandbox:api"))
}