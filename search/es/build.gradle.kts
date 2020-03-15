dependencies {
    implementation(project(":search:core"))
    implementation("io.github.openfeign:feign-core:9.4.0")
    implementation("io.github.openfeign:feign-jackson:9.4.0")
    implementation("io.github.openfeign:feign-slf4j:9.4.0")
    testImplementation("org.apache.lucene:lucene-queryparser:5.5.0")
    testImplementation("org.apache.lucene:lucene-analyzers-common:5.5.0")
}

java {
    withJavadocJar()
}
