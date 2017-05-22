package checklist;

import java.util.LinkedHashMap;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;

import mylogger.MyLogger;





/**
 * Implementation of the main CheckList GUI frame.
 * Main class is also a handler for menu bar items (MenuBarListener) so that
 * its actionPerformed() method is called when a menu item is clicked.
 * 
 * @author Zdenek Maxa
 *
 */
@SuppressWarnings("serial")
public final class CheckListGUI extends JFrame implements ActionListener
{
	private static MyLogger logger = MyLogger.getLogger(CheckListGUI.class);
	
	
	
	// reference to the GUI data table (to be able to change it on-the-fly)
	private DataTable table = null;
	
	// data store reference
	private LinkedHashMap<String, CheckListData> store = null;
	
	// title (name as defined in the XML as title attribute) of the active
	// checklist
	private String activeCheckListTitle = "<empty>";
	
	// file name of the required checklist from RunCom. this field is not
	// used at all when running in stand-alone mode
	private String activeCheckListFileName = null;
	
	private String deskNameInRunCom = null;
	
	private boolean isStandAlone = false;
	

	
	
	
	private CheckListGUI(boolean standAlone,
			             LinkedHashMap<String, CheckListData> store,
			             String deskNameInRunCom, String checkListFile)
	{
		super();
		
		this.store = store;
		this.deskNameInRunCom = deskNameInRunCom;
		this.isStandAlone = standAlone;
		this.activeCheckListFileName = checkListFile;
		
		// will always shut down JVM
		// this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.addWindowListener(new GUIWindowAdapter(this));
		
        // stand-alone application has menubar listing all checklists
		this.setJMenuBar(new MenuBar(this.store, this));

		// if there is only one checklist in the store, take its
		// title and fill in activeCheckListName variable. one checklist
		// in the store is when CheckList was run with -s <checklist> or
		// when invoked from RunCom to show a specific checklist, otherwise
		// active checklist is yet to be selected by the user
		if(store.keySet().size() == 1)
		{
		    String t = store.keySet().iterator().next();
		    logger.info("Only one checklist present in the store, title: \"" +
		                t + "\"");
		    this.activeCheckListTitle = t;
		}
		
		createGUI();
        
	} // CheckListGUI() -----------------------------------------------------

	
	
	private void createGUI()
	{

		this.setTitle(this.activeCheckListTitle);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		try
		{
			CheckListTableModel model = new CheckListTableModel(this.store,
			                                  this.activeCheckListTitle);
			this.table = new DataTable(model);
	        JScrollPane scrollPane = new JScrollPane(this.table);
	        panel.add(scrollPane);
		}
		catch(CheckListException cle)
		{
			logger.debug(cle.getMessage(), cle);
			logger.error(cle.getMessage());
			JOptionPane.showMessageDialog(this, cle.getMessage(), "CheckList",
					                      JOptionPane.ERROR_MESSAGE);
		}
		

		// create buttons OK and Close
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		JButton okButton = new JButton("OK");
		okButton.setActionCommand("OK");
		okButton.addActionListener(new ButtonListener(this.table,
		                                              this.store, this));
		
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("Close");
		closeButton.addActionListener(new ButtonListener(this.table,
		                                                 this.store, this));

		buttonPanel.add(okButton);
		buttonPanel.add(closeButton);

		panel.add(buttonPanel);
		
        
		panel.setOpaque(true);
        this.setContentPane(panel);
        
        // display the window centered on the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = panel.getPreferredSize();
        this.setLocation(screenSize.width / 2 - (labelSize.width / 2),
        		         screenSize.height / 2 - (labelSize.height / 2));
        
        this.pack();
        this.setVisible(true);
        this.setAlwaysOnTop(true);
        this.toFront();
		
	} // createGUI() --------------------------------------------------------
	
	
	
