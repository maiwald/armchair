#!/bin/bash -e

rm -fr docs/*

mkdir -p docs/css/compiled
mkdir -p docs/js/compiled

cp resources/public/index.html docs/index.html
cp resources/public/css/*.css docs/css/
cp -r resources/public/assets docs/
cp -r resources/public/images docs/
cp -r resources/public/fonts docs/
cp -r resources/public/webfonts docs/

lein cljsbuild once min
sass --sourcemap=none src/sass/app.sass docs/css/compiled/app.css
