package growth

import "time"

// StreakEndingAt counts consecutive occurred dates ending at endDate (YYYY-MM-DD).
// N for the GP coefficient includes the current record's date.
func StreakEndingAt(dates []string, endDate string) int {
	set := make(map[string]struct{}, len(dates))
	for _, d := range dates {
		set[d] = struct{}{}
	}
	end, err := time.Parse("2006-01-02", endDate)
	if err != nil {
		return 1
	}
	n := 0
	for {
		key := end.Format("2006-01-02")
		if _, ok := set[key]; !ok {
			break
		}
		n++
		end = end.AddDate(0, 0, -1)
	}
	if n < 1 {
		return 1
	}
	return n
}

// DisplayStreak uses the today/yesterday grace rule from ADR D2.
func DisplayStreak(dates []string, today string) int {
	set := make(map[string]struct{}, len(dates))
	for _, d := range dates {
		set[d] = struct{}{}
	}
	t, err := time.Parse("2006-01-02", today)
	if err != nil {
		return 0
	}
	yesterday := t.AddDate(0, 0, -1).Format("2006-01-02")
	start := today
	if _, ok := set[today]; !ok {
		if _, ok := set[yesterday]; ok {
			start = yesterday
		} else {
			return 0
		}
	}
	return StreakEndingAt(dates, start)
}

func StageFromGP(gp int64) string {
	switch {
	case gp < 50:
		return "SEED"
	case gp < 200:
		return "SPROUT"
	case gp < 500:
		return "SEEDLING"
	case gp < 1500:
		return "GROWING"
	case gp < 4000:
		return "FLOURISH"
	case gp < 10000:
		return "MATURE"
	default:
		return "DIVINE"
	}
}
