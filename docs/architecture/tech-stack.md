# Tech Stack

## Language and Platform
- Kotlin `2.1.0`
- Android Gradle Plugin aligned with Kotlin `2.1.x`
- Android SDK + Jetpack

## UI
- Compose BOM `2025.01.01`
- Material 3 via Compose BOM
- Navigation Compose `2.8.5`
- Coil `2.7.0`
- Markwon `4.6.2`

## DI and Async
- Hilt `2.51.1`
- Kotlin Coroutines + Flow
- kotlinx-serialization `1.7.3`

## Networking
- Retrofit `2.11.0`
- OkHttp `4.12.0`
- SSE parsing:
  - primary choice: manual OkHttp streaming reader
  - reason: FastGPT requires custom `event:` handling, `[DONE]`, and mixed structured payloads

## Local Persistence
- Room `2.6.1`
- DataStore `1.1.1`

## Test and Quality
- ktlint Gradle plugin
- JUnit
- Turbine
- MockWebServer

## Android Packaging Assumptions
- Min SDK and target SDK remain open until M1 scaffold, but nothing in M0 should depend on legacy View/XML stacks.
- All user-visible strings remain resource-backed once implementation starts.

## Related Specs
- [module-structure.md](module-structure.md)
- [networking.md](networking.md)
