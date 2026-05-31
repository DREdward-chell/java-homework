plugins {
    id("java")
}

group = "com.edwards"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform(libs.test.junit.bom))
    testImplementation(libs.test.junit.jupiter)
    testImplementation(libs.test.h2)
    testRuntimeOnly(libs.test.runtime.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}