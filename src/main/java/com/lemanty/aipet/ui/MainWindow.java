package com.lemanty.aipet.ui;

import com.lemanty.aipet.model.*;
import com.lemanty.aipet.service.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public final class MainWindow extends JFrame {
    private final AiAdapter adapter;
    private final TaskManager tasks;
    private final Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);
    private final PetWindow floating;
    private final PetView hero = new PetView();
    private final PetView preview = new PetView();
    private final JPanel sidebar = new JPanel(new BorderLayout(0, 18));
    private final DefaultListModel<Conversation> listModel = new DefaultListModel<>();
    private final JList<Conversation> conversationList = new JList<>(listModel);
    private final JPanel history = new JPanel();
    private final JScrollPane historyScroll = new JScrollPane(history);
    private final JTextArea input = new JTextArea(3, 30) {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus()) {
                g.setColor(Theme.MUTED); g.setFont(getFont()); g.drawString("写下你的问题…", 12, 27);
            }
        }
    };
    private final JButton send = Theme.button("发送消息  ↗", true);
    private final JLabel target = Theme.label("", 19, Theme.INK, true);
    private final JLabel source = Theme.label("", 12, Theme.MUTED, false);
    private final JLabel status = Theme.label("准备好了，随时出发", 13, Theme.GREEN, false);
    private final JLabel count = Theme.label("", 12, Theme.MUTED, false);
    private final JLabel notice = Theme.label("所有消息仅在本地演示，不会发送到真实 AI。", 12, Theme.MUTED, false);
    private final JButton latestButton = Theme.button("查看演示答案", false);
    private final JCheckBox failure = new JCheckBox("模拟一次出错");
    private final CardLayout pages = new CardLayout();
    private final JPanel content = new JPanel(pages);
    private Conversation selected;
    private TaskManager.Task latest;
    private boolean notifications;
    private Color accent;
    private final javax.swing.Timer resetMood;

    public MainWindow(AiAdapter adapter, TaskManager tasks) {
        super("AiPet · 你的桌面 AI 伙伴");
        this.adapter = adapter; this.tasks = tasks;
        notifications = prefs.getBoolean("notifications", true);
        accent = new Color(prefs.getInt("accent", Theme.PURPLE.getRGB()));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(860, 620)); setSize(1120, 800); setLocationRelativeTo(null);
        floating = new PetWindow(this::showPanel);
        floating.setAlwaysOnTop(prefs.getBoolean("petOnTop", true));
        int petSize = Math.max(100, Math.min(230, prefs.getInt("petSize", 160)));
        floating.setSize(petSize, petSize);
        applyAccent();
        resetMood = new javax.swing.Timer(4000, e -> setMood(activeCount() > 0 ? PetView.Mood.THINKING : PetView.Mood.IDLE));
        resetMood.setRepeats(false);
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(Theme.BG); setContentPane(root);
        root.add(buildHeader(), BorderLayout.NORTH);
        JPanel workspace = new JPanel(new BorderLayout(20, 0)); workspace.setBorder(new EmptyBorder(8, 22, 18, 22));
        workspace.add(buildSidebar(), BorderLayout.WEST);
        content.setOpaque(false); content.add(buildChat(), "chat"); content.add(buildConnections(), "connections"); content.add(buildSettings(), "settings");
        workspace.add(content, BorderLayout.CENTER); root.add(workspace, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        tasks.listen(task -> SwingUtilities.invokeLater(() -> taskChanged(task)));
        refreshList("");
        conversationList.setSelectedIndex(0);
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { setVisible(false); } });
        floating.setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()); header.setBorder(new EmptyBorder(22, 28, 20, 28));
        JPanel brand = Theme.row(); brand.add(Theme.label("●", 25, Theme.PURPLE, true));
        brand.add(Theme.label("AiPet", 25, Theme.INK, true)); brand.add(Theme.label("桌面上的小伙伴", 13, Theme.MUTED, false));
        header.add(brand, BorderLayout.WEST);
        JPanel actions = Theme.row();
        JLabel badge = Theme.label("  DEMO · 本地演示  ", 12, Theme.PURPLE, true); badge.setOpaque(true); badge.setBackground(Theme.LIGHT); badge.setBorder(new EmptyBorder(7, 8, 7, 8)); actions.add(badge);
        JButton chat = Theme.button("聊天", false); chat.addActionListener(e -> pages.show(content, "chat")); actions.add(chat);
        JButton connections = Theme.button("AI 连接", false); connections.addActionListener(e -> pages.show(content, "connections")); actions.add(connections);
        JButton settings = Theme.button("设置", false); settings.addActionListener(e -> pages.show(content, "settings")); actions.add(settings);
        header.add(actions, BorderLayout.EAST); return header;
    }

    private JPanel buildSidebar() {
        sidebar.setPreferredSize(new Dimension(245, 0)); sidebar.setBackground(Color.WHITE); sidebar.setBorder(new EmptyBorder(19, 15, 15, 15));
        JPanel top = new JPanel(); top.setOpaque(false); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(Theme.label("我的对话", 17, Theme.INK, true)); top.add(Box.createVerticalStrut(5)); top.add(Theme.label("选好目的地，再把问题交给我", 12, Theme.MUTED, false)); top.add(Box.createVerticalStrut(16));
        JTextField search = new JTextField(); search.setToolTipText("搜索 AI 或对话名称"); search.putClientProperty("JTextField.placeholderText", "搜索对话");
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.LINE), new EmptyBorder(9, 9, 9, 9)));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshList(search.getText()); }
            public void removeUpdate(DocumentEvent e) { refreshList(search.getText()); }
            public void changedUpdate(DocumentEvent e) { refreshList(search.getText()); }
        });
        top.add(search); top.add(Box.createVerticalStrut(6)); top.add(Theme.label("搜索 AI / 对话名称", 11, Theme.MUTED, false)); sidebar.add(top, BorderLayout.NORTH);
        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); conversationList.setBackground(Color.WHITE); conversationList.setFixedCellHeight(84);
        conversationList.setCellRenderer((list, value, index, selected, focus) -> {
            JPanel row = new JPanel(new BorderLayout(0, 7)); row.setBackground(selected ? Theme.LIGHT : Color.WHITE); row.setBorder(new EmptyBorder(12, 12, 12, 12));
            row.add(Theme.label(value.provider() + "  /  " + value.source(), 11, selected ? Theme.PURPLE : Theme.MUTED, false), BorderLayout.NORTH);
            row.add(Theme.label(value.title() + (tasks.isBusy(value.id()) ? "  ···" : ""), 14, Theme.INK, selected), BorderLayout.CENTER); return row;
        });
        conversationList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && conversationList.getSelectedValue() != null) select(conversationList.getSelectedValue()); });
        JScrollPane scroll = new JScrollPane(conversationList); scroll.setBorder(null); sidebar.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 10)); bottom.setOpaque(false); bottom.add(Theme.label("4 条示例会话 · 非后台检测结果", 11, Theme.MUTED, false), BorderLayout.NORTH);
        JButton refresh = Theme.button("刷新演示列表", false); refresh.addActionListener(e -> refreshList(search.getText())); bottom.add(refresh, BorderLayout.SOUTH); sidebar.add(bottom, BorderLayout.SOUTH); return sidebar;
    }

    private JPanel buildChat() {
        JPanel panel = new JPanel(new BorderLayout(0, 14)); panel.setOpaque(false);
        JPanel welcome = Theme.card(new BorderLayout(18, 0));
        hero.setPreferredSize(new Dimension(108, 112)); welcome.add(hero, BorderLayout.WEST);
        JPanel greeting = new JPanel(); greeting.setOpaque(false); greeting.setLayout(new BoxLayout(greeting, BoxLayout.Y_AXIS));
        greeting.add(Box.createVerticalStrut(9)); greeting.add(Theme.label("让等待，也有一点陪伴。", 23, Theme.INK, true)); greeting.add(Box.createVerticalStrut(12));
        greeting.add(Theme.label("选择一条对话，我会在回答完成时告诉你。", 13, Theme.MUTED, false)); greeting.add(Box.createVerticalStrut(12)); greeting.add(status); welcome.add(greeting, BorderLayout.CENTER); panel.add(welcome, BorderLayout.NORTH);
        JPanel chat = Theme.card(new BorderLayout(0, 12));
        JPanel heading = new JPanel(new BorderLayout(10, 0)); heading.setOpaque(false);
        JButton collapse = Theme.button("☰", false); collapse.setToolTipText("展开或收起会话栏"); collapse.addActionListener(e -> { sidebar.setVisible(!sidebar.isVisible()); revalidate(); }); heading.add(collapse, BorderLayout.WEST);
        JPanel destination = new JPanel(new GridLayout(2, 1, 0, 4)); destination.setOpaque(false); destination.add(target); destination.add(source); heading.add(destination, BorderLayout.CENTER); heading.add(count, BorderLayout.EAST); chat.add(heading, BorderLayout.NORTH);
        history.setLayout(new BoxLayout(history, BoxLayout.Y_AXIS)); history.setBackground(Color.WHITE); historyScroll.setBorder(null); historyScroll.getViewport().setBackground(Color.WHITE); historyScroll.getVerticalScrollBar().setUnitIncrement(18); chat.add(historyScroll, BorderLayout.CENTER);
        JPanel composer = new JPanel(new BorderLayout(0, 8)); composer.setOpaque(false);
        input.setLineWrap(true); input.setWrapStyleWord(true); input.setFont(new Font("SansSerif", Font.PLAIN, 14)); input.setBorder(new EmptyBorder(12, 12, 12, 12)); input.setBackground(Theme.BG);
        JScrollPane inputScroll = new JScrollPane(input); inputScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE)); composer.add(inputScroll, BorderLayout.CENTER);
        JPanel toolbar = new JPanel(new BorderLayout()); toolbar.setOpaque(false); JPanel hints = Theme.row(); failure.setOpaque(false); failure.setForeground(Theme.MUTED); failure.setFont(new Font("SansSerif", Font.PLAIN, 12)); hints.add(failure); hints.add(Theme.label("⌘ / Ctrl + Enter 发送", 11, Theme.MUTED, false)); toolbar.add(hints, BorderLayout.WEST); toolbar.add(send, BorderLayout.EAST); composer.add(toolbar, BorderLayout.SOUTH);
        send.addActionListener(e -> send());
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "send");
        input.getActionMap().put("send", new AbstractAction() { public void actionPerformed(ActionEvent e) { send(); } });
        chat.add(composer, BorderLayout.SOUTH); panel.add(chat, BorderLayout.CENTER); return panel;
    }

    private JPanel buildConnections() {
        JPanel panel = Theme.card(new BorderLayout(0, 20)); panel.add(Theme.label("AI 连接", 24, Theme.INK, true), BorderLayout.NORTH);
        JPanel items = new JPanel(); items.setOpaque(false); items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.add(Theme.text("目前运行的是本地演示模式。下方为规划中的连接方式，没有检测或读取你的后台 AI。")); items.add(Box.createVerticalStrut(20));
        for (String[] item : new String[][] {{"浏览器扩展", "连接已适配的 AI 网页，读取会话、发送消息、观察回答。"}, {"Windows 客户端", "预留 UI Automation 适配入口，后台能力待验证。"}, {"macOS 客户端", "预留 Accessibility 适配入口，后台能力待验证。"}}) {
            items.add(Theme.label(item[0] + "   ·   待接入", 17, Theme.INK, true)); items.add(Box.createVerticalStrut(7)); items.add(Theme.text(item[1])); items.add(Box.createVerticalStrut(25));
        }
        items.add(Theme.label("演示模式不会产生模型 API 费用。", 13, Theme.GREEN, false)); panel.add(items, BorderLayout.CENTER); return panel;
    }

    private JPanel buildSettings() {
        JPanel panel = Theme.card(new BorderLayout(20, 15)); panel.add(Theme.label("让它更像你的小伙伴", 24, Theme.INK, true), BorderLayout.NORTH);
        JPanel settings = new JPanel(); settings.setOpaque(false); settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));
        settings.add(Theme.label("角色外观", 16, Theme.INK, true)); settings.add(Box.createVerticalStrut(12));
        JPanel colors = Theme.row();
        for (Color color : new Color[]{Theme.PURPLE, new Color(66, 155, 140), new Color(208, 126, 153)}) {
            JButton b = Theme.button("●", false); b.setForeground(color); b.addActionListener(e -> { accent = color; prefs.putInt("accent", color.getRGB()); applyAccent(); }); colors.add(b);
        }
        settings.add(colors); settings.add(Box.createVerticalStrut(14)); settings.add(Theme.label("桌宠大小", 13, Theme.MUTED, false));
        JSlider size = new JSlider(100, 230, floating.getWidth()); size.setOpaque(false); size.setAlignmentX(Component.LEFT_ALIGNMENT); size.setMaximumSize(new Dimension(380, 44)); size.addChangeListener(e -> { floating.setSize(size.getValue(), size.getValue()); if (!size.getValueIsAdjusting()) prefs.putInt("petSize", size.getValue()); }); settings.add(size);
        JCheckBox top = new JCheckBox("桌宠始终置顶", floating.isAlwaysOnTop()); top.setOpaque(false); top.setAlignmentX(Component.LEFT_ALIGNMENT); top.addActionListener(e -> { floating.setAlwaysOnTop(top.isSelected()); prefs.putBoolean("petOnTop", top.isSelected()); }); settings.add(top);
        JCheckBox notify = new JCheckBox("显示完成 / 出错提醒", notifications); notify.setOpaque(false); notify.setAlignmentX(Component.LEFT_ALIGNMENT); notify.addActionListener(e -> { notifications = notify.isSelected(); prefs.putBoolean("notifications", notifications); }); settings.add(notify);
        settings.add(Box.createVerticalStrut(20)); settings.add(Theme.label("动作预览", 16, Theme.INK, true)); settings.add(Box.createVerticalStrut(12));
        JPanel moods = Theme.row(); String[] labels = {"待机", "思考", "完成", "出错"};
        for (int i=0;i<labels.length;i++) { final PetView.Mood mood = PetView.Mood.values()[i]; JButton b = Theme.button(labels[i], false); b.addActionListener(e -> { setMood(mood); resetMood.restart(); }); moods.add(b); } settings.add(moods);
        settings.add(Box.createVerticalStrut(20)); settings.add(Theme.text("当前提供内置角色的颜色、大小和动作预览。\n自定义素材导入、动作编辑及事件映射将在后续实现。\n聊天演示记录仅保存在本次运行内，外观设置会保存到本机。"));
        JButton clear = Theme.button("清除已保存的外观设置", false); clear.addActionListener(e -> { prefs.remove("accent"); prefs.remove("petSize"); prefs.remove("petOnTop"); prefs.remove("notifications"); accent = Theme.PURPLE; applyAccent(); size.setValue(160); top.setSelected(true); floating.setAlwaysOnTop(true); notifications = true; notify.setSelected(true); }); settings.add(clear);
        panel.add(settings, BorderLayout.CENTER); preview.setPreferredSize(new Dimension(170, 200)); panel.add(preview, BorderLayout.EAST); return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 0)); footer.setBorder(new EmptyBorder(10, 26, 14, 26)); footer.add(notice, BorderLayout.CENTER);
        JPanel actions = Theme.row(); latestButton.setVisible(false); latestButton.addActionListener(e -> { if (latest != null) { openTask(latest); } }); actions.add(latestButton);
        JButton quit = Theme.button("退出", false); quit.addActionListener(e -> { tasks.close(); floating.dispose(); dispose(); System.exit(0); }); actions.add(quit); footer.add(actions, BorderLayout.EAST); return footer;
    }
    private void refreshList(String search) {
        String id = selected == null ? "" : selected.id(); listModel.clear();
        for (Conversation c : adapter.conversations()) if ((c.provider()+c.title()).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))) listModel.addElement(c);
        for (int i=0;i<listModel.size();i++) if (listModel.get(i).id().equals(id)) conversationList.setSelectedIndex(i);
    }
    private void select(Conversation conversation) {
        selected = conversation; target.setText(conversation.title()); source.setText(conversation.provider()+"  /  "+conversation.source()+"  ·  发送目标已选定");
        pages.show(content, "chat"); renderHistory(); updateSend();
    }
    private void send() {
        if (selected == null || input.getText().isBlank()) { input.requestFocusInWindow(); return; }
        try {
            tasks.send(selected, input.getText(), failure.isSelected()); input.setText(""); failure.setSelected(false); updateSend(); renderHistory();
        } catch (IllegalArgumentException | IllegalStateException ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "暂时无法发送", JOptionPane.INFORMATION_MESSAGE); }
    }
    private void taskChanged(TaskManager.Task task) {
        renderHistory(); updateSend(); conversationList.repaint();
        if (task.state() == TaskState.GENERATING) { resetMood.stop(); setMood(PetView.Mood.THINKING); }
        else {
            latest = task; setMood(task.state() == TaskState.FAILED ? PetView.Mood.ERROR : PetView.Mood.HAPPY); resetMood.restart();
            if (notifications) { notice.setText(task.target().provider()+" / "+task.target().title()+" · "+task.state().label()); notice.setToolTipText(notice.getText()); latestButton.setVisible(true); }
        }
        status.setText(activeCount() > 0 ? activeCount()+" 个对话正在回答，我在帮你留意" : "准备好了，随时出发");
    }
    private long activeCount() { return tasks.tasks().stream().filter(t -> t.state() == TaskState.GENERATING).count(); }
    private void updateSend() { boolean busy = selected != null && tasks.isBusy(selected.id()); send.setEnabled(selected != null && !busy); send.setText(busy ? "正在回答…" : "发送消息  ↗"); }
    private void renderHistory() {
        history.removeAll();
        if (selected == null) return;
        List<TaskManager.Task> current = tasks.tasks().stream().filter(t -> t.target().id().equals(selected.id())).toList();
        count.setText(current.size()+" 条提问");
        if (current.isEmpty()) {
            JPanel empty = new JPanel(); empty.setOpaque(false); empty.setAlignmentX(Component.LEFT_ALIGNMENT); empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS)); empty.setBorder(new EmptyBorder(26, 6, 24, 6));
            empty.add(Theme.label("从一个小问题开始", 20, Theme.INK, true)); empty.add(Box.createVerticalStrut(12)); empty.add(Theme.text("选中的对话会一直作为发送目标。\n回答期间，你可以去其他对话继续提问。")); empty.add(Box.createVerticalStrut(12));
            JButton example = Theme.button("试试：帮我整理今天的灵感", false); example.addActionListener(e -> { input.setText("帮我整理今天的灵感"); input.requestFocusInWindow(); }); empty.add(example); history.add(empty);
        }
        for (TaskManager.Task task : current) {
            JPanel card = new JPanel(); card.setBackground(Theme.BG); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); card.setBorder(new EmptyBorder(13, 15, 13, 15)); card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(Theme.label("你  →  "+task.target().provider(), 12, Theme.PURPLE, true)); card.add(Theme.text(task.prompt())); card.add(Box.createVerticalStrut(8));
            card.add(Theme.label(task.state().label(), 12, task.state() == TaskState.FAILED ? Theme.RED : Theme.GREEN, true));
            card.add(Theme.text(task.state() == TaskState.GENERATING ? "正在本地模拟生成，请稍等……" : task.result()));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height+15)); history.add(card); history.add(Box.createVerticalStrut(12));
        }
        history.add(Box.createVerticalGlue()); history.revalidate(); history.repaint();
        SwingUtilities.invokeLater(() -> historyScroll.getVerticalScrollBar().setValue(historyScroll.getVerticalScrollBar().getMaximum()));
    }
    private void openTask(TaskManager.Task task) {
        select(task.target()); conversationList.setSelectedValue(task.target(), true);
        JTextArea answer = Theme.text(task.result()); answer.setColumns(42); answer.setRows(12);
        JScrollPane scroll = new JScrollPane(answer); scroll.setPreferredSize(new Dimension(560, 320));
        JOptionPane.showMessageDialog(this, scroll, task.target().provider()+" / "+task.target().title()+" · 演示答案", JOptionPane.PLAIN_MESSAGE);
    }
    private void applyAccent() { hero.setAccent(accent); preview.setAccent(accent); floating.pet.setAccent(accent); }
    private void setMood(PetView.Mood mood) { hero.setMood(mood); preview.setMood(mood); floating.pet.setMood(mood); }
    public void showPanel() { setVisible(true); setState(Frame.NORMAL); toFront(); }
}
