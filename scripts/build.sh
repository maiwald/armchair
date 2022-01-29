#!/bin/bash

set -e

source .env

rm -fr $RELEASE_FOLDER/*

mkdir -p $RELEASE_FOLDER/compiled/css
mkdir -p $RELEASE_FOLDER/compiled/js

ln -s ../resources/public/css/ $RELEASE_FOLDER/css
ln -s ../resources/public/images/ $RELEASE_FOLDER/images
ln -s ../resources/public/webfonts/ $RELEASE_FOLDER/webfonts

lein cljsbuild once min
sass --sourcemap=none src/sass/app.sass $RELEASE_FOLDER/compiled/css/app.css

mv $RELEASE_FOLDER/compiled/css/app.css $RELEASE_FOLDER/compiled/css/app-$VERSION.css
mv $RELEASE_FOLDER/compiled/js/app.js $RELEASE_FOLDER/compiled/js/app-$VERSION.js

cp resources/public/index.html $RELEASE_FOLDER/index.html
sed \
  -e "s/app\.css/app-$VERSION.css/" \
  -e "s/app\.js/app-$VERSION.js/" \
  $RELEASE_FOLDER/index.html > $RELEASE_FOLDER/index.html.tmp &&
  mv $RELEASE_FOLDER/index.html.tmp $RELEASE_FOLDER/index.html
