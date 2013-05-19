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

    // an optional text message to display at the top
    protected String label;

    /**
     * Creates a new BorderPanel without label and without
     * layout manager.
     */
    public BorderPanel() {
        this("", null);
    }

    /**
     * Creates a new BorderPanel with the given label and
     * layout manager.
     */
    public BorderPanel(String label, LayoutManager layoutManager) {
        super(layoutManager);
        this.label = label;
    }

    /**
     * Return the insets object of the border panel.
     */
    public Insets getInsets() {
        return insets;
    }

    /**
     *  The paint method that draws the border.
     */
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
