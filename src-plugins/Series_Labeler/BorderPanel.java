import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Panel;

/**
 * A panel with a border and an optional title.
 */
public class BorderPanel extends Panel {
    /* The insets of the border panel. This space must be
     * respected with from the layout manager.
     */
    protected static final Insets insets = new Insets(10,10,10,10);

    // an optional tex message to display at the top
    protected String label;

    public BorderPanel() {
        this("", null);
    }

    public BorderPanel(String label, LayoutManager layoutManager) {
        super(layoutManager);
        this.label = label;
    }

    // return the insets object of the border panel
    public Insets getInsets() {
        return insets;
    }

    // the paint mathod that draws the border
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(getForeground());
        g.drawRect(5, 5, getWidth() - 10, getHeight() - 10);
        if (!label.equals("")) {
            g.setColor(getBackground());
            int width = g.getFontMetrics().stringWidth(label);
            g.fillRect(10, 0, width, 10);
            g.setColor(getForeground());
            g.drawString(label, 10, 10);
        }
    }
}
