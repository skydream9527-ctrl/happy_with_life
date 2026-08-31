package auth

import "testing"

func TestNormalizeAccount(t *testing.T) {
	ok, err := NormalizeAccount("  Ada_01 ")
	if err != nil || ok != "ada_01" {
		t.Fatalf("got %q %v", ok, err)
	}
	if _, err := NormalizeAccount("ab"); err == nil {
		t.Fatal("short account should fail")
	}
	if _, err := NormalizeAccount("bad name"); err == nil {
		t.Fatal("space should fail")
	}
}
