# UI Quality Scorecard

## Purpose
- Use this scorecard to review every primary screen during the current frontend refactor.
- A page is not accepted only because it “looks better”; it must pass structural, interaction, and accessibility gates.

## Hard Gates
- Accessibility score under `8/10`: fail.
- Missing recovery path for empty or error state: fail.
- Touch targets under `44dp`: fail.
- Color-only status communication: fail.

## Weighted Scoring
- `20` Visual consistency
- `20` Interaction efficiency
- `15` Information hierarchy and readability
- `10` Accessibility
- `10` Feedback and motion
- `10` Navigation and state preservation
- `10` Component reuse and implementation convergence
- `5` Responsive and adaptive behavior

## Review Prompts
### Visual Consistency
- Does the page look like the same workspace as chat, knowledge, and settings?
- Are cards, pills, titles, and spacing using the same visual grammar?

### Interaction Efficiency
- Is the primary action obvious within 3 seconds?
- Can the user complete the main task without unnecessary jumps or hidden actions?

### Information Hierarchy
- Is the summary visible before the detail?
- Are metadata, controls, and content visually separated?

### Accessibility
- Are semantics, labels, contrast, and focus order correct?
- Does the page still work with larger text and reduced motion?

### Feedback and Motion
- Are loading, empty, error, and success states clearly differentiated?
- Do transitions reinforce state change instead of adding noise?

### Navigation and State
- Does the page preserve filters, scroll, and draft state when returning?
- Is back behavior predictable?

### Component Reuse
- Is the page built from shared workspace components and tokens?
- Did implementation avoid ad-hoc one-off styling?

### Responsive and Adaptive
- Does the page remain readable on phone, tablet, and landscape?
- Are panes, sheets, and list/detail layouts still coherent?

## Thresholds
- `ChatScreen` and `ConversationListScreen`: `>= 90`
- `KnowledgeOverviewScreen` and `DatasetBrowserScreen`: `>= 85`
- `SettingsOverviewScreen`: `>= 85`
- First milestone average: `>= 88`
