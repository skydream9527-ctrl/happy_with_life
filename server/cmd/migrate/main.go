package main

import (
	"fmt"
	"os"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/dbmigrate"
)

func main() {
	dir := os.Getenv("MIGRATIONS_PATH")
	direction := "up"
	if len(os.Args) > 1 {
		direction = os.Args[1]
	}
	if err := dbmigrate.Run(os.Getenv("POSTGRES_DSN"), dir, direction); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
	fmt.Println("migrate", direction, "ok")
}
