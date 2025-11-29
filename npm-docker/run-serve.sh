if [ -f ./ui/env.js ];then
  cp ./ui/index_serve.html ./ui/index.html
  docker run -it --rm -u $(id -u):$(id -g) -p 5173:5173 -v ./ui/:/ui npm-ui
else
  echo "--- ERROR ---"
  echo "Please provide /ui/env.js"
fi
