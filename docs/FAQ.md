# Questions and problems

**"The site didn't let the TV download it (403)"**
Some sites refuse downloads from apps. Put the picture somewhere you control (Google Drive or
Dropbox, [PICTURES.md](PICTURES.md)) and use that link.

**"That isn't a picture"**
The link opens a web page, not the picture. Drive links must be the
`uc?export=download&id=...` form; Dropbox links must end in `dl=1`. A Drive file must be shared
as *Anyone with the link*, or the TV gets Google's sign-in page.

**Download worked but the picture looks soft**
Use a bigger picture: 1920×1080 or 4K.

**There's no keyboard when I type a link**
Select the link box and press **OK**: the app's own keyboard opens. It doesn't need the TV's
keyboard, so it works the same on Google TV and Fire TV.

**Where's the screensaver on my Fire TV?**
Fire TV doesn't let other apps be its screensaver. *Preview* in Settings still shows it.

**Can I turn the ticking off?**
*Settings → Ticking sound → Off*. It never ticks from 11 pm to 7 am anyway.

**My pictures don't get glowing eyes**
Those effects are placed by hand on a few recognised pictures. Every picture still gets the
light, mist, dust, the camera move, the energy jumps and Summon.

**Does it work without internet?**
Yes, once your pictures are downloaded. The weather just hides.

**What happens on 16 December 2026?**
The heading changes to "Avengers: Doomsday is here".

**Is my data sent anywhere?**
No. The only network use is downloading your own picture links, the weather and town search from
Open-Meteo, and (with City on Automatic) finding your town from your connection with ipwho.is or
ipapi.co, once a day. Choose a town yourself and that lookup stops.

**The weather shows the wrong town**
Automatic goes by your internet connection, which can be a nearby big town (or the VPN's).
Settings → City → Search for a town, and pick yours.

**"App blocked to protect your device" (Google Play Protect)**
In some countries (such as India, Brazil, Singapore and Thailand) Play Protect blocks any
downloaded app that includes an accessibility service, whatever the app does. Only the **-full**
download has one (the Google TV Home-button helper). Install the standard download,
`doomsday-tv-vX.apk` without "-full", and it installs normally.

**Can Doomsday be my home screen?**
On boxes that let you pick a home app (Nvidia Shield and many others): yes with either download,
through Settings → Home button → Offer as home app. On Google TV, only with the **-full**
download (Settings → Home button → Open Accessibility), which Play Protect blocks in some
countries. On Fire TV, Amazon may block it. Details: [SETTINGS.md](SETTINGS.md#the-home-button).
