// Package pairing implementuje sesje parowania QR (sekcja 4 briefu).
//
// Relay jest zero-knowledge: sesja to wyłącznie identyfikator i TTL —
// żadnych kluczy, żadnego plaintextu. Klucze podróżują w zaszyfrowanych
// kopertach między urządzeniami.
package pairing

import (
	"crypto/rand"
	"encoding/hex"
	"errors"
	"sync"
	"time"
)

// TTL sesji parowania — wymóg bezpieczeństwa z sekcji 4 briefu: 60 sekund.
const SessionTTL = 60 * time.Second

var (
	ErrNotFound = errors.New("pairing: session not found or expired")
	ErrConsumed = errors.New("pairing: session already used")
)

// Session to jednorazowa sesja parowania.
type Session struct {
	ID        string
	CreatedAt time.Time
	ExpiresAt time.Time
	consumed  bool
}

// Store trzyma aktywne sesje w pamięci. Sesje są jednorazowe i wygasają
// po SessionTTL; wygasłe są usuwane leniwie przy każdym dostępie.
type Store struct {
	mu       sync.Mutex
	sessions map[string]*Session
	now      func() time.Time
}

func NewStore() *Store {
	return &Store{
		sessions: make(map[string]*Session),
		now:      time.Now,
	}
}

// newStoreAt pozwala testom kontrolować zegar.
func newStoreAt(now func() time.Time) *Store {
	s := NewStore()
	s.now = now
	return s
}

// Create zakłada nową sesję parowania z losowym ID (128 bitów).
func (s *Store) Create() (*Session, error) {
	var raw [16]byte
	if _, err := rand.Read(raw[:]); err != nil {
		return nil, err
	}
	id := hex.EncodeToString(raw[:])

	s.mu.Lock()
	defer s.mu.Unlock()
	s.gcLocked()

	now := s.now()
	sess := &Session{
		ID:        id,
		CreatedAt: now,
		ExpiresAt: now.Add(SessionTTL),
	}
	s.sessions[id] = sess
	return sess, nil
}

// Consume oznacza sesję jako zużytą — dokładnie raz. Druga próba
// (np. replay skanu QR) zwraca ErrConsumed; nieistniejąca/wygasła —
// ErrNotFound.
func (s *Store) Consume(id string) (*Session, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.gcLocked()

	sess, ok := s.sessions[id]
	if !ok {
		return nil, ErrNotFound
	}
	if sess.consumed {
		return nil, ErrConsumed
	}
	sess.consumed = true
	return sess, nil
}

// Active zwraca liczbę nieskonsumowanych, ważnych sesji.
func (s *Store) Active() int {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.gcLocked()

	n := 0
	for _, sess := range s.sessions {
		if !sess.consumed {
			n++
		}
	}
	return n
}

// gcLocked usuwa wygasłe sesje; wołający trzyma s.mu.
func (s *Store) gcLocked() {
	now := s.now()
	for id, sess := range s.sessions {
		if now.After(sess.ExpiresAt) {
			delete(s.sessions, id)
		}
	}
}
