import org.shypl.csi.api.generator.generateCsiApi

fun main() {
	generateCsiApi("sandbox.api", "sandbox/api/model.yml") {
		
		serverKotlin("sandbox/api/server/src/main/kotlin")
		clientKotlin("sandbox/api/client/src/main/kotlin")
		
		clientTypescript(
			"/Users/shnyaka/workspace/projects/shypl/cocos/cocos-lib/src",
			"sandbox",
			"test-csi-api",
			"main"
		)
	}
}