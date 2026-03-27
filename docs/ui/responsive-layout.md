# Responsive Layout

## Phone
- Default single-column layout.
- Chat screen keeps input pinned to bottom and message list as primary surface.
- Knowledge pages use stacked navigation rather than side-by-side panes.

## Tablet
- Support list-detail layout for:
  - conversations + active chat
  - dataset list + collection/chunk detail
  - settings categories + detail
- Preserve the same route semantics as phone.

## Foldables
- Switch between single-pane and dual-pane based on posture/available width.
- Avoid placing critical tap targets near the hinge/fold line.

## Layout Rules
- Primary content width remains readable; do not stretch markdown/code indefinitely on wide screens.
- Bottom sheets on tablets may become centered dialogs or side sheets when width is large enough.
- Empty/error states should still align to the active pane, not the whole window.