	/**
	 * Gets called when a menu item is selected.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		String choice = e.getActionCommand();
		
		logger.debug("MenuBar chosen item: \"" + choice + "\"");
		
		// first single item with special treatment
		if("debug".equals(choice))
		{
		    logger.openDebuggingWindow();
		    return;
		}

		// show requested checklist
		this.activeCheckListTitle = choice;
		this.changeDisplayedCheckList(this.activeCheckListTitle);
		
	} // actionPerformed() --------------------------------------------------
	
	 
	
	/**
	 * Call from outside to create GUI instance (always a new one ...)
	 * @param standAlone
	 * @param store
	 * @throws CheckListException
	 */
	protected static void createAndShowGUI(final boolean standAlone,
	                   final LinkedHashMap<String, CheckListData> store,
	                   final String deskNameInRunCom,
	                   final String checkListFile)
	                   throws CheckListException
	{
		
        logger.warn("GUI window initialisation ...");
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                logger.debug("Calling GUI constructor from " +
                             "event-dispatching thread now ...");
                new CheckListGUI(standAlone, store, deskNameInRunCom,
                                checkListFile);
                logger.debug("GUI window created, end of event-dispatching thread.");
            }
        });
	    
	} // createAndShowGUI() -------------------------------------------------

	
	
	protected void closeCheckList()
	{
	    logger.debug("Disposing the CheckList GUI window.");
	    this.dispose();
    	this.store = null;
    	
		if(isStandAlone)
		{
			logger.warn("Closing the CheckList stand-alone application.");
			System.exit(0);
		}
		else
		{
			// not stand-alone, started from RunCom
			logger.warn("Window of the non-stand-alone CheckList disposed.");
		}	    
	    
	} // closeCheckList() ---------------------------------------------------
	
	

	/**
	 * Called when another checklist is required to be displayed in the table
	 * @param checkListNameToShow
	 */
	private void changeDisplayedCheckList(String checkListNameToShow)
	{
		try
		{
		    CheckListTableModel model = (CheckListTableModel) table.getModel();
		    model.updateTableContents(checkListNameToShow);
		    this.setTitle(checkListNameToShow);
		}
		catch(CheckListException cle)
		{
			logger.debug(cle.getMessage(), cle);
			logger.error(cle.getMessage());
			JOptionPane.showMessageDialog(this, cle.getMessage(), "CheckList",
					                      JOptionPane.ERROR_MESSAGE);
		}
		
	} // changeDisplayedCheckList() -----------------------------------------


	
	protected boolean isStandAlone()
	{
		return this.isStandAlone;
		
	} // isStandAlone() -----------------------------------------------------
	
	
	
	protected String getDeskNameInRunCom()
	{
	    return this.deskNameInRunCom;
	    
	} // getDeskNameInRunCom() ----------------------------------------------
	
	
	
	protected String getActiveCheckListTitle()
	{
		return this.activeCheckListTitle;
		
	} // getActiveCheckListTitle() ------------------------------------------
	
	
	
	protected String getActiveCheckListFileName()
	{ 
	    String r = this.activeCheckListFileName;  
	    return r != null ? r : "<not set>";
	    
	} // getActiveCheckListFileName() ---------------------------------------
	
	
	
} // class CheckListGUI =====================================================



/**
 * Class MenuBar - menu bar for a stand alone CheckList application.
 * There three menus offering checklists according to category:
 * 1) desk checklists 2) shifter checklists and 3) other checklists
 * desk and shifter checklists are distinguished by
 * "signin-" or "DQcheck-" checklists title prefix.
 * @return
 */
@SuppressWarnings("serial")
class MenuBar extends JMenuBar
{
	
	// reference to the GUI window which acts as listener for menu items (its
	// actionPerformed() method is called when a menu item is selected)
	private CheckListGUI menuBarListener = null;
	
	

	public MenuBar(LinkedHashMap<String, CheckListData> store,
			       CheckListGUI menuBarListener)
	{
		this.menuBarListener = menuBarListener;
		
		// stand alone application has all checklists choices in menubar 
		if(this.menuBarListener.isStandAlone())
		{
		    createCheckListMenuBar(store);
		}
		
        // add debug menu bar item, regardless whether running stand-along or not
		JMenuItem menuItem = null;
		JMenu menuDebug = new JMenu("debug");
        menuItem = new JMenuItem("show debug window");
        menuItem.setActionCommand("debug");
        menuItem.addActionListener(this.menuBarListener);
        menuDebug.add(menuItem);
                
		this.add(menuDebug);
				
	} // MenuBar() ----------------------------------------------------------
	
	
	
