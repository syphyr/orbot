# Installing + Running Screengrab 

First make sure that you have a valid version of `ruby` installed in your `$PATH`.

Then grab screengrab.
```bash
gem install screengrab 
```

Ensure that you have a working Google Pixel emulator that is running Android 33+ up. This can be
configured in Android Studio.

Ensure that `adb` that you use with Android Studio is also added to your path.

Screenshots can be configured in the `Screengrabfile` 

```ruby
locales([
'en-US', 'fr-CA', 'es-MX', 'de-DE', 'fa', 'ar', 'he'
])

# clear all previously generated screenshots in your local output directory before creating new ones
clear_previous_screenshots(true)
reinstall_app(true)
```

This `locales` method can be invoked with just one locale if you want to run screenshots on a specific 
locale. If you're doing this, it makes to call `clear_previous_screenshots(false)` instead of with `true`


In the root of Orbot, clean and build the main app as well as the test suite.
```bash
./gradlew clean
./graldew assembleDebug assembleAndroidTest
fastlane screengrab
```

Screengrab will ask you for the debug Orbot APK. There are a lot of options, but you need to consider
what's the hardware architecture of the emulator you are using to run Orbot. Don't select the 
androidTest APK here.

For my case, my emulated pixel is `arm64` so I select `3` here.
```
[21:38:39]: Select your debug app APK
1. app/build/outputs/apk/androidTest/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-debug-androidTest.apk
2. app/build/outputs/apk/androidTest/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-debug-androidTest.apk
3. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-arm64-v8a-debug.apk
4. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-armeabi-v7a-debug.apk
5. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-universal-debug.apk
6. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-x86-debug.apk
7. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-x86_64-debug.apk
8. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-arm64-v8a-debug.apk
9. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-armeabi-v7a-debug.apk
10. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-universal-debug.apk
11. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-x86-debug.apk
12. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-x86_64-debug.apk
13. appcore/build/outputs/apk/androidTest/debug/appcore-debug-androidTest.apk
14. orbotservice/build/outputs/apk/androidTest/debug/orbotservice-debug-androidTest.apk
?
```

Then you'll be given the same prompt asking for the test APK. I select `1`. 
```
1. app/build/outputs/apk/androidTest/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-debug-androidTest.apk
2. app/build/outputs/apk/androidTest/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-debug-androidTest.apk
3. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-arm64-v8a-debug.apk
4. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-armeabi-v7a-debug.apk
5. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-universal-debug.apk
6. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-x86-debug.apk
7. app/build/outputs/apk/fullperm/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-fullperm-x86_64-debug.apk
8. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-arm64-v8a-debug.apk
9. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-armeabi-v7a-debug.apk
10. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-universal-debug.apk
11. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-x86-debug.apk
12. app/build/outputs/apk/nightly/debug/Orbot-17.5.0-RC-1-tor-0.4.8.16-143-g50f29e8c-nightly-x86_64-debug.apk
13. appcore/build/outputs/apk/androidTest/debug/appcore-debug-androidTest.apk
14. orbotservice/build/outputs/apk/androidTest/debug/orbotservice-debug-androidTest.apk
```
