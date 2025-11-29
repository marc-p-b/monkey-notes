# info

this docker container is used to :

- run vite server on dev env.
- build dist

## build image

bash run-build.sh

## generate dist (builded files for prod)

bash run-dist.sh

ui/env.js is saved automatically (must not be shipped !)

## dev server

create and set url `ui/env.js` (template ui/_env.js)
(index.html is created with `<script type="module" src="env.js"></script>`)

bash run-serve.sh