	private void createCheckListMenuBar(LinkedHashMap<String, CheckListData> store)
	{
	    
	    JMenuItem menuItem = null;
	    
        JMenu menusignIn = new JMenu("signin-checklists");
        JMenu menuDQCheck = new JMenu("DQcheck-checklists");
        JMenu menuInjection = new JMenu("injection-checklists");
        JMenu menuStableBeam = new JMenu("stablebeam-checklists");
        JMenu menuStartRun = new JMenu("startrun-checklists");
        
        JMenu menuOther = new JMenu("other checklists");
        
        
        // here the insertion order into store matters (if menu items
        // are to appear in the same order as defined in the main checklist)
        for(String key : store.keySet())
        {
            // key - title of the checkList, key into HashMap store
            menuItem = new JMenuItem(key);
            menuItem.setActionCommand(key);
            menuItem.addActionListener(this.menuBarListener);
            
            // sorting to three subsections of checklists according to checklist
            // title prefix
            if(key.startsWith("signin-"))
            {
                menusignIn.add(menuItem);
            }
            else if(key.startsWith("DQcheck-"))
            {
                menuDQCheck.add(menuItem);
            }            
            else if(key.startsWith("injection-"))
            {
                menuInjection.add(menuItem);
            }
            else if(key.startsWith("stablebeam-"))
            {
                menuStableBeam.add(menuItem);
            }
            else if(key.startsWith("startrun-"))
            {
                menuStartRun.add(menuItem);
            }
            else
            {
                menuOther.add(menuItem);
            }
        } // for - iterating over all checklists

        this.add(menusignIn);
        this.add(menuDQCheck);
        this.add(menuInjection);
        this.add(menuStableBeam);
        this.add(menuStartRun);
        
        this.add(menuOther);

	} // createCheckListMenuBar() -------------------------------------------
	
} // class MenuBar ==========================================================



/**
 * Easier than WindowListener - doesn't have to implement a number of
 * methods out of which majority would remain empty and CheckListGUI already
 * extends from JFrame ...
 */
class GUIWindowAdapter extends WindowAdapter
{
	private CheckListGUI gui = null;
		

	public GUIWindowAdapter(CheckListGUI gui)
	{
		this.gui = gui;
		
	} // GUIWindowAdapter() -------------------------------------------------

	
	
    public void windowClosing(WindowEvent we)
    {
    	gui.closeCheckList();
    	    		
    } // windowClosing() ----------------------------------------------------
    
} // GUIWindowAdapter =======================================================



class ButtonListener implements ActionListener
{
	private static MyLogger logger = MyLogger.getLogger(ButtonListener.class);
	
	// operating systems dependent line separator
	private static final String LS = System.getProperty("line.separator");
	
	// reference to the data table
	private DataTable table = null;
	
	// data store reference
	private LinkedHashMap<String, CheckListData> store = null;
	
	// reference to the GUI
	private CheckListGUI gui = null;
	
	
	
	
	public ButtonListener(DataTable table,
			              LinkedHashMap<String, CheckListData> store,
			              CheckListGUI gui)
	{
		super();
		this.table = table;
		this.store = store;
		this.gui = gui;
		
	} // ButtonListener() ---------------------------------------------------
	
	

	private void handleOKButtonPressed()
	{
		
		logger.debug("Active CheckList title: \"" +
		             gui.getActiveCheckListTitle() + "\" file name: \"" +
		             gui.getActiveCheckListFileName() + "\"");
		
		// too paranoid ...
		if(! store.containsKey(gui.getActiveCheckListTitle()))
		{
			logger.error("Requested checklist \"" +
			              gui.getActiveCheckListTitle() + "\" " +
					     "does not exist, no action performed.");
			return;
		}
		
		CheckListData currentData = store.get(gui.getActiveCheckListTitle());
		
		if(currentData.size() > 0)
		{
		    processCompletedCheckList(currentData);
		}
		else
		{
			// there are no instructions and no checkboxes
			String m = "Empty checklist \"" + currentData.getTitle() +
			           "\", nothing to tick.";
			logger.warn(m);
			JOptionPane.showMessageDialog(gui, m,
					                      "CheckList warning",
					                      JOptionPane.WARNING_MESSAGE);    
		}
		
	} // handleOKButtonPressed() --------------------------------------------
	
	
	
