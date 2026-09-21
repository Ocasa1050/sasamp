---
name: Texture cache aliases
description: How to handle texture database files whose names are aliases rather than actual compression conversions
---

Cache entries may use `.etc` or `.pvr` as filename aliases while the downloaded binary remains DXT. The manifest must carry the actual `contentFormat`, and cache selection and native loading must use that value instead of trusting the filename.

**Why:** Texture database files are proprietary binary data. Renaming a DXT database does not transcode its texture blocks; treating the alias as ETC or PVR can make the native loader fail during startup or streaming.

**How to apply:** When adding aliases to a cache manifest, preserve the source URL and size/hash, mark the real format explicitly, keep the native loader on that real format, and only select ETC/PVR when genuinely encoded assets are published.