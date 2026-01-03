

📁 Xplorer — Advanced Android File Manager

Xplorer is a modern, non-root, SAF-correct, dual-pane Android file manager built with Kotlin + Jetpack Compose, designed for power users and tablets.

It goes beyond traditional file managers by introducing rule-based file automation, robust foreground file operations, and a desktop-grade UX — without compromising Android security.





---

✨ Key Highlights

🚀 Foreground file operations (copy / move / delete / rename)

📊 Live progress + cancel + resume

🔍 Recursive, permission-aware search

📂 Dual-pane + multi-tab navigation

🧠 Rule-based file automation (unique feature)

🔐 Correct SAF & Android/data handling

🖼️ Built-in Image / PDF / ZIP / Large-Text viewers

🧩 Designed for tablets & landscape

❌ No root required

❌ No analytics / tracking



---

🧠 What Makes Xplorer Different?

Most Android file managers stop at “browse & copy”.

Xplorer introduces automation.

🔥 Rule-Based File Automation (Killer Feature)

Create smart rules that organize files automatically — safely and transparently.

Examples:

📺 Auto-move .srt subtitles next to matching videos

📥 Auto-rename files in Downloads

📷 Sort camera photos by year/month

🧹 Clean folders based on size, type, or name


✔ Preview before execution
✔ Manual or contextual triggering
✔ Undo support
✔ SAF-safe (no hidden background actions)

> Solid Explorer does not support this.




---

📦 Feature Overview

📁 File Operations

Copy / Move / Delete / Rename

Foreground Service (no UI freeze)

Progress notifications

Cancel anytime

Crash-safe execution


🔍 Search

Recursive search

Filename / extension filtering

SAF-aware (only searches permitted locations)

User-controlled search scopes via Settings


🧭 Navigation

Dual-pane layout

Independent back stacks

Multi-tab per pane

Breadcrumb path navigation


👁️ Built-in Viewers

🖼️ Image viewer (zoom, pan, swipe, EXIF)

📄 PDF viewer (PdfRenderer)

📦 ZIP browser (no extraction required)

📝 Large-file text viewer (100MB+ safe, lazy loading)


🧠 Power User Tools

Advanced multi-select

Select by extension

Range selection

Invert selection

Metadata previews (image, video, audio, APK)



---

🔐 Storage & Permissions (Done Right)

Full internal storage access

SAF-based Android/data & OBB access

Permission persistence & recovery

Graceful handling of revoked permissions

No dangerous background filesystem watching



---

⚙️ Settings

Xplorer includes a real settings system, not just toggles:

Search locations & exclusions

Default view mode (List / Grid / Details)

Per-folder view memory

Storage usage & free space

File operation safety controls

SAF permission management



---

🧱 Architecture

Language: Kotlin

UI: Jetpack Compose

Architecture: MVVM (lightweight)

Concurrency: Coroutines + Flow

Storage: DataStore

Min SDK: 26

Target SDK: 34

CI: GitHub Actions (headless, no Android Studio)


Designed to be:

Testable

Maintainable

CI-friendly

Tablet-first



---

📊 Comparison

Feature	Solid Explorer	Xplorer

Dual Pane	✅	✅
Tabs	⚠️	✅
Foreground Ops	⚠️	✅
Rule Automation	❌	✅
SAF Recovery UX	⚠️	✅
Large File Viewer	⚠️	✅
Compose UI	❌	✅



---

🚧 Status

Xplorer is actively developed and already stable for daily use.

Upcoming focus:

Operation resume after reboot

Workspace profiles

Keyboard / Chromebook support

Optional recycle bin



---

📜 Philosophy

> A file manager should never:

Freeze

Lose your data

Do things behind your back




Xplorer is built on explicit actions, user control, and trust.


---

🧑‍💻 Contributing

Contributions are welcome — especially in:

UI polish

Automation rules

Performance testing

Accessibility


Please keep changes:

SAF-correct

Non-blocking

CI-verified



---

📄 License

MIT License
Free to use, modify, and distribute.
