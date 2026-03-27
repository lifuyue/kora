# Share Auth

## Snapshot
- Primary sources:
  - `.reference/FastGPT/packages/service/support/outLink/runtime/auth.ts`
  - `.reference/FastGPT/packages/service/support/outLink/tools.ts`
  - `.reference/FastGPT/projects/app/src/pages/api/core/chat/outLink/init.ts`

## FastGPT Share Auth Model
FastGPT share links use a three-step external hook protocol when a share/out-link defines `tokenUrl` or `limit.hookUrl`:

1. `POST /shareAuth/init`
   - request body: `{ token: outLinkUid }`
   - purpose: normalize/validate the external user id before initializing the share session
2. `POST /shareAuth/start`
   - request body: `{ token: outLinkUid, question }`
   - purpose: preflight auth/rate-limit before a chat request starts
3. `POST /shareAuth/finish`
   - request body: `{ token: outLinkUid, shareId, chatId, appName, responses }`
   - purpose: post-result callback after chat finishes

## In-app Share Session Init
Kora's first local share bootstrap call is:
- `GET /api/core/chat/outLink/init?shareId=...&outLinkUid=...&chatId=...`

Response:
- `chatId`
- `appId`
- `title`
- `userAvatar`
- `variables`
- `app.chatConfig`
- `app.name/avatar/intro/type`
- `pluginInputs`

## Share Session Identity
- `shareId`
  Stable id for the published out-link.
- `outLinkUid`
  External user token before normalization.
- `uid`
  Normalized identity returned by `/shareAuth/init` or `/shareAuth/start`.

## Android Requirements
- Parse `shareId` and `outLinkUid` from deep links.
- Store share session state separately from the user's primary connection.
- Call out-link init before rendering the shared chat screen.
- Never write share-session credentials back into the saved primary API connection profile.

## Related Specs
- [authentication.md](authentication.md)
- [chat-records.md](chat-records.md)
