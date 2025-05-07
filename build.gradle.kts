plugins {
    id("java-library")
    id("java")
    id("maven-publish")
}

repositories {
    maven {
        name = "Verdox Reposilite"
        url = uri("https://repo.verdox.de/snapshots")
    }
    mavenCentral()
}

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api("de.verdox:vserializer:1.2.3-SNAPSHOT")

    compileOnly("org.jetbrains:annotations:26.0.1")
    api("commons-codec:commons-codec:1.15")
    api("com.google.code.gson:gson:2.10.1")
    api("commons-io:commons-io:2.14.0")
    api("com.google.guava:guava:32.0.1-android")
    api("org.redisson:redisson-all:3.20.0")
    api("org.mongodb:mongo-java-driver:3.12.12")
    api("org.mongodb:bson:4.9.0")
    api("org.jetbrains:annotations:24.0.1")
    api("mysql:mysql-connector-java:8.0.33")
    api("com.zaxxer:HikariCP:5.0.1")
    api("org.reflections:reflections:0.10.2")

    testImplementation("de.verdox:vserializer:1.2.3-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("com.github.kstyrc:embedded-redis:0.6")
}

tasks {
    compileJava {

        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(21)
    }


    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                groupId = "de.verdox"
                artifactId = "vpipeline"
                version = "1.0.4-SNAPSHOT"
                from(components["java"])
                url = "https://github.com/VerdoxLabs/VPipeline"
                licenses {
                    license {
                        name = "GNU GENERAL PUBLIC LICENSE Version 3"
                        url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                    }
                }
                developers {
                    developer {
                        id = "verdox"
                        name = "Lukas Jonsson"
                        email = "mail.ysp@web.de"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "verdox"
            url = uri("https://repo.verdox.de/snapshots")
            credentials {
                username = (findProperty("reposilite.verdox.user") ?: System.getenv("REPO_USER")).toString()
                password = (findProperty("reposilite.verdox.key") ?: System.getenv("REPO_PASSWORD")).toString()
            }
        }
    }
}
