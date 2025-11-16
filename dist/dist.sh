#!/bin/bash

DOMAIN=$1

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
mkdir -p ../docker/compose/data/nginx/www-ui
cp -R ui/dist/* ../docker/compose/data/nginx/www-ui/