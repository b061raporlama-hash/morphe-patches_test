import org.gradle.api.tasks.JavaExec

group = "app.morphe"

patches {
    about {
        name = "Cep Patches"
        description = "Independent Morphe patches derivative with Watch later shortcuts"
        source = "https://github.com/b061raporlama-hash/morphe-patches_test"
        author = "b061raporlama-hash; upstream Morphe contributors"
        contact = "na"
        website = "https://github.com/b061raporlama-hash/morphe-patches_test"
        license = "GNU General Public License v3.0, with additional GPL section 7 requirements"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")
val jamApk = providers.gradleProperty("jamApk")
val jamOutput = providers.gradleProperty("jamOutput")

dependencies {
    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    implementation(libs.morphe.patches.library)

    patchListGeneratorClasspath(libs.gson)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

tasks {
    test {
        useJUnitPlatform()
        jamApk.orNull?.let { systemProperty("jamApk", it) }
    }

    register<JavaExec>("checkStringResources") {
        description = "Checks resource strings for invalid formatting"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.morphe.patches.util.resource.CheckStringResourcesKt")
    }

    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("app.morphe.util.PatchListGeneratorKt")
    }

    register<JavaExec>("validateJam") {
        description = "Patches a supplied YouTube Music APK for Jam validation and optional device testing"
        group = "verification"

        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("app.morphe.patches.music.interaction.jam.JamDeviceBuildKt")

        doFirst {
            val apk = jamApk.orNull
                ?: throw GradleException("validateJam requires -PjamApk=/absolute/path/to/ytm.apk")
            setArgs(listOfNotNull(apk, jamOutput.orNull))
        }
    }
    register<JavaExec>("validateCepCoexistence") {
        group = "verification"
        description = "Runs production YouTube clone and GmsCore manifest identity code on an installable fixture"
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("app.morphe.patches.youtube.CepCoexistenceBuildKt")
        doFirst {
            setArgs(listOf(
                providers.gradleProperty("cepFixtureApk").get(),
                providers.gradleProperty("cepFixtureOutput").get()
            ))
        }
    }
    register<JavaExec>("validateWatchLater") {
        group = "verification"
        description = "Applies Watch later and validates context-copy hooks against a supplied real YouTube APK"
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("app.morphe.patches.youtube.WatchLaterDeviceBuildKt")
        doFirst {
            setArgs(listOfNotNull(providers.gradleProperty("youtubeApk").orNull
                ?: throw GradleException("Pass -PyoutubeApk=/path/to/original.apk"),
                providers.gradleProperty("youtubeOutput").orNull))
        }
    }
    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
