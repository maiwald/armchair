#!/bin/bash -e

VERSION=$(git rev-parse HEAD)
RELEASE_FOLDER=build

rm -fr $RELEASE_FOLDER/*

mkdir -p $RELEASE_FOLDER/css/compiled
mkdir -p $RELEASE_FOLDER/js/compiled

cp resources/public/css/*.css $RELEASE_FOLDER/css/
cp -r resources/public/assets $RELEASE_FOLDER/
cp -r resources/public/images $RELEASE_FOLDER/
cp -r resources/public/fonts $RELEASE_FOLDER/
cp -r resources/public/webfonts $RELEASE_FOLDER/

lein cljsbuild once min
sass --sourcemap=none src/sass/app.sass $RELEASE_FOLDER/css/compiled/app.css

mv $RELEASE_FOLDER/css/compiled/app.css $RELEASE_FOLDER/css/compiled/app-$VERSION.css
mv $RELEASE_FOLDER/js/compiled/app.js $RELEASE_FOLDER/js/compiled/app-$VERSION.js

cp resources/public/index.html $RELEASE_FOLDER/index.html
sed \
  -e "s/app\.css/app-$VERSION.css/" \
  -e "s/app\.js/app-$VERSION.js/" \
  $RELEASE_FOLDER/index.html > $RELEASE_FOLDER/index.html.tmp &&
  mv $RELEASE_FOLDER/index.html.tmp $RELEASE_FOLDER/index.html
