
# dev

## UI : install packages and serve vue.js using vite

cd npm-docker
docker build -t npm-ui .
docker run -it -u $(id -u):$(id -g) -p 5173:5173 -v ./ui/:/ui npm-ui sh -c "npm install"
docker run -it -u $(id -u):$(id -g) -p 5173:5173 -v ./ui/:/ui npm-ui sh -c "npm run dev -- --host"

## DB

## spring api app

