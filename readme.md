
# docker install

copy docker-compose.yml and .env
create dir data/nginx
copy app-run.conf and env.js to data/nginx/

edit .env and fill all the properties
YOUR_DOMAIN=
YOUR_DOMAIN_API=
CORS_DOMAINS=
GDRIVE_CLIENT_ID=
GDRIVE_SECRET=
OPENAI_API_KEY=
QWEN_API_KEY=

edit app-run.conf, replace <<MY_DOMAIN>> by your domain, like https://mn.mydomain.com
edit env.js, replace ___CHANGE_ME !___ by your domain. keep the trailing /api/

start the app using docker compose up

watch the log for admin password :
****** ----------------------- **************
****** Initializing admin user **************
password 25d9ccaa-efaf-4531-ae79-8aea7fd002bc
****** ----------------------- **************

use your fqdn https://mn.mydomain.com to login using the admin user