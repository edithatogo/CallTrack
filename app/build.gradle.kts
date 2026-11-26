import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
}

val developmentRelease = providers.gradleProperty("calltrackDevelopmentRelease").orNull == "true"
val strictVersions = providers.gradleProperty("strictVersions").orNull == "true"

android {
    namespace = "com.markobl.calllogsync"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }
    buildFeatures { compose = true }

    defaultConfig {
        applicationId = "org.calltrack.community.dev"
        minSdk = 37
        targetSdk = 37
        versionCode = 14
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    if (!developmentRelease) {
        sourceSets.named("release") { res.directories.add("src/community/res") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    lint {
        abortOnError = true
        warningsAsErrors = true
        checkAllWarnings = true
        checkDependencies = true
        error += setOf("ComposeM2Api", "ComposeModifierMissing", "ComposeUnstableCollections")
        val versionChecks = setOf("NewerVersionAvailable", "GradleDependency")
        if (strictVersions) error += versionChecks else disable += versionChecks
    }
}

androidComponents {
    if (!developmentRelease) {
        onVariants(selector().withBuildType("release")) { variant ->
            variant.applicationId.set("io.github.edithatogo.calltrack")
            variant.outputs.forEach { output ->
                output.versionCode.set(1)
                output.versionName.set("0.1.0")
            }
        }
    }
}

val robolectricSdk = configurations.create("robolectricSdk") {
    isCanBeConsumed = false
    isTransitive = false
}
val prepareRobolectricSdk = tasks.register<Sync>("prepareRobolectricSdk") {
    from(robolectricSdk)
    into(layout.buildDirectory.dir("robolectric-sdk"))
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

val architectureSources = rootProject.fileTree(rootProject.projectDir) {
    include("**/*.kt", "**/*.java", "**/res/layout*/**/*.xml")
    exclude("**/.*/**", "**/build/**", "**/node_modules/**", "**/target/**")
}
tasks.withType<Test>().configureEach {
    dependsOn(prepareRobolectricSdk)
    // Robolectric loads Android's native graphics runtime in the test JVM.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", layout.buildDirectory.dir("robolectric-sdk").get().asFile)
    systemProperty("calltrack.repository", rootProject.projectDir)
    inputs.files(architectureSources).withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    robolectricSdk(libs.robolectric.sdk)
    constraints {
        ksp("com.google.guava:guava:33.8.0-jre")
        implementation(libs.androidx.lifecycle.runtime) {
            because("Keep the qualified Lifecycle family for Activity, Compose and WorkManager")
        }
        implementation(libs.androidx.tracing)
        implementation(libs.androidx.window)
        androidTestImplementation(libs.androidx.test.rules)
        androidTestImplementation("com.google.android.material:material:1.14.0") {
            because("The accessibility checker uses the supported Material Views release")
        }
        androidTestImplementation("com.google.guava:guava:33.8.0-android")
    }
    implementation(enforcedPlatform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.adaptive.navigation)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.hilt.compose)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.navigation)
    implementation(libs.androidx.navigation.runtime)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.window.core)
    implementation(libs.androidx.work)
    implementation(libs.hilt.android)
    implementation(libs.protobuf.lite)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)
    lintChecks(libs.slack.compose.lints)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.konsist)
    testImplementation(libs.javaparser)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.test.junit)
    androidTestImplementation(enforcedPlatform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.compose.test.junit)
    androidTestImplementation(libs.androidx.compose.test.accessibility)
    debugImplementation(libs.androidx.compose.test.manifest)
}

protobuf {
    protoc { artifact = libs.protoc.get().toString() }
    generateProtoTasks {
        all().configureEach { builtins { maybeCreate("java").option("lite") } }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        allWarningsAsErrors.set(true)
    }
}
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Werror"))
}

ksp {
    arg("room.schemaLocation", file("schemas").absolutePath)
}
