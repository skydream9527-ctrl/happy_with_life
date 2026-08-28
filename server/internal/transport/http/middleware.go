package httpx

import (
	"context"
	"log/slog"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
)

const (
	ContextRequestID = "requestId"
	ContextUserID    = "userId"
	ContextDeviceID  = "deviceId"
	HeaderRequestID  = "X-Request-ID"
	HeaderDeviceID   = "X-Device-ID"
)

func RequestID() gin.HandlerFunc {
	return func(c *gin.Context) {
		rid := c.GetHeader(HeaderRequestID)
		if rid == "" {
			rid = id.New()
		}
		c.Set(ContextRequestID, rid)
		c.Writer.Header().Set(HeaderRequestID, rid)
		c.Next()
	}
}

func Recovery(log *slog.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if rec := recover(); rec != nil {
				log.Error("panic recovered", "err", rec, "path", c.Request.URL.Path, "requestId", c.GetString(ContextRequestID))
				WriteInternal(c)
			}
		}()
		c.Next()
	}
}

func Timeout(d time.Duration) gin.HandlerFunc {
	return func(c *gin.Context) {
		if d <= 0 || skipLargeBody(c) {
			c.Next()
			return
		}
		ctx, cancel := context.WithTimeout(c.Request.Context(), d)
		defer cancel()
		c.Request = c.Request.WithContext(ctx)
		c.Next()
	}
}

func BodyLimit(n int64) gin.HandlerFunc {
	return func(c *gin.Context) {
		if skipLargeBody(c) {
			c.Next()
			return
		}
		if n > 0 && c.Request.Body != nil {
			c.Request.Body = http.MaxBytesReader(c.Writer, c.Request.Body, n)
		}
		c.Next()
	}
}

func AccessLog(log *slog.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		c.Next()
		log.Info("http",
			"method", c.Request.Method,
			"path", c.FullPath(),
			"status", c.Writer.Status(),
			"latencyMs", time.Since(start).Milliseconds(),
			"requestId", c.GetString(ContextRequestID),
			"ip", clientIP(c),
		)
	}
}

func CORS(origins []string) gin.HandlerFunc {
	allowed := map[string]struct{}{}
	for _, o := range origins {
		allowed[o] = struct{}{}
	}
	return func(c *gin.Context) {
		origin := c.GetHeader("Origin")
		if origin == "" {
			c.Next()
			return
		}
		if _, ok := allowed[origin]; ok || (len(origins) == 1 && origins[0] == "*") {
			c.Header("Access-Control-Allow-Origin", origin)
			c.Header("Vary", "Origin")
			c.Header("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Request-ID, X-Device-ID, Idempotency-Key, X-App-Version, X-Platform")
			c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
			c.Header("Access-Control-Max-Age", "600")
		}
		if c.Request.Method == http.MethodOptions {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}
		c.Next()
	}
}

func clientIP(c *gin.Context) string {
	if xff := c.GetHeader("X-Forwarded-For"); xff != "" {
		return xff
	}
	return c.ClientIP()
}
