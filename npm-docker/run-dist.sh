#rm -rf ./ui/dist
saved=0
if [ -f ./ui/env.js ];then
  echo ">> Saving env.js"
  mv ./ui/env.js /tmp/env.js && saved=1
fi
cp ./ui/index_dist.html ./ui/index.html
docker run -it --rm -u $(id -u):$(id -g) -p 5173:5173 -v ./ui/:/ui npm-ui bash /dist.sh
if [ $saved = 1 ];then
  echo ">> Restoring env.js"
  mv /tmp/env.js ./ui/env.js
fi
