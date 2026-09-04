# Crawler boundary

The crawler is an isolated process. The Spring Boot application depends only on
its own `CrawlerGateway` contract and normalized DTOs; it does not add
`com.pcdd.sonovel.*` to the backend classpath.

`sonovel-adapter/` builds the pinned SoNovel `1.11.0` commit documented in
`docs/research-so-novel.md`, removes its unconditional client-reporting call,
and adds a bounded structured endpoint used by xmreader.

Local flow:

```text
Search page
  -> GET /api/crawler/search
  -> SoNovel /search/aggregated

Import button (authenticated)
  -> POST /api/crawler/imports
  -> returns a task id immediately
  -> SoNovel /xmreader/book
  -> private books / chapters in MySQL
  -> existing book detail and reader pages

Download button
  -> POST /api/crawler/downloads
  -> returns a UTF-8 TXT attachment without writing to MySQL
```

Run the stack with `start-local.cmd`, or start the dependencies directly:

```powershell
docker compose -f compose.local.yml up -d --build
```

The first Adapter build downloads a JDK image, the pinned upstream source, and
Maven dependencies. Later starts reuse Docker's build cache.

The backend also merges a cached metadata index from the open lnovel API so
linovelib light novels can be found. Because linovelib's current search/content
flow requires browser-side guards and is not a working SoNovel rule, these
results are deliberately metadata-only and link back to the source site.

Current limitations:

- import tasks are kept in process memory; an application restart discards task status, while already committed books remain;
- third-party source rules can become unavailable or stop matching;
- the Adapter must not be exposed to a public network;
- only crawl content that you are legally allowed to access, without bypassing
  authentication, payment, CAPTCHA, DRM, or other access controls.
