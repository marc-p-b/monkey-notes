project_root=`pwd`/..
ui=$project_root/ui
vite_local=`pwd`/vite-local
docker=`pwd`/nginx-dist
source_dist=$ui/dist
target_dist=$docker/dist

docker_username=monkeynotes
docker_uibuild="${docker_username}/vite-local-serve"
docker_ui="${docker_username}/ui"

echo ">> clean before $source_dist, $target_dist"
rm -rf $target_dist
rm -rf $source_dist

cd $vite_local
echo ">> build vite-local-serve"
docker build . -t "${docker_uibuild}"

echo ">> prepare dist"
docker run -it --rm -u $(id -u):$(id -g) -v $ui/:/ui "${docker_uibuild}":latest bash /dist.sh

cd $docker
echo ">> build ui"
cp -R $source_dist ./
docker build . -t "${docker_ui}"

echo ">> clean after $source_dist, $target_dist"
rm -rf $target_dist
rm -rf $source_dist
