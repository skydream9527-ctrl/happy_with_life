package httpx

import (
	"errors"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/auth"
)

type authHandlers struct {
	svc *auth.Service
}

type sendReq struct {
	Phone    string `json:"phone"`
	DeviceID string `json:"deviceId"`
}

type verifyReq struct {
	Phone      string `json:"phone"`
	Code       string `json:"code"`
	DeviceID   string `json:"deviceId"`
	Platform   string `json:"platform"`
	AppVersion string `json:"appVersion"`
}

type refreshReq struct {
	RefreshToken string `json:"refreshToken"`
	DeviceID     string `json:"deviceId"`
}

type logoutReq struct {
	RefreshToken string `json:"refreshToken"`
}

type passwordReq struct {
	Account    string `json:"account"`
	Password   string `json:"password"`
	DeviceID   string `json:"deviceId"`
	Platform   string `json:"platform"`
	AppVersion string `json:"appVersion"`
}

func (h authHandlers) send(c *gin.Context) {
	var req sendReq
	if err := c.ShouldBindJSON(&req); err != nil {
		WriteInvalid(c, "请求体无效")
		return
	}
	device := firstNonEmpty(req.DeviceID, c.GetHeader(HeaderDeviceID))
	err := h.svc.SendSMS(c.Request.Context(), req.Phone, device, c.ClientIP())
	switch {
	case err == nil:
		WriteOK(c, gin.H{"accepted": true, "retryAfterSec": h.svc.RetrySec})
	case errors.Is(err, auth.ErrInvalidPhone):
		WriteInvalid(c, "手机号格式不正确")
	case errors.Is(err, auth.ErrRateLimited):
		WriteRateLimited(c, h.svc.RetrySec)
	case errors.Is(err, auth.ErrSMSUnavailable):
		WriteUnavailable(c, "验证码服务暂不可用")
	default:
		WriteInternal(c)
	}
}

func (h authHandlers) verify(c *gin.Context) {
	var req verifyReq
	if err := c.ShouldBindJSON(&req); err != nil {
		WriteInvalid(c, "请求体无效")
		return
	}
	device := firstNonEmpty(req.DeviceID, c.GetHeader(HeaderDeviceID))
	platform := firstNonEmpty(req.Platform, c.GetHeader("X-Platform"))
	appv := firstNonEmpty(req.AppVersion, c.GetHeader("X-App-Version"))
	pair, err := h.svc.VerifySMS(c.Request.Context(), req.Phone, strings.TrimSpace(req.Code), device, platform, appv)
	switch {
	case err == nil:
		WriteOK(c, pair)
	case errors.Is(err, auth.ErrInvalidPhone), errors.Is(err, auth.ErrInvalidCode):
		WriteUnauthorized(c, CodeSMSCodeInvalid, "验证码无效或已过期")
	case errors.Is(err, auth.ErrSMSUnavailable):
		WriteUnavailable(c, "验证码服务暂不可用")
	default:
		WriteInternal(c)
	}
}

func (h authHandlers) register(c *gin.Context) {
	h.passwordAuth(c, true)
}

func (h authHandlers) login(c *gin.Context) {
	h.passwordAuth(c, false)
}

func (h authHandlers) passwordAuth(c *gin.Context, register bool) {
	var req passwordReq
	if err := c.ShouldBindJSON(&req); err != nil {
		WriteInvalid(c, "请求体无效")
		return
	}
	device := firstNonEmpty(req.DeviceID, c.GetHeader(HeaderDeviceID))
	platform := firstNonEmpty(req.Platform, c.GetHeader("X-Platform"))
	appv := firstNonEmpty(req.AppVersion, c.GetHeader("X-App-Version"))
	var pair *auth.TokenPair
	var err error
	if register {
		pair, err = h.svc.Register(req.Account, req.Password, device, platform, appv)
	} else {
		pair, err = h.svc.Login(req.Account, req.Password, device, platform, appv)
	}
	switch {
	case err == nil:
		WriteOK(c, pair)
	case errors.Is(err, auth.ErrInvalidAccount):
		WriteInvalid(c, "账号需为 3-32 位字母数字或下划线")
	case errors.Is(err, auth.ErrWeakPassword):
		WriteInvalid(c, "密码至少 6 位")
	case errors.Is(err, auth.ErrAccountTaken):
		WriteInvalid(c, "账号已被注册")
	case errors.Is(err, auth.ErrInvalidCredentials):
		WriteUnauthorized(c, CodeSMSCodeInvalid, "账号或密码不正确")
	default:
		WriteInternal(c)
	}
}

type changePasswordReq struct {
	OldPassword string `json:"oldPassword"`
	NewPassword string `json:"newPassword"`
}

type resetPasswordReq struct {
	NewPassword string `json:"newPassword"`
}

func (h authHandlers) changePassword(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	if userID == "" {
		WriteUnauthorized(c, CodeAuthRequired, "需要登录")
		return
	}
	var req changePasswordReq
	if err := c.ShouldBindJSON(&req); err != nil {
		WriteInvalid(c, "请求体无效")
		return
	}
	h.writePasswordErr(c, h.svc.ChangePassword(userID, req.OldPassword, req.NewPassword))
}

