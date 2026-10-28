package com.lemanty.aipet.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public final class PetWindow extends JWindow {
    public final PetView pet = new PetView();
    public PetWindow(Runnable open) {
        setSize(160, 160); setAlwaysOnTop(true); setFocusableWindowState(false);
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) setBackground(new Color(0, 0, 0, 0));
        else getContentPane().setBackground(Theme.BG);
        setContentPane(pet);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setLocation(screen.x+screen.width-195, screen.y+screen.height-190);
        MouseAdapter drag = new MouseAdapter() {
            Point start, origin; boolean moved;
            public void mousePressed(MouseEvent e) { start = e.getLocationOnScreen(); origin = getLocation(); moved = false; }
            public void mouseDragged(MouseEvent e) {
                Point p = e.getLocationOnScreen();
                if (start.distance(p) > 4) moved = true;
                if (moved) setLocation(origin.x+p.x-start.x, origin.y+p.y-start.y);
            }
            public void mouseReleased(MouseEvent e) { if (!moved && SwingUtilities.isLeftMouseButton(e)) open.run(); }
        };
        pet.addMouseListener(drag); pet.addMouseMotionListener(drag);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem show = new JMenuItem("打开聊天面板"); show.addActionListener(e -> open.run()); menu.add(show);
        JMenuItem quit = new JMenuItem("退出 AiPet"); quit.addActionListener(e -> { dispose(); System.exit(0); }); menu.add(quit);
        pet.setComponentPopupMenu(menu);
    }
}
