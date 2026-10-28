package com.lemanty.aipet.model;

/** Stable IDs, not visible titles, identify message destinations. */
public record Conversation(String id, String provider, String title, String source) {
    @Override public String toString() { return title; }
}
