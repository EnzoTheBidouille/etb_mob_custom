plugins {
    application
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // This dependency is used by the application.
    implementation(libs.guava)

    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "etb_mob_custom.App"
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.test {
    isEnabled = false
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "etb_mob_custom.CustomMobLoader"
    }

    from (
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )

    from("app/src/main/resources") {
        include("plugin.yml")
        into("")
    }
}