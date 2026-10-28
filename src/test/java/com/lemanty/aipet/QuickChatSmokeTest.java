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
    private static PetWindow pet;
    private static final CountDownLatch completed = new CountDownLatch(1);
    private static JTextArea input(Container parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JTextArea field && "quick-input".equals(field.getName())) return field;
            if (c instanceof Container child) { JTextArea result = input(child); if (result != null) return result; }
        }
        return null;
    }
    private static Component findNamed(Container parent,String name) {
        for(Component c:parent.getComponents()) {
            if(name.equals(c.getName())) return c;
            if(c instanceof Container child) { Component found=findNamed(child,name); if(found!=null) return found; }
        }
        return null;
    }
    private static void settleNativeLayout() throws Exception {
        CountDownLatch settled = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> { javax.swing.Timer t = new javax.swing.Timer(180,e -> settled.countDown()); t.setRepeats(false); t.start(); });
        if (!settled.await(2,TimeUnit.SECONDS)) throw new AssertionError("UI did not settle");
    }
    private static void capture() throws Exception {
        BufferedImage image = new BufferedImage(660, 340, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.setColor(new Color(29,30,34)); g.fillRect(0,0,660,340);
        for (Window w : Window.getWindows()) if (w instanceof PetWindow pet) {
            Graphics2D p = (Graphics2D)g.create(460,20,160,160); pet.pet.setSize(160,160); pet.pet.paint(p); p.dispose();
        }
        Graphics2D q = (Graphics2D) g.create(40,205,quick.getWidth(),quick.getHeight()); if (quick.getShape()!=null) q.clip(quick.getShape()); quick.getContentPane().paint(q); q.dispose(); g.dispose();
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
                JTextArea field = input(quick); field.setText("直接在桌宠旁边发送消息");
                field.getActionMap().get("quick-send").actionPerformed(new java.awt.event.ActionEvent(field,0,"send"));
                if (tasks.tasks().size()!=1 || !field.getText().isEmpty()) throw new AssertionError("quick Enter submission failed");
                if (!tasks.tasks().getFirst().target().id().equals("demo-doubao-design")) throw new AssertionError("wrong destination");
            });
            if (!completed.await(5,TimeUnit.SECONDS)) throw new AssertionError("demo did not complete");
            SwingUtilities.invokeAndWait(() -> {
                if (dashboard.isVisible()) throw new AssertionError("sending or completion opened dashboard");
                for(Window w:Window.getWindows()) if(w instanceof PetWindow p) pet=p;
                if(pet==null) throw new AssertionError("missing pet");
                quick.setComposerSize(650,112); quick.follow(pet);
            });
            settleNativeLayout();
            SwingUtilities.invokeAndWait(() -> {
                if(quick.getWidth()!=650 || quick.getHeight()!=112) throw new AssertionError("follow must preserve resized composer");
                Rectangle stable=quick.getBounds();
                for(int i=0;i<100;i++) quick.follow(pet);
                if(!quick.getBounds().equals(stable)) throw new AssertionError("stationary pet caused composer movement: " + stable + " -> " + quick.getBounds());
                // Exercise the actual resize handle listeners using local component events.
                Component grip = findNamed(quick,"composer-resize");
                Point at = grip.getLocationOnScreen();
                java.awt.event.MouseEvent press = new java.awt.event.MouseEvent(grip,java.awt.event.MouseEvent.MOUSE_PRESSED,0,0,3,3,at.x+3,at.y+3,1,false,java.awt.event.MouseEvent.BUTTON1);
                for(var listener:grip.getMouseListeners()) listener.mousePressed(press);
                java.awt.event.MouseEvent drag = new java.awt.event.MouseEvent(grip,java.awt.event.MouseEvent.MOUSE_DRAGGED,0,java.awt.event.InputEvent.BUTTON1_DOWN_MASK,43,19,at.x+43,at.y+19,1,false,java.awt.event.MouseEvent.BUTTON1);
                for(var listener:grip.getMouseMotionListeners()) listener.mouseDragged(drag);
                for(var listener:grip.getMouseListeners()) listener.mouseReleased(drag);
            });
            settleNativeLayout();
            SwingUtilities.invokeAndWait(() -> {
                if(quick.getWidth()!=690 || quick.getHeight()!=128) throw new AssertionError("resize grip did not change both dimensions: " + quick.getSize());
                quick.setComposerSize(570,64); quick.validate();
            });
            settleNativeLayout();
            SwingUtilities.invokeAndWait(() -> {
                // A double release must open the home; closing home must leave the composer alive.
                Point point = pet.pet.getLocationOnScreen();
                var doubleClick = new java.awt.event.MouseEvent(pet.pet,java.awt.event.MouseEvent.MOUSE_RELEASED,0,0,20,20,point.x+20,point.y+20,2,false,java.awt.event.MouseEvent.BUTTON1);
                for(var listener:pet.pet.getMouseListeners()) listener.mouseReleased(doubleClick);
                if(!dashboard.isVisible()) throw new AssertionError("double click did not open home");
                dashboard.dispatchEvent(new java.awt.event.WindowEvent(dashboard,java.awt.event.WindowEvent.WINDOW_CLOSING));
                if(dashboard.isVisible() || !quick.isVisible() || !pet.isVisible()) throw new AssertionError("closing home must preserve pet and composer");

            });
            SwingUtilities.invokeAndWait(() -> {
                try { capture(); } catch (Exception e) { throw new RuntimeException(e); }
            });
            System.out.println("PASS: startup without dashboard, focusable floating composer, Enter sends to selected conversation, completion without opening dashboard, resize grip, stable following, double-click home, home close");
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (tasks!=null) tasks.close(); for (Window w : Window.getWindows()) w.dispose(); });
        }
    }
}
