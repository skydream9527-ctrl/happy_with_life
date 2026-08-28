package httpx

import (
	"errors"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/ledger"
)

func (h ledgerHandlers) createSpace(c *gin.Context) {
	var req struct {
		Name      string `json:"name"`
		SpaceType string `json:"spaceType"`
		PlantType string `json:"plantType"`
		Timezone  string `json:"timezone"`
	}
	_ = c.ShouldBindJSON(&req)
	sp, err := h.svc.CreateShared(c.GetString(ContextUserID), req.Name, req.SpaceType, req.PlantType, req.Timezone)
	if err != nil {
		writeSpaceErr(c, err)
		return
	}
	WriteCreated(c, spaceJSON(sp))
}

func (h ledgerHandlers) patchSpace(c *gin.Context) {
	var req struct {
		Name      string `json:"name"`
		PlantType string `json:"plantType"`
	}
	_ = c.ShouldBindJSON(&req)
	sp, err := h.svc.PatchSpace(c.GetString(ContextUserID), c.Param("id"), req.Name, req.PlantType)
	if err != nil {
		writeSpaceErr(c, err)
		return
	}
	WriteOK(c, spaceJSON(sp))
}

func (h ledgerHandlers) members(c *gin.Context) {
	rows, err := h.svc.Members(c.GetString(ContextUserID), c.Param("id"))
	if err != nil {
		writeSpaceErr(c, err)
		return
	}
	items := make([]gin.H, 0, len(rows))
	for _, m := range rows {
		items = append(items, gin.H{
			"userId": m.UserID, "role": m.Role, "status": m.Status, "contributedGp": m.ContributedGP,
		})
	}
	WriteOK(c, gin.H{"items": items})
}

func (h ledgerHandlers) createInvite(c *gin.Context) {
	raw, link, inv, err := h.svc.CreateInvite(c.GetString(ContextUserID), c.Param("id"), h.public)
	if err != nil {
		writeSpaceErr(c, err)
		return
	}
	WriteOK(c, gin.H{
		"inviteId": inv.ID, "token": raw, "link": link,
		"expiresAt": inv.ExpiresAt.UTC().Format(time.RFC3339Nano),
		"maxUses": inv.MaxUses, "usedCount": inv.UsedCount,
	})
}

func (h ledgerHandlers) listInvites(c *gin.Context) {
	rows, err := h.svc.ListInvites(c.GetString(ContextUserID), c.Param("id"))
	if err != nil {
		writeSpaceErr(c, err)
		return
	}
	items := make([]gin.H, 0, len(rows))
	for _, inv := range rows {
		item := gin.H{
			"id": inv.ID, "expiresAt": inv.ExpiresAt.UTC().Format(time.RFC3339Nano),
			"maxUses": inv.MaxUses, "usedCount": inv.UsedCount, "revoked": inv.RevokedAt != nil,
		}
		items = append(items, item)
	}
	WriteOK(c, gin.H{"items": items})
}

func (h ledgerHandlers) revokeInvite(c *gin.Context) {
	if err := h.svc.RevokeInvite(c.GetString(ContextUserID), c.Param("id"), c.Param("inviteId")); err != nil {
		writeSpaceErr(c, err)
		return
	}
	WriteOK(c, gin.H{"revoked": true})
}

func (h ledgerHandlers) leaveSpace(c *gin.Context) {
	if err := h.svc.Leave(c.GetString(ContextUserID), c.Param("id")); err != nil {
		writeSpaceErr(c, err)
		return
	}
	WriteOK(c, gin.H{"left": true})
}

func (h ledgerHandlers) kickMember(c *gin.Context) {
	if err := h.svc.Kick(c.GetString(ContextUserID), c.Param("id"), c.Param("userId")); err != nil {
		writeSpaceErr(c, err)
		return
	}
	WriteOK(c, gin.H{"removed": true})
}

func (h ledgerHandlers) peekInvite(c *gin.Context) {
	sp, inv, err := h.svc.PeekInvite(c.Param("token"))
	if err != nil {
		writeSpaceErr(c, err)
		return
	}
	WriteOK(c, gin.H{
		"spaceId": sp.ID, "spaceName": sp.Name, "spaceType": sp.SpaceType,
		"plantType": sp.ActivePlantType, "expiresAt": inv.ExpiresAt.UTC().Format(time.RFC3339Nano),
		"seatsLeft": inv.MaxUses - inv.UsedCount,
	})
}

func (h ledgerHandlers) acceptInvite(c *gin.Context) {
	token := c.Param("token")
	if token == "" {
		var req struct {
			Token string `json:"token"`
		}
		_ = c.ShouldBindJSON(&req)
		token = req.Token
	}
	sp, err := h.svc.AcceptInvite(c.GetString(ContextUserID), token)
	if err != nil {
		writeSpaceErr(c, err)
		return
	}
	WriteOK(c, spaceJSON(sp))
}

func writeSpaceErr(c *gin.Context, err error) {
	switch {
	case errors.Is(err, ledger.ErrForbidden):
		WriteError(c, http.StatusForbidden, CodeSpaceForbidden, "无权操作该空间", false, nil)
	case errors.Is(err, ledger.ErrNotFound):
		WriteError(c, http.StatusNotFound, CodeRecordInvalid, "不存在", false, nil)
	case errors.Is(err, ledger.ErrPersonalSpace):
		WriteError(c, http.StatusBadRequest, CodeRecordInvalid, "个人空间不能邀请或退出", false, nil)
	case errors.Is(err, ledger.ErrInviteExpired):
		WriteError(c, http.StatusGone, "INVITE_EXPIRED", "邀请已过期或次数用尽", false, nil)
	case errors.Is(err, ledger.ErrInviteRevoked):
		WriteError(c, http.StatusGone, "INVITE_REVOKED", "邀请已撤销", false, nil)
	case errors.Is(err, ledger.ErrSpaceFull):
		WriteError(c, http.StatusConflict, "SPACE_FULL", "空间最多 6 人", false, nil)
	case errors.Is(err, ledger.ErrOwnerLeave):
		WriteError(c, http.StatusBadRequest, CodeRecordInvalid, "创建者不能退出，只能解散或先移交", false, nil)
	case errors.Is(err, ledger.ErrInvalid):
		WriteError(c, http.StatusBadRequest, CodeRecordInvalid, "请求无效", false, nil)
	default:
		WriteInternal(c)
	}
}
