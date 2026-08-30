project_root=`pwd`/..
ui=$project_root/ui
vite_local=`pwd`/vite-local
docker=`pwd`/nginx-dist
source_dist=$ui/dist
target_dist=$docker/dist

echo ">> clean before $source_dist, $target_dist"
rm -rf $target_dist
rm -rf $source_dist

cd $vite_local
echo ">> build vite-local-serve"
docker build . -t monkeynotes/mn-vite-local-serve

echo ">> prepare dist"
#mounted from compose
#cp $ui/env-mn.js $ui/public/env.js
cp $ui/index_dist.html $ui/index.html
docker run -it --rm -u $(id -u):$(id -g) -v $ui/:/ui monkeynotes/mn-vite-local-serve:latest bash /dist.sh

cd $docker
echo ">> build mn-ui"
cp -R $source_dist ./
docker build . -t monkeynotes/mn-ui

echo ">> clean after $source_dist, $target_dist"
rm -rf $target_dist
rm -rf $source_dist