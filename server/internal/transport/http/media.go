package httpx

import (
	"errors"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/ledger"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/media"
)

const (
	CodeMediaQuota      = "MEDIA_QUOTA"
	CodeMediaInvalid    = "MEDIA_INVALID"
	CodeMediaInUse      = "MEDIA_IN_USE"
	CodeMediaMissing    = "MEDIA_MISSING"
	CodeMediaForbidden  = "MEDIA_FORBIDDEN"
	maxUploadBody int64 = 6 << 20
)

type mediaHandlers struct {
	svc    *media.Service
	ledger *ledger.Service
	public string
}

func (h mediaHandlers) sts(c *gin.Context) {
	var req struct {
		Type      string `json:"type"`
		MimeType  string `json:"mimeType"`
		SizeBytes int64  `json:"sizeBytes"`
		SHA256    string `json:"sha256"`
		Width     int    `json:"width"`
		Height    int    `json:"height"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		WriteInvalid(c, "请求体无效")
		return
	}
	res, err := h.svc.IssueSTS(c.GetString(ContextUserID), media.STSRequest{
		Type: req.Type, MimeType: req.MimeType, SizeBytes: req.SizeBytes,
		SHA256: req.SHA256, Width: req.Width, Height: req.Height,
	}, h.base(c))
	if err != nil {
		writeMediaErr(c, err)
		return
	}
	WriteOK(c, gin.H{
		"mediaId": res.MediaID, "objectKey": res.ObjectKey, "method": res.Method,
		"uploadUrl": res.UploadURL, "headers": res.Headers,
		"expiresAt": res.ExpiresAt.UTC().Format(time.RFC3339Nano),
		"provider": res.Provider, "quotaUsed": res.QuotaUsed, "quotaMax": res.QuotaMax,
	})
}

func (h mediaHandlers) complete(c *gin.Context) {
	var req struct {
		MediaID string `json:"mediaId"`
	}
	_ = c.ShouldBindJSON(&req)
	if req.MediaID == "" {
		req.MediaID = c.Param("id")
	}
	obj, err := h.svc.Complete(c.GetString(ContextUserID), req.MediaID)
	if err != nil {
		writeMediaErr(c, err)
		return
	}
	WriteOK(c, gin.H{
		"id": obj.ID, "uploadStatus": obj.UploadStatus, "sizeBytes": obj.SizeBytes,
		"mimeType": obj.MimeType, "type": obj.Type,
	})
}

func (h mediaHandlers) downloadURL(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	id := c.Param("id")
	url, exp, err := h.svc.SignDownload(userID, id, func(o media.Object) bool {
		if h.ledger == nil || o.RecordID == "" {
			return false
		}
		rec, _ := h.ledger.Store.GetRecord(o.RecordID)
		if rec == nil {
			return false
		}
		mem, _ := h.ledger.Store.GetMember(rec.SpaceID, userID)
		return mem != nil && mem.Status == "ACTIVE"
	}, h.base(c))
	if err != nil {
		writeMediaErr(c, err)
		return
	}
	WriteOK(c, gin.H{"url": url, "expiresAt": exp.UTC().Format(time.RFC3339Nano)})
}

func (h mediaHandlers) delete(c *gin.Context) {
	if err := h.svc.Delete(c.GetString(ContextUserID), c.Param("id")); err != nil {
		writeMediaErr(c, err)
		return
	}
	WriteOK(c, gin.H{"deleted": true})
}

func (h mediaHandlers) upload(c *gin.Context) {
	ticket := c.Param("ticket")
	c.Request.Body = http.MaxBytesReader(c.Writer, c.Request.Body, maxUploadBody)
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		WriteError(c, http.StatusRequestEntityTooLarge, CodeMediaInvalid, "文件太大", false, nil)
		return
	}
	if err := h.svc.AcceptUpload(ticket, c.GetHeader("Content-Type"), body); err != nil {
		writeMediaErr(c, err)
		return
	}
	c.Status(http.StatusOK)
}

func (h mediaHandlers) content(c *gin.Context) {
	obj, body, err := h.svc.ReadContent(c.Param("ticket"))
	if err != nil {
		writeMediaErr(c, err)
		return
	}
	ct := obj.MimeType
	if ct == "" {
		ct = "application/octet-stream"
	}
	c.Data(http.StatusOK, ct, body)
}

func (h mediaHandlers) quota(c *gin.Context) {
	used, max, err := h.svc.Quota(c.GetString(ContextUserID))
	if err != nil {
		WriteInternal(c)
		return
	}
	WriteOK(c, gin.H{"usedBytes": used, "maxBytes": max})
}

func (h mediaHandlers) base(c *gin.Context) string {
	if h.public != "" {
		return h.public
	}
	return ""
}

func writeMediaErr(c *gin.Context, err error) {
	switch {
	case errors.Is(err, media.ErrQuota):
		WriteError(c, http.StatusForbidden, CodeMediaQuota, "照片容量已满（每用户 200MB）", false, nil)
	case errors.Is(err, media.ErrTooLarge):
		WriteError(c, http.StatusBadRequest, CodeMediaInvalid, "单张照片不能超过 5MB，或超过预申请大小", false, nil)
	case errors.Is(err, media.ErrUnsupported):
		WriteError(c, http.StatusBadRequest, CodeMediaInvalid, "不支持的文件类型", false, nil)
	case errors.Is(err, media.ErrNotFound):
		WriteError(c, http.StatusNotFound, CodeMediaInvalid, "媒体不存在", false, nil)
	case errors.Is(err, media.ErrForbidden):
		WriteError(c, http.StatusForbidden, CodeMediaForbidden, "无权访问该媒体", false, nil)
	case errors.Is(err, media.ErrInUse):
		WriteError(c, http.StatusConflict, CodeMediaInUse, "照片已绑定记录，请先从记录中移除", false, nil)
	case errors.Is(err, media.ErrMissingBlob):
		WriteError(c, http.StatusConflict, CodeMediaMissing, "文件尚未上传完成", true, nil)
	case errors.Is(err, media.ErrInvalid):
		WriteError(c, http.StatusBadRequest, CodeMediaInvalid, "媒体请求无效", false, nil)
	default:
		WriteInternal(c)
	}
}

func decorateRecord(h ledgerHandlers, rec ledger.Record) ledger.RecordDTO {
	dto := rec.DTO()
	if h.media == nil {
		return dto
	}
	items, err := h.media.ListByRecord(rec.ID)
	if err != nil || len(items) == 0 {
		return dto
	}
	arr := make([]any, 0, len(items))
	for _, it := range items {
		arr = append(arr, it)
	}
	dto.Media = arr
	return dto
}

func skipLargeBody(c *gin.Context) bool {
	p := c.Request.URL.Path
	return strings.Contains(p, "/media/upload/") || strings.Contains(p, "/media/content/")
}
