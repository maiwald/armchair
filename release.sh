#!/bin/bash

set -e

source .env
source build.sh

aws s3 sync \
  --follow-symlinks \
  --acl public-read \
  --delete \
  $RELEASE_FOLDER s3://$ARMCHAIR_S3_BUCKET/alpha

aws cloudfront create-invalidation \
  --distribution-id $ARMCHAIR_CLOUDFRONT_DISTRIBUTION_ID \
  --paths /alpha /alpha/ /alpha/index.html /alpha/compiled/*
