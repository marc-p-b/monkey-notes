#!/bin/bash

# ------------
#  !!! WIP !!!
# ------------


echo "build spring api app"
mvn clean package
cp target/mn-api.jar docker/api/

echo "build docker api container"
cd docker/api/
docker build -t monkeynotes/mn-api .
docker login
docker push monkeynotes/mn-api
cd ../..

echo "build ui dist"
cd npm_docker
docker build -t npm-ui .
docker run -it -u $(id -u):$(id -g) -v ./ui/:/ui npm-ui sh -c "npm install"
docker run -it -u $(id -u):$(id -g) -v ./ui/:/ui npm-ui sh -c "npm run build"
cd ..

echo "build docker nginx container"
cd docker/nginx
cp -R ../../npm-docker/ui/dist/* ../docker/compose/data/nginx/www-ui/

# REPLACE ui/index.html %RUNTIME_CONFIG% by <script type="module" src="env.js"></script>

docker build -t monkeynotes/mn-nginx .
docker login
docker push monkeynotes/mn-nginx