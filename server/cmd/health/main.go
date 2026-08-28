package main

import (
	"net/http"
	"os"
	"strings"
	"time"
)

func main() {
	addr := strings.TrimSpace(os.Getenv("HTTP_ADDR"))
	if addr == "" {
		addr = ":8080"
	}
	if strings.HasPrefix(addr, ":") {
		addr = "127.0.0.1" + addr
	}
	url := "http://" + addr + "/health/live"
	client := &http.Client{Timeout: 2 * time.Second}
	res, err := client.Get(url)
	if err != nil || res.StatusCode != 200 {
		os.Exit(1)
	}
	os.Exit(0)
}