	private void processCompletedCheckList(CheckListData currentData)
	{
	    // getCellEditor() returns active editor and notifies it that editing
	    // finished (in fact haven't finished but user pressed OK button -
	    // submitted the whole checklist)
	    // fireEditingStopped() method causes models setValueAt() method
	    // called are updates the internal table data accordingly
	    // however, there may not be an active editor if clicking a 
	    // checkbox was the last operation (then getCellEditor() returns null) 
	    CellEditor cellEditor = (CellEditor) table.getCellEditor();
        logger.debug("Updating lastly edited cell (comment cell which " +
                     "has still active cell editor)");	    
	    if(cellEditor != null)
	    {
	        cellEditor.fireEditingStopped();
	        logger.debug("Lastly edited cell updated.");
	    }
	    else
	    {
	        logger.debug("Active editor not available, no update performed, " +
	                     "lastly edited cell should be already updated.");
	    }
	    	    
        // first confirmation dialog
        int first = JOptionPane.showConfirmDialog(this.gui,
                    "Are you sure to proceed?", "CheckList confirmation",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if(first == JOptionPane.CANCEL_OPTION)
        {
            return; // Cancel was pressed
        }
        
        // first confirmation dialog confirmed - OK was pressed 
        
        // construct message for ATLOG ELog: loop over all instructions
        // and get checklist checkbox status and comments
        StringBuffer msg = new StringBuffer(LS); // final message for elog
        int untickedCheckBoxCounter = 0;
        for(int i = 0; i < currentData.size(); i++)
        {
            // loop over all instructions
            Boolean checkBoxValue = (Boolean) table.getValueAt(i, 1);
            if(checkBoxValue.booleanValue())
            {
                // this checklist instruction check box is ticked
                msg.append("OK: " + LS +
                           table.getValueAt(i, 0).toString() +
                           LS + "COMMENT: " +
                           table.getValueAt(i, 2).toString());
            }
            else
            {
                // this checklist instruction check box is not ticked
                msg.append("FAILED: " + LS +
                           table.getValueAt(i, 0).toString() +
                           LS + "COMMENT: "  +
                           table.getValueAt(i, 2).toString());
                untickedCheckBoxCounter++;
            }
            msg.append(LS + LS);
        } // for loop all instructions
        
        
        if(untickedCheckBoxCounter == 0)
        {
            // all checkboxes were ticked, all instructions OK
             
            logger.debug("CheckList was completed ...");
             
            // if CheckList was called from RunCom, RunCom will change
            // its state, RunCom needs to know the checklist file to 
            // distinguish which checklist type was completed here in
            // the CheckList application
            // do not call RunCom if CheckList run as a stand-alone
            // application
            if(! gui.isStandAlone())
            {
                logger.info("CheckList was invoked from RunCom, " +
                            "calling RunCom back ...");
                RunComCaller.call(gui.getActiveCheckListFileName(),
                                  gui.getDeskNameInRunCom(), gui);
                // old CheckList did ...
                // leave here for reference, very instructive piece of code...                      
                // button.app.ButtonPanelApp.newContentPane.comboList[Table.getTableChoice()].setSelectedIndex(Table.getTableChoice(), "Ready");
            }
            else
            {
                logger.info("CheckList runs as a stand-alone application ...");
            }
                                 
            // confirmation dialog - insert log into ATLOG
            String m = "Insert CheckList output into ATLOG?\n" +
                       "(no option doesn't cancel CheckList)";
            int atlog = JOptionPane.showConfirmDialog(gui, m,
                        "CheckList ATLOG confirm",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
            if(atlog == JOptionPane.YES_OPTION)
            {
                // insert log into ATLOG
                CheckList.submitIntoATLog(msg.toString(),
                                          gui.getActiveCheckListTitle(),
                                          currentData.getElogAffectedSystem());
            }
             
            // finally, closing the application / window
            gui.closeCheckList();
             
        } // if(untickedCheckBoxCounter == 0)
        else
        {
            // some of checkboxes were not ticked
            
            String m = "Checklist is not completed.\n" +
                       "No state change.\n" +
                       "Still insert output into ATLOG?\n" +
                       "(no option doesn't cancel CheckList)";
            int incomplete = JOptionPane.showConfirmDialog(gui, m,
                             "CheckList ATLOG confirm",
                             JOptionPane.YES_NO_OPTION,
                             JOptionPane.QUESTION_MESSAGE);
            if(incomplete == JOptionPane.YES_OPTION)
            {
                // insert log into ATLOG
                CheckList.submitIntoATLog(msg.toString(),
                                          gui.getActiveCheckListTitle(),
                                          currentData.getElogAffectedSystem());
            }
             
            // finally, closing the application / window
            gui.closeCheckList();    
        }
             
	} // processCompletedCheckList() ----------------------------------------
	
	
	
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		
		if("OK".equals(command))
		{
			logger.debug("Button pressed, its command: " + command);	
			handleOKButtonPressed();
		}
		if("Close".equals(command))
		{
			logger.debug("Button pressed, its command: " + command);
			
			// finally, closing the application / window
			gui.closeCheckList();
		}
		
	} // actionPerformed() --------------------------------------------------
	
} // class ButtonListener ===================================================