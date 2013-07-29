package jist.swans.gui;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


import driver.Visualizer.FieldPanel;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.ListIterator;

import jist.swans.field.streets.RoadSegment;

/*
 * ListDialog.java is a 1.4 class meant to be used by programs such as
 * ListDialogRunner.  It requires no additional files.
 */

/**
 * Use this modal dialog to let the user choose one string from a long
 * list.  See ListDialogRunner.java for an example of using ListDialog.
 * The basics:
 * <pre>
    String[] choices = {"A", "long", "array", "of", "strings"};
    String selectedName = ListDialog.showDialog(
                                componentInControllingFrame,
                                locatorComponent,
                                "A description of the list:",
                                "Dialog Title",
                                choices,
                                choices[0]);
 * </pre>
 */
public class ListDialog extends JDialog
                        implements ActionListener {
    private static ListDialog dialog;
    public LinkedList value = new LinkedList();
    private JList list;
    private DefaultListModel listModel = new DefaultListModel();
	public FieldPanel fieldPanel;
	private JScrollPane mapPane;

    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     */
//    public static LinkedList showDialog(Component frameComp,
//                                    Component locationComp,
//                                    String labelText,
//                                    String title,
//                                    String[] possibleValues,
//                                    Object initialValue,
//                                    String longValue) {
//        Frame frame = JOptionPane.getFrameForComponent(frameComp);
//        dialog = new ListDialog(frame,
//                                locationComp,
//                                labelText,
//                                title,
//                                possibleValues,
//                                
//                                longValue);
//        dialog.setVisible(true);
//        return value;
//    }

//    private void setValue(String newValue) {
//        value = newValue;
//        list.setSelectedValue(value, true);
//    }

    public ListDialog(Frame frame,
                       Component locationComp,
                       String labelText,
                       String title,
                       Object[] data,                       
                       String longValue, 
                       JScrollPane map) {
        super(frame, title, true);

        init(locationComp, labelText, longValue, map);
    }

    /**
     * @param locationComp
     * @param labelText
     * @param longValue
     * @param map
     */
    private void init(Component locationComp, String labelText, String longValue, JScrollPane map) {
        this.mapPane = map;
        //Create and initialize the buttons.
        JButton cancelButton = new JButton("Exit");
        cancelButton.addActionListener(this);
        //
        final JButton setButton = new JButton("Add route");
        setButton.setActionCommand("Set");
        setButton.addActionListener(this);
        getRootPane().setDefaultButton(setButton);

        //main part of the dialog
        list = new JList(listModel) {
            //Subclass JList to workaround bug 4832765, which can cause the
            //scroll pane to not let the user easily scroll up to the beginning
            //of the list.  An alternative would be to set the unitIncrement
            //of the JScrollBar to a fixed value. You wouldn't get the nice
            //aligned scrolling, but it should work.
            public int getScrollableUnitIncrement(Rectangle visibleRect,
                                                  int orientation,
                                                  int direction) {
                int row;
                if (orientation == SwingConstants.VERTICAL &&
                      direction < 0 && (row = getFirstVisibleIndex()) != -1) {
                    Rectangle r = getCellBounds(row, row);
                    if ((r.y == visibleRect.y) && (row != 0))  {
                        Point loc = r.getLocation();
                        loc.y--;
                        int prevIndex = locationToIndex(loc);
                        Rectangle prevR = getCellBounds(prevIndex, prevIndex);

                        if (prevR == null || prevR.y >= r.y) {
                            return 0;
                        }
                        return prevR.height;
                    }
                }
                return super.getScrollableUnitIncrement(
                                visibleRect, orientation, direction);
            }
        };

        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        if (longValue != null) {
            list.setPrototypeCellValue("1                              2"); //get extra space
        }
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setButton.doClick(); //emulate button click
                }
            }
        });
        
        list.addListSelectionListener(new ListSelectionListener(){

            public void valueChanged(ListSelectionEvent e) {
                updateMap(list.getSelectedValue());
                
            }
            
        });
        
        list.addKeyListener(new KeyListener(){

            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar()=='d'){
                    JList l = (JList)e.getComponent();
                    RoadSegment rs = (RoadSegment)l.getSelectedValue();
                    l.removeSelectionInterval(l.getSelectedIndex(), l.getSelectedIndex());
                    removeStreet(rs);
                }
                
            }

            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }

            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }
            
        });
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 80));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        //Create a container so that we can add a title around
        //the scroll pane.  Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to bottom.
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        JLabel label = new JLabel(labelText);
        label.setLabelFor(list);
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0,5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(setButton);

        //Put everything together, using the content pane's BorderLayout.
        Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.PAGE_START);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);
        contentPane.add(map, BorderLayout.CENTER);
        //Initialize values.
//        setValue(initialValue);
        pack();
        setLocationRelativeTo(locationComp);
    }

    /**
     * @param f
     * @param generalEditorPane
     * @param string
     * @param string2
     * @param s
     * @param string3
     * @param jsp
     * @param routeToEdit
     * @param fp 
     */
    public ListDialog(Frame frame,
            Component locationComp,
            String labelText,
            String title,
            Object[] data,                       
            String longValue, 
            JScrollPane map, 
            LinkedList routeToEdit, FieldPanel fp){
        super(frame, title, true);

        init(locationComp, labelText, longValue, map);
        
        this.value = routeToEdit;
        
        ListIterator li = routeToEdit.listIterator();
        while (li.hasNext()){
            listModel.addElement((RoadSegment)li.next());
        }
        this.fieldPanel = fp;
//        updateMap();
        
    }

    //Handle clicks on the Set and Cancel buttons.
    public void actionPerformed(ActionEvent e) {
        if ("Set".equals(e.getActionCommand())) {
//            ListDialog.value = (String)(list.getSelectedValue());
        }
        setVisible(false);
    }
    
    public void addStreet(RoadSegment rs)
    {    	
    	if (rs!=null){
    	value.add(rs);
    	listModel.addElement(rs);
    	}
    	
    	updateMap();
    }

    /**
     * 
     */
    private void updateMap() {
        updateMap(null);
    }
    
    private void updateMap(Object o){
        ListIterator li = value.listIterator();
        RoadSegment roads[] = new RoadSegment[value.size()];
        Color cols[] = new Color[value.size()];
        int i =0;
        while (li.hasNext()){
            RoadSegment rsTemp = (RoadSegment)li.next();
            roads[i] = rsTemp;
            if (o!=null && rsTemp.equals(o)) cols[i] = Color.RED;
            else cols[i] = Color.GREEN;
            i++;            
        }
        // set colors
        fieldPanel.setSegmentsColor(roads, cols); 
        mapPane.repaint();
        
    }

    public boolean contains(RoadSegment rs) {
        return value.contains(rs);
    }
    
    public void removeStreet(RoadSegment rs){
        if (rs!=null){
            value.remove(rs);
            listModel.removeElement(rs);
            
        }
        updateMap();
    }

    /**
     * @return
     */
    public RoadSegment getLastStreet() {
        if (listModel.size()==0) return null;
        return (RoadSegment)listModel.getElementAt(listModel.size()-1);
    }
}
