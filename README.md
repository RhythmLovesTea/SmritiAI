# SmritiAI

SmritiAI is an Android application designed to support people living with dementia and memory-related conditions. It helps users recognize familiar faces, store important memories, and interact through an intelligent assistant in a simple and accessible way.

The goal of SmritiAI is to use AI thoughtfully for care, comfort, and independence.

---

## Features

### Face Recognition

* Register family members, friends, and caregivers
* Recognize known faces using on-device processing
* Help users remember names and relationships

### Memory Assistance

* Save important personal memories
* Store notes linked to people
* View memory history anytime

### Smart Chat Assistant

* Friendly AI conversation support
* Helpful reminders and guidance
* Designed with a calm and simple interface

### Accessibility Focused UI

* Clean design
* Easy navigation
* Dementia-friendly experience

---

## Built With

* Kotlin
* Android Studio
* Jetpack Compose
* Room Database
* CameraX
* ML / Face Recognition
* Gemini API (where enabled)

---

## Project Structure

```text
app/
 ├── data/
 ├── ui/
 ├── utils/
 ├── viewmodel/
 └── res/
```

---

## Installation

1. Clone the repository

```bash
git clone https://github.com/RhythmLovesTea/SmritiAI.git
```

2. Open in Android Studio

3. Sync Gradle

4. Add your API keys locally (do not commit secrets)

5. Build and run on an Android device

---

## Security Notes

* Do not upload signing keys
* Keep API keys in local configuration files
* Use private builds for testing sensitive features

---

## Vision

SmritiAI aims to become a compassionate digital companion for dementia care by helping users remember people, moments, and daily life with dignity.

---

## Roadmap

* Voice assistant improvements
* Better offline AI responses
* Medication reminders
* Family dashboard
* Emergency contact tools
* Multi-language support

---

## Contributing

Contributions, suggestions, and improvements are welcome.

1. Fork the repository
2. Create a new branch
3. Commit changes
4. Open a pull request

---

## Disclaimer

SmritiAI is a supportive tool and not a replacement for professional medical care, diagnosis, or emergency services.

---

## Author

Built with purpose by RhythmLovesTea.

---

## Hackathon Phase 1 (Local Laptop AI)

This repo includes a **local FastAPI + Ollama server** for same-WiFi chat:

- Server code: `server/README.md:1`
- Android base URL: `app/build.gradle.kts:1` (`BuildConfig.LOCAL_AI_BASE_URL`)

**Required permissions**
- `INTERNET`, `RECORD_AUDIO`, `CAMERA` (`app/src/main/AndroidManifest.xml:1`)

**Demo flow**
1. Start laptop server (`server/README.md:1`)
2. Put phone + laptop on same WiFi
3. Set `LOCAL_AI_BASE_URL` to laptop IP (example: `http://192.168.1.5:8000/`)
4. In app: `Ask Smriti AI` → tap mic → speak a question
5. For “Who is this?”-style queries, the app opens a camera flow and sends recognized context to the laptop server
