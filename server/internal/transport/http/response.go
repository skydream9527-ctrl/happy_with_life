package httpx

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

type Meta struct {
	RequestID  string `json:"requestId"`
	ServerTime string `json:"serverTime,omitempty"`
}

type Success struct {
	Data any  `json:"data"`
	Meta Meta `json:"meta"`
}

type ErrorDetail struct {
	Code      string `json:"code"`
	Message   string `json:"message"`
	Details   any    `json:"details,omitempty"`
	Retryable bool   `json:"retryable"`
}

type ErrorBody struct {
	Error ErrorDetail `json:"error"`
	Meta  Meta        `json:"meta"`
}

func meta(c *gin.Context) Meta {
	rid, _ := c.Get(ContextRequestID)
	s, _ := rid.(string)
	return Meta{RequestID: s, ServerTime: time.Now().UTC().Format(time.RFC3339Nano)}
}

func WriteOK(c *gin.Context, data any) {
	c.JSON(http.StatusOK, Success{Data: data, Meta: meta(c)})
}

func WriteCreated(c *gin.Context, data any) {
	c.JSON(http.StatusCreated, Success{Data: data, Meta: meta(c)})
}

func WriteError(c *gin.Context, status int, code, message string, retryable bool, details any) {
	c.AbortWithStatusJSON(status, ErrorBody{
		Error: ErrorDetail{Code: code, Message: message, Details: details, Retryable: retryable},
		Meta:  meta(c),
	})
}

const (
	CodeAuthRequired   = "AUTH_REQUIRED"
	CodeTokenExpired   = "TOKEN_EXPIRED"
	CodeRefreshReused  = "REFRESH_REUSED"
	CodeSMSRateLimited = "SMS_RATE_LIMITED"
	CodeSMSCodeInvalid = "SMS_CODE_INVALID"
	CodeSMSUnavailable = "SERVICE_UNAVAILABLE"
	CodeSpaceForbidden = "SPACE_FORBIDDEN"
	CodeRecordInvalid  = "RECORD_INVALID"
	CodeInternal       = "INTERNAL_ERROR"
	CodeUnavailable    = "SERVICE_UNAVAILABLE"
	CodeInvalidRequest = "RECORD_INVALID"
)

func WriteInvalid(c *gin.Context, message string) {
	WriteError(c, http.StatusBadRequest, CodeInvalidRequest, message, false, nil)
}

func WriteUnauthorized(c *gin.Context, code, message string) {
	WriteError(c, http.StatusUnauthorized, code, message, false, nil)
}

func WriteRateLimited(c *gin.Context, retryAfter int) {
	c.Header("Retry-After", itoa(retryAfter))
	WriteError(c, http.StatusTooManyRequests, CodeSMSRateLimited, "发送过于频繁，请稍后再试", true, map[string]int{"retryAfterSec": retryAfter})
}

func WriteInternal(c *gin.Context) {
	WriteError(c, http.StatusInternalServerError, CodeInternal, "服务暂时不可用", true, nil)
}

func WriteUnavailable(c *gin.Context, message string) {
	WriteError(c, http.StatusServiceUnavailable, CodeUnavailable, message, true, nil)
}

func itoa(n int) string {
	if n == 0 {
		return "0"
	}
	var b [16]byte
	i := len(b)
	for n > 0 {
		i--
		b[i] = byte('0' + n%10)
		n /= 10
	}
	return string(b[i:])
}
