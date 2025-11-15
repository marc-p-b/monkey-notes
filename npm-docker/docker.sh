#docker run -it -p 5173:5173 -v "$PWD":/usr/src/app -w /usr/src/app/ui mn-node:latest
docker run -it -p 5173:5173 -v ./ui/:/ui npm-ui /bin/sh
