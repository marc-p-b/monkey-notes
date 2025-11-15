
# docker

## build api container

mvn clean package
cd docker/api
cp ../../target/mn-api.jar ./
docker build -t mn .
cd ../..

## create ui dist

todo build

cd docker/compose
cp -R ../../npm-docker/ui/dist/* data/nginx/www-ui/

todo edit files (domains)

