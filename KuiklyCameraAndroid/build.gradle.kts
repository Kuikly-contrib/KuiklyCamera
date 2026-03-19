plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

// 从 Gradle 参数读取发布配置
val mavenVersion: String by project
val groupId: String? by project
val mavenRepoUrl: String? by project
val mavenUsername: String? by project
val mavenPassword: String? by project

group = groupId ?: "com.tencent.kuiklybase"
version = mavenVersion

android {
    namespace = "com.tencent.kuiklybase.android"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Kuikly Android Render SDK
    implementation("com.tencent.kuikly-open:core-render-android:${Version.getKuiklyVersion()}")

    // CameraX 依赖
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // AndroidX
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = "kuikly-camera-android"
                version = project.version.toString()
            }
        }
        repositories {
            maven {
                url = uri(mavenRepoUrl ?: "https://mirrors.tencent.com/repository/maven/kuikly-open/")
                credentials {
                    username = mavenUsername ?: ""
                    password = mavenPassword ?: ""
                }
            }
        }
    }
}
