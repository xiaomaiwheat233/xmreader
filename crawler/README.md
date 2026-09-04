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
  -> SoNovel /xmreader/book?limit=5
  -> books / chapters in MySQL
  -> existing book detail and reader pages
```

Run the stack with `start-local.cmd`, or start the dependencies directly:

```powershell
docker compose -f compose.local.yml up -d --build
```

The first Adapter build downloads a JDK image, the pinned upstream source, and
Maven dependencies. Later starts reuse Docker's build cache.

Current MVP limitations:

- imports are synchronous and limited to the first 5 chapters;
- third-party source rules can become unavailable or stop matching;
- the Adapter must not be exposed to a public network;
- only crawl content that you are legally allowed to access, without bypassing
  authentication, payment, CAPTCHA, DRM, or other access controls.
