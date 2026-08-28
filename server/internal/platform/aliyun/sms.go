package aliyun

import (
	"context"
	"fmt"
	"log/slog"
)

// SMSProvider sends verification codes. Implementations must never log the
// plaintext code or full phone number.
type SMSProvider interface {
	Name() string
	Send(ctx context.Context, e164Phone, code string) (providerRequestID string, err error)
}

// MockSMS is the default Dev provider. It does not print the verification code.
type MockSMS struct {
	Log *slog.Logger
}

func (m MockSMS) Name() string { return "mock" }

func (m MockSMS) Send(_ context.Context, _, _ string) (string, error) {
	if m.Log != nil {
		m.Log.Info("mock SMS dispatched", "hint", "use configured SMS_DEV_CODE; plaintext code is never logged")
	}
	return "mock-ok", nil
}

// AliyunSMS is a typed provider. Without RAM Role / SDK credentials it must
// fail closed instead of faking a successful send.
type AliyunSMS struct {
	SignName     string
	TemplateCode string
	Endpoint     string
	Configured   bool
	Log          *slog.Logger
}

func (a AliyunSMS) Name() string { return "aliyun" }

func (a AliyunSMS) Send(_ context.Context, _, _ string) (string, error) {
	if !a.Configured || a.SignName == "" || a.TemplateCode == "" {
		if a.Log != nil {
			a.Log.Warn("aliyun SMS skipped: credentials or template not configured")
		}
		return "", fmt.Errorf("aliyun SMS is not configured")
	}
	// Real Dysmsapi call is wired in S6 when ECS RAM Role is available.
	return "", fmt.Errorf("aliyun SMS runtime is not enabled in S0/S1")
}
