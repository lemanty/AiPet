package com.lemanty.aipet.ui;

import com.lemanty.aipet.model.Conversation;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Locale;
import java.util.function.*;

/** Search and provider filters never change the destination until explicitly chosen. */
public final class ConversationPicker extends JPanel {
    private final JTextField search=new JTextField();
    private final JComboBox<String> providers=new JComboBox<>();
    private final DefaultListModel<Conversation> model=new DefaultListModel<>();
    private final JList<Conversation> list=new JList<>(model);
    private final JLabel count=new JLabel();
    private final List<Conversation> conversations;
    private final Conversation current;

    public ConversationPicker(List<Conversation> conversations, Conversation current,
                              Function<Conversation,String> status, Consumer<Conversation> choose) {
        super(new BorderLayout(0,8));
        this.conversations=List.copyOf(conversations); this.current=current;
        setBorder(new EmptyBorder(12,12,12,12));
        JPanel filters=new JPanel(new BorderLayout(8,6));
        filters.add(new JLabel("选择会话 · 本地演示"),BorderLayout.NORTH);
        providers.addItem("全部 AI"); conversations.stream().map(Conversation::provider).distinct().forEach(providers::addItem);
        providers.setName("picker-provider"); search.setName("picker-search");
        search.setToolTipText("搜索对话标题、AI 或来源"); search.getAccessibleContext().setAccessibleName("搜索会话");
        filters.add(providers,BorderLayout.WEST); filters.add(search,BorderLayout.CENTER); add(filters,BorderLayout.NORTH);
        list.setName("picker-results"); list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(52);
        list.setCellRenderer((items,value,index,selected,focused) -> {
            JPanel row=new JPanel(new BorderLayout()); row.setBorder(new EmptyBorder(4,6,4,6));
            row.setBackground(selected?items.getSelectionBackground():items.getBackground());
            Color color=selected?items.getSelectionForeground():items.getForeground();
            JLabel title=new JLabel((value.equals(current)?"✓ ":"")+value.provider()+" / "+value.title()); title.setForeground(color);
            JLabel detail=new JLabel(value.source()+" · "+status.apply(value)); detail.setForeground(color);
            row.add(title,BorderLayout.NORTH); row.add(detail,BorderLayout.SOUTH); return row;
        });
        add(new JScrollPane(list),BorderLayout.CENTER);
        JButton confirm=new JButton("选择此对话");
        Runnable accept=() -> { if(list.getSelectedValue()!=null) choose.accept(list.getSelectedValue()); };
        confirm.addActionListener(e -> accept.run());
        list.addListSelectionListener(e -> confirm.setEnabled(list.getSelectedValue()!=null));
        list.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
            int index=list.locationToIndex(e.getPoint());
            if(e.getClickCount()==2 && SwingUtilities.isLeftMouseButton(e) && index>=0 && list.getCellBounds(index,index).contains(e.getPoint())) accept.run();
        }});
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"choose");
        list.getActionMap().put("choose",new AbstractAction() { public void actionPerformed(ActionEvent e) { accept.run(); } });
        search.addActionListener(e -> accept.run());
        JPanel footer=new JPanel(new BorderLayout()); footer.add(count,BorderLayout.WEST); footer.add(confirm,BorderLayout.EAST); add(footer,BorderLayout.SOUTH);
        providers.addActionListener(e -> filter());
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); } public void removeUpdate(DocumentEvent e) { filter(); } public void changedUpdate(DocumentEvent e) { filter(); }
        });
        filter(); confirm.setEnabled(list.getSelectedValue()!=null);
    }
    public void focusSearch() { search.requestFocusInWindow(); }
    private void filter() {
        String query=search.getText().strip().toLowerCase(Locale.ROOT);
        String provider=(String)providers.getSelectedItem();
        model.clear();
        for(Conversation c:conversations) if((providers.getSelectedIndex()==0 || c.provider().equals(provider))
            && (c.provider()+" "+c.title()+" "+c.source()).toLowerCase(Locale.ROOT).contains(query)) model.addElement(c);
        if(current!=null && model.contains(current)) list.setSelectedValue(current,true);
        else if(!model.isEmpty()) list.setSelectedIndex(0);
        count.setText(model.isEmpty()?"没有匹配的会话":model.size()+" 条会话");
    }
}
