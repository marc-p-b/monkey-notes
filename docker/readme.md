TODO : adapt to buildV3

# deploy

* copy compose / edit
* set proper profile (dist at least)
* update .env
* copy certs (cloudflare origin)

# config

## env.js

local dev : ui/public/env.js
dist server (nginx) : compose mounted volume docker/compose/data/nginx/env.js
  no longer needs editing - API_URL is the relative '/api/', same origin via traefik
non-dist server (vite) : TODO mount env.js instead of modifying /ui/public/env.js

# containers

## vite-local

name : monkeynotes/mn-vite-local-serve

node container to run local dev ui
also used to build ui as a dist/ version

## api

spring api docker container (server usage with compose)
- Dockerfile
- along jar file (copied by script)

## nginx-dist (ui prefered server version)

nginx container to run vue ui  (server usage with compose)
using ui dist/ version

- Dockerfile
- app-run.conf : nginx config file, no values to set up
  - no server_name : sole server block = default server, traefik already routed by Host
- dist/ : ui dist version (copied by script)


## vite-server (ui alternative server version)

node container to run vue ui  (server usage with compose)
using ui plain version (as in dev environment)

- Dockerfile
- ui/ : ui dev version (copied by script)

# builds

## api

name : monkeynotes/mn-api

run : bash build_api.sh

process :
- maven build
- copy jar to api folder
- build container mn-api image

## nginx-dist

name : monkeynotes/mn-ui

run : bash build-dist.sh

process :
- build vite-local container
- build dist/ using vite-local container
- build container mn-ui image

## vite-server

name : monkeynotes/mn-vite

run : bash build_vite_server.sh

process :
- copy ui folder
- build container mn-vite image