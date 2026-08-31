project_root=`pwd`/..
ui_root=$project_root/ui

cp $ui_root/env-dev.js $ui_root/env.js
docker run -it --rm -u $(id -u):$(id -g) -p 5173:5173 -v $ui_root/:/ui npm-ui

