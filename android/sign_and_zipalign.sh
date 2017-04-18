#!/bin/bash
cp build/outputs/apk/android-release-unsigned.apk build/outputs/apk/android-release-signed-unaligned.apk
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ~/android-release-key.keystore build/outputs/apk/android-release-signed-unaligned.apk android-release-key
rm build/outputs/apk/android-release.apk
~/Downloads/android-sdk-linux/build-tools/23.0.3/zipalign -v 4 build/outputs/apk/android-release-signed-unaligned.apk build/outputs/apk/android-release.apk
