plugins {
    id("java")
}

group = "org.cecad.lmd"
version = "0.2.0"

repositories {
    mavenCentral()
    maven("https://maven.scijava.org/content/repositories/releases")
    maven("https://maven.scijava.org/content/repositories/snapshots")
    maven("https://maven.imagej.net/content/repositories/releases/")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // https://mvnrepository.com/artifact/io.github.qupath/qupath-gui-fx
    implementation("io.github.qupath:qupath-gui-fx:0.5.0")
    implementation("io.github.qupath:qupath-fxtras:0.1.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
    implementation("org.slf4j:slf4j-api:1.7.9")
    implementation("org.slf4j:slf4j-log4j12:2.0.7")
}

tasks.test {
    useJUnitPlatform()
}