plugins {
    id("application")
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

application {
    mainClass.set("de.haw.Main")
}

group = "de.haw"
version = "develop"

repositories {
    mavenCentral()
    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/11.3/cdt-11.3.1/plugins")
        metadataSources {
            artifact()
        }

        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")

    implementation( "org.jetbrains.kotlin:kotlin-stdlib" )

    val cpgVersion = "8.3.0"
    implementation("de.fraunhofer.aisec", "cpg-core", cpgVersion)
    implementation("de.fraunhofer.aisec", "cpg-language-java", cpgVersion)
    implementation("de.fraunhofer.aisec", "cpg-language-cxx", cpgVersion)
    implementation("de.fraunhofer.aisec", "cpg-neo4j", cpgVersion)

    val graphStreamVersion = "2.0"
    // https://mvnrepository.com/artifact/org.graphstream
    implementation("org.graphstream", "gs-core", graphStreamVersion)
    implementation("org.graphstream", "gs-algo", graphStreamVersion)
    implementation("org.graphstream", "gs-ui-swing", graphStreamVersion)

    // https://mvnrepository.com/artifact/com.opencsv/opencsv
    implementation("com.opencsv:opencsv:5.9")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.17.0")
    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.16.1")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.2.13")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:1.7.32")

    // https://mvnrepository.com/artifact/org.projectlombok/lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "de.haw.Main"
}

tasks.shadowJar {
    isZip64 = true
    archiveClassifier.set("")
}