plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(21)
    jvm()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    sourceSets {
        commonMain {
            kotlin.srcDirs("src/main/kotlin")
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            }
        }
        jvmMain {
            kotlin.srcDirs("src/jvmMain/kotlin")
        }
        commonTest {
            kotlin.srcDirs("src/test/kotlin")
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
