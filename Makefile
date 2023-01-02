.PHONY: watch
watch:
	npm exec -- foreman start

.PHONY: build
build:
	./scripts/build.sh

release: build
release:
	./scripts/release.sh
