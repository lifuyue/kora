# Animations

## Approved Motion
- Page enter/exit: short fade + slide.
- Bottom sheet/dialog: standard Material motion.
- List insertion/removal: subtle placement animation only.
- Streaming text: no per-character flourish; animate container/state transitions instead.

## Chat Motion Rules
- New assistant message may fade in once; subsequent token updates should not reanimate the whole bubble.
- Tool status cards may pulse or crossfade when status changes.
- Citation sheet and message action sheet use the same motion family as other modal surfaces.

## Constraints
- Motion must not delay content readability.
- Error states appear immediately; do not hide failures behind long transitions.
- Recording and voice-call indicators can animate, but must also expose static state cues.
