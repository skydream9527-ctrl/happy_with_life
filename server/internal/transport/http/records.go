package httpx

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/id"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/ledger"
	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/media"
)

type ledgerHandlers struct {
	svc    *ledger.Service
	media  *media.Service
	public string
}

func (h ledgerHandlers) spaces(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	spaces, err := h.svc.Store.ListSpacesForUser(userID)
	if err != nil {
		WriteInternal(c)
		return
	}
	out := make([]gin.H, 0, len(spaces))
	for _, s := range spaces {
		out = append(out, spaceJSON(s))
	}
	WriteOK(c, gin.H{"items": out})
}

func (h ledgerHandlers) space(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	sp, err := h.svc.Store.GetSpace(c.Param("id"))
	if err != nil || sp == nil {
		WriteError(c, http.StatusNotFound, CodeRecordInvalid, "空间不存在", false, nil)
		return
	}
	mem, err := h.svc.Store.GetMember(sp.ID, userID)
	if err != nil || mem == nil || mem.Status != "ACTIVE" {
		WriteError(c, http.StatusForbidden, CodeSpaceForbidden, "无权访问该空间", false, nil)
		return
	}
	WriteOK(c, spaceJSON(*sp))
}

func (h ledgerHandlers) plant(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	sp, err := h.svc.Store.GetSpace(c.Param("id"))
	if err != nil || sp == nil {
		WriteError(c, http.StatusNotFound, CodeRecordInvalid, "空间不存在", false, nil)
		return
	}
	mem, _ := h.svc.Store.GetMember(sp.ID, userID)
	if mem == nil || mem.Status != "ACTIVE" {
		WriteError(c, http.StatusForbidden, CodeSpaceForbidden, "无权访问该空间", false, nil)
		return
	}
	WriteOK(c, gin.H{
		"spaceId": sp.ID, "plantType": sp.ActivePlantType, "stage": sp.PlantStage,
		"totalGp": sp.TotalGP,
	})
}

func (h ledgerHandlers) calendar(c *gin.Context) {
	spaceID := c.Query("spaceId")
	if spaceID == "" {
		WriteInvalid(c, "spaceId 必填")
		return
	}
	cal, err := h.svc.Calendar(c.GetString(ContextUserID), spaceID, c.Query("from"), c.Query("to"))
	switch {
	case err == nil:
		WriteOK(c, cal)
	case errors.Is(err, ledger.ErrForbidden):
		WriteError(c, http.StatusForbidden, CodeSpaceForbidden, "无权访问该空间", false, nil)
	case errors.Is(err, ledger.ErrNotFound):
		WriteError(c, http.StatusNotFound, CodeRecordInvalid, "空间不存在", false, nil)
	default:
		WriteInternal(c)
	}
}

func (h ledgerHandlers) listRecords(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	spaceID := c.Query("spaceId")
	if spaceID == "" {
		WriteInvalid(c, "spaceId 必填")
		return
	}
	mem, _ := h.svc.Store.GetMember(spaceID, userID)
	if mem == nil || mem.Status != "ACTIVE" {
		WriteError(c, http.StatusForbidden, CodeSpaceForbidden, "无权访问该空间", false, nil)
		return
	}
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "50"))
	if limit <= 0 || limit > 100 {
		limit = 50
	}
	from, to, mood := c.Query("from"), c.Query("to"), c.Query("mood")
	afterOccurred, afterID := "", ""
	if cur := c.Query("cursor"); cur != "" {
		parts := strings.SplitN(cur, "|", 2)
		afterOccurred = parts[0]
		if len(parts) == 2 {
			afterID = parts[1]
		}
	}
	var rows []ledger.Record
	var err error
	if from != "" || to != "" || mood != "" {
		live, e := h.svc.Store.LiveRecords(spaceID)
		err = e
		for _, r := range live {
			if from != "" && r.OccurredDate < from {
				continue
			}
			if to != "" && r.OccurredDate > to {
				continue
			}
			if mood != "" && r.MoodTag != mood {
				continue
			}
			key := r.OccurredAt.UTC().Format(time.RFC3339Nano) + "|" + r.ID
			if afterOccurred != "" && key >= afterOccurred+"|"+afterID {
				continue
			}
			rows = append(rows, r)
		}
		sortRecordsDesc(rows)
	} else {
		rows, err = h.svc.Store.ListRecords(spaceID, afterOccurred, afterID, limit+1)
	}
	if err != nil {
		WriteInternal(c)
		return
	}
	hasMore := len(rows) > limit
	if hasMore {
		rows = rows[:limit]
	}
	items := make([]ledger.RecordDTO, 0, len(rows))
	next := ""
	for _, r := range rows {
		items = append(items, decorateRecord(h, r))
		next = r.OccurredAt.UTC().Format(time.RFC3339Nano) + "|" + r.ID
	}
	WriteOK(c, gin.H{"items": items, "nextCursor": next, "hasMore": hasMore})
}

