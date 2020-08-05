import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
	kotlin("jvm") version "1.3.60"
	kotlin("plugin.serialization") version "1.3.61"
	application
	`maven-publish`
	signing
}


group = "edu.duke.cs"
version = "0.2"

fun versionIdentifier(major: Int, minor: Int): String {
	return when {
		hasProperty("AZURE_BUILD_ID") && hasProperty("RELEASE") -> "$major.$minor." + property("AZURE_BUILD_ID")
		hasProperty("AZURE_BUILD_ID") -> "$major.$minor." + property("AZURE_BUILD_ID") + "-SNAPSHOT"
		hasProperty("RELEASE") -> "$major.$minor"
		else -> "$major.$minor-SNAPSHOT"
	}
}

version = versionIdentifier(0, 2)

repositories {
	jcenter()
}

java {
	withSourcesJar()
	withJavadocJar()
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

		// always update the build properties
		outputs.upToDateWhen { false }

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
		compression = Compression.GZIP
	}
}

distributions {

	get("main").apply {

		contents {

			// add extra documentation
			into("") { // project root
				from("LICENSE.txt")
			}

			// add progs
			into("progs") {
				from("progs")
			}
		}
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = "ospreyservice"
			from(components["java"])

			pom {
				name.set("OSPREY Service")
				description.set("Classes needed to access the Osprey webservice")
				url.set("https://github.com/donaldlab/osprey-service")
				licenses {
					license {
						name.set("GPL-2.0 License")
						url.set("https://www.gnu.org/licenses/old-licenses/gpl-2.0.html")
					}
				}
				developers {
					developer {
						id.set("jmartin")
						name.set("Jeff Martin")
						email.set("jmartin@cs.duke.edu")
					}
					developer {
						id.set("nguerin")
						name.set("Nate Guerin")
						email.set("nguerin@cs.duke.edu")
					}
				}
				scm {
					connection.set("scm:git:git://github.com/donaldlab/osprey-service.git")
					developerConnection.set("scm:git:ssh://github.com:donaldlab/osprey-service.git")
					url.set("https://github.com/donaldlab/osprey-service/tree/master")
				}
			}
		}
	}

	repositories {
		maven {
			// change URLs to point to your repos, e.g. http://my.org/repo
			name = "OSSRH"
			val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
			val snapshotsRepoUrl = uri( "https://oss.sonatype.org/content/repositories/snapshots")
			url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

			credentials {
				username = findProperty("ossrhUsername").toString()
				password = findProperty("ossrhPassword").toString()
			}
		}
	}
}

signing {
	val signingKey: String? by project
	val signingPassword: String? by project
	useInMemoryPgpKeys(signingKey, signingPassword)
	sign(publishing.publications["mavenJava"])
}
