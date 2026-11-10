# Setting up

## What you need

- A **Google TV / Android TV** device (Android 9 or newer), or an **Amazon Fire TV Stick**
  (Fire OS 7 or newer).
- The app's APK, `doomsday.apk`, from this repo's Releases page (or built yourself:
  [BUILDING.md](BUILDING.md)).

## Google TV and Android TV

1. **Install Downloader** (by AFTVnews) from the Play Store on the TV.
2. **Allow installing from Downloader**:
   - *Settings → System → About*, select *Android TV OS build* seven times until it says
     you're a developer.
   - *Settings → Apps → Security & restrictions → Unknown sources* → turn on **Downloader**.
3. **Install**: open Downloader, enter the APK's link, press Go, then Install.
4. **Open it**: *Doomsday* is in your apps. You can move it to your favourites row.

## Amazon Fire TV Stick

1. **Install Downloader** from the Amazon Appstore.
2. **Allow it**: *Settings → My Fire TV → Developer options → Install unknown apps* → turn on
   **Downloader**. If there's no *Developer options*: *My Fire TV → About*, select your device's
   name seven times, then go back.
3. **Install** the APK through Downloader, then open **Doomsday** from *Your apps & channels*.

Fire TV doesn't allow other apps as its screensaver, so on a Fire TV the Screensaver setting
only offers a preview.

## With a computer (adb)

1. On the TV, turn on developer options (as above) and *USB / Network debugging*.
2. `adb connect <tv-ip-address>` (Google TV may show a pairing code: `adb pair <ip>:<port>`).
3. `adb install doomsday.apk` (to update, `adb install -r doomsday.apk`).

## First run

The app opens on the countdown over its own drawn hall. Press **Pictures** to add your
pictures ([PICTURES.md](PICTURES.md)), and **Settings** for the rest ([SETTINGS.md](SETTINGS.md)).

## The screensaver (Google TV / Android TV)

In the app: *Settings → Screensaver → Open TV settings*, then choose **Doomsday** as the
screen saver (on Google TV it's under *Settings → System → Ambient mode* or *Screen saver*,
depending on the device). *Preview* shows it without changing anything.