func (h ledgerHandlers) getRecord(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	rec, err := h.svc.Store.GetRecord(c.Param("id"))
	if err != nil || rec == nil {
		WriteError(c, http.StatusNotFound, CodeRecordInvalid, "记录不存在", false, nil)
		return
	}
	mem, _ := h.svc.Store.GetMember(rec.SpaceID, userID)
	if mem == nil || mem.Status != "ACTIVE" {
		WriteError(c, http.StatusForbidden, CodeSpaceForbidden, "无权访问该空间", false, nil)
		return
	}
	if rec.DeletedAt != nil {
		WriteError(c, http.StatusGone, "RECORD_DELETED", "记录已删除", false, nil)
		return
	}
	WriteOK(c, decorateRecord(h, *rec))
}

type recordWrite struct {
	SpaceID       string              `json:"spaceId"`
	ContentText   string              `json:"contentText"`
	MoodTag       string              `json:"moodTag"`
	StatusTags    []string            `json:"statusTags"`
	Media         []ledger.MediaInput `json:"media"`
	OccurredAt    string              `json:"occurredAt"`
	OccurredDate  string              `json:"occurredDate"`
	Timezone      string              `json:"timezone"`
	BaseVersion   int64               `json:"baseVersion"`
	ClientLocalID *int64              `json:"clientLocalId"`
}

func (h ledgerHandlers) createRecord(c *gin.Context) {
	h.writeRecord(c, "UPSERT", "")
}

func (h ledgerHandlers) patchRecord(c *gin.Context) {
	h.writeRecord(c, "UPSERT", c.Param("id"))
}

func (h ledgerHandlers) deleteRecord(c *gin.Context) {
	h.writeRecord(c, "DELETE", c.Param("id"))
}

func (h ledgerHandlers) writeRecord(c *gin.Context, op, serverID string) {
	userID := c.GetString(ContextUserID)
	deviceID := c.GetString(ContextDeviceID)
	var body recordWrite
	_ = c.ShouldBindJSON(&body)
	mutID := c.GetHeader("Idempotency-Key")
	if mutID == "" {
		mutID = id.New()
	}
	occurred := time.Time{}
	if body.OccurredAt != "" {
		if t, err := time.Parse(time.RFC3339Nano, body.OccurredAt); err == nil {
			occurred = t
		} else if t, err := time.Parse(time.RFC3339, body.OccurredAt); err == nil {
			occurred = t
		}
	}
	res := h.svc.Apply(userID, deviceID, ledger.Mutation{
		MutationID: mutID, EntityType: "RECORD", Operation: op,
		ClientLocalID: body.ClientLocalID, ServerID: serverID, BaseVersion: body.BaseVersion,
		OccurredAt: occurred, OccurredDate: body.OccurredDate, Timezone: body.Timezone,
		Payload: ledger.MutationPayload{
			SpaceID: body.SpaceID, ContentText: body.ContentText, MoodTag: body.MoodTag,
			StatusTags: body.StatusTags, Media: body.Media,
		},
	})
	writeMutation(c, res)
}

func (h ledgerHandlers) push(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	deviceID := c.GetString(ContextDeviceID)
	raw, err := io.ReadAll(io.LimitReader(c.Request.Body, 1<<20+1))
	if err != nil {
		WriteInvalid(c, "请求体无效")
		return
	}
	if len(raw) > 1<<20 {
		WriteError(c, http.StatusRequestEntityTooLarge, CodeRecordInvalid, "批次过大", false, nil)
		return
	}
	var req struct {
		BatchID   string            `json:"batchId"`
		Mutations []json.RawMessage `json:"mutations"`
	}
	if err := json.Unmarshal(raw, &req); err != nil {
		WriteInvalid(c, "请求体无效")
		return
	}
	muts := make([]ledger.Mutation, 0, len(req.Mutations))
	for _, rawm := range req.Mutations {
		m, err := decodeMutation(rawm)
		if err != nil {
			WriteInvalid(c, "mutation 无效")
			return
		}
		muts = append(muts, m)
	}
	results := h.svc.Push(userID, deviceID, muts)
	WriteOK(c, gin.H{"results": results})
}

