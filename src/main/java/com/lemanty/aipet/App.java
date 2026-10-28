package com.lemanty.aipet;

import com.lemanty.aipet.service.*;
import com.lemanty.aipet.ui.*;
import javax.swing.*;

public final class App {
    private App() { }
    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "AiPet");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            DemoAdapter adapter = new DemoAdapter();
            TaskManager tasks = new TaskManager(adapter);
            Runtime.getRuntime().addShutdownHook(new Thread(tasks::close));
            new MainWindow(adapter, tasks).setVisible(true);
        });
    }
}
