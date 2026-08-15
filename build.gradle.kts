plugins {
    id("java")
    id("maven-publish")
}

repositories {
    mavenLocal()
    mavenCentral()
    // Upstream Mangolise libraries, only needed by the test server
    maven("https://maven.serble.net/snapshots/")
}

dependencies {
    implementation("net.minestom:minestom:2026.07.01-26.1.2")
    // Only the test server needs the game SDK, keeping it out of the published POM
    // means consumers don't have to add the Serble repository
    testImplementation("net.mangolise:mango-game-sdk:latest")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("net.mangolise:mango-combat:latest")
    testImplementation("dev.hollowcube:polar:1.16.0")
    // polar only declares fastutil at runtime, but javac needs it to read PolarLoader's signatures
    testCompileOnly("it.unimi.dsi:fastutil:8.5.19")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial"))
}

tasks.test {
    useJUnitPlatform()
    // The test source set currently only holds the manual test server, not JUnit tests
    failOnNoDiscoveredTests = false
}

/** Starts the manual test server from `src/test`, useful for trying checks against a real client. */
tasks.register<JavaExec>("runTestServer") {
    group = "application"
    description = "Runs the Minestom test server used to try out the checks."
    mainClass = "net.onthepixel.anticheat.Test"
    classpath = sourceSets.test.get().runtimeClasspath
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    // Minestom's API is only partly documented, so strict doclint would fail the build
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/OnThePixel-net/anticheat")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }

    }

    publications {
        create<MavenPublication>("maven") {
            artifactId = "onthepixel-anti-cheat"
            from(components["java"])
        }
    }

    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "OnThePixel Anti Cheat"
            description = "An open source anti cheat for Minestom servers, forked from Mangolise Anti Cheat."
            url = "https://github.com/OnThePixel-net/anticheat"

            licenses {
                license {
                    name = "MIT License"
                    url = "https://github.com/OnThePixel-net/anticheat/blob/main/LICENSE"
                }
            }

            scm {
                url = "https://github.com/OnThePixel-net/anticheat"
                connection = "scm:git:https://github.com/OnThePixel-net/anticheat.git"
                developerConnection = "scm:git:git@github.com:OnThePixel-net/anticheat.git"
            }
        }
    }
}