func (h ledgerHandlers) pull(c *gin.Context) {
	userID := c.GetString(ContextUserID)
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "100"))
	changes, next, hasMore, err := h.svc.Pull(userID, c.Query("cursor"), limit)
	if err != nil {
		WriteInternal(c)
		return
	}
	items := make([]gin.H, 0, len(changes))
	for _, ch := range changes {
		items = append(items, gin.H{
			"sequence":   strconv.FormatInt(ch.Sequence, 10),
			"entityType": ch.EntityType,
			"operation":  ch.Op,
			"serverId":   ch.EntityID,
			"version":    ch.Version,
			"spaceId":    ch.SpaceID,
			"payload":    ch.Payload,
		})
	}
	WriteOK(c, gin.H{"changes": items, "nextCursor": next, "hasMore": hasMore})
}

func decodeMutation(raw json.RawMessage) (ledger.Mutation, error) {
	var wire struct {
		MutationID          string          `json:"mutationId"`
		DependsOnMutationID *string         `json:"dependsOnMutationId"`
		EntityType          string          `json:"entityType"`
		Operation           string          `json:"operation"`
		ClientLocalID       *int64          `json:"clientLocalId"`
		ServerID            *string         `json:"serverId"`
		BaseVersion         int64           `json:"baseVersion"`
		OccurredAt          string          `json:"occurredAt"`
		OccurredDate        string          `json:"occurredDate"`
		Timezone            string          `json:"timezone"`
		Payload             json.RawMessage `json:"payload"`
	}
	if err := json.Unmarshal(raw, &wire); err != nil {
		return ledger.Mutation{}, err
	}
	var payload ledger.MutationPayload
	_ = json.Unmarshal(wire.Payload, &payload)
	m := ledger.Mutation{
		MutationID: wire.MutationID, EntityType: wire.EntityType, Operation: wire.Operation,
		ClientLocalID: wire.ClientLocalID, BaseVersion: wire.BaseVersion,
		OccurredDate: wire.OccurredDate, Timezone: wire.Timezone, Payload: payload,
	}
	if wire.DependsOnMutationID != nil {
		m.DependsOnMutationID = *wire.DependsOnMutationID
	}
	if wire.ServerID != nil {
		m.ServerID = *wire.ServerID
	}
	if wire.OccurredAt != "" {
		if t, err := time.Parse(time.RFC3339Nano, wire.OccurredAt); err == nil {
			m.OccurredAt = t
		} else if t, err := time.Parse(time.RFC3339, wire.OccurredAt); err == nil {
			m.OccurredAt = t
		}
	}
	return m, nil
}

func writeMutation(c *gin.Context, res ledger.MutationResult) {
	if res.Error != nil {
		status := http.StatusBadRequest
		switch res.Error.Code {
		case CodeSpaceForbidden:
			status = http.StatusForbidden
		case "RECORD_VERSION_CONFLICT":
			status = http.StatusConflict
		case "RECORD_DELETED":
			status = http.StatusGone
		case "MUTATION_ID_REUSED", "MUTATION_DEPENDENCY_MISSING":
			status = http.StatusConflict
		case "INTERNAL_ERROR":
			status = http.StatusInternalServerError
		}
		WriteError(c, status, res.Error.Code, res.Error.Message, res.Error.Retryable, res)
		return
	}
	if res.Status == "CONFLICT" {
		WriteError(c, http.StatusConflict, "RECORD_VERSION_CONFLICT", "记录已在其他设备更新", false, res)
		return
	}
	WriteOK(c, res)
}

func spaceJSON(s ledger.Space) gin.H {
	return gin.H{
		"id": s.ID, "name": s.Name, "spaceType": s.SpaceType, "ownerId": s.OwnerID,
		"totalGp": s.TotalGP, "activePlantType": s.ActivePlantType, "plantStage": s.PlantStage,
		"timezone": s.Timezone, "version": s.Version,
	}
}

func sortRecordsDesc(out []ledger.Record) {
	for i := 0; i < len(out); i++ {
		for j := i + 1; j < len(out); j++ {
			if out[j].OccurredAt.After(out[i].OccurredAt) ||
				(out[j].OccurredAt.Equal(out[i].OccurredAt) && out[j].ID > out[i].ID) {
				out[i], out[j] = out[j], out[i]
			}
		}
	}
}
