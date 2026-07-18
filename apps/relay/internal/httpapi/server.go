// Package httpapi wystawia minimalne API relaya (sekcja 5 briefu).
// Relay to głupia skrzynka — endpointy nie dotykają żadnych kluczy.
package httpapi

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"time"

	"github.com/australopitekv1/szuflada/apps/relay/internal/pairing"
)

type Server struct {
	pairing   *pairing.Store
	publicURL string
	log       *slog.Logger
}

func New(store *pairing.Store, publicURL string, log *slog.Logger) *Server {
	return &Server{pairing: store, publicURL: publicURL, log: log}
}

func (s *Server) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", s.handleHealthz)
	mux.HandleFunc("POST /pairing/session", s.handleCreatePairingSession)
	return mux
}

func (s *Server) handleHealthz(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

// pairingSessionResponse odpowiada krokowi 1 protokołu parowania:
// desktop dostaje session_id + relay_url i wyświetla je w QR.
type pairingSessionResponse struct {
	SessionID string    `json:"sessionId"`
	RelayURL  string    `json:"relayUrl"`
	ExpiresAt time.Time `json:"expiresAt"`
}

func (s *Server) handleCreatePairingSession(w http.ResponseWriter, r *http.Request) {
	sess, err := s.pairing.Create()
	if err != nil {
		s.log.Error("pairing session create failed", "err", err)
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "internal"})
		return
	}
	writeJSON(w, http.StatusCreated, pairingSessionResponse{
		SessionID: sess.ID,
		RelayURL:  s.publicURL,
		ExpiresAt: sess.ExpiresAt.UTC(),
	})
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
