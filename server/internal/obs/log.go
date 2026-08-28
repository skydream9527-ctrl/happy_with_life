package obs

import (
	"log/slog"
	"os"
	"strings"
)

func NewLogger(level, env string) *slog.Logger {
	var lv slog.Level
	switch strings.ToLower(level) {
	case "debug":
		lv = slog.LevelDebug
	case "warn":
		lv = slog.LevelWarn
	case "error":
		lv = slog.LevelError
	default:
		lv = slog.LevelInfo
	}
	h := slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: lv,
		ReplaceAttr: func(_ []string, a slog.Attr) slog.Attr {
			switch a.Key {
			case "phone", "code", "smsCode", "accessToken", "refreshToken", "authorization", "token":
				return slog.String(a.Key, "[redacted]")
			}
			return a
		},
	})
	return slog.New(h).With("service", "xqx-api", "env", env)
}
