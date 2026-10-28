package com.lemanty.aipet.service;

import com.lemanty.aipet.model.Conversation;
import java.util.List;
import java.util.concurrent.CompletionStage;

/** Real browser/client integrations implement this boundary; demo never reads other apps. */
public interface AiAdapter extends AutoCloseable {
    List<Conversation> conversations();
    CompletionStage<String> send(Conversation target, String text, boolean simulateFailure);
    @Override default void close() { }
}
