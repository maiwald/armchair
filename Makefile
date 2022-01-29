
.PHONY: tmux
tmux:
	./scripts/start_dev.sh

.PHONY: build
build:
	./scripts/build.sh

release: build
release:
	./scripts/release.sh
