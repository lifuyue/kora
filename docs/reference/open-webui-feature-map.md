# Open WebUI Feature Map

## Snapshot
- Upstream: Open WebUI `v0.8.12`
- Purpose: map reusable interaction patterns and component entrypoints to Kora specs.

## Chat Surface
| Open WebUI source | Capability | Kora spec | Priority | Phase |
|---|---|---|---|---|
| `src/lib/components/chat/Chat.svelte` | streaming chat orchestration | [../features/chat/streaming-chat.md](../features/chat/streaming-chat.md) | P0 | 1-mvp |
| `src/lib/components/chat/Messages/Markdown.svelte` | markdown body rendering | [../features/chat/markdown-rendering.md](../features/chat/markdown-rendering.md) | P1 | 1-mvp |
| `src/lib/components/chat/Messages/ResponseMessage.svelte` | assistant message shell | [../features/chat/message-actions.md](../features/chat/message-actions.md) | P1 | 1-mvp |
| `src/lib/components/chat/Messages/Citations.svelte` | citations panel | [../features/chat/citations.md](../features/chat/citations.md) | P1 | 2-knowledge |
| `src/lib/components/chat/MessageInput.svelte` | multimodal composer | [../features/chat/multimodal-input.md](../features/chat/multimodal-input.md) | P2 | 3-advanced |
| `src/lib/components/chat/MessageInput/VoiceRecording.svelte` | voice input affordance | [../features/voice/speech-to-text.md](../features/voice/speech-to-text.md) | P2 | 3-advanced |
| `src/lib/components/chat/MessageInput/CallOverlay.svelte` | voice call mode | [../features/voice/voice-call-mode.md](../features/voice/voice-call-mode.md) | P3 | 3-advanced |
| `src/lib/components/chat/SettingsModal.svelte` | grouped settings IA | [../features/settings/settings-overview.md](../features/settings/settings-overview.md) | P1 | 1-mvp |

## Conversation and Navigation
| Open WebUI source | Capability | Kora spec | Priority | Phase |
|---|---|---|---|---|
| `src/lib/components/layout/Sidebar.svelte` | conversation navigation | [../features/conversations/conversation-list.md](../features/conversations/conversation-list.md) | P0 | 1-mvp |
| `src/lib/components/layout/Sidebar/ChatItem.svelte` | row-level chat actions | [../features/conversations/conversation-crud.md](../features/conversations/conversation-crud.md) | P0 | 1-mvp |
| `src/routes/(app)/+layout.svelte` | global layout + settings entry | [../architecture/navigation.md](../architecture/navigation.md) | P1 | 1-mvp |

## Knowledge and App Patterns
| Open WebUI source | Capability | Kora spec | Priority | Phase |
|---|---|---|---|---|
| `backend/open_webui/*` knowledge flows | RAG / file / knowledge mental model | [../features/knowledge/knowledge-overview.md](../features/knowledge/knowledge-overview.md) | P1 | 2-knowledge |
| `src/lib/components/workspace/Knowledge/*` | knowledge creation patterns | [../features/knowledge/document-upload.md](../features/knowledge/document-upload.md) | P1 | 2-knowledge |
| `src/lib/components/app/*` | app selection and shell conventions | [../features/apps/app-selector.md](../features/apps/app-selector.md) | P1 | 2-knowledge |

## Deliberately Not Copied
| Open WebUI area | Why excluded from Kora |
|---|---|
| PWA install / browser-specific flows | Android native app does not need browser/PWA semantics |
| Desktop hover-heavy admin panels | Mobile-first layout and interaction constraints |
| Browser store/router internals | Replaced by Compose Navigation + ViewModel + Flow |
| Backend-owned orchestration logic | Consumed as server behavior, not reimplemented in client |

## Coverage Summary
- P0/P1 patterns mapped to current Kora MVP: streaming chat, markdown, conversation navigation, settings IA.
- P2/P3 patterns mapped as advanced extensions: citations, multimodal input, voice, analytics-like secondary views.
