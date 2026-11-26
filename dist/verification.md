# Nook 0.12.0 verification

Reviewed on 26 November 2026 using the isolated Android 35 NookTest emulator. Minimum Android version: 9 (API 28). The portable APK uses the existing development signing key for in-place upgrades.

## Results

- The focused inline-video suite passed **40 checks**. It verifies actual adapter-backed image views, visible frames after six seconds, the final second and loop of a 12-second source, pause/resume, frozen frames, real Previous/Next controls, wraparound, rotated portrait/square video, aspect-provider binding, Universal playback and independent widget copies.
- The complete 0.12.0 regression passed **22,459 checks**, including the previous tools, video suite, native bindings and reminder delivery/cancellation. A final follow-up music run passed **2,605 checks**, including the volume-capability guard. Reminder completion verification waits up to four seconds for the system notification to disappear.
- A generated 12-second video was pinned through Android's real launcher confirmation. The launcher belongs to a different app UID; it successfully bound the frame service and displayed the video on the actual home screen. A 15-second screen recording and native screenshot are included below.
- Java 17 / Android SDK 35 build and APK Signature Scheme v3 verification passed. Version code: 13.

## Music redesign

- The final APK passed **2,605 focused native music checks**. The fixture creates real framework MediaSessions, metadata/artwork, queue items and an absolute VolumeProvider.
- Four responsive layouts: Album Card, a one-row Compact player, full-cover artwork and Vinyl. Bounds checks cover all eight design languages, four surfaces and 320×250, 320×182, 180×120 and 320×100 dp spaces. A separate 4×1 compact provider can be chosen during placement.
- Native elapsed time advances inside the widget at normal playback speed. Other speeds use reported position snapshots. There is no repeating Nook music-update loop. Metadata, playback, seeks and queue changes refresh music widgets only; ordinary position-only events are throttled to 15 seconds.
- Each transport PendingIntent identifies the displayed session. A second active session was created after an older view rendered; clicking the older view reached its own session and never paused the other player. Unsupported actions are disabled and checked again when invoked.
- Actual pointer gestures on the expanded player's seek and volume sliders reached the MediaSession seek callback and remote VolumeProvider. A native queue dialog selected its original player's exact shared queue ID even after another session became active. A seek gesture also retained its displayed player while a competing session started. A custom Favorite action reached the player and its updated Saved label appeared. Relative-only volume outputs disable the unsupported absolute slider and explain that volume must be adjusted in the source app; switching back to an absolute provider restores the slider.
- Preferred inactive players do not fall through to another app. Revoking media access removes stale artwork/controls and reveals an explicit reconnect action. Universal supports the music view and hidden captions. Compact-provider binding and tool identity were verified through AppWidgetManager.
- Artwork is read from MediaMetadata bitmaps, with bounded native treatments. Hardware bitmaps are copied for software rendering. Nook reads no notification messages, downloads no remote cover images and starts no music audio stream; it controls the selected source app.

[Native design comparison](screenshots/music-layouts.png) · [Music widget](screenshots/music-widget.png) · [Expanded Now Playing](screenshots/music-now-playing.png). All pictured tracks, artists and artwork are generated fixtures.

## Video behavior

The video tool now plays the complete source silently inside an AdapterViewFlipper backed by a RemoteViewsService. A sequential MediaCodec decoder reads only the video track; MediaMetadataRetriever supplies a compatibility fallback. The launcher requests frames lazily and keeps a bounded cache. Nook retains a single recent frame per active widget for pause snapshots; it does not embed every source frame in RemoteViews, start an audio track, or run a repeating foreground-service widget-update loop.

Up to 12 individually selected videos can form a carousel. Previous/Next wrap around and preserve pause state. Each copy, including Universal, owns its selection and transport state. Existing single-video configurations continue to work. A hidden-text option removes captions, filenames and counters, including Universal's captions, while retaining accessible icon controls. Full video is the default; still poster and a 1–6 second short loop remain available.

Initial placement can match the selected video's landscape, square or portrait shape. Rotation metadata and the video's aspect are respected. The launcher's outer grid remains under launcher/user control; mixed-aspect videos fit or crop within that slot. A placed widget cannot force its neighbors to move or automatically resize its outer grid when the carousel advances.

There are 37 tool families plus Universal (38 catalog entries), with two additional video aspect providers and a one-row music provider: **41 registered widget providers**. Stock-family coverage remains 149 named entries / 912 layout-size presets.

## Native evidence

All media here is generated test content. No personal phone media or screenshots are published.

- [Actual launcher screenshot](screenshots/inline-video-homescreen.png)
- [15-second home screen recording](previews/inline-video-homescreen.mp4)
- [Generated 12-second video source](previews/inline-proof-source.mp4)
- [Instrumentation source](../app/src/androidTest/java/com/nook/widgets/test/InlineVideoSmoke.java)

## Limits and privacy

Inline playback is silent, frame-based and lower resolution, with a long edge up to 320 pixels and 3, 5 or 8 requested frames per second. It is not native full-frame-rate HD video. Actual speed depends on the codec, phone, launcher and power settings. HDR, DRM, uncommon codecs and manufacturer-specific launcher behavior have not been comprehensively tested. Automated fixtures use AVC with no audio track.

The document picker grants access to individual selected files. Sources are read locally, never uploaded or modified; posters and paused frames remain private. Removing a carousel item leaves its source intact. Playback needs continuing access to the original source. The optional full-screen player and PiP remain separate, explicit actions for full-resolution playback/audio.

Neither physical phone was connected for this release. Music validation uses framework test sessions; third-party apps, their subscription features and device-specific media behavior have not been exhaustively tested. The previous 0.10.0 APK was installed and launched on OnePlus CPH2717 / Android 16 and vivo V2222 / Android 15; those checks do not establish 0.11.0 codec or launcher coverage.

Platform references: [RemoteViews supported views](https://developer.android.com/reference/android/widget/RemoteViews), [collection widgets](https://developer.android.com/develop/ui/views/appwidgets/collections), [widget sizing](https://developer.android.com/develop/ui/views/appwidgets/layouts), [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec).

APK SHA-256:

```text
6CA71BFB19E38F52FF03FB02EBE3B837B286379B4770100A7529C0A386A1DCA9
```
