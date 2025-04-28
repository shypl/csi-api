import org.shypl.csi.api.generator.generateCsiApi

fun main() {
	generateCsiApi("sandbox.api", "sandbox/api/csi.yml") {
		
		serverKotlin("sandbox/api/server/src/main/kotlin")
		clientKotlin("sandbox/api/client/src/main/kotlin")
		
		clientTypescript(
			"sandbox/api/client/src/main/typescript",
			"sandbox",
			"app.server.generated",
			"lib"
		)
	}
}