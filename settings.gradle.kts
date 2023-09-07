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

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("shypl-tool-lang", "org.shypl.tool:tool-lang:1.0.0-SNAPSHOT")
			library("shypl-tool-utils", "org.shypl.tool:tool-utils:1.0.0-SNAPSHOT")
			library("shypl-tool-logging", "org.shypl.tool:tool-logging:1.0.0-SNAPSHOT")
			library("shypl-tool-io", "org.shypl.tool:tool-io:1.0.0-SNAPSHOT")
			library("shypl-tool-biser", "org.shypl.tool:tool-biser:1.0.0-SNAPSHOT")
			
			version("csi-core", "1.0.0-SNAPSHOT")
			library("shypl-csi-core-common", "org.shypl.csi", "csi-core-common").versionRef("csi-core")
			library("shypl-csi-core-client", "org.shypl.csi", "csi-core-client").versionRef("csi-core")
			library("shypl-csi-core-server", "org.shypl.csi", "csi-core-server").versionRef("csi-core")
			
			version("csi-transport", "1.0.0-SNAPSHOT")
			library("shypl-csi-transport-common", "org.shypl.csi", "csi-transport-common").versionRef("csi-transport")
			library("shypl-csi-transport-client", "org.shypl.csi", "csi-transport-client").versionRef("csi-transport")
			library("shypl-csi-transport-server", "org.shypl.csi", "csi-transport-server").versionRef("csi-transport")
			
			library("coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
		}
	}
}
