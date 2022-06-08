#!/bin/bash

set -e

source .env

rm -fr $RELEASE_FOLDER/*

mkdir -p $RELEASE_FOLDER/compiled/css
mkdir -p $RELEASE_FOLDER/compiled/js

ln -s ../resources/public/css/ $RELEASE_FOLDER/css
ln -s ../resources/public/images/ $RELEASE_FOLDER/images
ln -s ../resources/public/webfonts/ $RELEASE_FOLDER/webfonts

npm exec -- shadow-cljs release app \
  --config-merge "{:output-dir \"$RELEASE_FOLDER/compiled/js/\"}"
npm exec -- sass --source-map src/sass/app.sass $RELEASE_FOLDER/compiled/css/app.css

mv $RELEASE_FOLDER/compiled/css/app{,-$VERSION}.css
mv $RELEASE_FOLDER/compiled/css/app{,-$VERSION}.css.map
sed -i "" \
  -e "s/sourceMappingURL=app\.css\.map/sourceMappingURL=app-$VERSION.css.map/" \
  $RELEASE_FOLDER/compiled/css/app-$VERSION.css

mv $RELEASE_FOLDER/compiled/js/app{,-$VERSION}.js
mv $RELEASE_FOLDER/compiled/js/app{,-$VERSION}.js.map
sed -i "" \
  -e "s/sourceMappingURL=app\.js\.map/sourceMappingURL=app-$VERSION.js.map/" \
  $RELEASE_FOLDER/compiled/js/app-$VERSION.js

cp resources/public/index.html $RELEASE_FOLDER/index.html
sed -i "" \
  -e "s/app\.css/app-$VERSION.css/" \
  -e "s/app\.js/app-$VERSION.js/" \
  $RELEASE_FOLDER/index.html
