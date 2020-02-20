import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
	kotlin("jvm") version "1.3.60"
	kotlin("plugin.serialization") version "1.3.61"
	application
}


group = "edu.duke.cs"
version = "0.1"

repositories {
	jcenter()
}

dependencies {
	
	implementation(kotlin("stdlib-jdk8"))
	implementation("ch.qos.logback:logback-classic:1.2.3")

	val ktorVersion = "1.3.0"
	api("io.ktor:ktor-server-netty:$ktorVersion")
	implementation("io.ktor:ktor-serialization:$ktorVersion")

	testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.0")
}

configure<JavaPluginConvention> {
	sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {

	kotlinOptions {

		jvmTarget = "1.8"

		// enable experimental features so we can use the fancy ktor stuff
		freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}


tasks {

	// tell gradle to write down the version number where the app can read it
	processResources {

		from(sourceSets["main"].resources.srcDirs) {
			include("**/build.properties")
			expand(
				"version" to "$version"
			)
		}
	}
}

application {
	mainClassName = "edu.duke.cs.ospreyservice.MainKt"
}

tasks {

	// turn off the "main" distribution
	distZip {
		enabled = false
	}
	distTar {
		enabled = false
	}

	// turn off tar distributions
	for (task in this) {
		if (task.name.endsWith("DistTar")) {
			task.enabled = false
		}
	}
}

