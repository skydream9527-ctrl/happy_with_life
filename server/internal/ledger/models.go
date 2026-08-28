package ledger

import (
	"time"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/growth"
)

type Space struct {
	ID              string
	Name            string
	SpaceType       string
	OwnerID         string
	TotalGP         int64
	ActivePlantType string
	PlantStage      string
	Timezone        string
	Version         int64
	CreatedAt       time.Time
}

type Member struct {
	SpaceID       string
	UserID        string
	Role          string
	Status        string
	ContributedGP int64
}

type Record struct {
	ID               string
	ClientLocalID    *int64
	SpaceID          string
	AuthorID         string
	ContentText      string
	MoodTag          string
	StatusTags       []string
	PhotoCount       int
	HasVoice         bool
	HasMusic         bool
	HasLink          bool
	HasLocation      bool
	OccurredAt       time.Time
	OccurredDate     string
	OccurredTimezone string
	IsBackdated      bool
	GPFinal          int
	GPCapped         bool
	GPBreakdown      growth.Breakdown
	Version          int64
	DeletedAt        *time.Time
	CreatedAt        time.Time
	UpdatedAt        time.Time
}

type Change struct {
	Sequence   int64
	EntityType string
	EntityID   string
	SpaceID    string
	Version    int64
	Op         string
	Payload    map[string]any
	ChangedAt  time.Time
}

type AppliedMutation struct {
	MutationID   string
	UserID       string
	DeviceID     string
	RequestHash  string
	ResponseJSON []byte
}

type DailyStats struct {
	SpaceID             string
	OccurredDate        string
	GPTotal             int
	RecordCount         int
	DistinctAuthorCount int
}

type Mutation struct {
	MutationID          string
	DependsOnMutationID string
	EntityType          string
	Operation           string
	ClientLocalID       *int64
	ServerID            string
	BaseVersion         int64
	OccurredAt          time.Time
	OccurredDate        string
	Timezone            string
	Payload             MutationPayload
}

type MutationPayload struct {
	SpaceID     string       `json:"spaceId"`
	ContentText string       `json:"contentText"`
	MoodTag     string       `json:"moodTag"`
	StatusTags  []string     `json:"statusTags"`
	Media       []MediaInput `json:"media"`
}

type MediaInput struct {
	Type     string `json:"type"`
	MimeType string `json:"mimeType"`
}

type MutationResult struct {
	MutationID    string         `json:"mutationId"`
	Status        string         `json:"status"`
	ClientLocalID *int64         `json:"clientLocalId"`
	ServerID      string         `json:"serverId"`
	Version       int64          `json:"version"`
	Authoritative *Authoritative `json:"authoritative"`
	Error         *ResultError   `json:"error"`
}

type Authoritative struct {
	GPFinal              int      `json:"gpFinal"`
	GPCapped             bool     `json:"gpCapped"`
	SpaceTotalGP         int64    `json:"spaceTotalGp"`
	PlantStage           string   `json:"plantStage"`
	StreakDays           int      `json:"streakDays"`
	UnlockedAchievements []string `json:"unlockedAchievements"`
}

type ResultError struct {
	Code      string `json:"code"`
	Message   string `json:"message"`
	Retryable bool   `json:"retryable"`
}

type RecordDTO struct {
	ID            string           `json:"id"`
	ClientLocalID *int64           `json:"clientLocalId"`
	SpaceID       string           `json:"spaceId"`
	AuthorID      string           `json:"authorId"`
	ContentText   string           `json:"contentText"`
	MoodTag       string           `json:"moodTag"`
	StatusTags    []string         `json:"statusTags"`
	OccurredAt    string           `json:"occurredAt"`
	OccurredDate  string           `json:"occurredDate"`
	Timezone      string           `json:"timezone"`
	IsBackdated   bool             `json:"isBackdated"`
	GPFinal       int              `json:"gpFinal"`
	GPCapped      bool             `json:"gpCapped"`
	GPBreakdown   growth.Breakdown `json:"gpBreakdown"`
	Version       int64            `json:"version"`
	DeletedAt     *string          `json:"deletedAt,omitempty"`
}

func (r Record) DTO() RecordDTO {
	d := RecordDTO{
		ID: r.ID, ClientLocalID: r.ClientLocalID, SpaceID: r.SpaceID, AuthorID: r.AuthorID,
		ContentText: r.ContentText, MoodTag: r.MoodTag, StatusTags: r.StatusTags,
		OccurredAt: r.OccurredAt.UTC().Format(time.RFC3339Nano), OccurredDate: r.OccurredDate,
		Timezone: r.OccurredTimezone, IsBackdated: r.IsBackdated, GPFinal: r.GPFinal,
		GPCapped: r.GPCapped, GPBreakdown: r.GPBreakdown, Version: r.Version,
	}
	if r.DeletedAt != nil {
		s := r.DeletedAt.UTC().Format(time.RFC3339Nano)
		d.DeletedAt = &s
	}
	return d
}

var AllowedMoods = map[string]struct{}{
	"开心": {}, "平静": {}, "兴奋": {}, "感动": {}, "想念": {},
	"疲惫": {}, "难过": {}, "愤怒": {}, "惊喜": {},
}
