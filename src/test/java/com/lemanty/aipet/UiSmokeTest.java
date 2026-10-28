package com.lemanty.aipet;

import com.lemanty.aipet.service.*;
import com.lemanty.aipet.ui.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** In-process Swing smoke check; does not drive other applications or synthesize system input. */
public final class UiSmokeTest {
    private static MainWindow window;
    private static TaskManager tasks;
    private static final java.util.concurrent.CountDownLatch completed = new java.util.concurrent.CountDownLatch(1);
    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && b.getText().equals(text)) return b;
            if (c instanceof Container child) { JButton b = findButton(child, text); if (b != null) return b; }
        }
        return null;
    }
    private static void capture(String name) throws Exception {
        window.validate();
        BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); window.paint(g); g.dispose();
        Path dir = Path.of("out", "preview"); Files.createDirectories(dir); ImageIO.write(image, "png", dir.resolve(name+".png").toFile());
    }
    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                Theme.install(); DemoAdapter demo = new DemoAdapter(); tasks = new TaskManager(demo); tasks.listen(t -> { if (t.state() == com.lemanty.aipet.model.TaskState.COMPLETED) completed.countDown(); }); window = new MainWindow(demo, tasks); window.setVisible(true);
            });
            SwingUtilities.invokeAndWait(() -> {
                try {
                    capture("chat");
                    findButton(window, "试试：帮我整理今天的灵感").doClick();
                    findButton(window, "发送消息  ↗").doClick();
                    if (tasks.tasks().size() != 1) throw new AssertionError("send button did not create task");
                    findButton(window, "设置").doClick(); capture("settings");
                    findButton(window, "AI 连接").doClick(); capture("connections");
                    findButton(window, "聊天").doClick();
                    findButton(window, "☰").doClick(); capture("collapsed");
                    findButton(window, "☰").doClick();
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            if (!completed.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("demo did not complete");
            SwingUtilities.invokeAndWait(() -> {
                try { capture("answer"); } catch (Exception e) { throw new RuntimeException(e); }
            });
            System.out.println("PASS: startup, sample input, send action, settings, connections, sidebar collapse; previews in out/preview");
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (tasks != null) tasks.close(); for (Window w : Window.getWindows()) w.dispose(); });
        }
    }
}
