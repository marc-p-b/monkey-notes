
project_root=`pwd`

backend_docker_root=$project_root/docker/api/
npm_root=$project_root/npm-docker
ui_dist=$npm_root/ui/dist
ui_docker_root=$project_root/docker/nginx

jar_name="mn-api.jar"

echo
echo "-- ----------------------"
echo ">> Cleaning"
echo "-- ----------------------"
echo

mvn clean
rm -f $backend_docker_root/$jar_name
rm -rf $ui_docker_root/dist
rm -rf $ui_dist

echo
echo "-- ----------------------"
echo ">> Done"
echo "-- ----------------------"
echo