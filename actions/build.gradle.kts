dependencies {
    implementation(project(":problem"))
    implementation(project(":policy"))
    implementation(project(":simplex"))
    implementation("com.google.inject:guice:4.2.1")
    implementation("org.synchronoss.cloud:nio-multipart-parser:1.0.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.10.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.10.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
    implementation("com.auth0:java-jwt:3.4.0")
    testImplementation("org.eclipse.jetty:jetty-server:9.4.27.v20200227")
    testImplementation("org.eclipse.jetty:jetty-servlet:9.4.27.v20200227")
    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    compileOnly("com.amazonaws:aws-lambda-java-core:1.2.0")
    compileOnly("org.xerial.snappy:snappy-java:1.1.4")
}

java {
    withJavadocJar()
}
