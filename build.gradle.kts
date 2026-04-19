plugins {
    kotlin("jvm") version "2.3.10"
    idea
}

group = "net.trilleo"
version = "0.1.0"

idea {
    module {
        isDownloadSources = true
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}

tasks.processResources {
    filesMatching("*.yml") {
        expand("projectVersion" to project.version)
    }
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.jar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "spigot"
    }
}

tasks.register<Copy>("copyPlugin") {
    dependsOn("jar")
    from(tasks.jar.get().archiveFile)
    into("E:\\Minecraft Dev\\Plugins Dev\\Test Paper Server\\plugins")
}

tasks.register<JavaExec>("startServer") {
    dependsOn("copyPlugin")
    workingDir("E:\\Minecraft Dev\\Plugins Dev\\Test Paper Server")
    classpath("E:\\Minecraft Dev\\Plugins Dev\\Test Paper Server\\paper.jar")
}