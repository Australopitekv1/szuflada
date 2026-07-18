package httpapi

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/australopitekv1/szuflada/apps/relay/internal/pairing"
)

func newTestServer() http.Handler {
	log := slog.New(slog.NewTextHandler(&nullWriter{}, nil))
	return New(pairing.NewStore(), "https://relay.example.com", log).Handler()
}

type nullWriter struct{}

func (*nullWriter) Write(p []byte) (int, error) { return len(p), nil }

func TestCreatePairingSession(t *testing.T) {
	h := newTestServer()

	req := httptest.NewRequest(http.MethodPost, "/pairing/session", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	if rec.Code != http.StatusCreated {
		t.Fatalf("status = %d, oczekiwano 201", rec.Code)
	}

	var body pairingSessionResponse
	if err := json.NewDecoder(rec.Body).Decode(&body); err != nil {
		t.Fatalf("decode: %v", err)
	}
	if len(body.SessionID) != 32 {
		t.Fatalf("sessionId ma %d znaków, oczekiwano 32", len(body.SessionID))
	}
	if body.RelayURL != "https://relay.example.com" {
		t.Fatalf("relayUrl = %q", body.RelayURL)
	}
	if until := time.Until(body.ExpiresAt); until <= 0 || until > pairing.SessionTTL {
		t.Fatalf("expiresAt poza oknem TTL: %v", body.ExpiresAt)
	}
}

func TestPairingSessionRejectsGet(t *testing.T) {
	h := newTestServer()

	req := httptest.NewRequest(http.MethodGet, "/pairing/session", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	if rec.Code != http.StatusMethodNotAllowed {
		t.Fatalf("status = %d, oczekiwano 405", rec.Code)
	}
}

func TestHealthz(t *testing.T) {
	h := newTestServer()

	req := httptest.NewRequest(http.MethodGet, "/healthz", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, oczekiwano 200", rec.Code)
	}
}
