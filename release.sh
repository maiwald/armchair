#!/bin/bash -e

VERSION=$(git rev-parse HEAD)

rm -fr docs/*

mkdir -p docs/css/compiled
mkdir -p docs/js/compiled

cp resources/public/css/*.css docs/css/
cp -r resources/public/assets docs/
cp -r resources/public/images docs/
cp -r resources/public/fonts docs/
cp -r resources/public/webfonts docs/

lein cljsbuild once min
sass --sourcemap=none src/sass/app.sass docs/css/compiled/app.css

mv docs/css/compiled/app.css docs/css/compiled/app-$VERSION.css
mv docs/js/compiled/app.js docs/js/compiled/app-$VERSION.js

cp resources/public/index.html docs/index.html
sed \
  -e "s/app\.css/app-$VERSION.css/" \
  -e "s/app\.js/app-$VERSION.js/" \
  docs/index.html > docs/index.html.tmp &&
  mv docs/index.html.tmp docs/index.html
