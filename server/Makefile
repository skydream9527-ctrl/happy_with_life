GO ?= go
BIN := .bin
VERSION ?= $(shell git rev-parse --short HEAD 2>/dev/null || echo dev)
LDFLAGS := -s -w -X main.version=$(VERSION)

.PHONY: tidy test test-race build lint openapi-lint migrate-up migrate-down docker-build run-api

tidy:
	$(GO) mod tidy

test:
	$(GO) test ./...

test-race:
	$(GO) test -race ./...

build:
	mkdir -p $(BIN)
	$(GO) build -ldflags '$(LDFLAGS)' -o $(BIN)/xqx-api ./cmd/api
	$(GO) build -ldflags '$(LDFLAGS)' -o $(BIN)/xqx-worker ./cmd/worker
	$(GO) build -ldflags '$(LDFLAGS)' -o $(BIN)/xqx-migrate ./cmd/migrate
	$(GO) build -ldflags '$(LDFLAGS)' -o $(BIN)/xqx-health ./cmd/health

lint:
	golangci-lint run ./...

openapi-lint:
	npx --yes @redocly/cli@1 lint openapi/openapi.yaml

migrate-up:
	$(GO) run ./cmd/migrate up

migrate-down:
	$(GO) run ./cmd/migrate down

docker-build:
	docker build -t xiaoquexing/xqx-api:$(VERSION) -f deploy/docker/Dockerfile .

run-api:
	$(GO) run ./cmd/api
