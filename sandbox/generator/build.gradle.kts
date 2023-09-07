plugins {
	kotlin("jvm")
}

dependencies {
	implementation(project(":sandbox:api"))
	implementation(project(":csi-api-generator"))
}