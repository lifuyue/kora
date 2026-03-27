# Dataset Search

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/global/core/dataset/constants.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/dataset/searchTest.ts`

## Search Mode Enum
- `embedding`
- `fullTextRecall`
- `mixedRecall`

## Score Type Enum
- `embedding`
- `fullText`
- `reRank`
- `rrf`

## `searchTest` Request
Important request fields:
- `datasetId: string`
- `text: string`
- `limit?: number` with server cap `min(limit, 20000)`
- `similarity?: number`
- `searchMode?: DatasetSearchModeEnum`
- `embeddingWeight?: number`
- `usingReRank?: boolean`
- `rerankModel?: string`
- `rerankWeight?: number`
- `datasetSearchUsingExtensionQuery?: boolean`
- `datasetSearchExtensionModel?: string`
- `datasetSearchExtensionBg?: string`
- `datasetDeepSearch?: boolean`
- `datasetDeepSearchModel?: string`
- `datasetDeepSearchMaxTimes?: number`
- `datasetDeepSearchBg?: string`

## `searchTest` Response
```ts
{
  list: SearchResultItem[];
  duration: "0.123s";
  queryExtensionModel?: string;
  usingReRank: boolean;
  // deep-search / debug result fields may also be present
}
```

## Android Requirements
- Surface search mode, similarity, embedding weight, and rerank toggles as explicit controls.
- Show per-result score type and score only when the score type supports a visible score.
- Display duration and whether re-rank/extension-query actually ran.

## Related Specs
- [dataset-management.md](dataset-management.md)
- [features/knowledge/search-test.md](../features/knowledge/search-test.md)
