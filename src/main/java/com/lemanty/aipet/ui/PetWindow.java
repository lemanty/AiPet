package com.lemanty.aipet.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public final class PetWindow extends JWindow {
    public final PetView pet = new PetView();
    private final Timer singleClick;
    private final WindowFrameBuffer surface = new WindowFrameBuffer();
    public PetWindow(Runnable open, Runnable toggleDashboard, Runnable dashboard) {
        Object interval = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
        singleClick = new Timer(interval instanceof Number n ? n.intValue() : 400, e -> open.run());
        singleClick.setRepeats(false);
        setSize(160, 160); setAlwaysOnTop(true); setFocusableWindowState(false);
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) setBackground(new Color(0, 0, 0, 0));
        else getContentPane().setBackground(Theme.BG);
        setContentPane(pet);
        pet.setFrameRequester(this::repaint);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setLocation(screen.x+screen.width-195, screen.y+screen.height-305);
        MouseAdapter drag = new MouseAdapter() {
            Point start, origin; boolean moved;
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) { singleClick.stop(); start = null; return; }
                start = e.getLocationOnScreen(); origin = getLocation(); moved = false;
            }
            public void mouseDragged(MouseEvent e) {
                if (start == null) return;
                Point p = e.getLocationOnScreen();
                if (start.distance(p) > 4) { moved = true; singleClick.stop(); }
                if (moved) { Point next = new Point(origin.x+p.x-start.x, origin.y+p.y-start.y); if (!getLocation().equals(next)) setLocation(next); }
            }
            public void mouseReleased(MouseEvent e) {
                if (!moved && SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() >= 2 && e.getClickCount() % 2 == 0) { singleClick.stop(); toggleDashboard.run(); }
                    else if (e.getClickCount() == 1) singleClick.restart();
                }
                start = null;
            }
        };
        pet.addMouseListener(drag); pet.addMouseMotionListener(drag);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem show = new JMenuItem("输入消息"); show.addActionListener(e -> open.run()); menu.add(show);
        JMenuItem settings = new JMenuItem("打开主页"); settings.addActionListener(e -> dashboard.run()); menu.add(settings);
        JMenuItem quit = new JMenuItem("退出 AiPet"); quit.addActionListener(e -> { dispose(); System.exit(0); }); menu.add(quit);
        pet.setComponentPopupMenu(menu);
    }
    @Override public void paint(Graphics graphics) {
        if (surface == null) { super.paint(graphics); return; }
        surface.paint(graphics, getWidth(), getHeight(), back -> super.paint(back));
    }
    @Override public void update(Graphics graphics) { paint(graphics); }
    @Override public void dispose() { singleClick.stop(); super.dispose(); }
}
