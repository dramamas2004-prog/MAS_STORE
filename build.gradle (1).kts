plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
  namespace = "com.mas.store"
  compileSdk = 34
  defaultConfig { applicationId = "com.mas.store"; minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0" }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
  kotlinOptions { jvmTarget = "17" }
}
dependencies {
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.browser:browser:1.8.0")
}
