# Dataset Management

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/global/core/dataset/constants.ts`
  - `.reference/FastGPT/packages/global/core/dataset/type.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/dataset/list.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/dataset/create.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/dataset/update.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/dataset/delete.ts`

## Dataset Type Enum
- `folder`
- `dataset`
- `websiteDataset`
- `externalFile`
- `apiDataset`
- `feishu`
- `yuque`

## Dataset Status Enum
- `active`
- `syncing`
- `waiting`
- `error`

## `DatasetSchemaType`
```ts
type DatasetSchemaType = {
  _id: string;
  parentId: ParentIdType;
  userId: string;
  teamId: string;
  tmbId: string;
  updateTime: Date;
  avatar: string;
  name: string;
  intro: string;
  type: DatasetTypeEnum;
  vectorModel: string;
  agentModel: string;
  vlmModel?: string;
  websiteConfig?: { url: string; selector: string };
  chunkSettings?: ChunkSettingsType;
  inheritPermission: boolean;
  apiDatasetServer?: ApiDatasetServerType;
  deleteTime?: Date | null;
}
```

## `ChunkSettingsType`
Important fields Kora must preserve:
- `trainingType`
- `chunkTriggerType`
- `chunkTriggerMinSize`
- `dataEnhanceCollectionName`
- `imageIndex`
- `autoIndexes`
- `indexPrefixTitle`
- `chunkSettingMode`
- `chunkSplitMode`
- `paragraphChunkAIMode`
- `paragraphChunkDeep`
- `paragraphChunkMinSize`
- `chunkSize`
- `chunkSplitter`
- `indexSize`
- `qaPrompt`

## CRUD Surface
- List datasets for a folder / search scope.
- Read dataset detail.
- Create dataset / folder / website dataset / external dataset.
- Update metadata, model bindings, permissions, chunk settings.
- Delete dataset by soft-delete semantics.

## Android Requirements
- Model dataset list rows separately from full dataset detail.
- Preserve `vectorModel`, `agentModel`, `vlmModel` so settings and search-test screens stay consistent.
- Treat `inheritPermission` and dataset `type` as first-class display fields.

## Related Specs
- [dataset-collections.md](dataset-collections.md)
- [dataset-search.md](dataset-search.md)
