plugins {
    id("java")
    id("maven-publish")
}

var versionStr = System.getenv("GIT_COMMIT") ?: "dev"

group = "net.mangolise"
version = versionStr

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.serble.net/snapshots/")
}

dependencies {
    implementation("net.mangolise:mango-game-sdk:latest")
    implementation("net.minestom:minestom:2025.07.10b-1.21.7")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.mangolise:mango-combat:latest")
    testImplementation("dev.hollowcube:polar:1.14.5")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "serbleMaven"
            url = uri("https://maven.serble.net/snapshots/")
            credentials {
                username = System.getenv("SERBLE_REPO_USERNAME") ?: ""
                password = System.getenv("SERBLE_REPO_PASSWORD") ?: ""
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    if (project.name != "connector-commons") {
        publications {
            create<MavenPublication>("mavenGitCommit") {
                groupId = "net.mangolise"
                artifactId = "mango-anti-cheat"
                version = versionStr
                from(components["java"])
            }

            create<MavenPublication>("mavenLatest") {
                groupId = "net.mangolise"
                artifactId = "mango-anti-cheat"
                version = "latest"
                from(components["java"])
            }
        }
    }
}