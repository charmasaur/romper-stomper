# Romper Android app

Information about Gradle: https://gradle.org/gradle-download/

To do a release:
 - Update the versions in `build.gradle`
 - `gradle assembleRelease`
 - `sh sign_and_zipalign.sh` (you'll need the appropriate keystore for this)
 - Upload `build/outputs/apk/android-release.apk` to the Google Play Store
