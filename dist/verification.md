# Nook 0.10.0 verification

Reviewed on 23 November 2026 using the isolated Android 35 NookTest emulator. The app targets Android 9 or newer. On 26 November 2026, the same APK was installed and launched successfully on a OnePlus CPH2717 running Android 16 and a vivo V2222 running Android 15. Those are installation/launch checks; full physical widget, codec and hardware coverage remains unverified.

## Results

- The final build passed **19,769 integration checks** in the complete regression suite. This includes the original tools, all 38 providers, seven Universal picker pages, 37 tool views, styles, exact transparency, saved configuration, notes/lifetimes/recovery, reminders, device-backed data and the new media suite.
- The dedicated media suite passed **410 checks**, including real attached frame advancement, photo fades, pause/resume/change/next, MP4 encoding and decoding, video frame changes, full video playback and actual Android picture-in-picture.
- The APK built with Java 17 and Android SDK 35. Signature Scheme v3 verification passed with the existing development key, preserving in-place updates from earlier portable builds.

## New media tools

- **Animated scenes:** four original Canvas compositions, three speeds, pause/resume and scene switching. Frames cycle through a native ViewFlipper in the launcher. Pause replaces the sequence with a still view.
- **Photo slideshow:** up to six individually chosen photos, fade or cut, crop/fit, 2–60 seconds per photo, pause and next. Native tests created and removed their own sample images, verified a real fade animation and observed the displayed photo change.
- **Video frame:** selected local video, a 1–6 second silent frame loop or still poster, and Play/Float/Options controls. The segment start is editable. Choosing a video prepares it locally; changing the segment requires Update looping preview. Preparation runs off the UI thread, reports actual progress and refreshes placed widgets on completion.
- Widget video is a bounded, low-resolution preview with up to 16 extracted frames, rather than full-length or full-frame-rate video. No audio plays in the widget. The frame images are cached privately and scaled to the widget bitmap budget; no continuous Nook foreground service or repeated background widget-update loop is used.
- **Play** opens a native VideoView player with seeking, play/pause, mute/unmute and optional repeat. Playback starts muted by default. **Float** opens the real Android PiP window; the test observed the activity enter PiP and verified that playback continued. Leaving the normal player pauses video.
- All eight design languages, four surfaces, transparency and typography options remain available. Compact layouts were checked across every design/surface combination for visible image space and controls within bounds. Animated image descendants are hidden from accessibility traversal; the media stage supplies a meaningful description.
- The real Android document-picker flow was reviewed: selecting the generated sample in Downloads granted access, automatically prepared its preview, and reached the normal launcher Add confirmation. The video widget was then placed through that confirmation, with its chosen source retained.

## Native evidence

The images below are native captures. Sample photos and the short MP4 are generated test media, not personal files or remote content.

- [Animated widget](screenshots/animated-widget.png)
- [Photo slideshow](screenshots/slideshow-widget.png)
- [Video widget](screenshots/video-widget.png)
- [Full player](screenshots/video-player.png)
- [Actual floating playback](screenshots/video-floating.png)
- [Generated test clip](previews/sample-motion.mp4)
- [Actual animated home screen placement](screenshots/animation-homescreen.png)
- [Home screen motion recording](previews/motion-homescreen.mp4)

The animation was added through the normal launcher confirmation, then recorded on the actual home screen. The video source picker and [video placement confirmation](screenshots/video-pin-ready.png) were also reviewed with generated media.

## Existing features retained

The Pixel clock's symmetric face/hand alignment checks remain in the regression suite. Temporary and permanent notes, expiry recovery, checklist/habit actions, reminder notifications, independent widget copies and Universal per-tool settings remain covered. Existing city weather, Android calendars, selected photos, battery data and media-session controls retain their sources and permissions.

Liquid Glass still supports clear/tinted appearances, 0–100% transparency and selected-wallpaper crop/blur/refraction. Text and controls retain their opacity. Its optical material is independently rendered for Android and uses a static selected image for blur/refraction, rather than sampling the launcher wallpaper live. The earlier [home screen capture](screenshots/liquid-glass-homescreen.png) and [comparison board](screenshots/liquid-glass.png) document that appearance.

The five stock-family catalogs remain at 149 entries and 912 layout/size presets. These counts include repeated categories and sizes, rather than distinct proprietary integrations. [Coverage](stock-library-coverage.md) and [inventory](stock-catalog.json) identify their sources. Private vendor services remain explicitly labeled manual cards/app shortcuts; health-style logs use user entries.

## Limits and privacy

Media selection uses persisted, individual document grants, without broad photo/video-library permission. Video files are not uploaded or modified. Full playback requires the original file and its grant; cached previews can outlast temporary source unavailability. Clear removes the selection without deleting the source file. Preview caches are private and removed on uninstall.

Supported codecs, HDR/DRM behavior, audio output and PiP availability depend on the phone and Android settings. Automated media uses a generated AVC MP4 with no audio track; real-device audio routing and manufacturer-specific PiP/launcher behavior remain unverified. Streaming URLs, custom GIF import and playback of an entire video inside a standard widget are not included.

The existing optional calendar, usage, microphone and media access remain separate. Weather, RSS and authenticated quote integrations retain their prior setup requirements. No microphone recording or private account access was used for these media checks.

Platform references: [RemoteViews supported views](https://developer.android.com/reference/android/widget/RemoteViews), [ViewFlipper](https://developer.android.com/reference/android/widget/ViewFlipper), [video frame retrieval](https://developer.android.com/reference/android/media/MediaMetadataRetriever), [Android PiP](https://developer.android.com/develop/ui/views/picture-in-picture).

APK SHA-256:

```text
37DE397177C1900976C22B414B5B7F78AB2E95E4DA7E99362D89A269ADD1DAA8
```
