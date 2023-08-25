plugins {
    id("java")
}

group = "org.dgsob"
version = "0.0.9-5"

repositories {
    mavenCentral()
    maven("https://maven.scijava.org/content/repositories/releases")
    maven("https://maven.scijava.org/content/repositories/snapshots")
}

dependencies {
    val qupathVersion = "0.4.3" // For now
    // https://mvnrepository.com/artifact/io.github.qupath/qupath-gui-fx
    implementation("io.github.qupath:qupath-gui-fx:$qupathVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

}

tasks.test {
    useJUnitPlatform()
}