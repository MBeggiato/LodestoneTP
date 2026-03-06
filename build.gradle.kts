plugins {
    java
}

group = "io.github.marcel"
version = "1.1.1"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

// Deploy jar to server/plugins
tasks.register<Copy>("deployToServer") {
    from(tasks.jar)
    into(layout.projectDirectory.dir("server/plugins"))
}

tasks.build {
    finalizedBy("deployToServer")
}
