package httpx

import (
	"context"
	"log/slog"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/auth"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/config"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/ledger"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/media"
)

type Deps struct {
	ReadyPostgres func(context.Context) error
	ReadyRedis    func(context.Context) error
	StoreMode     string
	Auth          *auth.Service
	Ledger        *ledger.Service
	Media         *media.Service
	PublicBaseURL string
}

func NewRouter(cfg config.Config, log *slog.Logger, deps Deps) *gin.Engine {
	gin.SetMode(gin.ReleaseMode)
	r := gin.New()
	r.Use(RequestID(), Recovery(log), Timeout(cfg.HTTPTimeout), BodyLimit(cfg.HTTPMaxBodyBytes), AccessLog(log))
	origins := cfg.CORSOrigins
	if len(origins) == 0 && (cfg.AppEnv == "dev" || cfg.AppEnv == "test") {
		origins = []string{"*"}
	}
	if len(origins) > 0 {
		r.Use(CORS(origins))
	}

	r.GET("/health/live", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status": "ok",
			"meta":   Meta{RequestID: c.GetString(ContextRequestID), ServerTime: time.Now().UTC().Format(time.RFC3339Nano)},
		})
	})
	r.GET("/health/ready", func(c *gin.Context) {
		ctx, cancel := context.WithTimeout(c.Request.Context(), 2*time.Second)
		defer cancel()
		checks := map[string]string{}
		ready := true
		if deps.ReadyPostgres != nil {
			if err := deps.ReadyPostgres(ctx); err != nil {
				checks["postgres"] = "down"
				ready = false
			} else {
				checks["postgres"] = "ok"
			}
		} else {
			checks["postgres"] = deps.StoreMode
		}
		if deps.ReadyRedis != nil {
			if err := deps.ReadyRedis(ctx); err != nil {
				checks["redis"] = "down"
				ready = false
			} else {
				checks["redis"] = "ok"
			}
		}
		status := http.StatusOK
		if !ready {
			status = http.StatusServiceUnavailable
		}
		label := "not_ready"
		if ready {
			label = "ok"
		}
		c.JSON(status, gin.H{
			"status": label,
			"checks": checks,
			"mode":   deps.StoreMode,
			"meta":   Meta{RequestID: c.GetString(ContextRequestID), ServerTime: time.Now().UTC().Format(time.RFC3339Nano)},
		})
	})

	r.GET("/api/v1/meta", func(c *gin.Context) {
		WriteOK(c, gin.H{
			"name":               "xiaoquexing-server",
			"process":            "xqx-api",
			"version":            cfg.AppVersion,
			"env":                cfg.AppEnv,
			"domainRulesVersion": config.DomainRulesVersion,
			"storeMode":          deps.StoreMode,
			"smsProvider":        cfg.SMSProvider,
			"ossProvider":        cfg.OSSProvider,
			"mediaQuotaBytes":    cfg.MediaQuotaBytes,
			"maxPhotoBytes":      cfg.MaxPhotoBytes,
			"accountDeleteGraceSec": int(cfg.AccountDeleteGrace.Seconds()),
			"inviteMethod":       "link",
			"spaceReadReceipts":  false,
			"accessTokenTtlSec":  int(cfg.AccessTokenTTL.Seconds()),
			"refreshTokenTtlSec": int(cfg.RefreshTokenTTL.Seconds()),
		})
	})

	h := authHandlers{svc: deps.Auth}
	v1 := r.Group("/api/v1")
	v1.POST("/auth/sms/send", h.send)
	v1.POST("/auth/sms/verify", h.verify)
	v1.POST("/auth/token/refresh", h.refresh)
	v1.POST("/auth/logout", optionalBearer(deps.Auth), h.logout)
	v1.GET("/me", bearerAuth(deps.Auth), h.me)
	v1.PATCH("/me", bearerAuth(deps.Auth), h.patchMe)
	v1.DELETE("/account", bearerAuth(deps.Auth), h.deleteAccount)

	if deps.Ledger != nil {
		lh := ledgerHandlers{svc: deps.Ledger, media: deps.Media, public: deps.PublicBaseURL}
		authed := v1.Group("")
		authed.Use(bearerAuth(deps.Auth))
		authed.GET("/spaces", lh.spaces)
		authed.POST("/spaces", lh.createSpace)
		authed.GET("/spaces/:id", lh.space)
		authed.PATCH("/spaces/:id", lh.patchSpace)
		authed.GET("/spaces/:id/plant", lh.plant)
		authed.GET("/spaces/:id/members", lh.members)
		authed.POST("/spaces/:id/invites", lh.createInvite)
		authed.GET("/spaces/:id/invites", lh.listInvites)
		authed.DELETE("/spaces/:id/invites/:inviteId", lh.revokeInvite)
		authed.POST("/spaces/:id/leave", lh.leaveSpace)
		authed.DELETE("/spaces/:id/members/:userId", lh.kickMember)
		authed.POST("/invites/accept", lh.acceptInvite)
		authed.POST("/invites/:token/accept", lh.acceptInvite)
		authed.GET("/stats/calendar", lh.calendar)
		authed.GET("/records", lh.listRecords)
		authed.GET("/records/:id", lh.getRecord)
		authed.POST("/records", lh.createRecord)
		authed.PATCH("/records/:id", lh.patchRecord)
		authed.DELETE("/records/:id", lh.deleteRecord)
		authed.POST("/sync/push", lh.push)
		authed.GET("/sync/pull", lh.pull)
		if deps.Media != nil {
			mh := mediaHandlers{svc: deps.Media, ledger: deps.Ledger, public: deps.PublicBaseURL}
			authed.POST("/media/sts", mh.sts)
			authed.POST("/media/complete", mh.complete)
			authed.GET("/media/quota", mh.quota)
			authed.GET("/media/:id/download-url", mh.downloadURL)
			authed.DELETE("/media/:id", mh.delete)
		}
	}
	if deps.Ledger != nil {
		v1.GET("/invites/:token", ledgerHandlers{svc: deps.Ledger, public: deps.PublicBaseURL}.peekInvite)
	}
	if deps.Media != nil {
		mh := mediaHandlers{svc: deps.Media, ledger: deps.Ledger, public: deps.PublicBaseURL}
		v1.PUT("/media/upload/:ticket", mh.upload)
		v1.GET("/media/content/:ticket", mh.content)
	}
	return r
}
