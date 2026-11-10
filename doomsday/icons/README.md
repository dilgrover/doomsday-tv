# The app's icon

Original art drawn in code (no film images): `make_icons.py` draws each option into
`option-N/` (banner 320×180 for the TV's apps row, square icon 512, adaptive icon layers 432),
and `sheet.py OUT.png` puts them side by side as they'd look in Google TV's row.

In use: option 5, "Glowing eyes": two green eye slits in the dark, the gold
DOOMSDAY wordmark on the banner. The option folders aren't kept: to change the icon, run
`python3 make_icons.py` (it draws all of them again), then `./use_icon.sh N`, then build.
Needs Python with Pillow and numpy.
