plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "csi-api"

include(
	"runtime:csi-api-common",
	"runtime:csi-api-client",
	"runtime:csi-api-server",
	
	"csi-api-generator",
	
	"sandbox:api",
	"sandbox:api:client",
	"sandbox:api:server",
	"sandbox:generator",
	"sandbox:app",
)