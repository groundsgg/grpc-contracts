plugins {
    id("java-library")
    id("maven-publish")
}

subprojects {
    group = "gg.grounds"

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    // Pin every module to JVM 21 so the published artifacts stay resolvable by
    // Paper plugins (Paper 1.21.x runs on Java 21). Without this the proto-only
    // modules inherit CI's newer build JDK (25/26) and Gradle tags them as
    // "JVM 25+", which makes a JVM-21 plugin consumer refuse to resolve them.
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    sourceSets {
        main {
            resources {
                srcDir("src/main/proto")
            }
        }
    }

    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/groundsgg/${rootProject.name}")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }

        publications {
            create<MavenPublication>("java") {
                from(components["java"])
                artifactId = rootProject.name + "-" + project.name
            }
        }
    }
}
