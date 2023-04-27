plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    targetHierarchy.default()

    jvm {
        compilations.configureEach {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        nodejs()
    }

    // Still experimental
    // Blocked by coroutines (issue: https://github.com/Kotlin/kotlinx.coroutines/issues/3713),
    // Okio (issue: https://github.com/square/okio/issues/1203), and Kotest (depends on coroutines)
    // wasm()

    macosX64()
    macosArm64()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    watchosArm32()
    watchosArm64()
    watchosX64()
    // Blocked by coroutines (expected in 1.7.0), Okio (issue: https://github.com/square/okio/issues/1242), and Kotest (depends on coroutines)
    // watchosDeviceArm64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    linuxX64()
    // Blocked by coroutines (expected in 1.7.0), Okio (issue: https://github.com/square/okio/issues/1242), and Kotest (depends on coroutines)
    // linuxArm64()

    mingwX64()

    // Blocked by coroutines (expected in 1.7.0), Okio (issue: https://github.com/square/okio/issues/1242), and Kotest (depends on coroutines)
    // androidNativeArm32()
    // androidNativeArm64()
    // androidNativeX86()
    // androidNativeX64()

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kache"))
                implementation(libs.okio)

                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions)

                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}