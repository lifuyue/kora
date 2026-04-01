# Design System

## Direction
- Visual direction: deep-dark workspace, aligned to Open WebUI's professional console feel, but rendered as Android-native Compose surfaces.
- Product tone: focused, content-dense, low-noise, high-contrast, suitable for long chat and knowledge work sessions.
- Primary experience rule: users should always know where they are, what state the workspace is in, and what the next high-value action is.

## Foundations
- Base stack: Material 3 with Kora semantic overrides.
- Supported variants:
  - `light` as compatibility theme
  - `dark` as default design target
- Layout rhythm: 4dp grid with 8/12/16/24/32 spacing tiers.

## Color Tokens
- Workspace:
  - `background`: deep near-black canvas
  - `surface`: primary content layer
  - `surfaceContainer`: secondary panels and cards
  - `surfaceContainerHigh`: hero/status summary cards
  - `outline`: low-contrast separators
- Actions:
  - `primary`: cold blue for main progression
  - `tertiary`: warm orange for highlights and urgency
  - `error`: destructive or failed flows only
- Chat semantics:
  - `chat.userBubble`
  - `chat.assistantBubble`
  - `chat.reasoningBubble`
  - `chat.toolStatus`
  - `chat.citationCard`
- Navigation semantics:
  - `nav.active`
  - `nav.inactive`
  - `nav.surface`

## Typography
- Headings: serif-led editorial hierarchy for page and hero titles.
- Body: sans-serif for dense operational content and controls.
- Required scale:
  - `headlineLarge/headlineMedium`: workspace hero and onboarding
  - `headlineSmall`: screen-level statements
  - `titleLarge/titleMedium`: section titles, cards, sheets
  - `bodyLarge/bodyMedium/bodySmall`: content, summaries, metadata
  - `labelLarge/labelMedium`: pills, buttons, filters, status text
- Long-form text rule: prioritize wrapping over truncation unless the control is structurally constrained.

## Shape, Layer, Motion
- Shape tokens:
  - hero and summary cards: `extraLarge`
  - standard cards: `large`
  - text fields: `medium`
  - chips and status pills: rounded pill
- Elevation rule: rely on tonal separation before shadow; shadows stay subtle.
- Motion family:
  - transitions 150-300ms
  - state changes use opacity/transform, not layout reflow
  - sheets and dialogs use one consistent motion family
  - streaming text never re-animates the whole bubble on token updates

## Interaction Rules
- Every primary screen must show explicit `loading / empty / error / success` states.
- Each screen has one dominant CTA; secondary actions must be visually quieter.
- Navigation must preserve scroll, filters, and drafts when switching top-level tabs.
- Destructive actions always combine color, text, and confirmation.
- Bottom sheets are for actions and detail inspection, not core navigation.

## Accessibility Gates
- Text contrast: minimum 4.5:1 for body, 3:1 for large or secondary text.
- Touch targets: minimum 44x44dp.
- Icon-only controls require semantic labels.
- TalkBack order must match visual reading order.
- Reduced-motion mode must remain readable and functional.

## Acceptance
- Chat and conversation entry surfaces must score `>= 90/100`.
- Knowledge and settings overview surfaces must score `>= 85/100`.
- See [quality-scorecard.md](quality-scorecard.md) for scoring and hard gates.
