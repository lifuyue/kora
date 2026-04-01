# Navigation

## Top-Level Routes
- `onboarding`
- `app-selector`
- `chat/{appId}`
- `chat/{appId}/{chatId}`
- `conversations/{appId}`
- `knowledge`
- `knowledge/datasets`
- `knowledge/datasets/{datasetId}`
- `knowledge/datasets/{datasetId}/collections`
- `knowledge/datasets/{datasetId}/collections/{collectionId}`
- `knowledge/datasets/{datasetId}/search-test`
- `settings`
- `settings/connection`
- `settings/language`
- `settings/storage`
- `settings/theme`
- `settings/about`
- `share/chat?shareId=...&outLinkUid=...&chatId=...`

## Navigation Rules
- Route params carry ids only.
- Detail data is always restored through repository calls keyed by ids.
- Share links open into an isolated nav graph branch with share-scoped auth state.

## Tablet / Large Screen Adaptation
- Conversations and datasets may render list-detail panes in one activity.
- Route semantics do not change between phone and tablet.

## Related Specs
- [local-storage.md](local-storage.md)
- [features/auth/share-link-auth.md](../features/auth/share-link-auth.md)
