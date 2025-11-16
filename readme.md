
# Docker install

In a new directory, copy the content of https://github.com/marc-p-b/monkey-notes/tree/main/docker/compose 

docker-compose.yml

## Edit nginx config

Open `data/nginx/app-run.conf` and replace `<<MY_DOMAIN>>` by your domain, like `https://mn.mydomain.com`

## Edit ui config

Open `data/nginx/env.js` and replace `___CHANGE_ME !___` by your domain. Keep the trailing `/api/`

## Edit backend config

Open `.env` and fill all the properties, assuming your are using `mn.mydomain.com`

```
YOUR_DOMAIN = https://mn.mydomain.com
YOUR_DOMAIN_API = https://mn.mydomain.com/api
CORS_DOMAINS = https://mn.mydomain.com
GDRIVE_CLIENT_ID = <<your google console client_id>>
GDRIVE_SECRET = <<your google console client_secret>>
OPENAI_API_KEY = <<your openai api key>>
QWEN_API_KEY = <<your openai api key>>
```

## Run MonkeyNotes App

Start the app using `docker compose up`

Watch the logs for admin password :

```
****** ----------------------- **************
****** Initializing admin user **************
password 25d9ccaa-efaf-4531-ae79-8aea7fd002bc
****** ----------------------- **************
```

Use your fqdn https://mn.mydomain.com to login using the admin user