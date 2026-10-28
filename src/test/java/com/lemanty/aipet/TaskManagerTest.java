package com.lemanty.aipet;

import com.lemanty.aipet.model.*;
import com.lemanty.aipet.service.*;
import java.util.*;
import java.util.concurrent.*;

/** Dependency-free regression checks; run using test.sh or test.bat. */
public final class TaskManagerTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Conversation a = new Conversation("a", "AI A", "会话 A", "test");
        Conversation b = new Conversation("b", "AI B", "会话 B", "test");
        Map<String, CompletableFuture<String>> pending = new HashMap<>();
        AiAdapter adapter = new AiAdapter() {
            public List<Conversation> conversations() { return List.of(a, b); }
            public CompletionStage<String> send(Conversation target, String text, boolean failure) {
                CompletableFuture<String> result = new CompletableFuture<>(); pending.put(target.id(), result); return result;
            }
        };
        TaskManager manager = new TaskManager(adapter);
        List<TaskManager.Task> events = new ArrayList<>(); manager.listen(events::add);
        UUID first = manager.send(a, "给 A 的消息", false);
        UUID second = manager.send(b, "给 B 的消息", false);
        check(manager.isBusy("a") && manager.isBusy("b"), "different conversations should run concurrently");
        try { manager.send(a, "重复发送", false); throw new AssertionError("duplicate must be blocked"); }
        catch (IllegalStateException expected) { }
        pending.get("b").complete("B 先完成");
        pending.get("a").complete("A 后完成");
        TaskManager.Task firstResult = manager.tasks().stream().filter(t -> t.id().equals(first)).findFirst().orElseThrow();
        TaskManager.Task secondResult = manager.tasks().stream().filter(t -> t.id().equals(second)).findFirst().orElseThrow();
        check(firstResult.target().equals(a) && firstResult.result().equals("A 后完成"), "A must retain its own target and answer");
        check(secondResult.target().equals(b) && secondResult.result().equals("B 先完成"), "B must retain its own target and answer");
        check(!manager.isBusy("a") && !manager.isBusy("b"), "completion must release conversation locks");
        UUID failed = manager.send(a, "错误场景", true);
        pending.get("a").completeExceptionally(new IllegalStateException("服务失败"));
        check(manager.tasks().stream().anyMatch(t -> t.id().equals(failed) && t.state() == TaskState.FAILED && t.result().equals("服务失败")), "error must be distinct from completion");
        check(!manager.isBusy("a"), "failure must release lock");
        try { manager.send(a, " \n ", false); throw new AssertionError("blank input must be rejected"); }
        catch (IllegalArgumentException expected) { }
        check(events.size() == 6, "one start and one terminal event per task");
        manager.close();
        System.out.println("PASS: target binding, concurrent completion, duplicate prevention, error state, empty input, event count");
    }
}
