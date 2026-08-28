package auth

import "testing"

func TestNormalizePhone(t *testing.T) {
	got, err := NormalizePhone("13812345678")
	if err != nil || got != "+8613812345678" {
		t.Fatalf("got %q err=%v", got, err)
	}
	if _, err := NormalizePhone("123"); err == nil {
		t.Fatal("expected error")
	}
}

func TestPhoneHashAndEncryptRoundtrip(t *testing.T) {
	key := make([]byte, 32)
	for i := range key {
		key[i] = byte(i)
	}
	e164 := "+8613812345678"
	enc, err := EncryptPhone(e164, key)
	if err != nil {
		t.Fatal(err)
	}
	plain, err := DecryptPhone(enc, key)
	if err != nil || plain != e164 {
		t.Fatalf("plain=%q err=%v", plain, err)
	}
	h1 := HashPhone(e164, "pepper-pepper-pepper")
	h2 := HashPhone(e164, "pepper-pepper-pepper")
	if h1 != h2 || len(h1) != 64 {
		t.Fatalf("hash %s", h1)
	}
}

func TestMaskPhone(t *testing.T) {
	if got := MaskPhone("+8613812345678"); got != "138****5678" {
		t.Fatalf("got %s", got)
	}
}
