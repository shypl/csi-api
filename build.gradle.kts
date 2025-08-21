plugins {
	kotlin("jvm") version "2.2.10" apply false
	id("nebula.release") version "19.0.10"
}

subprojects {
	afterEvaluate {
		
		extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()?.apply {
			jvmToolchain(21)
			
			repositories {
				mavenCentral()
				maven("https://maven.pkg.github.com/shypl/packages").credentials {
					username = ""
					password = project.property("shypl.gpr.key") as String
				}
			}
		}
		
		extensions.findByType<PublishingExtension>()?.apply {
			group = "org.shypl.csi"
			
			configure<JavaPluginExtension> {
				withSourcesJar()
			}
			publications.create<MavenPublication>("Library") {
				from(components["java"])
			}
			repositories.maven("https://maven.pkg.github.com/shypl/packages").credentials {
				username = project.property("shypl.gpr.user") as String
				password = project.property("shypl.gpr.key") as String
			}
			rootProject.tasks["release"].apply {
				dependsOn(tasks["assemble"])
				finalizedBy(tasks["publish"])
			}
		}
	}
}
