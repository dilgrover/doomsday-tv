# Building from source

## You need

- **JDK 17**
- **Android SDK** with platform 35 (Android Studio installs it; or the command-line tools)
- The Gradle wrapper in this repo downloads Gradle 8.10.2 itself.

## Build

```sh
./gradlew assembleDebug          # doomsday/build/outputs/apk/debug/doomsday-debug.apk
./gradlew testDebugUnitTest      # unit tests (Robolectric renders the screens off the TV)
```

If Gradle can't find the SDK, create `local.properties` with `sdk.dir=/path/to/android-sdk`.

The screenshots in `docs/images` come from `ShotsTest` (`./gradlew testDebugUnitTest`, written
to `doomsday/build/shots/`).

## Install your build

```sh
adb connect <tv-ip-address>
adb install -r doomsday/build/outputs/apk/debug/doomsday-debug.apk
```

## A release build with your own key

Debug builds are signed with Android's debug key. For a release, make your own key once and
**keep it safe**: every update must be signed with the same key, or it won't install over the
last one.

```sh
keytool -genkeypair -v -keystore release.jks -alias doomsday -keyalg RSA -keysize 4096 -validity 10000
```

Then create `keystore.properties` in the project folder (it's in `.gitignore`; never commit it):

```properties
storeFile=release.jks
storePassword=...
keyAlias=doomsday
keyPassword=...
```

and run `./gradlew assembleRelease` (output: `doomsday/build/outputs/apk/release/`).

## The code

- `doomsday/`: the app (`com.avanhq.doomsday`): the countdown screen, Pictures with its
  keyboard, Settings, the apps row, the weather and the screensaver (`DoomDream`).
- `doomcore/`: the look and the life of the screen (package `com.avanhq.tv`): `Ui` and the
  bundled fonts, `FramePace` (a steady 30 frames a second), `RollingClock`, `DoomCounter`
  (the countdown), `DoomTick` (the tick, made in code: no sound files) and `DoomFx` (the
  pictures' light, mist, energy and Summon).

Everything is drawn for a 1920×1080 screen and scaled to the TV.
