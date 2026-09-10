name: Build Nova Android APK

on:
  push:
    branches:
      - main
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build APK
        run: |
          chmod +x gradlew
          ./gradlew :app:assembleDebug \
            -Pandroid.useAndroidX=true \
            -Pandroid.enableJetifier=true

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: Nova-AI-APK
          path: app/build/outputs/apk/debug/app-debug.apk
