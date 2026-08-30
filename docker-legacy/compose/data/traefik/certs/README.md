Place the Cloudflare Origin Certificate for this domain here (gitignored, not committed):

- `cloudflare-origin.pem` — the origin certificate
- `cloudflare-origin.key` — its private key

Generate them in the Cloudflare dashboard: SSL/TLS > Origin Server > Create Certificate.
Set Cloudflare's SSL/TLS encryption mode to "Full (strict)" so Cloudflare validates this cert
when connecting to the origin.