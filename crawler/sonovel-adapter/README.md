# xmreader SoNovel Adapter

This image builds the upstream SoNovel `1.11.0` source at commit
`76150dbd2827b4de97cde83cffb3ee367bc9be3a` and adds one bounded structured
endpoint for xmreader preview imports.

The build deliberately removes the unconditional upstream client-reporting
thread. It also disables auto-update and caps an xmreader import request at 20
chapters. The application currently requests only the first 5 chapters.

SoNovel is licensed under AGPL-3.0. The complete corresponding source is the
pinned upstream commit together with the Dockerfile and
`XmReaderBookServlet.java` in this directory. The upstream license is copied
into the resulting image at `/sonovel/LICENSE`.

Only crawl content you are legally allowed to access. Do not use the adapter to
bypass authentication, payment, CAPTCHA, DRM, or other access controls.
