import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    groovy
    kotlin("jvm") version "1.2.51"
    id("org.asciidoctor.convert") version "1.5.3"
}

group = "nchain"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    implementation("com.madgag.spongycastle", "core", "1.58.0.0")
    implementation("com.lambdaworks", "scrypt", "1.4.0")
    testCompile("junit", "junit", "4.12")
    testCompile("org.codehaus.groovy", "groovy", "2.4.15")
    testCompile("org.spockframework", "spock-core", "1.1-groovy-2.4")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}