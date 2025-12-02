package project_starter.view;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JButton;

/**
 * Bottone con immagine di sfondo personalizzata.
 */
class IconButton extends JButton {
    private final Image background;

    public IconButton(Image img) {
        this.background = img;
        setContentAreaFilled(false);
        setBorderPainted(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

