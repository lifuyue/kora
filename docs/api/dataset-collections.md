# Dataset Collections

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/global/core/dataset/constants.ts`
  - `.reference/FastGPT/packages/global/core/dataset/type.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/dataset/collection/create/*.ts`

## Collection Type Enum
- `folder`
- `virtual`
- `file`
- `link`
- `externalFile`
- `apiFile`
- `images`

## Training Mode Enum
- `parse`
- `chunk`
- `qa`
- `auto`
- `image`
- `imageParse`

## `DatasetCollectionSchemaType`
```ts
type DatasetCollectionSchemaType = ChunkSettingsType & {
  _id: string;
  teamId: string;
  tmbId: string;
  datasetId: string;
  parentId?: string;
  name: string;
  type: DatasetCollectionTypeEnum;
  tags?: string[];
  createTime: Date;
  updateTime: Date;
  forbid?: boolean;
  fileId?: string;
  rawLink?: string;
  externalFileId?: string;
  apiFileId?: string;
  apiFileParentId?: string;
  externalFileUrl?: string;
  rawTextLength?: number;
  hashRawText?: string;
  metadata?: {
    webPageSelector?: string;
    relatedImgId?: string;
    [key: string]: any;
  };
  customPdfParse?: boolean;
  trainingType: DatasetCollectionDataProcessModeEnum;
}
```

## Create Endpoints Kora Must Cover
- `POST /api/core/dataset/collection/create/text`
- `POST /api/core/dataset/collection/create/link`
- `POST /api/core/dataset/collection/create/localFile`
- `POST /api/core/dataset/collection/create/fileId`
- `POST /api/core/dataset/collection/create/apiCollection`
- `POST /api/core/dataset/collection/create/apiCollectionV2`

## Create Semantics
- `text`
  Creates a temporary `.txt` file in S3, then imports it as `type=file`.
- `link`
  Creates `type=link`, sets `rawLink`, and stores `metadata.webPageSelector`.
- `localFile`
  Multipart upload, then imports as `type=file`.
- `fileId`
  Reuses an already uploaded dataset object key after validating S3 metadata.
- `apiCollection`
  Imports one API-backed file id.
- `apiCollectionV2`
  Imports multiple API-backed files recursively and deduplicates by `apiFileId`.

## Android Requirements
- Collection creation UI must branch by source type, not by a single generic form.
- File-import flows must support:
  - local multipart
  - prior uploaded `fileId`
  - remote API-backed file trees
- Persist training status and creation mode in local cached rows so long-running imports survive app restarts.

## Related Specs
- [file-upload.md](file-upload.md)
- [dataset-data.md](dataset-data.md)
