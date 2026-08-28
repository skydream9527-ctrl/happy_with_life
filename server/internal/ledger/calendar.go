package ledger

import (
	"sort"
	"time"

	"github.com/skydream9527-ctrl/xiaoquexing-server/internal/growth"
)

type DayStat struct {
	Date        string `json:"date"`
	MoodTag     string `json:"moodTag"`
	RecordCount int    `json:"recordCount"`
	GPTotal     int    `json:"gpTotal"`
}

type Calendar struct {
	SpaceID    string    `json:"spaceId"`
	From       string    `json:"from"`
	To         string    `json:"to"`
	TotalGP    int64     `json:"totalGp"`
	PlantStage string    `json:"plantStage"`
	StreakDays int       `json:"streakDays"`
	Days       []DayStat `json:"days"`
}

func (s *Service) Calendar(userID, spaceID, from, to string) (*Calendar, error) {
	mem, err := s.Store.GetMember(spaceID, userID)
	if err != nil {
		return nil, err
	}
	if mem == nil || mem.Status != "ACTIVE" {
		return nil, ErrForbidden
	}
	sp, err := s.Store.GetSpace(spaceID)
	if err != nil || sp == nil {
		return nil, ErrNotFound
	}
	tz := sp.Timezone
	if tz == "" {
		tz = "Asia/Shanghai"
	}
	loc, err := time.LoadLocation(tz)
	if err != nil {
		loc = time.UTC
	}
	now := s.Now().In(loc)
	if to == "" {
		to = now.Format("2006-01-02")
	}
	if from == "" {
		from = now.AddDate(0, 0, -30).Format("2006-01-02")
	}
	live, err := s.Store.LiveRecords(spaceID)
	if err != nil {
		return nil, err
	}
	type agg struct {
		count int
		gp    int
		mood  string
		last  time.Time
	}
	by := map[string]*agg{}
	dates := []string{}
	for _, r := range live {
		dates = append(dates, r.OccurredDate)
		if r.OccurredDate < from || r.OccurredDate > to {
			continue
		}
		a := by[r.OccurredDate]
		if a == nil {
			a = &agg{}
			by[r.OccurredDate] = a
		}
		a.count++
		a.gp += r.GPFinal
		if r.OccurredAt.After(a.last) {
			a.last = r.OccurredAt
			a.mood = r.MoodTag
		}
	}
	days := make([]DayStat, 0, len(by))
	for d, a := range by {
		gp := a.gp
		if gp > growth.DailyGPLimit {
			gp = growth.DailyGPLimit
		}
		days = append(days, DayStat{Date: d, MoodTag: a.mood, RecordCount: a.count, GPTotal: gp})
	}
	sort.Slice(days, func(i, j int) bool { return days[i].Date < days[j].Date })
	today := now.Format("2006-01-02")
	return &Calendar{
		SpaceID: spaceID, From: from, To: to,
		TotalGP: sp.TotalGP, PlantStage: sp.PlantStage,
		StreakDays: growth.DisplayStreak(dates, today), Days: days,
	}, nil
}
