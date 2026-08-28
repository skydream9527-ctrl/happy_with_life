package dbmigrate

import (
	"database/sql"
	"fmt"

	"github.com/golang-migrate/migrate/v4"
	"github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	_ "github.com/jackc/pgx/v5/stdlib"
)

func Run(dsn, dir, direction string) error {
	if dsn == "" {
		return fmt.Errorf("POSTGRES_DSN is required")
	}
	if dir == "" {
		dir = "file://migrations"
	}
	if direction == "" {
		direction = "up"
	}
	db, err := sql.Open("pgx", dsn)
	if err != nil {
		return err
	}
	defer db.Close()
	driver, err := postgres.WithInstance(db, &postgres.Config{})
	if err != nil {
		return err
	}
	m, err := migrate.NewWithDatabaseInstance(dir, "postgres", driver)
	if err != nil {
		return err
	}
	switch direction {
	case "up":
		err = m.Up()
	case "down":
		err = m.Down()
	default:
		return fmt.Errorf("usage: migrate [up|down]")
	}
	if err != nil && err != migrate.ErrNoChange {
		return err
	}
	return nil
}
