import java.util.*

plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
    id("com.jfrog.bintray")
}

android {
    compileSdkVersion(30)
    defaultConfig {
        minSdkVersion(26)
        targetSdkVersion(30)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api("org.openvoipalliance:AndroidPhoneLib:0.6.26")
    api("com.google.firebase:firebase-messaging-ktx:21.0.1")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.10")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.2.1")
    implementation("com.takwolf.android:foreback:0.1.1")
    implementation("com.tomash:androidcontacts:1.14.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.2.0")
    implementation("org.koin:koin-android:2.2.0")

    testImplementation("junit:junit:4.+")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

val libraryVersion = "0.0.20"

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

publishing {
    publications {
        create<MavenPublication>("Production") {
            artifact("$buildDir/outputs/aar/pil-release.aar")
            groupId = "org.openvoipalliance"
            artifactId = "AndroidPlatformIntegration"
            version = libraryVersion
            artifact(sourcesJar.get())

            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")

                configurations.implementation.allDependencies.forEach {
                    if (it.name != "unspecified") {
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", it.group)
                        dependencyNode.appendNode("artifactId", it.name)
                        dependencyNode.appendNode("version", it.version)
                    }
                }
            }
        }
    }
}

fun findProperty(s: String) = project.findProperty(s) as String?

bintray {
    user = findProperty("bintray.user")
    key = findProperty("bintray.token")
    setPublications("Production")
    pkg(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
        repo = "AndroidPlatformIntegration"
        name = "AndroidPlatformIntegration"
        websiteUrl = "https://github.com/open-voip-alliance/AndroidPlatformIntegration"
        vcsUrl = "https://github.com/open-voip-alliance/AndroidPlatformIntegration"
        githubRepo = "open-voip-alliance/AndroidPlatformIntegration"
        description = "Integrating VoIP into the Android platform.."
        setLabels("kotlin")
        setLicenses("Apache-2.0")
        publish = true
        desc = description
        version(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.VersionConfig> {
            name = libraryVersion
            released = Date().toString()
        })
    })
}
