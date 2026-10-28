package com.lemanty.aipet.service;

import com.lemanty.aipet.model.Conversation;
import java.util.List;
import java.util.concurrent.*;

public final class DemoAdapter implements AiAdapter {
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "aipet-demo"); t.setDaemon(true); return t;
    });
    private final List<Conversation> conversations = List.of(
        new Conversation("demo-doubao-design", "豆包", "桌宠产品设计", "浏览器 · 演示"),
        new Conversation("demo-doubao-trip", "豆包", "周末旅行计划", "浏览器 · 演示"),
        new Conversation("demo-claude-code", "Claude", "一起写点代码", "客户端 · 演示"),
        new Conversation("demo-deepseek-notes", "DeepSeek", "灵感收集箱", "浏览器 · 演示")
    );
    public List<Conversation> conversations() { return conversations; }
    public CompletionStage<String> send(Conversation target, String text, boolean simulateFailure) {
        CompletableFuture<String> result = new CompletableFuture<>();
        if (!conversations.contains(target)) {
            result.completeExceptionally(new IllegalArgumentException("目标会话不存在，消息未发送。"));
            return result;
        }
        worker.schedule(() -> {
            if (simulateFailure) result.completeExceptionally(new IllegalStateException("演示错误：模拟 AI 服务返回失败。你可以重新发送。"));
            else result.complete("这是「" + target.provider() + " / " + target.title() + "」的演示回答。\n\n你刚刚输入：" + text
                + "\n\n本次任务始终绑定这条对话，切换会话不会改变发送目标。现在可以查看桌宠的完成动画。\n\n真实 AI 尚未连接；这段内容由本地演示生成，没有调用模型或读取后台软件。");
        }, 2400, TimeUnit.MILLISECONDS);
        return result;
    }
    public void close() { worker.shutdownNow(); }
}
