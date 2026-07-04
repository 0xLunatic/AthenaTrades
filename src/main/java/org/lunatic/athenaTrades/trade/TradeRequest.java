package org.lunatic.athenaTrades.trade;

import java.util.UUID;

/**
 * Merepresentasikan satu trade request yang belum diterima/ditolak.
 * Otomatis expired setelah 30 detik supaya tidak menumpuk kalau diabaikan.
 */
public class TradeRequest {

    private static final long EXPIRY_MS = 30_000L;

    private final UUID sender;
    private final UUID target;
    private final long createdAt;

    public TradeRequest(UUID sender, UUID target) {
        this.sender = sender;
        this.target = target;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getTarget() {
        return target;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > EXPIRY_MS;
    }
}