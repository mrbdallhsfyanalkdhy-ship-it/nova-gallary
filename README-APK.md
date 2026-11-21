# NOVA GALLERY — Android APK Build Guide (Capacitor)

This kit includes native Android MediaStore integration (`MediaLibraryPlugin.java`) and Gallery System Intent Filters (`AndroidManifest.xml`) so NOVA GALLERY appears alongside all system gallery apps and automatically loads all phone photos and videos once permitted!

Option A — Local Build (Android Studio):
1. npm install && npm run build
2. npm install @capacitor/core @capacitor/cli @capacitor/android
3. npx cap add android
4. cp android-native/AndroidManifest.xml android/app/src/main/AndroidManifest.xml
   cp android-native/MainActivity.java android/app/src/main/java/com/novagallery/app/MainActivity.java
   cp android-native/MediaLibraryPlugin.java android/app/src/main/java/com/novagallery/app/MediaLibraryPlugin.java
5. npx cap sync android && cd android && ./gradlew assembleDebug

Option B — Cloud Build (GitHub Actions without Android Studio):
Push the project + this kit (including `.github/workflows/build-apk.yml`) to GitHub and download the compiled `app-debug.apk` artifact directly from the Actions tab.
