# Accessibility

## Core Rules
- Text and interactive icons must meet contrast requirements in light, dark, and OLED themes.
- All icon-only controls require content descriptions.
- Every page must be fully navigable with TalkBack.

## Chat-Specific Rules
- Streaming updates must announce message completion without spamming per-character accessibility events.
- Voice input, recording, and playback states need explicit spoken labels.
- Citation counts, message feedback state, and action menus must be semantically exposed.

## Motion and Focus
- Respect reduced-motion preferences where possible.
- Focus order in message lists should remain chronological and predictable.
- Bottom sheets must trap focus correctly and restore it on dismiss.

## Forms
- Connection config, interactive nodes, and dataset import forms require field labels, errors, and hint text.
- Validation errors should be announced and visually persistent until fixed.
