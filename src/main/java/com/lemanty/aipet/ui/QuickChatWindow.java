package com.lemanty.aipet.ui;

import com.lemanty.aipet.model.*;
import com.lemanty.aipet.service.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;

/** Always-available composer next to the pet. Opening the dashboard is optional. */
public final class QuickChatWindow extends JFrame {
    private final TaskManager tasks;
    private final List<Conversation> conversations;
    private final Consumer<Conversation> onSelect;
    private final Consumer<TaskManager.Task> onAnswer;
    private final JTextField input = new JTextField() {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setColor(new Color(125, 128, 140)); g.setFont(getFont());
                g.drawString("直接输入，和 AI 聊聊…", 2, (getHeight()-g.getFontMetrics().getHeight())/2+g.getFontMetrics().getAscent()); g.dispose();
            }
        }
    };
    private final JButton send = circle("↑", true);
    private final JButton destination = plain("");
    private final JButton result = plain("本地演示");
    private final JPopupMenu picker = new JPopupMenu();
    private Conversation selected;
    private TaskManager.Task latest;

    public QuickChatWindow(TaskManager tasks, List<Conversation> conversations, Consumer<Conversation> onSelect,
                           Consumer<TaskManager.Task> onAnswer, Runnable openSettings) {
        this.tasks = tasks; this.conversations = List.copyOf(conversations); this.onSelect = onSelect; this.onAnswer = onAnswer;
        setUndecorated(true); setType(Window.Type.UTILITY);
        setName("quick-chat"); setSize(570, 102); setAlwaysOnTop(true); setFocusableWindowState(true); setAutoRequestFocus(false);
        if (getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) setBackground(new Color(0,0,0,0));
        JPanel root = new JPanel(new BorderLayout(0, 7)); root.setOpaque(false); root.setBorder(new EmptyBorder(3, 5, 6, 5));
        JPanel meta = new JPanel(new BorderLayout(8,0)); meta.setBackground(new Color(246,247,251)); meta.setBorder(new EmptyBorder(3, 14, 3, 12));
        destination.setToolTipText("选择 AI 和对话 · 当前为本地演示"); destination.addActionListener(e -> showPicker(destination));
        result.setFont(new Font("SansSerif", Font.PLAIN, 11)); result.setForeground(Theme.MUTED);
        result.addActionListener(e -> { if (latest != null) onAnswer.accept(latest); });
        meta.add(destination, BorderLayout.CENTER); meta.add(result, BorderLayout.EAST); root.add(meta, BorderLayout.NORTH);
        JPanel capsule = new JPanel(new BorderLayout(12,0)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(228,229,235)); g.fillRoundRect(0,0,getWidth(),getHeight(),60,60);
                g.setColor(new Color(252,252,255)); g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,58,58); g.dispose();
            }
        };
        capsule.setOpaque(false); capsule.setBorder(new EmptyBorder(8, 10, 8, 10));
        JButton choose = circle("+", false); choose.setToolTipText("选择已有对话"); choose.addActionListener(e -> showPicker(choose)); capsule.add(choose, BorderLayout.WEST);
        input.setName("quick-input"); input.setOpaque(false); input.setBorder(null); input.setFont(new Font("SansSerif", Font.PLAIN, 17)); input.setForeground(Theme.INK);
        input.getAccessibleContext().setAccessibleName("悬浮聊天输入框"); input.setToolTipText("Enter 发送到上方选中的对话"); input.addActionListener(e -> submit());
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSend(); } public void removeUpdate(DocumentEvent e) { updateSend(); } public void changedUpdate(DocumentEvent e) { updateSend(); }
        });
        capsule.add(input, BorderLayout.CENTER); send.setName("quick-send"); send.setToolTipText("发送 · Enter"); send.addActionListener(e -> submit()); capsule.add(send, BorderLayout.EAST);
        root.add(capsule, BorderLayout.CENTER); setContentPane(root);
        JPopupMenu menu = new JPopupMenu(); JMenuItem settings = new JMenuItem("完整记录与设置"); settings.addActionListener(e -> openSettings.run()); menu.add(settings);
        capsule.setComponentPopupMenu(menu);
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "release-focus");
        input.getActionMap().put("release-focus", new AbstractAction() { public void actionPerformed(ActionEvent e) { KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner(); } });
        updateSend();
    }
    private static JButton plain(String text) {
        JButton b = new JButton(text); b.setHorizontalAlignment(SwingConstants.LEFT); b.setFont(new Font("SansSerif", Font.PLAIN, 12)); b.setForeground(Theme.INK);
        b.setBorderPainted(false); b.setContentAreaFilled(false); b.setFocusPainted(false); b.setBorder(new EmptyBorder(2,0,2,0)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private static JButton circle(String text, boolean primary) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(primary ? (isEnabled() ? new Color(128,165,222) : new Color(179,193,217)) : new Color(212,214,221));
                g.fillOval(0,0,getWidth(),getHeight()); g.dispose(); super.paintComponent(graphics);
            }
        };
        b.setPreferredSize(new Dimension(42,42)); b.setForeground(primary ? Color.WHITE : Theme.INK); b.setFont(new Font("SansSerif", Font.PLAIN, 26));
        b.setBorder(null); b.setBorderPainted(false); b.setContentAreaFilled(false); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    public void select(Conversation conversation) {
        selected = conversation;
        destination.setText(conversation.provider()+" / "+conversation.title()+" · 演示  ▾"); updateSend();
    }
    private void showPicker(Component anchor) {
        picker.removeAll(); JMenuItem heading = new JMenuItem("选择已有对话 · 本地演示"); heading.setEnabled(false); picker.add(heading); picker.addSeparator();
        for (Conversation conversation : conversations) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(conversation.provider()+" / "+conversation.title()+"  · "+conversation.source(), conversation.equals(selected));
            item.addActionListener(e -> { select(conversation); onSelect.accept(conversation); input.requestFocusInWindow(); }); picker.add(item);
        }
        picker.show(anchor, 0, -picker.getPreferredSize().height);
    }
    private void submit() {
        if (selected == null || input.getText().isBlank() || tasks.isBusy(selected.id())) return;
        try { tasks.send(selected, input.getText(), false); input.setText(""); updateSend(); }
        catch (IllegalArgumentException | IllegalStateException ex) { result.setText("发送失败"); result.setToolTipText(ex.getMessage()); }
    }
    public void taskChanged(TaskManager.Task task, boolean notify) {
        updateSend();
        if (task.state() == TaskState.GENERATING) {
            if (selected != null && selected.equals(task.target())) result.setText("正在回答… · 演示");
        } else if (notify) {
            latest = task; result.setText(task.state() == TaskState.FAILED ? "出错了 · 查看" : "回答完成 · 查看");
            result.setToolTipText(task.target().provider()+" / "+task.target().title());
        }
    }
    private void updateSend() { send.setEnabled(selected != null && !input.getText().isBlank() && !tasks.isBusy(selected.id())); }
    public void focusInput() { setVisible(true); toFront(); input.requestFocusInWindow(); }
    public void follow(Window pet) {
        Rectangle bounds = pet.getGraphicsConfiguration().getBounds(); Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(pet.getGraphicsConfiguration());
        Rectangle usable = new Rectangle(bounds.x+insets.left, bounds.y+insets.top, bounds.width-insets.left-insets.right, bounds.height-insets.top-insets.bottom);
        int width = Math.min(570, usable.width-20); if (getWidth() != width) setSize(width, 102);
        int x = Math.max(usable.x+5, Math.min(pet.getX()+pet.getWidth()-width, usable.x+usable.width-width-5));
        int y = pet.getY()+pet.getHeight()+4;
        if (y+getHeight() > usable.y+usable.height) y = pet.getY()-getHeight()-4;
        setLocation(x, Math.max(usable.y, Math.min(y, usable.y+usable.height-getHeight())));
    }
}
