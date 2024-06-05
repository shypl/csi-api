plugins {
	kotlin("jvm") version "2.0.0" apply false
	id("nebula.release") version "19.0.8"
}

subprojects {
	afterEvaluate {
		
		extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()?.apply {
			jvmToolchain(17)
			
			repositories {
				mavenCentral()
				mavenLocal()
			}
		}
		
		extensions.findByType<PublishingExtension>()?.apply {
			group = "org.shypl.csi"
			
			publications.create<MavenPublication>("Library") {
				from(components["java"])
			}
			configure<JavaPluginExtension> {
				withSourcesJar()
			}
		}
	}
}
