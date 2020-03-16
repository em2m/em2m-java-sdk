dependencies {
    api(project(":simplex"))
    api(project(":geo"))
    api(project(":utils"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
    implementation("org.apache.lucene:lucene-analyzers-common:5.3.1")
    implementation("org.apache.lucene:lucene-queryparser:5.3.1")
    implementation("joda-time:joda-time:2.9.3")
    testImplementation("com.nhaarman:mockito-kotlin:1.1.0")
}
