import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20"
}

val versionOverride = project.findProperty("versionOverride") as? String
version = versionOverride ?: "local-SNAPSHOT"

repositories {
    mavenCentral()
}

val grpcVersion = "1.81.0"
// 2.21.4+ for Options.Builder.tokenSupplier (sends the bearer in the
// `auth_token` CONNECT field, re-read per reconnect). Pinned to 2.25.3 to
// match service-nats-authz's jnats so client + auth-callout server agree.
val natsVersion = "2.25.3"

dependencies {
    api("io.grpc:grpc-stub:$grpcVersion")
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.nats:jnats:$natsVersion")
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Target JVM 21 (LTS). CI runs JDK 25, locally devs may have anything 21+;
// pinning the bytecode target keeps the artefact portable.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
}
