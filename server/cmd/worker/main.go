package main

import (
	"os"
	"os/signal"
	"syscall"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/obs"
)

func main() {
	log := obs.NewLogger(getenv("LOG_LEVEL", "info"), getenv("APP_ENV", "dev"))
	log.Info("xqx-worker idle", "note", "S0/S1 has no background jobs; process waits for SIGTERM")
	ch := make(chan os.Signal, 1)
	signal.Notify(ch, syscall.SIGINT, syscall.SIGTERM)
	<-ch
	log.Info("xqx-worker stopped")
}

func getenv(k, d string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return d
}
