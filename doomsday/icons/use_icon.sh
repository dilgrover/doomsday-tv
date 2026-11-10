#!/bin/bash
# Makes icon option N the app's icon: its banner (the TV's apps row), square icon and adaptive
# icon layers go into the app; the manifest points at the adaptive icon. Usage: icons/use_icon.sh N
set -e
n=${1:?"Usage: icons/use_icon.sh N (an option-N folder made by make_icons.py)"}
here=$(cd "$(dirname "$0")" && pwd)
src="$here/option-$n"
res="$here/../src/main/res"
[ -d "$src" ] || { echo "No $src: run make_icons.py first"; exit 1; }
cp "$src/banner.png" "$res/drawable-nodpi/banner.png"
cp "$src/icon.png" "$res/drawable-nodpi/icon.png"
cp "$src/fg.png" "$res/drawable-nodpi/icon_fg.png"
cp "$src/bg.png" "$res/drawable-nodpi/icon_bg.png"
mkdir -p "$res/mipmap-anydpi-v26"
cat > "$res/mipmap-anydpi-v26/icon.xml" <<XML
<?xml version="1.0" encoding="utf-8"?>
<!-- The adaptive icon (Android 8+): option $n of icons/make_icons.py. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/icon_bg" />
    <foreground android:drawable="@drawable/icon_fg" />
</adaptive-icon>
XML
# A mipmap "icon" for older Androids too (the square picture), so @mipmap/icon always resolves.
mkdir -p "$res/mipmap-nodpi"
cp "$src/icon.png" "$res/mipmap-nodpi/icon.png"
sed -i 's|android:icon="@drawable/icon"|android:icon="@mipmap/icon"|' "$here/../src/main/AndroidManifest.xml"
echo "Option $n ($(cat "$src/name.txt")) is now the app's icon. Build the app to see it."
