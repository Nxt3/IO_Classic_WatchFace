v2.5.1 / August 28, 2017
=========================
* Fixed bug where manually toggling night mode wouldn't happen instantly if you had disabled the second hand

v2.5 / August 17, 2017
=========================
* Added a setting for smooth second animation ticks (sweeping animation)
* Fixed bug where if you disabled the second hand, the display would continue refreshing once a second anyways. Now, if you hide the second hand, the display will only update once a minute! Embarrassing I never noticed this until today!

v2.4 / July 26, 2017
=========================
* Support RANGED complication types (there is no progress bar around the complication since I felt it looked out of place)
* Improved internal logic for supported complication types

v2.3 / July 26, 2017
=========================
* Use Android's auto-backup to backup Settings

v2.2 / July 9, 2017
=========================
* Added "About" page in the settings to show the current version and allow the user to open this changelog on your phone

v2.1 / June 29, 2017 (PRODUCTION: v2.0 - 2.1)
=========================
* Added a setting for manually toggling night mode by tapping the center of the watch face

v2.0.2 / June 27, 2017 (BETA)
=========================
* Fixed issue with night mode not triggering under certain time ranges

v2.0.1 / June 27, 2017 (BETA)
=========================
* Fixed a crash when resetting night mode colors

v2.0 / June 27, 2017 (BETA)
=========================
* Night mode settings!
    * Choose the time night mode is active and the colors for the mode as well!
    * Reset all night mode colors to their defaults with the new reset option
* Optimized code and improved readability drastically

v1.9.2 / June 26, 2017
=========================
* Fixed crash when showing the second hand after it had been hidden

v1.9.1 / June 15, 2017
=========================
* CRITICAL FIX
    * Fixes watch face crashing constantly. So sorry about that. It was an issue with Proguard stripping out a critical class when building release builds--that's why it didn't show up when I was testing.

v1.9 / June 15, 2017
=========================
* Added a setting for displaying a notification indicator (Disabled, Unread, All)
    * The circle will take the color of the hour hand; the text will take the color of the center circle color

v1.8 / June 15, 2017
=========================
* Changed how complications are displayed--now uses `ComplicationDrawable`
    * Only thing that changes user facing is that certain text will be smaller in complications when you have the border enabled. Nothing I can do about this unfortunately; also, the bottom complication will now look like the left/right/top complications regardless of data displayed (no more "long text" if that makes sense)
    * Makes the codebase easier to read and gets rid of lots of code pertaining to drawing complications
    * Makes modifying complication styles super easy for future changes
    * Theoretically makes the watch face more efficient since we're not constantly measuring where to draw complications and we only do the measurements once in `onSurfaceChanged()`

v1.7.1 / June 11, 2017
=========================
* Fixed "Dark green" being the wrong shade of green in the color selections
* Made second hand slightly thicker
* Further adjusted the offset of the hour labels
* Minor code readability cleanups


v1.7 / June 10, 2017
=========================
* New "Classic Mode" setting for a more traditional style! (under "Background" settings)
* Setting for showing hour tick mark labels


v1.5 / June 6, 2017
=========================
* Hour tick marks are slightly longer when "Show Minute tick marks" are enabled (improves readability)


v1.4 / June 4, 2017
=========================
* Ability to show/hide minute tick marks
* Minor performance improvements


v1.3 / June 4, 2017
=========================
* Better ambient mode (anti-aliasing is only turned off if the watch supports low-bit ambient mode)
* Improved color selections (hand colors and background colors use the same palette now)
* Different shade for "dark" versions of colors (you'll have to reselect your background colors to see the new choices--sorry about that!)


v1.2 / June 1, 2017
=========================
* Reduced APK size using Proguard


v1.1 / May 31, 2017
=========================
* Inital release
