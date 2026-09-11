plugins { kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(21) }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}
tasks.test { useJUnitPlatform() }
