package com.lemanty.aipet.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Code-drawn placeholder character. Replace rendering with imported animation assets later. */
public final class PetView extends JComponent {
    public enum Mood { IDLE, THINKING, HAPPY, ERROR }
    private Mood mood = Mood.IDLE;
    private Color accent = Theme.PURPLE;
    private double phase;
    private final Timer timer;
    public PetView() {
        setPreferredSize(new Dimension(150, 150)); setOpaque(false);
        timer = new Timer(40, e -> { phase += .11; repaint(); });
        getAccessibleContext();
        setToolTipText("点击打开 AiPet · 拖动移动");
    }
    @Override public void addNotify() { super.addNotify(); timer.start(); }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }
    public void setMood(Mood mood) { this.mood = mood; repaint(); }
    public void setAccent(Color accent) { this.accent = accent; repaint(); }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = Math.min(getWidth(), getHeight()) / 160.0;
        g.translate((getWidth()-160*scale)/2, (getHeight()-160*scale)/2); g.scale(scale, scale);
        g.setColor(new Color(94, 83, 145, 25)); g.fill(new Ellipse2D.Double(35, 137, 90, 12));
        double bounce = Math.sin(phase) * (mood == Mood.HAPPY ? 9 : 2.5);
        g.translate(mood == Mood.ERROR ? Math.sin(phase*3)*2 : 0, -bounce);
        g.setColor(accent);
        Path2D ear = new Path2D.Double(); ear.moveTo(39, 58); ear.lineTo(35, 20); ear.quadTo(53, 23, 66, 47); ear.closePath(); g.fill(ear);
        Path2D right = new Path2D.Double(); right.moveTo(95, 47); right.quadTo(110, 23, 126, 20); right.lineTo(122, 61); right.closePath(); g.fill(right);
        g.setColor(new Color(250, 201, 218)); g.fillOval(42, 31, 12, 26); g.fillOval(109, 31, 11, 26);
        g.setPaint(new GradientPaint(35, 45, new Color(235, 230, 255), 120, 135, new Color(201, 191, 250)));
        g.fillRoundRect(28, 42, 105, 95, 48, 48);
        g.setColor(accent); g.fillOval(42, 125, 24, 16); g.fillOval(96, 125, 24, 16);
        g.setColor(Color.WHITE); g.fillRoundRect(41, 61, 79, 57, 33, 33);
        g.setColor(Theme.INK); g.setStroke(new BasicStroke(3.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        boolean blink = ((int)(phase * 10) % 120) > 114;
        if (mood == Mood.HAPPY) { g.drawArc(55, 77, 12, 12, 0, 180); g.drawArc(95, 77, 12, 12, 0, 180); }
        else if (blink) { g.drawLine(56, 84, 65, 84); g.drawLine(96, 84, 105, 84); }
        else { g.fillOval(57, 76, 8, 13); g.fillOval(97, 76, 8, 13); }
        g.setColor(new Color(244, 179, 199)); g.fillOval(47, 92, 15, 7); g.fillOval(101, 92, 15, 7);
        g.setColor(Theme.INK);
        if (mood == Mood.ERROR) g.drawArc(75, 98, 12, 9, 0, 180);
        else g.drawArc(74, 91, 14, 12, 190, 160);
        if (mood == Mood.THINKING) {
            g.setColor(accent);
            for (int i=0;i<3;i++) g.fillOval(60+i*15, 8-(int)(Math.sin(phase*2+i)*3), 7, 7);
        }
        if (mood == Mood.HAPPY) { g.setColor(new Color(236, 177, 69)); g.drawLine(139, 52, 149, 52); g.drawLine(144, 47, 144, 57); }
        if (mood == Mood.ERROR) { g.setColor(Theme.RED); g.fillOval(130, 30, 21, 21); g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 16)); g.drawString("!", 138, 47); }
        g.dispose();
    }
}
