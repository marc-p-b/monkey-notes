# config

## env.js

local dev : ui/public/env.js
dist server (nginx) : compose mounted volume docker/compose/data/nginx/env.js
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
- app-run.conf : nginx config file with values to setup
  - server_name <<YOUR_DOMAIN>>;
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