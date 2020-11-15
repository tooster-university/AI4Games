import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "1.4.10"
    application //to run JVM part
    kotlin("plugin.serialization") version "1.4.10"
}
group = "me.tooster"
version = "1.0-SNAPSHOT"
val serializationVersion = "1.0.0-RC"
val ktorVersion = "1.4.0"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/nwillc/maven")
}
dependencies {
    testImplementation(kotlin("test-junit5"))
    implementation("com.github.nwillc:ksvg:3.0.0") // drawing SVG
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "13"
}

kotlin {
    jvm {
        withJava()
    }
    js {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:1.2.3")
                implementation("io.ktor:ktor-websockets:$ktorVersion")
            }
        }

        val jsMain by getting {
            dependencies {
            }
        }
    }

}

application {
    mainClassName = "lander.ServerKt"
}

//val compileKotlin: KotlinCompile by tasks
//compileKotlin.kotlinOptions {
//}