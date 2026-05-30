# Project Aegis - J.A.R.V.I.S. Assistant

**An advanced voice-controlled Android virtual assistant providing local task management, intelligent scheduling, calendar coordination, and AI-powered daily optimization with optional Notion synchronization.**

Project Aegis delivers a privacy-first, locally-hosted virtual assistant inspired by the J.A.R.V.I.S. system. The application leverages on-device SQLite storage for tasks and calendar events, integrates Google's Gemini AI API for conversational intelligence and schedule optimization, and features offline text-to-speech capabilities with a sophisticated British accent voice synthesis.

---

## Key Features

- **Voice-Controlled Task Management**: Create, prioritize, and manage tasks using natural language voice commands with priority levels (High, Medium, Low) and automated reminder scheduling
- **Intelligent Conversational Interface**: Context-aware AI responses powered by Google Gemini API with full awareness of local schedule data and task history
- **Local-First Privacy Architecture**: All sensitive data (tasks, calendar events, chat history) stored exclusively on-device using Room SQLite database with optional encrypted Notion synchronization
- **AI-Powered Schedule Optimization**: Advanced reasoning engine that analyzes daily agenda and generates prioritized briefings with strategic time-blocking recommendations
- **Offline Text-to-Speech Synthesis**: British-accented voice responses using Android's native TTS engine, fully functional without network connectivity

---

## Architecture / System Design

The application follows a modern **Android MVVM (Model-View-ViewModel)** architecture pattern with reactive state management:

1. **Presentation Layer**: Jetpack Compose UI with Material Design 3 components, custom Arc Reactor animation core, and tab-based navigation (Console, Agenda, AI Digest, Notion)
2. **Business Logic Layer**: `JarvisViewModel` orchestrates all business operations including voice command parsing, AI API calls, database transactions, and TTS coordination
3. **Data Layer**: Room SQLite database with DAO interfaces (`JarvisRepository`) providing reactive `Flow`-based state streams for real-time UI updates
4. **Network Layer**: Retrofit + OkHttp client for Gemini API integration with Moshi JSON serialization and 60-second timeout configurations
5. **Voice Processing**: Android Speech Recognizer for STT (Speech-to-Text) input and native TextToSpeech engine for audio synthesis

**Data Flow**: Voice Input → Speech Recognizer → ViewModel Command Parser → (Local SQLite Storage OR Gemini API Call) → UI State Update → TTS Audio Response

---

## Prerequisites

The following tools and runtimes are required to build and run this project:

