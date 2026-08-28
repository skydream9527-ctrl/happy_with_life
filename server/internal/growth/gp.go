package growth

import "math"

// Frozen GP formula from docs/domain-rules.md (DOMAIN_RULES_VERSION=1).
// HTTP endpoints for records/sync are S2; this package is the shared calculator.

const (
	BaseGP             = 10
	TextBonus          = 5
	TextLongBonus      = 5
	PhotoBonusPer      = 3
	PhotoMax           = 9
	VoiceBonus         = 5
	MusicBonus         = 5
	LinkBonus          = 5
	LocationBonus      = 3
	StatusTagBonusPer  = 2
	StatusTagMax       = 3
	StreakRate         = 0.05
	BackdateMultiplier = 0.8
	DailyGPLimit       = 100
	EventAnniversary   = 20
	EventFestival      = 10
	EventResonance     = 15
)

type Input struct {
	HasText        bool
	TextLength     int
	PhotoCount     int
	HasVoice       bool
	HasMusic       bool
	HasLink        bool
	HasLocation    bool
	StatusTagCount int
	StreakDays     int
	IsBackdated    bool
	EventBonus     int
	TodayGpSoFar   int
}

type Breakdown struct {
	BaseGP           int     `json:"baseGp"`
	TextBonus        int     `json:"textBonus"`
	PhotoBonus       int     `json:"photoBonus"`
	VoiceBonus       int     `json:"voiceBonus"`
	MusicBonus       int     `json:"musicBonus"`
	LinkBonus        int     `json:"linkBonus"`
	LocationBonus    int     `json:"locationBonus"`
	StatusBonus      int     `json:"statusBonus"`
	Subtotal         int     `json:"subtotal"`
	StreakMultiplier float64 `json:"streakMultiplier"`
	BackdateMult     float64 `json:"backdateMultiplier"`
	EventBonus       int     `json:"eventBonus"`
	RawTotal         int     `json:"rawTotal"`
	FinalGP          int     `json:"finalGp"`
	IsCapped         bool    `json:"isCapped"`
	DailyLimit       int     `json:"dailyLimit"`
}

func Calculate(in Input) Breakdown {
	textBonus := 0
	if in.HasText {
		textBonus = TextBonus
		if in.TextLength > 50 {
			textBonus += TextLongBonus
		}
	}
	photos := in.PhotoCount
	if photos > PhotoMax {
		photos = PhotoMax
	}
	statusN := in.StatusTagCount
	if statusN > StatusTagMax {
		statusN = StatusTagMax
	}
	subtotal := BaseGP + textBonus + photos*PhotoBonusPer
	if in.HasVoice {
		subtotal += VoiceBonus
	}
	if in.HasMusic {
		subtotal += MusicBonus
	}
	if in.HasLink {
		subtotal += LinkBonus
	}
	if in.HasLocation {
		subtotal += LocationBonus
	}
	subtotal += statusN * StatusTagBonusPer

	n := in.StreakDays
	if n < 1 {
		n = 1
	}
	s := 1 + float64(n)*StreakRate
	if s > 2 {
		s = 2
	}
	raw := int(math.Floor(float64(subtotal) * s))
	b := 1.0
	if in.IsBackdated {
		b = BackdateMultiplier
		raw = int(math.Floor(float64(raw) * b))
	}
	rawPlus := raw + in.EventBonus
	remaining := DailyGPLimit - in.TodayGpSoFar
	if remaining < 0 {
		remaining = 0
	}
	final := rawPlus
	capped := false
	if final > remaining {
		final = remaining
		capped = true
	}
	if final < 0 {
		final = 0
	}
	return Breakdown{
		BaseGP: BaseGP, TextBonus: textBonus, PhotoBonus: photos * PhotoBonusPer,
		VoiceBonus: boolBonus(in.HasVoice, VoiceBonus), MusicBonus: boolBonus(in.HasMusic, MusicBonus),
		LinkBonus: boolBonus(in.HasLink, LinkBonus), LocationBonus: boolBonus(in.HasLocation, LocationBonus),
		StatusBonus: statusN * StatusTagBonusPer, Subtotal: subtotal,
		StreakMultiplier: s, BackdateMult: b, EventBonus: in.EventBonus,
		RawTotal: rawPlus, FinalGP: final, IsCapped: capped, DailyLimit: DailyGPLimit,
	}
}

func boolBonus(ok bool, n int) int {
	if ok {
		return n
	}
	return 0
}
