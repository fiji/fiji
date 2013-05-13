package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;

import com.itextpdf.text.Font;

/**
 * A JcomboBox that displays categories, and return the category the selected item belong to. 
 * @author Jean-Yves Tinevez, adapted from http://java-swing-tips.blogspot.fr/2010/03/non-selectable-jcombobox-items.html
 *
 * @param <K> the type of the category objects
 * @param <V> the type of the items 
 */
public class CategoryJComboBox<K, V> extends JComboBox {

	private static final long serialVersionUID = 1L;
	protected static final String INDENT = "  ";
	/** Indices of items that should be displayed as a category name.  */
	private final HashSet<Integer> categoryIndexSet = new HashSet<Integer>();
	private boolean isCategoryIndex = false;
	private final Map<V, String> itemNames;
	private final HashMap<V, K> invertMap;
	private final Map<K, String> categoryNames;


	/*
	 * CONSTRUCTOR
	 */

	public CategoryJComboBox(Map<K, List<V>> items, Map<V, String> itemNames, Map<K, String> categoryNames) {
		super();
		this.invertMap = new HashMap<V, K>();
		this.itemNames = itemNames;
		this.categoryNames = categoryNames;
		init();
		
		// Feed the combo box
		for (K category : items.keySet()) {
			addItem(category, true);

			List<V> categoryItems = items.get(category);
			for (V item : categoryItems) {
				addItem(item, false);
				invertMap.put(item, category);
			}
		}
		
		setSelectedIndex(1);
	}



	/*
	 * METHODS
	 */
	
	public K getSelectedCategory() {
		Object obj = getSelectedItem();
		return invertMap.get(obj);
	}

	public void setDisableIndex(HashSet < Integer > set) {
		categoryIndexSet.clear();
		for(Integer i:set) {
			categoryIndexSet.add(i);
		}
	}

	@Override
	public void setPopupVisible(boolean v) {
		if(!v && isCategoryIndex) {
			isCategoryIndex = false;
		}else{
			super.setPopupVisible(v);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public V getSelectedItem() {
		return (V) super.getSelectedItem();
	}

	@Override
	public void setSelectedIndex(int index) {
		if(categoryIndexSet.contains(index)) {
			isCategoryIndex = true;
		}else{
			//isDisableIndex = false;
			super.setSelectedIndex(index);
		}
	}


	/*
	 * PRIVATE METHODS
	 */

	private void addItem(Object anObject, boolean isCategoryName) {
		super.addItem(anObject);
		if (isCategoryName) {
			categoryIndexSet.add(getItemCount() - 1);
		}
	}


	/**
	 * Called at instantiation: prepare the {@link JComboBox} with correct listeners
	 * and logic for categories.
	 */
	private void init() {
		setFont(SMALL_FONT);
		final ListCellRenderer r = getRenderer();
		setRenderer(new ListCellRenderer() {
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel c;
				if (categoryIndexSet.contains(index)) {
					c = (JLabel) r.getListCellRendererComponent(list,value,index,false,false);
					c.setEnabled(false);
					c.setFont(SMALL_FONT.deriveFont(Font.BOLD));
					c.setText(categoryNames.get(value));
				} else {
					c = (JLabel) r.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
					c.setEnabled(true);
					c.setFont(SMALL_FONT);
					c.setText(INDENT + itemNames.get(value));
				}
				return c;
			}
		});

		Action up = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				int si = getSelectedIndex();
				for(int i = si-1;i >= 0;i--) {
					if(!categoryIndexSet.contains(i)) {
						setSelectedIndex(i);
						break;
					}
				}
			}
		};
		Action down = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				int si = getSelectedIndex();
				for(int i = si+1;i < getModel().getSize();i++) {
					if(!categoryIndexSet.contains(i)) {
						setSelectedIndex(i);
						break;
					}
				}
			}
		};

		ActionMap am = getActionMap();
		am.put("selectPrevious", up);
		am.put("selectNext", down);
		InputMap im = getInputMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),      "selectPrevious");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0),   "selectPrevious");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),    "selectNext");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), "selectNext");
	}
	
	
	/**
	 * Demo
	 */
	public static void main(String[] args) {
		//
		List<String> fruits = new ArrayList<String>(5);
		fruits.add("Apple");
		fruits.add("Pear");
		fruits.add("Orange");
		fruits.add("Strawberry");
		//
		List<String> cars = new ArrayList<String>(3);
		cars.add("Peugeot");
		cars.add("Ferrari");
		cars.add("Ford");
		//
		List<String> computers = new ArrayList<String>(2);
		computers.add("PC");
		computers.add("Mac");
		//
		LinkedHashMap<String, List<String>> items = new LinkedHashMap<String, List<String>>(3);
		items.put("Fruits", fruits);
		items.put("Cars", cars);
		items.put("Computers", computers);
		//
		Map<String, String> itemNames = new HashMap<String, String>();
		for (String key : items.keySet()) {
			for (String string : items.get(key)) {
				itemNames.put(string, string);
			}
		}
		// 
		Map<String, String> categoryNames = new HashMap<String, String>();
		for (String key : items.keySet()) {
			categoryNames.put(key, key);
		}
		// Ouf!
		
		final CategoryJComboBox<String, String> cb = new CategoryJComboBox<String, String>(items, itemNames, categoryNames);
		cb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Selected " + cb.getSelectedItem() + " in category " + cb.getSelectedCategory());
			}
		});
		
		JFrame frame = new JFrame();
		frame.getContentPane().add(cb);
		frame.setVisible(true);
		
	}
}