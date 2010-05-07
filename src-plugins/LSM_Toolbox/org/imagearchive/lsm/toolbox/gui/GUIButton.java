package org.imagearchive.lsm.toolbox.gui;

import java.awt.Font;
import java.awt.SystemColor;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public class GUIButton extends JButton {

    public GUIButton(String buttonText, String tooltipText) {
        Font font = new Font(null);
        float fontsize = 11;
        font = font.deriveFont(fontsize);
        font = font.deriveFont(Font.BOLD);
        this.setFont(font);
        this.setText(buttonText);
        this.setForeground(SystemColor.windowText);
        this.setToolTipText(tooltipText);
    }

    public GUIButton(String buttonText, String imageResource,String tooltipText) {
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