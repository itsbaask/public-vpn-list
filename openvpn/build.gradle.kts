import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider

/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

fun obtainTestBuildType(): String {
    var result = "debug";

    if (project.hasProperty("testBuildType")) {
        result = project.property("testBuildType").toString()
    }
    return result
}

android {
    buildFeatures {
        aidl = true
        buildConfig = true
    }
    namespace = "de.blinkt.openvpn"
    compileSdk = 35
    //compileSdkPreview = "UpsideDownCake"

    // Also update runcoverity.sh
    // ndkVersion = "30.0.14904198"

    defaultConfig {
        minSdk = 23
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
        externalNativeBuild {
            cmake {
                //arguments+= "-DCMAKE_VERBOSE_MAKEFILE=1"
            }
        }
    }

    /*
    /*
    externalNativeBuild {
        cmake {
            path = file("${projectDir}/src/main/cpp/CMakeLists.txt")
        }
    }
    */
    */

    sourceSets {
        getByName("main") {
            assets.directories.add("build/ovpnassets")
            jniLibs.srcDirs("src/main/jniLibs")
        }

        create("corvusui") {
            java.srcDirs("src/corvusui/java")
            kotlin.srcDirs("src/corvusui/java")
            res.srcDirs("src/corvusui/res")
            aidl.srcDirs("src/corvusui/aidl")
        }

        create("skeleton") {}

        getByName("debug") {}

        getByName("release") {}
    }

    signingConfigs {
        create("release") {
            // ~/.gradle/gradle.properties
            val keystoreFile: String? = project.findProperty("keystoreFile")?.toString()
            storeFile = keystoreFile?.let { file(it) }
            storePassword = project.findProperty("keystorePassword")?.toString()
            keyPassword = project.findProperty("keystoreAliasPassword")?.toString()
            keyAlias = project.findProperty("keystoreAlias")?.toString()
            enableV1Signing = true
            enableV2Signing = true
        }

        create("releaseOvpn2") {
            // ~/.gradle/gradle.properties
            val keystoreO2File: String? = project.findProperty("keystoreO2File")?.toString()
            storeFile = keystoreO2File?.let { file(it) }
            val keystoreO2Password: String? = project.findProperty("keystoreO2Password")?.toString()
            storePassword = keystoreO2Password
            val keystoreO2AliasPassword: String? = project.findProperty("keystoreO2AliasPassword")?.toString()
            keyPassword = keystoreO2AliasPassword
            val keystoreO2Alias: String? = project.findProperty("keystoreO2Alias")?.toString()
            keyAlias = keystoreO2Alias
            enableV1Signing = true
            enableV2Signing = true
        }

    }

    lint {
        enable += setOf(
            "BackButton",
            "EasterEgg",
            "StopShip",
            "IconExpectedSize",
            "GradleDynamicVersion",
            "NewerVersionAvailable"
        )
        checkOnly += setOf("ImpliedQuantity", "MissingQuantity")
        disable += setOf("MissingTranslation", "UnsafeNativeCodeLocation")
    }


    flavorDimensions += listOf("implementation", "ovpnimpl")

    productFlavors {
        create("corvusui") {
            dimension = "implementation"
        }

        create("skeleton") {
            dimension = "implementation"
        }

        create("ovpn23") {
            dimension = "ovpnimpl"
            buildConfigField("boolean", "openvpn3", "true")
        }

        create("ovpn2") {
            dimension = "ovpnimpl"
            buildConfigField("boolean", "openvpn3", "false")
        }
    }

    buildTypes {
        getByName("release") {
            if (project.hasProperty("icsopenvpnDebugSign")) {
                logger.warn("property icsopenvpnDebugSign set, using debug signing for release")
                signingConfig = android.signingConfigs.getByName("debug")
            } else {
                productFlavors["ovpn23"].signingConfig = signingConfigs.getByName("release")
                productFlavors["ovpn2"].signingConfig = signingConfigs.getByName("releaseOvpn2")
            }
        }
    }

    testBuildType = obtainTestBuildType()

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

}

var swigcmd = "swig"
// Workaround for macOS(arm64) and macOS(intel) since it otherwise does not find swig and
// I cannot get the Exec task to respect the PATH environment :(
if (file("/opt/homebrew/bin/swig").exists()) swigcmd = "/opt/homebrew/bin/swig"
else if (file("/usr/local/bin/swig").exists()) swigcmd = "/usr/local/bin/swig"


abstract class GenerateSwigTask : Exec() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
}

fun registerGenSwigTask(variantName: String, variantDirName: String): TaskProvider<GenerateSwigTask> {
    val baseDir = layout.buildDirectory.dir("generated/source/ovpn3swig/${variantDirName}")

    val genTask = tasks.register<GenerateSwigTask>("generateOpenVPN3Swig${variantName}") {
        val genDir = baseDir.get().asFile.resolve("net/openvpn/ovpn3")
        outputDir.set(baseDir)

        doFirst {
            mkdir(genDir)
        }
        commandLine(
            listOf(
                swigcmd,
                "-outdir",
                genDir.absolutePath,
                "-outcurrentdir",
                "-c++",
                "-java",
                "-package",
                "net.openvpn.ovpn3",
                "-Isrc/main/cpp/openvpn3/client",
                "-Isrc/main/cpp/openvpn3/",
                "-DOPENVPN_PLATFORM_ANDROID",
                "-o",
                "${genDir}/ovpncli_wrap.cxx",
                "-oh",
                "${genDir}/ovpncli_wrap.h",
                "src/main/cpp/openvpn3/client/ovpncli.i"
            )
        )
        inputs.files("src/main/cpp/openvpn3/client/ovpncli.i")

    }
    return genTask
}

androidComponents {
    onVariants(selector().all()) { variant ->
        if (variant.name.contains("ovpn23", ignoreCase = true)) {
            val execTask = registerGenSwigTask(variant.name, variant.name.replace("-", "/"))
            variant.sources.java?.addGeneratedSourceDirectory(execTask, GenerateSwigTask::outputDir)
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.view.material)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation(libs.androidx.preference.ktx)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.androidx.security.crypto)
    implementation(libs.mpandroidchart)
    implementation(libs.square.okhttp)

    testImplementation(libs.junit)
}
