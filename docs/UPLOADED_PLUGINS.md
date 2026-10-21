# Uploaded plugin porting notes

The supplied Python files target the Mark-LVI desktop runtime and are not directly executable in an Android Kotlin application.

## Included in this starter
- Plugin Deck with example prompts for the 13 feature plugins.
- Persistent local water intake and goal tracking.
- Intent-based camera launch for calorie/photo workflows.
- YouTube search links, Play Store search links, notification settings, and share-sheet message/CSV drafts.
- Explicit manual guidance for features that need AI, background services, screen capture, or desktop-only automation.

## Not implemented yet
- Secure AI provider/backend and tool/function calling.
- Scheduled hydration notifications.
- Food recognition and nutrition estimation.
- Full Excel `.xlsx` creation and chart generation.
- Android NotificationListenerService and its opt-in settings flow.
- Android MediaProjection screen recording and screenshot capture.
- Persistent always-on wake-word detection.
- Quiz generation/marking and document analysis.
- Direct YouTube playback control, Discord UI automation, chat takeover, and Steam/Epic updating.

The Windows desktop implementations must be re-designed for Android permissions, lifecycle, APIs, and Play policy; copying their Python dependencies into the APK is not a valid port.
