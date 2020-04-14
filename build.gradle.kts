plugins {
    kotlin("jvm") version "1.3.70" apply false
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

subprojects {

    apply(plugin = "kotlin")
    apply(plugin = "java-library")

    group = "io.em2m.sdk-java"
    version = "3.0-SNAPSHOT"

    repositories {
        jcenter()
    }

    java {
        withJavadocJar()
    }

    dependencies {
        "implementation"("org.slf4j:slf4j-api:1.7.30")
        "implementation"("io.reactivex:rxjava:1.3.8")
        "implementation"("io.reactivex:rxkotlin:1.0.0")
        "implementation"("com.typesafe:config:1.3.3")
        "testImplementation"("org.apache.logging.log4j:log4j-slf4j-impl:2.13.1")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test:1.3.70")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test-junit:1.3.70")
        "testImplementation"("junit:junit:4.12")
        "compileOnly"("org.jetbrains.kotlin:kotlin-stdlib:1.3.70")
        "compileOnly"("org.jetbrains.kotlin:kotlin-reflect:1.3.70")
    }

}
