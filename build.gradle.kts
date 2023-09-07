plugins {
	kotlin("jvm") version "1.9.20" apply false
	id("nebula.release") version "17.2.2"
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
