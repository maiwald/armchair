.PHONY: build
build:
	./scripts/build.sh

release: build
release:
	./scripts/release.sh
