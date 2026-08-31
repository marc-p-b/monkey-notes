project_root=`pwd`/..
docker=`pwd`/vite-server

echo ">> clean before folder $docker/ui/"
rm -rf $docker/ui/*

cd $docker

echo ">> copy ui/ from $project_root/ui"
cp -R $project_root/ui ./

echo ">> docker build"
docker build . -t monkeynotes/mn-vite

echo ">> clean after folder $docker/ui/"
rm -rf $docker/ui/*