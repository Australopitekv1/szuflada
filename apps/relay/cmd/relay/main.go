// Relay Szuflady — zero-knowledge skrzynka na zaszyfrowane koperty.
// Konfiguracja przez env: RELAY_ADDR (default :8080), RELAY_PUBLIC_URL.
package main

import (
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/australopitekv1/szuflada/apps/relay/internal/httpapi"
	"github.com/australopitekv1/szuflada/apps/relay/internal/pairing"
)

func main() {
	log := slog.New(slog.NewJSONHandler(os.Stdout, nil))

	addr := envOr("RELAY_ADDR", ":8080")
	publicURL := envOr("RELAY_PUBLIC_URL", "http://localhost:8080")

	srv := httpapi.New(pairing.NewStore(), publicURL, log)

	httpServer := &http.Server{
		Addr:              addr,
		Handler:           srv.Handler(),
		ReadHeaderTimeout: 5 * time.Second,
	}

	log.Info("relay listening", "addr", addr, "publicUrl", publicURL)
	if err := httpServer.ListenAndServe(); err != nil {
		log.Error("server exited", "err", err)
		os.Exit(1)
	}
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
