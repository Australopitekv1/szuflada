/**
 * Spike Fazy 0 (ryzyko #1): czy lazysodium (JVM/Android) produkuje bajt w bajt
 * te same wyniki co libsodium-wrappers (TS/wasm)?
 *
 * Konsumuje kanoniczne wektory: packages/protocol/test-vectors/crypto-vectors.json
 * Uruchomienie: gradle test
 */
plugins {
    kotlin("jvm") version "2.1.20"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("com.goterl:lazysodium-java:5.1.4")
    testImplementation("net.java.dev.jna:jna:5.14.0")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}
