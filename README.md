# JARVIS for Android

A native Android personal-assistant starter built with Kotlin and Jetpack Compose. It begins with a combined chat-and-voice interface, Android speech recognition, and text-to-speech.

## Current starter features
- Native Compose chat screen with dark, cyan-accented JARVIS styling.
- Text input and message history in the current app session.
- Voice input using Android `SpeechRecognizer` (requires microphone permission and a compatible recognition service).
- Spoken assistant replies using Android `TextToSpeech`.
- Permission request flow and lifecycle cleanup for speech services.
- Basic phone commands for launching supported apps/settings, opening the dialler, opening the messaging composer, setting an alarm, and searching the web.
- A small local demo responder so the interface can be exercised without an API key.
- A Plugin Deck listing all 13 uploaded plugin ideas with example prompts.
- Water tracker: locally stores a daily total and goal using Android app preferences.
- Android intent/share-sheet adaptations for opening YouTube search, launching the camera, searching the Play Store, opening notification settings, sharing a Discord/message draft, and preparing a sample CSV draft.
- Guided placeholders for document review, quizzes, screen recording, screenshot annotation, notification reading, and desktop game updates until their Android permissions/services or AI backend are implemented.

## Uploaded plugin compatibility

The uploaded `.py` files are desktop plugins, not Android plug-ins that can be copied into a Kotlin app. Several depend on Windows APIs, desktop screen/window automation, Python packages, or a Gemini model/API key. This build ports the user-facing ideas into an Android Plugin Deck and implements a small, safe subset natively.

| Uploaded plugin | Android starter status |
|---|---|
| `_template.py` | Reference template only; not a feature |
| `water_reminder.py.py` | Local water logging and daily goal; scheduled reminders still pending |
| `youtube_control.py` | YouTube search/opening works; cross-app playback control is not implemented |
| `calorie_counter.py` | Opens camera; image recognition/calorie estimation awaits AI vision |
| `document_review.py` | Guided placeholder; requires AI connection and document text/file import |
| `excel_writer.py` | Shares a sample CSV draft; full workbook/chart export is pending |
| `game_updater.py` | Opens Play Store search; Steam/Epic desktop updater is not an Android feature |
| `notification_reader.py` | Opens notification settings; reading notifications requires a dedicated user-granted Notification Listener service |
| `quiz.py` | Listed in Plugin Deck; question generation/marking requires AI connection |
| `screen_recorder.py` | Guided placeholder; Android MediaProjection consent flow is not implemented |
| `screenshot_annotate.py` | Guided placeholder; screen capture consent and AI vision are not implemented |
| `wake_up.py` | Manual mic button remains available; always-on wake-word detection is not enabled |
| `discord_messenger.py` | Share-sheet draft path; no automated Discord UI control or auto-send |
| `chat_takeover.py` | Draft-only safety guidance; no background monitoring or automatic replies |

Privacy-sensitive actions are intentionally not automated silently: calling and messaging open user-controlled screens, notification access requires Android settings consent, and screen capture requires a system prompt. The app does not secretly read messages or send replies.

## Important scope notes
This is a starter implementation, not a finished Mark-LVI clone. A real AI provider, durable memory, integrated web research, reminders, always-on wake-word service, plugin tools, and stronger structured action confirmation still need implementation. Phone actions are deliberately limited to Android intents and share sheets; calls and messages are not silently placed or sent. Plugin placeholders are labelled honestly and are not full implementations. The demo responder is intentionally local and must not be mistaken for an LLM.

The Mark-LVI repository is licensed CC BY-NC 4.0. This project is an independent Android implementation inspired by the publicly described feature set; it does not copy Mark-LVI source code. Check license terms before any commercial use of material derived from that project.

## Build
1. Open this folder in a recent Android Studio.
2. Allow Gradle sync and install the required Android SDK (compile SDK 35).
3. Run the `app` configuration on an Android device or emulator (Android 8.0 / API 26 or newer).

A local Android SDK/Gradle installation is required to compile and package the app. No APK is included or claimed to have been built in this environment.

## Next milestones
1. Add a provider abstraction and secure backend proxy for AI chat (never embed a production API secret in the APK).
2. Add persistent, user-controlled memory with delete/export controls.
3. Add opt-in web search, reminders, and document summarization.
4. Add Android-specific actions with explicit permission and confirmation gates.
5. Add automated unit/UI tests and CI build validation.
