package auth

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"strings"
	"unicode"
)

func NormalizePhone(raw string) (string, error) {
	var b strings.Builder
	for _, r := range raw {
		if unicode.IsDigit(r) || r == '+' {
			b.WriteRune(r)
		}
	}
	s := b.String()
	s = strings.ReplaceAll(s, " ", "")
	if strings.HasPrefix(s, "0086") {
		s = "+86" + strings.TrimPrefix(s, "0086")
	}
	if strings.HasPrefix(s, "86") && !strings.HasPrefix(s, "+") && len(s) == 13 {
		s = "+" + s
	}
	if !strings.HasPrefix(s, "+") {
		if len(s) == 11 && s[0] == '1' {
			s = "+86" + s
		} else {
			return "", fmt.Errorf("invalid phone")
		}
	}
	if !strings.HasPrefix(s, "+86") || len(s) != 14 {
		return "", fmt.Errorf("invalid phone")
	}
	digits := s[3:]
	if digits[0] != '1' {
		return "", fmt.Errorf("invalid phone")
	}
	return s, nil
}

func HashPhone(e164, pepper string) string {
	mac := hmac.New(sha256.New, []byte(pepper))
	mac.Write([]byte(e164))
	return hex.EncodeToString(mac.Sum(nil))
}

func EncryptPhone(e164 string, key []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	nonce := make([]byte, gcm.NonceSize())
	if _, err := rand.Read(nonce); err != nil {
		return nil, err
	}
	return gcm.Seal(nonce, nonce, []byte(e164), nil), nil
}

func DecryptPhone(blob, key []byte) (string, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return "", err
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", err
	}
	ns := gcm.NonceSize()
	if len(blob) < ns {
		return "", fmt.Errorf("ciphertext too short")
	}
	plain, err := gcm.Open(nil, blob[:ns], blob[ns:], nil)
	if err != nil {
		return "", err
	}
	return string(plain), nil
}

func HashToken(raw string) string {
	sum := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(sum[:])
}

func RandomToken() (string, error) {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return hex.EncodeToString(b), nil
}

func MaskPhone(e164 string) string {
	digits := e164
	if strings.HasPrefix(digits, "+86") {
		digits = digits[3:]
	}
	if len(digits) < 7 {
		return "****"
	}
	return digits[:3] + "****" + digits[len(digits)-4:]
}
