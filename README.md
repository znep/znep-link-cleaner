# CleanLink Android Project

CleanLink is a lightweight, privacy-focused Android utility that removes tracking parameters from URLs and expands shortened redirect links.

## Building the Project

### Prerequisites
- JDK 17 or higher
- Android SDK (API 34/35)

### Command Line
Run the included Gradle wrapper from the project root:
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Android Studio
1. Open Android Studio.
2. Select **File > Open...** and choose this project directory.
3. Allow Gradle to sync dependencies and click **Run** or **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
