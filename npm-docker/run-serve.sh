
project_root=`pwd`/..
ui_root=$project_root/ui

if [ -f $ui_root/env.js ];then
  cp $ui_root/index_serve.html $ui_root/index.html
  docker run -it --rm -u $(id -u):$(id -g) -p 5173:5173 -v $ui_root/:/ui npm-ui
else
  echo "--- ERROR ---"
  echo "Please provide ${ui_root}/env.js"
fi
