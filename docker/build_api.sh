project_root=`pwd`/..
docker_root="${project_root}/docker"

docker_api_build_root="${docker_root}/api-build"
docker_api_root="${docker_root}/api"
api_jar="${docker_api_build_root}/app.jar"

docker_username=monkeynotes
docker_apibuild="${docker_username}/api-build"
docker_api="${docker_username}/api"

# jar builder container
docker build -t "${docker_apibuild}" api-build

# build using container
docker run --rm -u "$(id -u):$(id -g)" -v "$project_root":/app "${docker_apibuild}"

cp "$api_jar" "$docker_api_root/"
docker build api/ -t "${docker_api}"

rm "$api_jar"

#docker login
#docker push almatrade/api:latest

