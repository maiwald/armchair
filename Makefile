
.PHONY: tmux
tmux:
	./scripts/start_dev.sh

watch:
	npm exec -- shadow-cljs watch app

.PHONY: build
build:
	./scripts/build.sh

release: build
release:
	./scripts/release.sh
