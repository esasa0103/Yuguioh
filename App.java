// src/main/java/com/yugi/App.java
package com.yugi;

import javax.swing.SwingUtilities;
import com.yugi.ui.MainWindow;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow w = new MainWindow();
            w.setVisible(true);
        });
    }
}
