#!/bin/bash -e

rm -fr \
  docs/app.js \
  docs/css/ \
  docs/assets \
  docs/images \
  docs/fonts \
  docs/webfonts

mkdir -p docs/css
cp resources/public/css/*.css docs/css/

cp -r resources/public/assets docs/
cp -r resources/public/images docs/
cp -r resources/public/fonts docs/
cp -r resources/public/webfonts docs/

lein cljsbuild once min
sass --sourcemap=none src/sass/app.sass docs/css/app.css