func (h authHandlers) resetPassword(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	if userID == "" {
		WriteUnauthorized(c, CodeAuthRequired, "需要登录")
		return
	}
	var req resetPasswordReq
	if err := c.ShouldBindJSON(&req); err != nil {
		WriteInvalid(c, "请求体无效")
		return
	}
	h.writePasswordErr(c, h.svc.ResetPasswordOnDevice(userID, req.NewPassword))
}

func (h authHandlers) writePasswordErr(c *gin.Context, err error) {
	switch {
	case err == nil:
		WriteOK(c, gin.H{"ok": true})
	case errors.Is(err, auth.ErrWeakPassword):
		WriteInvalid(c, "密码至少 6 位")
	case errors.Is(err, auth.ErrInvalidCredentials):
		WriteUnauthorized(c, CodeSMSCodeInvalid, "原密码不正确或账号不是密码登录")
	default:
		WriteInternal(c)
	}
}

func (h authHandlers) refresh(c *gin.Context) {
	var req refreshReq
	_ = c.ShouldBindJSON(&req)
	device := firstNonEmpty(req.DeviceID, c.GetHeader(HeaderDeviceID))
	pair, err := h.svc.Refresh(strings.TrimSpace(req.RefreshToken), device)
	switch {
	case err == nil:
		WriteOK(c, pair)
	case errors.Is(err, auth.ErrRefreshReused):
		WriteUnauthorized(c, CodeRefreshReused, "登录状态已失效，请重新验证")
	case errors.Is(err, auth.ErrTokenExpired):
		WriteUnauthorized(c, CodeTokenExpired, "刷新令牌已过期")
	default:
		WriteUnauthorized(c, CodeAuthRequired, "需要重新登录")
	}
}

func (h authHandlers) logout(c *gin.Context) {
	var req logoutReq
	_ = c.ShouldBindJSON(&req)
	userID := c.GetString(ContextUserID)
	deviceID := c.GetString(ContextDeviceID)
	if err := h.svc.Logout(strings.TrimSpace(req.RefreshToken), userID, deviceID); err != nil {
		WriteInternal(c)
		return
	}
	WriteOK(c, gin.H{"ok": true})
}

func (h authHandlers) deleteAccount(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	if userID == "" {
		WriteUnauthorized(c, CodeAuthRequired, "需要登录")
		return
	}
	if err := h.svc.DeleteAccount(userID); err != nil {
		WriteInternal(c)
		return
	}
	grace := h.svc.AccountDeleteGrace()
	until := time.Now().UTC().Add(grace)
	WriteOK(c, gin.H{
		"status": "PENDING_DELETE",
		"coolingHours": int(grace.Hours()),
		"purgeAfter": until.Format(time.RFC3339Nano),
	})
}

func (h authHandlers) me(c *gin.Context) {
	p, err := h.svc.Me(c.GetString(ContextUserID))
	if err != nil {
		WriteUnauthorized(c, CodeAuthRequired, "需要登录")
		return
	}
	WriteOK(c, p)
}

func (h authHandlers) patchMe(c *gin.Context) {
	var req struct {
		DisplayName string `json:"displayName"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		WriteInvalid(c, "请求体无效")
		return
	}
	p, err := h.svc.PatchMe(c.GetString(ContextUserID), req.DisplayName)
	switch {
	case err == nil:
		WriteOK(c, p)
	case errors.Is(err, auth.ErrInvalidName):
		WriteInvalid(c, "昵称需要 1 到 20 个字")
	default:
		WriteInternal(c)
	}
}

func bearerAuth(svc *auth.Service) gin.HandlerFunc {
	return func(c *gin.Context) {
		header := c.GetHeader("Authorization")
		if !strings.HasPrefix(header, "Bearer ") {
			WriteUnauthorized(c, CodeAuthRequired, "需要登录")
			return
		}
		claims, err := svc.ParseAccess(strings.TrimPrefix(header, "Bearer "))
		if err != nil {
			code := CodeAuthRequired
			if errors.Is(err, auth.ErrTokenExpired) {
				code = CodeTokenExpired
			}
			WriteUnauthorized(c, code, "需要重新登录")
			return
		}
		c.Set(ContextUserID, claims.UserID)
		c.Set(ContextDeviceID, claims.DeviceID)
		c.Next()
	}
}

func optionalBearer(svc *auth.Service) gin.HandlerFunc {
	return func(c *gin.Context) {
		header := c.GetHeader("Authorization")
		if strings.HasPrefix(header, "Bearer ") {
			if claims, err := svc.ParseAccess(strings.TrimPrefix(header, "Bearer ")); err == nil {
				c.Set(ContextUserID, claims.UserID)
				c.Set(ContextDeviceID, claims.DeviceID)
			}
		}
		c.Next()
	}
}

func firstNonEmpty(v ...string) string {
	for _, s := range v {
		if strings.TrimSpace(s) != "" {
			return strings.TrimSpace(s)
		}
	}
	return ""
}
