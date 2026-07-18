package pairing

import (
	"errors"
	"sync"
	"testing"
	"time"
)

func TestCreateAndConsume(t *testing.T) {
	s := NewStore()

	sess, err := s.Create()
	if err != nil {
		t.Fatalf("Create: %v", err)
	}
	if len(sess.ID) != 32 {
		t.Fatalf("ID ma %d znaków, oczekiwano 32 (128 bitów hex)", len(sess.ID))
	}
	if got := sess.ExpiresAt.Sub(sess.CreatedAt); got != SessionTTL {
		t.Fatalf("TTL = %v, oczekiwano %v", got, SessionTTL)
	}

	if _, err := s.Consume(sess.ID); err != nil {
		t.Fatalf("Consume: %v", err)
	}
}

func TestConsumeIsOneTime(t *testing.T) {
	s := NewStore()
	sess, _ := s.Create()

	if _, err := s.Consume(sess.ID); err != nil {
		t.Fatalf("pierwsze Consume: %v", err)
	}
	if _, err := s.Consume(sess.ID); !errors.Is(err, ErrConsumed) {
		t.Fatalf("drugie Consume = %v, oczekiwano ErrConsumed (ochrona przed replayem QR)", err)
	}
}

func TestUnknownSession(t *testing.T) {
	s := NewStore()
	if _, err := s.Consume("deadbeefdeadbeefdeadbeefdeadbeef"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("Consume nieznanej sesji = %v, oczekiwano ErrNotFound", err)
	}
}

func TestSessionExpiresAfterTTL(t *testing.T) {
	now := time.Unix(1_700_000_000, 0)
	clock := func() time.Time { return now }
	s := newStoreAt(clock)

	sess, _ := s.Create()

	now = now.Add(SessionTTL - time.Second)
	if _, err := s.Consume(sess.ID); err != nil {
		t.Fatalf("sesja tuż przed TTL powinna działać: %v", err)
	}

	sess2, _ := s.Create()
	now = now.Add(SessionTTL + time.Second)
	if _, err := s.Consume(sess2.ID); !errors.Is(err, ErrNotFound) {
		t.Fatalf("sesja po TTL = %v, oczekiwano ErrNotFound", err)
	}
	if got := s.Active(); got != 0 {
		t.Fatalf("Active po wygaśnięciu = %d, oczekiwano 0", got)
	}
}

func TestConcurrentConsumeExactlyOnce(t *testing.T) {
	s := NewStore()
	sess, _ := s.Create()

	const goroutines = 64
	var wg sync.WaitGroup
	successes := make(chan struct{}, goroutines)

	for range goroutines {
		wg.Add(1)
		go func() {
			defer wg.Done()
			if _, err := s.Consume(sess.ID); err == nil {
				successes <- struct{}{}
			}
		}()
	}
	wg.Wait()
	close(successes)

	n := 0
	for range successes {
		n++
	}
	if n != 1 {
		t.Fatalf("Consume powiodło się %d razy przy współbieżności, oczekiwano dokładnie 1", n)
	}
}
