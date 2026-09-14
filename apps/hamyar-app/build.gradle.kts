plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// مقدارهای Appwrite از ریشه‌ی پروژه می‌آیند (local.properties / متغیر محیطی / ‑P).
// اگر projectId خالی باشد، اپ در «حالت محلی» بالا می‌آید و چیزی به سرور نمی‌فرستد.
val appwriteEndpoint = findProperty("resolvedAppwriteEndpoint") as? String
    ?: "https://fra.cloud.appwrite.io/v1"
// پیش‌فرض سخت‌کد‌شده (پروژه‌ی «همیار من» در Appwrite Cloud — fra)؛ با local.properties قابل override.
val appwriteProjectId = findProperty("resolvedAppwriteProjectId") as? String ?: "6a9d59e3002751cc3ea8"
val appwriteDatabaseId = findProperty("resolvedAppwriteDatabaseId") as? String ?: "main_db"

android {
    namespace = "com.hamyareman.ir"

    // امضای دیباگِ ثابت (keystore در ریشه‌ی ریپو؛ PKCS12) — تا APK هر ران CI
    // امضای یکسان داشته باشد و نصبِ روی نسخه‌ی قبلی همیشه «آپدیت» بماند.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeType = "pkcs12"
        }
    }

    compileSdk = 37

    defaultConfig {
        applicationId = "com.hamyareman.ir"   // Platform ثبت‌شده در کنسول Appwrite
        minSdk = 26
        targetSdk = 36
        versionCode = 27
        versionName = "1.26"
        buildConfigField("String", "APPWRITE_ENDPOINT", "\"$appwriteEndpoint\"")
        buildConfigField("String", "APPWRITE_PROJECT_ID", "\"$appwriteProjectId\"")
        // برای مانیفستِ ادغام‌شده (اگر نسخه‌ای از SDK اسکیم را با ${appwriteProjectId} بخواهد)
        manifestPlaceholders["appwriteProjectId"] = appwriteProjectId
        buildConfigField("String", "APPWRITE_PROJECT_NAME", "\"همیار من\"")
        buildConfigField("String", "APPWRITE_DATABASE_ID", "\"$appwriteDatabaseId\"")
        // شناسه‌ی OAuth Client «وب» گوگل — برای Credential Manager (لیست اکانت‌های اندروید).
        val googleWebClientId = (findProperty("googleWebClientId") as? String)?.trim().orEmpty()
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        debug {
            // بدون applicationIdSuffix: هر اپ فقط یک «Platform» در کنسول Appwrite لازم دارد.
            versionNameSuffix = "-debug"
        }
        release {
            // تا زمانی که R8 با کتابخانه‌های WebRTC/Appwrite تست نشده، minify خاموش است.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    lint {
        // پروژه آگاهانه از APIهای unstable پخش media3 استفاده می‌کند (پلیر/سشن)؛
        // هشدارهای UnstableApi را به‌جای ۷ خطای لینت، یک‌جا بی‌اثر می‌کنیم.
        disable += "UnsafeOptInUsageError"
    }
}
// AGP 9: پلاگین Kotlin Android حذف شده (Kotlin داخلی است)؛
// گزینه‌های کامپایلر از `android.kotlinOptions` به این بلوک منتقل شده‌اند.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    // بیومتریک: androidx.biometric برای API<28 دیالوگ سازگاریِ AppCompat دارد، پس
    // تم اکتیویتی باید از Theme.AppCompat باشد و appcompat هم روی classpath باشد.
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.appcompat)
    // ورود native گوگل — Credential Manager + play-services provider
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(project(":core-common"))
    implementation(project(":core-designsystem"))
    implementation(project(":core-appwrite"))
    implementation(project(":core-security"))
    implementation(project(":core-notifications"))
    implementation(project(":feature-pairing"))
    implementation(project(":feature-hearttoheart"))
    implementation(project(":feature-calls"))
    implementation(project(":feature-playback"))
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(project(":feature-study"))
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    implementation(project(":core-sync"))
}
