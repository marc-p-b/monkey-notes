
project_root=`pwd`/..
ui_root=$project_root/ui

saved=0

if [ -f $ui_root/env.js ];then
  echo ">> Saving env.js"
  mv $ui_root/env.js /tmp/env.js && saved=1
fi

rm -rf $ui_root/dist
cp $ui_root/index_dist.html $ui_root/index.html
docker run -it --rm -u $(id -u):$(id -g) -p 5173:5173 -v $ui_root/:/ui npm-ui bash /dist.sh

if [ $saved = 1 ];then
  echo ">> Restoring env.js"
  mv /tmp/env.js $ui_root/env.js
fi
