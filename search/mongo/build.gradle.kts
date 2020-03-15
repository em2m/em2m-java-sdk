dependencies {
    api(project(":search:core"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
    implementation("de.undercouch:bson4jackson:2.7.0")
    implementation("org.mongodb:mongodb-driver:3.6.4")
    implementation("org.mongodb:mongodb-driver-async:3.6.4")
    implementation("org.mongodb:mongodb-driver-rx:1.5.0")
    testImplementation("ch.qos.logback:logback-classic:1.1.3")
}
