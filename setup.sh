#!/bin/bash

# إنشاء المجلدات
mkdir -p .github/workflows
mkdir -p app/src/main/assets
mkdir -p app/src/main/java/com/abualzahra/parent
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/xml
mkdir -p gradle

# إنشاء الملفات فارغة
touch .github/workflows/android-build.yml
touch app/src/main/assets/index.html
touch app/src/main/java/com/abualzahra/parent/MainActivity.kt
touch app/src/main/java/com/abualzahra/parent/WebAppInterface.kt
touch app/src/main/res/layout/activity_main.xml
touch app/src/main/res/values/strings.xml
touch app/src/main/res/values/styles.xml
touch app/src/main/res/xml/network_security_config.xml
touch app/src/main/AndroidManifest.xml
touch app/build.gradle
touch build.gradle
touch settings.gradle
touch gradle.properties
