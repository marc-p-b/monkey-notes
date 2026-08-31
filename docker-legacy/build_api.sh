project_root=`pwd`/..
docker_api_root=`pwd`/api
api_jar=$project_root/target/mn-api.jar

cd $project_root
mvn clean install -DskipTests

cd $docker_api_root
cp $api_jar ./

docker build . -t monkeynotes/mn-api

#docker login
#docker push monkeynotes/mn-api:latest