package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/alicebob/miniredis/v2"
	goredis "github.com/redis/go-redis/v9"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/auth"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/config"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/dbmigrate"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/ledger"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/obs"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/platform/aliyun"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/platform/postgres"
	redisx "github.com/skydream9527-ctrl/xiaoquexing-server/internal/platform/redis"
	httpx "github.com/skydream9527-ctrl/xiaoquexing-server/internal/transport/http"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		slog.Error("config", "err", err)
		os.Exit(1)
	}
	log := obs.NewLogger(cfg.LogLevel, cfg.AppEnv)
	if err := run(cfg, log); err != nil {
		log.Error("api exit", "err", err)
		os.Exit(1)
	}
}

func run(cfg config.Config, log *slog.Logger) error {
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	storeMode := "postgres"
	var store auth.Store
	var ledStore ledger.Store
	var pgReady func(context.Context) error
	var closer func()

	if cfg.PostgresDSN != "" {
		pool, err := postgres.Connect(ctx, cfg.PostgresDSN)
		if err != nil {
			return err
		}
		store = auth.NewPGStore(pool)
		ledStore = ledger.NewPG(pool)
		pgReady = func(c context.Context) error { return pool.Ping(c) }
		closer = pool.Close
		if cfg.RunMigrations {
			path := os.Getenv("MIGRATIONS_PATH")
			if path == "" {
				path = "file://migrations"
			}
			if err := dbmigrate.Run(cfg.PostgresDSN, path, "up"); err != nil {
				pool.Close()
				return err
			}
			log.Info("migrations applied")
		}
		log.Info("postgres connected")
	} else if cfg.AllowInMemory {
		store = auth.NewMemoryStore()
		ledStore = ledger.NewMemory()
		storeMode = "memory"
		log.Warn("DEV_INMEMORY enabled; data is not durable and must not be used in staging/prod")
	} else {
		return errors.New("POSTGRES_DSN is required (or set DEV_INMEMORY=true in dev)")
	}

	var rdb *redisx.Client
	var mini *miniredis.Miniredis
	if cfg.AllowInMemory && cfg.RedisAddr == "127.0.0.1:6379" && os.Getenv("REDIS_ADDR") == "" {
		m, err := miniredis.Run()
		if err != nil {
			return err
		}
		mini = m
		client := goredis.NewClient(&goredis.Options{Addr: m.Addr()})
		rdb = redisx.NewWith(client, cfg.RedisKeyPrefix)
		log.Warn("using in-process redis (miniredis) for dev")
	} else {
		c, err := redisx.Connect(cfg.RedisAddr, cfg.RedisPassword, cfg.RedisDB, cfg.RedisKeyPrefix)
		if err != nil {
			if !cfg.AllowInMemory {
				if closer != nil {
					closer()
				}
				return err
			}
			log.Warn("redis connect failed, falling back to miniredis", "err", err)
			m, err2 := miniredis.Run()
			if err2 != nil {
				if closer != nil {
					closer()
				}
				return err2
			}
			mini = m
			client := goredis.NewClient(&goredis.Options{Addr: m.Addr()})
			rdb = redisx.NewWith(client, cfg.RedisKeyPrefix)
		} else {
			rdb = c
		}
	}

	var sms aliyun.SMSProvider
	switch cfg.SMSProvider {
	case "aliyun":
		sms = aliyun.AliyunSMS{
			SignName:     cfg.AliyunSMSSignName,
			TemplateCode: cfg.AliyunSMSTemplate,
			Endpoint:     cfg.AliyunSMSEndpoint,
			Configured:   cfg.AliyunSMSSignName != "" && cfg.AliyunSMSTemplate != "",
			Log:          log,
		}
	default:
		sms = aliyun.MockSMS{Log: log}
	}

	svc := auth.NewService(cfg, store, rdb, sms, log)
	ledSvc := ledger.NewService(ledStore)
	svc.Bootstrap = ledSvc
	engine := httpx.NewRouter(cfg, log, httpx.Deps{
		ReadyPostgres: pgReady,
		ReadyRedis:    rdb.HealthError,
		StoreMode:     storeMode,
		Auth:          svc,
		Ledger:        ledSvc,
	})

	srv := &http.Server{
		Addr:              cfg.HTTPAddr,
		Handler:           engine,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       cfg.HTTPTimeout + 5*time.Second,
		WriteTimeout:      cfg.HTTPTimeout + 5*time.Second,
		IdleTimeout:       60 * time.Second,
	}

	errCh := make(chan error, 1)
	go func() {
		log.Info("xqx-api listening", "addr", cfg.HTTPAddr, "mode", storeMode)
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			errCh <- err
		}
		close(errCh)
	}()

	select {
	case <-ctx.Done():
	case err := <-errCh:
		if err != nil {
			return err
		}
	}

	shutCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(shutCtx)
	_ = rdb.Close()
	if mini != nil {
		mini.Close()
	}
	if closer != nil {
		closer()
	}
	log.Info("xqx-api stopped")
	return nil
}
