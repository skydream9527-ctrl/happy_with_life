package redisx

import (
	"context"
	"fmt"
	"time"

	"github.com/redis/go-redis/v9"
)

type Client struct {
	rdb    redis.UniversalClient
	prefix string
}

func Connect(addr, password string, db int, prefix string) (*Client, error) {
	rdb := redis.NewClient(&redis.Options{
		Addr:         addr,
		Password:     password,
		DB:           db,
		ReadTimeout:  2 * time.Second,
		WriteTimeout: 2 * time.Second,
	})
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	if err := rdb.Ping(ctx).Err(); err != nil {
		_ = rdb.Close()
		return nil, err
	}
	return &Client{rdb: rdb, prefix: prefix}, nil
}

func NewWith(rdb redis.UniversalClient, prefix string) *Client {
	return &Client{rdb: rdb, prefix: prefix}
}

func (c *Client) Close() error {
	if c == nil || c.rdb == nil {
		return nil
	}
	return c.rdb.Close()
}

func (c *Client) Ping(ctx context.Context) error {
	return c.rdb.Ping(ctx).Err()
}

func (c *Client) key(parts ...string) string {
	k := c.prefix
	for i, p := range parts {
		if i > 0 {
			k += ":"
		}
		k += p
	}
	return k
}

func (c *Client) IncrWindow(ctx context.Context, scope, id string, window time.Duration) (int64, error) {
	k := c.key("rl", scope, id)
	n, err := c.rdb.Incr(ctx, k).Result()
	if err != nil {
		return 0, err
	}
	if n == 1 {
		_ = c.rdb.Expire(ctx, k, window).Err()
	}
	return n, nil
}

func (c *Client) SetCodeHash(ctx context.Context, phoneHash, codeHash string, ttl time.Duration) error {
	k := c.key("sms", "code", phoneHash)
	pipe := c.rdb.TxPipeline()
	pipe.Set(ctx, k, codeHash, ttl)
	pipe.Del(ctx, c.key("sms", "tries", phoneHash))
	_, err := pipe.Exec(ctx)
	return err
}

func (c *Client) GetCodeHash(ctx context.Context, phoneHash string) (string, error) {
	v, err := c.rdb.Get(ctx, c.key("sms", "code", phoneHash)).Result()
	if err == redis.Nil {
		return "", nil
	}
	return v, err
}

func (c *Client) IncrCodeTries(ctx context.Context, phoneHash string, ttl time.Duration) (int64, error) {
	k := c.key("sms", "tries", phoneHash)
	n, err := c.rdb.Incr(ctx, k).Result()
	if err != nil {
		return 0, err
	}
	if n == 1 {
		_ = c.rdb.Expire(ctx, k, ttl).Err()
	}
	return n, nil
}

func (c *Client) DeleteCode(ctx context.Context, phoneHash string) error {
	return c.rdb.Del(ctx, c.key("sms", "code", phoneHash), c.key("sms", "tries", phoneHash)).Err()
}

func (c *Client) HealthError(ctx context.Context) error {
	if c == nil || c.rdb == nil {
		return fmt.Errorf("redis not configured")
	}
	return c.Ping(ctx)
}
