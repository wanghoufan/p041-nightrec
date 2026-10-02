import java.util.Properties
plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.compose); alias(libs.plugins.ksp) }
val local = Properties().apply { rootProject.file("local.properties").inputStream().use { load(it) } }
android {
 namespace = "com.nightrec.app"
 compileSdk = 36
 defaultConfig { applicationId = "com.nightrec.app"; minSdk = 29; targetSdk = 36; versionCode = 1; versionName = "1.0.0-debug"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildFeatures { compose = true; buildConfig = true }
 buildTypes {
  debug { buildConfigField("String", "AUDD_TOKEN", "\"" + (System.getenv("AUDD_API_TOKEN") ?: local.getProperty("AUDD_API_TOKEN", "")).replace("\\", "\\\\").replace("\"", "\\\"") + "\""); buildConfigField("int", "AUDD_LIMIT", local.getProperty("AUDD_REQUEST_LIMIT", "300")) }
  release { buildConfigField("String", "AUDD_TOKEN", "\"\""); buildConfigField("int", "AUDD_LIMIT", "0") }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 testOptions { animationsDisabled = true }
 lint { abortOnError = true }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation(platform(libs.compose.bom))
 implementation("androidx.activity:activity-compose:1.12.4")
 implementation("androidx.compose.material3:material3")
 implementation(libs.compose.material.icons.core)
 implementation("androidx.compose.ui:ui-tooling-preview")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.core:core-splashscreen:1.2.0")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
 implementation(libs.room.runtime); ksp(libs.room.compiler)
 implementation(libs.media3.exoplayer); implementation(libs.media3.session)
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("androidx.work:work-runtime-ktx:2.11.1")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("org.tensorflow:tensorflow-lite:2.17.0")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation(platform(libs.compose.bom))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
 androidTestImplementation("androidx.room:room-testing:2.8.5")
}
