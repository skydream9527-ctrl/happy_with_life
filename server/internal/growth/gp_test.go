package growth

import "testing"

func TestMinimumRecord(t *testing.T) {
	b := Calculate(Input{HasText: true, TextLength: 4, StreakDays: 1})
	if b.Subtotal != 15 {
		t.Fatalf("subtotal=%d", b.Subtotal)
	}
	if b.FinalGP != 15 {
		t.Fatalf("final=%d", b.FinalGP)
	}
}

func TestLongTextAndStreakCap(t *testing.T) {
	b := Calculate(Input{
		HasText: true, TextLength: 80, PhotoCount: 9, HasVoice: true, HasMusic: true,
		HasLink: true, HasLocation: true, StatusTagCount: 3, StreakDays: 20,
	})
	if b.Subtotal != 71 {
		t.Fatalf("subtotal=%d want 71", b.Subtotal)
	}
	if b.StreakMultiplier != 2 {
		t.Fatalf("streak=%v", b.StreakMultiplier)
	}
	if b.RawTotal != 142 {
		t.Fatalf("raw=%d want 142", b.RawTotal)
	}
}

func TestBackdateAndDailyCap(t *testing.T) {
	b := Calculate(Input{HasText: true, TextLength: 80, StreakDays: 20, IsBackdated: true, TodayGpSoFar: 90})
	if b.BackdateMult != 0.8 {
		t.Fatalf("backdate=%v", b.BackdateMult)
	}
	if b.FinalGP != 10 || !b.IsCapped {
		t.Fatalf("final=%d capped=%v", b.FinalGP, b.IsCapped)
	}
}

func TestMoodIsNotBonus(t *testing.T) {
	a := Calculate(Input{HasText: true, StreakDays: 1})
	b := Calculate(Input{HasText: true, StreakDays: 1})
	if a.FinalGP != b.FinalGP {
		t.Fatal("mood must not change GP")
	}
}
