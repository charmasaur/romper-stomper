#!/bin/bash
/home/harry/Downloads/android-sdk-linux/build-tools/33.0.2/apksigner sign -v --ks /home/harry/android-release-key.keystore --out build/outputs/apk/release/android-release.apk build/outputs/apk/release/android-release-unsigned.apk
