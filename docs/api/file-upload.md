# File Upload

## Snapshot
- Primary sources:
  - `.reference/FastGPT/projects/app/src/pages/api/core/dataset/collection/create/localFile.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/dataset/collection/create/fileId.ts`
  - `.reference/FastGPT/packages/global/core/app/constants.ts`

## Dataset Collection Upload Flow
### Local multipart import
1. Client submits multipart form to `/api/core/dataset/collection/create/localFile`.
2. FastGPT parses form data with `multer.resolveFormData`.
3. File is uploaded into dataset S3/object storage.
4. Server creates a `DatasetCollection` with `type=file`.
5. Response returns:
   - `collectionId`
   - `results`

### Reuse previously uploaded object
1. Client provides `fileId`.
2. Server validates the key with `isS3ObjectKey(fileId, 'dataset')`.
3. Server loads file metadata.
4. Server creates collection via `/create/fileId`.

## Supported App-level Attachment Extensions
Default selectable file types from `defaultFileExtensionTypes`:
- Documents: `.pdf`, `.docx`, `.pptx`, `.xlsx`, `.txt`, `.md`, `.html`, `.csv`
- Images: `.jpg`, `.jpeg`, `.png`, `.gif`, `.bmp`, `.webp`, `.svg`
- Video: `.mp4`, `.mov`, `.avi`, `.mpeg`, `.webm`
- Audio: `.mp3`, `.wav`, `.ogg`, `.m4a`, `.amr`, `.mpga`

## Android Requirements
- Validate file extension before upload when `fileSelectConfig` is known.
- Show upload progress and resumable local state for long uploads.
- Keep dataset-import uploads and chat attachments separate in storage and UI copy.
- Preserve returned `fileId` because later collection creation can reuse it.

## Related Specs
- [dataset-collections.md](dataset-collections.md)
- [app-management.md](app-management.md)
