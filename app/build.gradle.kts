plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.xdearboy.bookkeeper"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xdearboy.bookkeeper"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Схема базы данных Room
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    
    // SwipeRefreshLayout для обновления по свайпу
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // ViewPager2 для онбординга
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // Room для локальной базы данных
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    // Retrofit для работы с API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Gson для работы с JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Glide для загрузки изображений
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // CircleImageView для круглых изображений профиля
    implementation("de.hdodenhof:circleimageview:3.1.0")
    
    // SharedPreferences для хранения настроек
    implementation("androidx.preference:preference:1.2.1")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    
    // Play Services Auth для Firebase Auth
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}