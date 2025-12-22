plugins {
    id("java-library")
    id("maven-publish")
}

subprojects {
    group = "gg.grounds"

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

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
                url = uri("https://maven.pkg.github.com/groundsgg/grpc-contracts")
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
