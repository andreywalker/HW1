plugins {
    kotlin("jvm") version "2.3.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
}

application {
    mainClass = "MainKt"
}