package com.lemanty.aipet.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Theme {
    public static final Color BG = new Color(246, 247, 251), INK = new Color(37, 42, 61),
        MUTED = new Color(117, 124, 146), PURPLE = new Color(112, 91, 219), LINE = new Color(229, 232, 241),
        LIGHT = new Color(239, 235, 255), GREEN = new Color(39, 150, 119), RED = new Color(199, 80, 98);
    private Theme() { }
    public static void install() {
        Font font = new Font("SansSerif", Font.PLAIN, 14);
        for (Object key : UIManager.getDefaults().keySet().toArray())
            if (UIManager.get(key) instanceof Font) UIManager.put(key, font);
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextArea.foreground", INK);
        UIManager.put("TextArea.selectionBackground", LIGHT);
    }
    public static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label;
    }
    public static JButton button(String text, boolean primary) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D p = (Graphics2D) g.create();
                p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                p.setColor(!isEnabled() ? LINE : primary ? (getModel().isRollover() ? PURPLE.darker() : PURPLE) : (getModel().isRollover() ? LIGHT : Color.WHITE));
                p.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                if (hasFocus()) { p.setColor(PURPLE); p.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 14, 14); }
                p.dispose(); super.paintComponent(g);
            }
        };
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setForeground(primary ? Color.WHITE : INK);
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 15, 10, 15)); return b;
    }
    public static JPanel row() { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); p.setOpaque(false); p.setAlignmentX(Component.LEFT_ALIGNMENT); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48)); return p; }
    public static JPanel card(LayoutManager layout) {
        JPanel p = new JPanel(layout); p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(18, 20, 18, 20)); return p;
    }
    public static JTextArea text(String content) {
        JTextArea a = new JTextArea(content) {
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, Math.max(45, getPreferredSize().height)); }
        };
        a.setAlignmentX(Component.LEFT_ALIGNMENT); a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setOpaque(false); a.setFont(new Font("SansSerif", Font.PLAIN, 14)); a.setBorder(new EmptyBorder(5, 0, 5, 0)); return a;
    }
}
