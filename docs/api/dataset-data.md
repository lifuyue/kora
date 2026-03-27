# Dataset Data

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/global/core/dataset/type.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/dataset/data/*.ts`

## `DatasetDataSchemaType`
```ts
type DatasetDataSchemaType = {
  _id: string;
  userId: string;
  teamId: string;
  tmbId: string;
  datasetId: string;
  collectionId: string;
  chunkIndex: number;
  updateTime: Date;
  q: string;
  a?: string;
  imageId?: string;
  history?: Array<{ q: string; a?: string; imageId?: string; updateTime: Date }>;
  forbid?: boolean;
  fullTextToken: string;
  indexes: Array<{
    type: string;
    dataId: string;
    text: string;
  }>;
  rebuilding?: boolean;
  imageDescMap?: Record<string, string>;
}
```

## Data Operations
- Insert / push chunk data
- Update one chunk
- Delete one chunk
- Read detail
- List paginated chunk rows
- Resolve quote data for chat citations

## Batch Insert Constraints
- FastGPT dataset APIs cap batch push at `200` items per request.
- Kora must chunk larger local edit/import jobs into batches of at most `200`.

## Index / Search Fields
- Each data row carries `indexes`.
- Index item fields:
  - `type`
  - `dataId`
  - `text`

## Android Requirements
- Treat `q` as the primary visible text in the chunk list.
- Expose `a` only in detail/editor views.
- Preserve `chunkIndex` ordering from server.
- Support optimistic local edits with rollback when update/delete fails.

## Related Specs
- [dataset-collections.md](dataset-collections.md)
- [dataset-search.md](dataset-search.md)