- **Android Studio**: Ladybug (2024.2.1) or later ([Download](https://developer.android.com/studio))
- **Java Development Kit (JDK)**: Version 11 or higher
- **Android SDK**: API Level 24 (Android 7.0 Nougat) minimum, API Level 36 target
- **Gradle**: Version 8.x (included via Gradle Wrapper)
- **Google Gemini API Key**: Required for AI conversational features ([Get API Key](https://ai.google.dev/))
- **Physical Android Device or Emulator**: With microphone access for voice input testing

---

## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/JonathanMwangiMaina/project-aegis.git
cd project-aegis
```

### 2. Configure Environment Variables

Create a `.env` file in the project root directory:

```bash
cp .env.example .env
```

Edit the `.env` file and set your Gemini API key:

```properties
# .env
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

**Important**: Never commit the `.env` file to version control. The `.gitignore` is pre-configured to exclude it.

### 3. Open Project in Android Studio

1. Launch **Android Studio**
2. Select **Open** from the welcome screen
3. Navigate to the cloned `project-aegis` directory and click **OK**
4. Allow Android Studio to sync Gradle dependencies (this may take several minutes on first run)
5. Resolve any SDK or build tool incompatibilities using the IDE prompts

### 4. Remove Debug Signing Configuration (for Production Builds)

For local development on a physical device or emulator, remove the following line from `app/build.gradle.kts`:

```kotlin
// Remove this line for local builds:
signingConfig = signingConfigs.getByName("debugConfig")
```

This step is only necessary if you encounter signing errors during build.

### 5. Build and Run

1. Connect an Android device via USB with **Developer Mode** and **USB Debugging** enabled, or launch an **Android Virtual Device (AVD)** emulator
2. Select your target device from the device dropdown in Android Studio
3. Click the **Run** button (▶️) or press `Shift + F10`
4. Grant the application **microphone** and **audio recording** permissions when prompted

---

## Usage

### Running the Application

Once installed, tap the **Arc Reactor Core** (animated cyan circular button) to activate voice input. Speak commands such as:

- **"Remind me to inspect the thermal thrusters tomorrow at 8 PM"** – Creates a high-priority task with a scheduled reminder
- **"Schedule meeting with Pepper Stark tonight"** – Adds a calendar event with default location
- **"Prioritize my schedule"** – Triggers AI-powered daily optimization and generates a strategic briefing
- **"Send email to Stark Industries regarding Mark 85"** – Queues a simulated outgoing email transmission
- **"Sync with Notion"** – Synchronizes local tasks and events to configured Notion database

### Console Tab

Type or speak commands directly into the conversational interface. The system provides:

- Full chat history with message bubbles (user vs. J.A.R.V.I.S.)
- Quick action suggestion chips for common operations
- Real-time AI-generated responses with local schedule context

### Agenda Tab

- **Create Tasks**: Tap the **+** icon to manually add tasks with priority levels
- **Manage Calendar Events**: Schedule meetings with location and time details
- **Toggle Completion**: Check/uncheck tasks to mark as completed
- **Delete Items**: Swipe or tap the delete icon to remove entries

### AI Digest Tab

Click **"INITIATE PRIORITIZATION RUN"** to execute the AI reasoning engine. The system analyzes all tasks and events, then generates:

- Grouped priority directives (High → Medium → Low)
- Optimized calendar protocol recommendations
- Strategic reasoning and witty commentary in J.A.R.V.I.S. tone

### Notion Integration Tab

Configure your Notion API credentials to enable cloud synchronization:

1. Enter your **Notion Integration Token** (obtain from [Notion Developers](https://www.notion.so/my-integrations))
2. Enter your **Notion Database ID** (extract from database URL)
3. Click **SAVE CONFIG** to store credentials locally
4. Use **SYNC DATA** to push all local tasks and events to Notion

---

## Running Tests

Execute the test suite using Gradle commands:

### Unit Tests (Robolectric)

```bash
./gradlew test
```

### Instrumentation Tests (on Device/Emulator)

```bash
./gradlew connectedAndroidTest
```

### Screenshot Testing (Roborazzi)

```bash
./gradlew recordRoborazziDebug
./gradlew verifyRoborazziDebug
```

Test coverage includes:

- `ExampleUnitTest.kt` – Basic assertion tests
- `ExampleRobolectricTest.kt` – ViewModel logic tests
- `ExampleInstrumentedTest.kt` – UI component integration tests
- `GreetingScreenshotTest.kt` – Visual regression tests

---

## Project Structure

```
project-aegis/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt              # Main UI entry point with Compose navigation
│   │   │   │   ├── data/
│   │   │   │   │   ├── AppDatabase.kt           # Room database configuration
│   │   │   │   │   ├── Entities.kt              # Data models (Task, CalendarEvent, ChatMessage, NotionSettings)
│   │   │   │   │   ├── Daos.kt                  # Database access objects
│   │   │   │   │   └── Repository.kt            # Data repository abstraction layer
│   │   │   │   ├── network/
│   │   │   │   │   └── GeminiApiClient.kt       # Retrofit API client for Gemini integration
│   │   │   │   └── ui/
│   │   │   │       ├── JarvisViewModel.kt       # Core business logic and state management
│   │   │   │       └── theme/                   # Material Design 3 color schemes and typography
│   │   │   └── AndroidManifest.xml              # Permissions and application configuration
│   │   ├── test/                                 # Unit and Robolectric tests
│   │   └── androidTest/                          # Instrumentation tests
│   └── build.gradle.kts                          # App-level Gradle configuration
├── gradle/
│   └── libs.versions.toml                        # Centralized dependency version catalog
├── .env.example                                  # Environment variable template
├── .gitignore                                    # Git exclusion rules
├── build.gradle.kts                              # Project-level Gradle configuration
├── settings.gradle.kts                           # Gradle multi-project settings
└── README.md                                     # This file
```

---

## Technology Stack

| Layer                     | Technology                                                                 |
|---------------------------|---------------------------------------------------------------------------|
| **Programming Language**  | Kotlin (100%)                                                            |
| **UI Framework**          | Jetpack Compose with Material Design 3                                  |
| **Architecture Pattern**  | MVVM (Model-View-ViewModel)                                              |
| **Dependency Injection**  | Manual DI with ViewModelProvider                                         |
| **Database**              | Room SQLite ORM with Kotlin Coroutines Flow                             |
| **Networking**            | Retrofit 2 + OkHttp 4 + Moshi JSON converter                             |
| **AI Integration**        | Google Gemini API (gemini-3.5-flash model)                               |
| **Voice Processing**      | Android SpeechRecognizer (STT) + TextToSpeech (TTS)                      |
| **Asynchronous Operations** | Kotlin Coroutines + StateFlow                                          |
| **Build System**          | Gradle 8.x with Kotlin DSL                                               |
| **Testing Frameworks**    | JUnit 4, Robolectric, Espresso, Roborazzi (screenshot testing)          |
| **Code Generation**       | KSP (Kotlin Symbol Processing) for Room and Moshi                        |

---

## Key Dependencies

```kotlin
// Core Android & Jetpack
androidx.core:core-ktx:1.15+
androidx.lifecycle:lifecycle-runtime-compose:2.8+
androidx.activity:activity-compose:1.9+

// Jetpack Compose UI
androidx.compose.material3:material3:1.4+
androidx.compose.ui:ui-tooling:1.8+

// Room Database
androidx.room:room-runtime:2.7+
androidx.room:room-ktx:2.7+

// Networking
com.squareup.retrofit2:retrofit:2.9+
com.squareup.okhttp3:okhttp:4.12+
com.squareup.moshi:moshi-kotlin:1.15+

// Kotlin Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9+

// Testing
junit:junit:4.13+
androidx.test.ext:junit:1.2+
io.github.takahirom.roborazzi:roborazzi:1.34+
```

Full dependency catalog available in `gradle/libs.versions.toml`.

---

## Configuration

### Gradle Properties

Optimize build performance by adjusting `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
kotlin.compiler.execution.strategy=in-process
```

### API Configuration

The Gemini API base URL and model version are configured in `GeminiApiClient.kt`:

```kotlin
private const val BASE_URL = "https://generativelanguage.googleapis.com/"
private const val MODEL = "gemini-3.5-flash"
```

Update the `MODEL` constant to use alternative Gemini models (e.g., `gemini-3.5-pro` for enhanced reasoning).

---

## Troubleshooting

### Issue: "Speech recognition not supported on this device"

**Solution**: Ensure Google app is installed and updated on your device. Speech recognition requires Google Play Services.

### Issue: "Awaiting connection, Sir. Please configure your GEMINI_API_KEY"

**Solution**: Verify that:
1. `.env` file exists in project root
2. `GEMINI_API_KEY` is set to a valid API key (not the placeholder value)
3. Gradle sync completed successfully after adding the key

### Issue: Build fails with "Could not connect to Kotlin compile daemon"

**Solution**: Add the following to `gradle.properties`:

```properties
kotlin.compiler.execution.strategy=in-process
```

### Issue: Room database schema migration errors

**Solution**: Uninstall the app completely from device/emulator and perform a clean rebuild:

```bash
./gradlew clean
./gradlew assembleDebug
```

---

## Roadmap

- [ ] **Cloud Backup**: Implement Firebase Firestore sync for cross-device data persistence
- [ ] **Push Notifications**: Add WorkManager-based reminder notifications
- [ ] **Voice Profiles**: Multi-user support with voice recognition
- [ ] **Natural Language Processing**: On-device NLP for improved offline command parsing
- [ ] **Wear OS Companion**: Android smartwatch interface for quick voice commands
- [ ] **Email Integration**: Real Gmail/Outlook API integration for actual email dispatch

---

## Contributing

Contributions are welcome. Please follow these guidelines:

1. Fork the repository and create a feature branch (`git checkout -b feature/your-feature-name`)
2. Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
3. Write unit tests for new functionality (minimum 70% code coverage)
4. Ensure all tests pass: `./gradlew test connectedAndroidTest`
5. Update documentation for API changes
6. Submit a pull request with a clear description of changes

---

## License

```
MIT License

Copyright (c) 2025 Jonathan Mwangi Maina

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Acknowledgments

- **Google AI Studio**: For providing the Gemini API integration framework
- **Jetpack Compose**: For modern declarative UI development
- **Room Persistence Library**: For robust local database management
- **Marvel Cinematic Universe**: For J.A.R.V.I.S. character inspiration

---

## Contact

**Jonathan Mwangi Maina**
GitHub: [@JonathanMwangiMaina](https://github.com/JonathanMwangiMaina)
Email: jonathanmainast29@yahoo.com

**Project Repository**: [https://github.com/JonathanMwangiMaina/project-aegis](https://github.com/JonathanMwangiMaina/project-aegis)

---

<div align="center">
  <sub>Built with ❤️ using Kotlin and Jetpack Compose</sub>
</div>
