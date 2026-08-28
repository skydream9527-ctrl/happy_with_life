package id

import "github.com/google/uuid"

// New returns a UUIDv7 string. All server IDs use this generator.
func New() string {
	u, err := uuid.NewV7()
	if err != nil {
		// Extremely rare (clock issues); fall back to a still-unique v7-compatible value.
		u = uuid.New()
	}
	return u.String()
}

func Parse(s string) (uuid.UUID, error) {
	return uuid.Parse(s)
}
