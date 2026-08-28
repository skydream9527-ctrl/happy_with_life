package growth

import "testing"

func TestStreakEndingAt(t *testing.T) {
	n := StreakEndingAt([]string{"2026-08-25", "2026-08-26", "2026-08-27"}, "2026-08-27")
	if n != 3 {
		t.Fatalf("got %d", n)
	}
	n = StreakEndingAt([]string{"2026-08-25", "2026-08-27"}, "2026-08-27")
	if n != 1 {
		t.Fatalf("gap should break, got %d", n)
	}
}

func TestDisplayStreakGrace(t *testing.T) {
	if DisplayStreak([]string{"2026-08-26"}, "2026-08-27") != 1 {
		t.Fatal("yesterday grace")
	}
	if DisplayStreak([]string{"2026-08-25"}, "2026-08-27") != 0 {
		t.Fatal("older than yesterday")
	}
}

func TestStageFromGP(t *testing.T) {
	if StageFromGP(0) != "SEED" || StageFromGP(50) != "SPROUT" || StageFromGP(10000) != "DIVINE" {
		t.Fatal("stage mapping")
	}
}
