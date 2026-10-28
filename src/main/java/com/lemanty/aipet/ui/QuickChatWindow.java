package com.lemanty.aipet.ui;

import com.lemanty.aipet.model.*;
import com.lemanty.aipet.service.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/** An opaque, optionally shaped composer avoids translucent-window caret repaint flashes. */
public final class QuickChatWindow extends JFrame {
    public static final int MIN_WIDTH = 320, MAX_WIDTH = 900, MIN_HEIGHT = 64, MAX_HEIGHT = 200;
    private static final Color BACKGROUND = new Color(228,229,235);
    private final Preferences prefs = Preferences.userNodeForPackage(QuickChatWindow.class);
    private final TaskManager tasks;
    private final List<Conversation> conversations;
    private final Consumer<Conversation> onSelect;
    private final Consumer<TaskManager.Task> onAnswer;
    private final JButton choose = circle("+", false);
    private final JButton send = circle("↑", true);
    private final JTextArea input = new JTextArea() {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty()) {
                Graphics2D g = (Graphics2D)graphics.create(); g.setColor(new Color(125,128,140)); g.setFont(getFont());
                g.drawString("直接输入，和 AI 聊聊…", 2, getFontMetrics(getFont()).getAscent()+getInsets().top); g.dispose();
            }
        }
    };
    private final JPopupMenu picker = new JPopupMenu();
    private final JMenuItem answerItem = new JMenuItem("暂无完成的回答");
    private final javax.swing.Timer followTimer;
    private Window anchor;
    private Conversation selected;
    private TaskManager.Task latest;
    private int preferredWidth, preferredHeight;

    public QuickChatWindow(TaskManager tasks, List<Conversation> conversations, Consumer<Conversation> onSelect,
                           Consumer<TaskManager.Task> onAnswer, Runnable openHome) {
        this.tasks=tasks; this.conversations=List.copyOf(conversations); this.onSelect=onSelect; this.onAnswer=onAnswer;
        setUndecorated(true); setType(Window.Type.UTILITY); setName("quick-chat");
        setAlwaysOnTop(true); setAutoRequestFocus(false); setBackground(BACKGROUND);
        preferredWidth = clamp(prefs.getInt("composerWidth",570),MIN_WIDTH,MAX_WIDTH);
        preferredHeight = clamp(prefs.getInt("composerHeight",64),MIN_HEIGHT,MAX_HEIGHT);
        setSize(preferredWidth,preferredHeight);
        followTimer = new javax.swing.Timer(16,e -> positionAtPet()); followTimer.setRepeats(false); followTimer.setCoalesce(true);

        JPanel capsule = new JPanel(new BorderLayout(12,0)); capsule.setBackground(BACKGROUND); capsule.setBorder(new EmptyBorder(11,12,11,18));
        choose.setName("quick-choose"); choose.addActionListener(e -> showPicker(choose));
        capsule.add(centered(choose),BorderLayout.WEST);
        input.setName("quick-input"); input.setBackground(BACKGROUND); input.setBorder(new EmptyBorder(9,0,0,0));
        input.setLineWrap(true); input.setWrapStyleWord(true); input.setFont(new Font("SansSerif",Font.PLAIN,17)); input.setForeground(Theme.INK);
        input.getAccessibleContext().setAccessibleName("悬浮聊天输入框");
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"quick-send");
        input.getActionMap().put("quick-send",new AbstractAction() { public void actionPerformed(ActionEvent e) { submit(); } });
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.SHIFT_DOWN_MASK),"insert-break");
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSend(); } public void removeUpdate(DocumentEvent e) { updateSend(); } public void changedUpdate(DocumentEvent e) { updateSend(); }
        });
        JScrollPane scroll = new JScrollPane(input); scroll.setBorder(null); scroll.setBackground(BACKGROUND); scroll.getViewport().setBackground(BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); scroll.getVerticalScrollBar().setPreferredSize(new Dimension(7,0));
        capsule.add(scroll,BorderLayout.CENTER);
        send.setName("quick-send"); send.addActionListener(e -> submit()); capsule.add(centered(send),BorderLayout.EAST);
        JComponent grip = resizeGrip();
        JLayeredPane layers = new JLayeredPane() {
            @Override public void doLayout() { capsule.setBounds(0,0,getWidth(),getHeight()); grip.setBounds(getWidth()-25,getHeight()-22,19,17); }
        };
        layers.setOpaque(true); layers.setBackground(BACKGROUND); layers.add(capsule,JLayeredPane.DEFAULT_LAYER); layers.add(grip,JLayeredPane.PALETTE_LAYER); setContentPane(layers);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem home = new JMenuItem("打开主页"); home.addActionListener(e -> openHome.run()); menu.add(home);
        answerItem.setEnabled(false); answerItem.addActionListener(e -> { if(latest!=null) onAnswer.accept(latest); }); menu.add(answerItem);
        JMenuItem reset = new JMenuItem("恢复输入栏默认大小"); reset.addActionListener(e -> setComposerSize(570,64)); menu.add(reset);
        capsule.setComponentPopupMenu(menu); input.setComponentPopupMenu(menu); grip.setComponentPopupMenu(menu);
        addComponentListener(new ComponentAdapter() { public void componentResized(ComponentEvent e) { updateShape(); } });
        updateShape(); updateSend();
    }
    private static JPanel centered(JComponent component) {
        JPanel panel = new JPanel(new GridBagLayout()); panel.setOpaque(false); panel.add(component); return panel;
    }
    private static JButton circle(String text,boolean primary) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(primary ? (isEnabled()?new Color(128,165,222):new Color(179,193,217)) : new Color(212,214,221));
                g.fillOval(0,0,getWidth(),getHeight()); g.dispose(); super.paintComponent(graphics);
            }
        };
        button.setPreferredSize(new Dimension(42,42)); button.setForeground(primary?Color.WHITE:Theme.INK); button.setFont(new Font("SansSerif",Font.PLAIN,26));
        button.setBorder(null); button.setBorderPainted(false); button.setContentAreaFilled(false); button.setFocusPainted(false); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return button;
    }
    private JComponent resizeGrip() {
        JComponent grip = new JComponent() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g=(Graphics2D)graphics.create(); g.setColor(new Color(145,150,164));
                g.drawLine(7,13,14,6); g.drawLine(11,13,14,10); g.dispose();
            }
        };
        grip.setName("composer-resize"); grip.setToolTipText("拖动调整输入栏宽度和高度"); grip.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        MouseAdapter resize = new MouseAdapter() {
            Point origin; Dimension initial;
            public void mousePressed(MouseEvent e) { if(SwingUtilities.isLeftMouseButton(e)) { followTimer.stop(); origin=e.getLocationOnScreen(); initial=getSize(); } }
            public void mouseDragged(MouseEvent e) { if(origin!=null) resizeComposer(initial.width+e.getXOnScreen()-origin.x,initial.height+e.getYOnScreen()-origin.y); }
            public void mouseReleased(MouseEvent e) { if(origin!=null) { saveSize(); origin=null; } }
        };
        grip.addMouseListener(resize); grip.addMouseMotionListener(resize); return grip;
    }
    public void setComposerSize(int width,int height) { resizeComposer(width,height); saveSize(); }
    private void resizeComposer(int width,int height) {
        preferredWidth=clamp(width,MIN_WIDTH,MAX_WIDTH); preferredHeight=clamp(height,MIN_HEIGHT,MAX_HEIGHT);
        Rectangle area=usableArea(getGraphicsConfiguration());
        int w=Math.min(preferredWidth,area.width), h=Math.min(preferredHeight,area.height);
        Rectangle next=new Rectangle(clamp(getX(),area.x,area.x+area.width-w),clamp(getY(),area.y,area.y+area.height-h),w,h);
        if (anchor != null) {
            // Keep the composer's left edge under the user's drag, and move the pet
            // with its new right edge. The same layout is used by settings and resets.
            int petX = next.x + next.width - anchor.getWidth();
            if (anchor.getX() != petX) anchor.setLocation(petX, anchor.getY());
            positionAtPet();
        } else if(!getBounds().equals(next)) setBounds(next);
        updateShape();
    }
    private void saveSize() { prefs.putInt("composerWidth",preferredWidth); prefs.putInt("composerHeight",preferredHeight); }
    private void updateShape() {
        if(getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT))
            setShape(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),Math.min(getHeight(),48),Math.min(getHeight(),48)));
        // Opaque rectangular fallback on systems without window shaping; never clear the whole window to transparent.
    }
    public void select(Conversation conversation) { selected=conversation; updateSend(); }
    private void showPicker(Component component) {
        picker.removeAll(); JMenuItem heading=new JMenuItem("选择已有对话 · 本地演示"); heading.setEnabled(false); picker.add(heading); picker.addSeparator();
        for(Conversation conversation:conversations) {
            JRadioButtonMenuItem item=new JRadioButtonMenuItem(conversation.provider()+" / "+conversation.title()+" · "+conversation.source(),conversation.equals(selected));
            item.addActionListener(e -> { select(conversation); onSelect.accept(conversation); input.requestFocusInWindow(); }); picker.add(item);
        }
        picker.show(component,0,-picker.getPreferredSize().height);
    }
    private void submit() {
        if(selected==null || input.getText().isBlank() || tasks.isBusy(selected.id())) return;
        try { tasks.send(selected,input.getText(),false); input.setText(""); updateSend(); }
        catch(IllegalArgumentException|IllegalStateException ex) { input.setToolTipText("发送失败："+ex.getMessage()); }
    }
    public void taskChanged(TaskManager.Task task,boolean notify) {
        if(task.state()!=TaskState.GENERATING && notify) {
            latest=task; answerItem.setText((task.state()==TaskState.FAILED?"查看错误":"查看回答")+" · "+task.target().title()); answerItem.setEnabled(true);
        }
        updateSend();
    }
    private void updateSend() {
        boolean hasText=!input.getText().isBlank();
        send.setEnabled(selected!=null && hasText && !tasks.isBusy(selected.id()));
        String target=selected==null?"请选择对话":selected.provider()+" / "+selected.title();
        choose.setText("+");
        // Clicking + always selects a conversation. Completed answers live in the right-click menu.
        choose.setToolTipText("选择对话 · 当前："+target+" · 本地演示"+(latest==null?"":" · 右键输入栏查看结果"));
        input.setToolTipText(target+" · 本地演示 · Enter 发送，Shift+Enter 换行");
        send.setToolTipText(selected!=null && tasks.isBusy(selected.id())?"这条对话正在回答":"发送到 "+target);
    }
    public void focusInput() { if(!isVisible()) setVisible(true); if(!isFocused()) { toFront(); requestFocus(); } input.requestFocusInWindow(); }
    public void follow(Window pet) { anchor=pet; positionAtPet(); }
    public void scheduleFollow(Window pet) { anchor=pet; if(!followTimer.isRunning()) followTimer.start(); }
    private void positionAtPet() {
        if(anchor==null) return;
        Rectangle area=usableArea(anchor.getGraphicsConfiguration()); int width=Math.min(preferredWidth,area.width), height=Math.min(preferredHeight,area.height);
        int x=clamp(anchor.getX()+anchor.getWidth()-width,area.x,area.x+area.width-width);
        // Clamp the pair together at screen edges, rather than detaching the bar.
        int petX = x + width - anchor.getWidth();
        if (anchor.getX() != petX) anchor.setLocation(petX, anchor.getY());
        int y=anchor.getY()+anchor.getHeight()+4; if(y+height>area.y+area.height) y=anchor.getY()-height-4;
        Rectangle next=new Rectangle(x,clamp(y,area.y,area.y+area.height-height),width,height);
        if(!getBounds().equals(next)) setBounds(next);
    }
    private static Rectangle usableArea(GraphicsConfiguration configuration) {
        Rectangle b=configuration.getBounds(); Insets i=Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        return new Rectangle(b.x+i.left,b.y+i.top,b.width-i.left-i.right,b.height-i.top-i.bottom);
    }
    private static int clamp(int value,int min,int max) { return Math.max(min,Math.min(value,max)); }
    @Override public void dispose() { if(followTimer!=null) followTimer.stop(); super.dispose(); }
}
