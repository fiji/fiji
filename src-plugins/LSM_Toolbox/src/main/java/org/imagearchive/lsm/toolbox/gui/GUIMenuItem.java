package org.imagearchive.lsm.toolbox.gui;

import java.awt.Font;
import java.awt.SystemColor;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;

public class GUIMenuItem extends JMenuItem {

    public GUIMenuItem(String itemText, String tooltipText) {
        Font font = new Font(null);
        float fontsize = 11;
        font = font.deriveFont(fontsize);
        font = font.deriveFont(Font.BOLD);
        this.setFont(font);
        this.setText(itemText);
        this.setForeground(SystemColor.windowText);
        this.setToolTipText(tooltipText);
    }
    public GUIMenuItem(String buttonText, String imageResource,String tooltipText) {
        Font font = new Font(null);
        float fontsize = 11;
        font = font.deriveFont(fontsize);
        font = font.deriveFont(Font.BOLD);
        this.setIcon(new ImageIcon(getClass().getResource(imageResource)));
        this.setFont(font);
        this.setHorizontalAlignment(JButton.LEFT);
        this.setText(buttonText);
        this.setForeground(SystemColor.windowText);
        this.setToolTipText(tooltipText);
    }
}