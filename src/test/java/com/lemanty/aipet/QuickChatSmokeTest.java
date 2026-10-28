package com.lemanty.aipet;

import com.lemanty.aipet.service.*;
import com.lemanty.aipet.model.TaskState;
import com.lemanty.aipet.ui.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;

public final class QuickChatSmokeTest {
    private static MainWindow dashboard;
    private static TaskManager tasks;
    private static QuickChatWindow quick;
    private static final CountDownLatch completed = new CountDownLatch(1);
    private static JTextField input(Container parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JTextField field && "quick-input".equals(field.getName())) return field;
            if (c instanceof Container child) { JTextField result = input(child); if (result != null) return result; }
        }
        return null;
    }
    private static void capture() throws Exception {
        BufferedImage image = new BufferedImage(660, 340, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.setColor(new Color(29,30,34)); g.fillRect(0,0,660,340);
        for (Window w : Window.getWindows()) if (w instanceof PetWindow pet) {
            Graphics2D p = (Graphics2D)g.create(460,20,160,160); pet.pet.setSize(160,160); pet.pet.paint(p); p.dispose();
        }
        Graphics2D q = (Graphics2D) g.create(40,205,quick.getWidth(),quick.getHeight()); quick.getContentPane().paint(q); q.dispose(); g.dispose();
        Files.createDirectories(Path.of("out/preview")); ImageIO.write(image,"png",Path.of("out/preview/quick-chat.png").toFile());
    }
    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                Theme.install(); DemoAdapter adapter = new DemoAdapter(); tasks = new TaskManager(adapter);
                tasks.listen(t -> { if (t.state() == TaskState.COMPLETED) completed.countDown(); });
                dashboard = new MainWindow(adapter,tasks);
                for (Window window : Window.getWindows()) if (window instanceof QuickChatWindow q) quick = q;
            });
            SwingUtilities.invokeAndWait(() -> {
                if (dashboard.isVisible()) throw new AssertionError("dashboard should stay hidden at startup");
                if (quick == null || !quick.isVisible() || !quick.isFocusableWindow()) throw new AssertionError("composer must be visible and focusable without dashboard");
                JTextField field = input(quick); field.setText("直接在桌宠旁边发送消息"); field.postActionEvent();
                if (tasks.tasks().size()!=1 || !field.getText().isEmpty()) throw new AssertionError("quick Enter submission failed");
                if (!tasks.tasks().getFirst().target().id().equals("demo-doubao-design")) throw new AssertionError("wrong destination");
            });
            if (!completed.await(5,TimeUnit.SECONDS)) throw new AssertionError("demo did not complete");
            SwingUtilities.invokeAndWait(() -> {
                if (dashboard.isVisible()) throw new AssertionError("sending or completion opened dashboard");
                try { capture(); } catch (Exception e) { throw new RuntimeException(e); }
            });
            System.out.println("PASS: startup without dashboard, focusable floating composer, Enter sends to selected conversation, completion without opening dashboard");
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (tasks!=null) tasks.close(); for (Window w : Window.getWindows()) w.dispose(); });
        }
    }
}
