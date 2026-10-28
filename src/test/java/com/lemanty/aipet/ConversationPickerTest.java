package com.lemanty.aipet;

import com.lemanty.aipet.model.Conversation;
import com.lemanty.aipet.ui.ConversationPicker;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ConversationPickerTest {
    private static Component named(Container parent,String name) {
        for(Component c:parent.getComponents()) {
            if(name.equals(c.getName())) return c;
            if(c instanceof Container child) { Component found=named(child,name); if(found!=null) return found; }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var a=new Conversation("a","豆包","设计","浏览器");
            var b=new Conversation("b","Claude","Code","客户端");
            var chosen=new AtomicReference<Conversation>();
            var panel=new ConversationPicker(List.of(a,b),a,c -> "尚未发送",chosen::set);
            var search=(JTextField)named(panel,"picker-search");
            var provider=(JComboBox<?>)named(panel,"picker-provider");
            var list=(JList<?>)named(panel,"picker-results");
            if(list.getModel().getSize()!=2 || !a.equals(list.getSelectedValue())) throw new AssertionError("initial selection");
            provider.setSelectedItem("Claude");
            if(list.getModel().getSize()!=1 || chosen.get()!=null) throw new AssertionError("filter changed destination");
            search.setText("missing"); search.postActionEvent();
            if(list.getModel().getSize()!=0 || chosen.get()!=null) throw new AssertionError("empty search chose a target");
            search.setText("CODE"); search.postActionEvent();
            if(!b.equals(chosen.get())) throw new AssertionError("case insensitive search / explicit choice");
            search.setText(""); provider.setSelectedIndex(0);
            if(list.getModel().getSize()!=2) throw new AssertionError("reset filter");
        });
        System.out.println("PASS: provider filtering, search, empty results, explicit destination selection");
    }
}
