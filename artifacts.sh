VERSION=$1
cd ..
zip -r openrefine-${VERSION}.zip openrefine
scp openrefine-${VERSION}.zip root@140.96.83.224:/home/workspace/www/html/fastai/
rm openrefine-${VERSION}.zip