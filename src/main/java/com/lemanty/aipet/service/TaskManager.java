package com.lemanty.aipet.service;

import com.lemanty.aipet.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Keeps task routing independent of whatever conversation the UI currently selects. */
public final class TaskManager implements AutoCloseable {
    public record Task(UUID id, Conversation target, String prompt, TaskState state, String result, Instant createdAt) { }
    private final AiAdapter adapter;
    private final Map<UUID, Task> tasks = new LinkedHashMap<>();
    private final Set<String> busy = new HashSet<>();
    private final List<Consumer<Task>> listeners = new CopyOnWriteArrayList<>();
    public TaskManager(AiAdapter adapter) { this.adapter = adapter; }
    public void listen(Consumer<Task> listener) { listeners.add(listener); }
    public synchronized List<Task> tasks() { return List.copyOf(tasks.values()); }
    public synchronized boolean isBusy(String conversationId) { return busy.contains(conversationId); }
    public UUID send(Conversation target, String prompt, boolean simulateFailure) {
        Objects.requireNonNull(target);
        String clean = prompt.strip();
        if (clean.isEmpty()) throw new IllegalArgumentException("请输入消息。");
        Task task;
        synchronized (this) {
            if (!busy.add(target.id())) throw new IllegalStateException("这条对话还在回答，请稍后再发送。");
            task = new Task(UUID.randomUUID(), target, clean, TaskState.GENERATING, "", Instant.now());
            tasks.put(task.id(), task);
        }
        publish(task);
        try {
            adapter.send(target, clean, simulateFailure).whenComplete((answer, error) -> finish(task, answer, error));
        } catch (RuntimeException error) { finish(task, null, error); }
        return task.id();
    }
    private void finish(Task original, String answer, Throwable error) {
        Throwable cause = error;
        if (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
        String result = cause == null ? answer : Objects.toString(cause.getMessage(), "未知错误");
        Task updated = new Task(original.id(), original.target(), original.prompt(),
            cause == null ? TaskState.COMPLETED : TaskState.FAILED, result, original.createdAt());
        synchronized (this) { tasks.put(updated.id(), updated); busy.remove(updated.target().id()); }
        publish(updated);
    }
    private void publish(Task task) { listeners.forEach(listener -> listener.accept(task)); }
    public void close() { adapter.close(); }
}
