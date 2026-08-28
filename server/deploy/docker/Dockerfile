# syntax=docker/dockerfile:1
FROM golang:1.24.6-bookworm AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
    go build -trimpath -ldflags '-s -w' -o /out/xqx-api ./cmd/api \
 && go build -trimpath -ldflags '-s -w' -o /out/xqx-worker ./cmd/worker \
 && go build -trimpath -ldflags '-s -w' -o /out/xqx-migrate ./cmd/migrate \
 && go build -trimpath -ldflags '-s -w' -o /out/xqx-health ./cmd/health

FROM gcr.io/distroless/static-debian12:nonroot
WORKDIR /app
COPY --from=build /out/xqx-api /usr/local/bin/xqx-api
COPY --from=build /out/xqx-worker /usr/local/bin/xqx-worker
COPY --from=build /out/xqx-migrate /usr/local/bin/xqx-migrate
COPY --from=build /out/xqx-health /usr/local/bin/xqx-health
COPY migrations /app/migrations
USER nonroot:nonroot
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/xqx-api"]
