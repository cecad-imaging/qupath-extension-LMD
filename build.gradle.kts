plugins {
    id("java")
}

group = "org.dgsob"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://maven.scijava.org/content/repositories/releases")
    maven("https://maven.scijava.org/content/repositories/snapshots")
    maven("https://maven.imagej.net/content/repositories/releases/")
}

dependencies {
    // https://mvnrepository.com/artifact/io.github.qupath/qupath-gui-fx
    implementation("io.github.qupath:qupath-gui-fx:0.4.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
    implementation("org.slf4j:slf4j-api:1.7.9")
    implementation("org.slf4j:slf4j-log4j12:2.0.7")
}

tasks.test {
    useJUnitPlatform()
}