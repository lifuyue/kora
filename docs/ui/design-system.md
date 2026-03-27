# Design System

## Foundation
- Base system: Material 3
- Variant support:
  - light
  - dark
  - OLED dark
  - dynamic color when supported

## Color Tokens
- `surface/background` for page and sheet layers
- `primary/secondary/tertiary` for actions and accents
- `error` for destructive or failed states
- `outline/outlineVariant` for separators and subtle borders
- Chat-specific semantic tokens:
  - `chat.userBubble`
  - `chat.assistantBubble`
  - `chat.reasoningBubble`
  - `chat.toolStatus`
  - `chat.citationCard`

## Typography Scale
- `displaySmall` for onboarding hero
- `headlineMedium` for page titles
- `titleLarge` for section titles
- `bodyLarge/bodyMedium` for message text
- `labelLarge/labelMedium` for chips and buttons
- `codeBody` custom token for code blocks and chunk preview

## Shape Tokens
- Message bubbles: 20dp rounded family
- Cards: 16dp
- Text fields: 14dp
- Bottom sheets/dialogs: 24dp top corners
- Chips: pill / 999dp radius

## Elevation and State
- Use low elevation for cards and sheets; rely on tonal contrast first.
- All interactive components must define:
  - default
  - pressed
  - focused
  - disabled
  - error
  - loading

## Kora-Specific UI Rules
- User and assistant bubbles must be visually distinct without relying only on color.
- Reasoning text should look subordinate to final answer.
- Destructive actions always use explicit error semantics, not only iconography.
- Loading and empty states should reuse the same spacing and typography tokens as content states.

## Related Specs
- [component-catalog.md](component-catalog.md)
- [accessibility.md](accessibility.md)
