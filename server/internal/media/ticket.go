package media

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"strconv"
	"strings"
	"time"
)

func signTicket(secret, mediaID, op string, exp time.Time) string {
	mac := hmac.New(sha256.New, []byte(secret))
	msg := fmt.Sprintf("%s|%d|%s", mediaID, exp.Unix(), op)
	mac.Write([]byte(msg))
	return fmt.Sprintf("%s.%d.%s.%s", mediaID, exp.Unix(), op, hex.EncodeToString(mac.Sum(nil)[:16]))
}

func parseTicket(secret, ticket string) (mediaID, op string, exp time.Time, ok bool) {
	parts := strings.Split(ticket, ".")
	if len(parts) != 4 {
		return "", "", time.Time{}, false
	}
	mediaID, expStr, op, sig := parts[0], parts[1], parts[2], parts[3]
	unix, err := strconv.ParseInt(expStr, 10, 64)
	if err != nil {
		return "", "", time.Time{}, false
	}
	exp = time.Unix(unix, 0).UTC()
	if time.Now().UTC().After(exp) {
		return "", "", time.Time{}, false
	}
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(fmt.Sprintf("%s|%d|%s", mediaID, unix, op)))
	want := hex.EncodeToString(mac.Sum(nil)[:16])
	if !hmac.Equal([]byte(sig), []byte(want)) {
		return "", "", time.Time{}, false
	}
	if op != "p" && op != "g" {
		return "", "", time.Time{}, false
	}
	return mediaID, op, exp, true
}
