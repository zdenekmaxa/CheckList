package checklist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import mylogger.MyLogger;




/**
 * Table data model. Manipulates contents of the table.
 * 
 * @author Zdenek Maxa
 * 
 */
@SuppressWarnings("serial")
public final class CheckListTableModel extends AbstractTableModel
{
    private static MyLogger logger = MyLogger.getLogger(CheckListTableModel.class);    
    
    // reference to the main CheckList data store containing
    // contents of all checklists
    private LinkedHashMap<String, CheckListData> store = null;
    
    private String activeCheckList = null; 
    
    // definition of table header titles
    private static String[] header = {"CheckList instructions", "Check",
                                      "Comments", "Help" };
    
    private static HashSet<Integer> editableColumnIndices = new HashSet<Integer>();
    static
    {
        editableColumnIndices.add(1); // checkbox column
        editableColumnIndices.add(2); // comment column
        editableColumnIndices.add(3); // help column
    }
    

    // all table data currently displayed in the table
    private ArrayList<CheckListRow> allRows = null;
    
    
    
    
    /** 
     * @param dataStore
     * @param currentSelection - String title of the checklist to show
     */
    public CheckListTableModel(LinkedHashMap<String, CheckListData> dataStore,
                        String checkListToShow) throws CheckListException
    {
        this.store = dataStore;
        this.activeCheckList = checkListToShow;
        this.allRows = prepareTableData();
                
    } // TableModel() -------------------------------------------------------
    
    

    /**
     * Method called from GUI when user requires to display different
     * CheckList
     * 
     * @param checkListToShow
     * @throws CheckListException
     */
    public void updateTableContents(String checkListToShow) 
    throws CheckListException
    {
        this.activeCheckList = checkListToShow;
        
        final ArrayList<CheckListRow> newTableData = prepareTableData();
        
        // table content changing via event-dispatching thread, thread safely
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                CheckListTableModel.this.allRows = newTableData;
                // now the GUI table updates
                CheckListTableModel.this.fireTableDataChanged();                
            }
        });
    
    } // updateTableContents() ----------------------------------------------
    
    
    
    /**
     * Convert CheckList content from internal store into
     * ArrayList<CheckListRow> required by the table data model.
     * Relies that this.activeCheckList was appropriately set beforehand.
     * 
     */
    private ArrayList<CheckListRow> prepareTableData() throws CheckListException
    {
        ArrayList<CheckListRow> rows = new ArrayList<CheckListRow>();

        if(store == null)
        {
            throw new CheckListException("Fatal error: checklist data store is null.");
        }

        // is the required checklist title to be shown valid?
        if(! store.containsKey(this.activeCheckList))
        {
            logger.error("Required checklist \"" + this.activeCheckList + "\" " +
                         "does not exist, showing empty table.");
            return rows; // empty table
        }

        // it's ok, required checklist title is valid
        CheckListData data = store.get(this.activeCheckList);
                
        ArrayList<Instruction> instructions = data.getInstructions();
        for(Instruction i : instructions)
        {
            CheckListRow row = new CheckListRow();
            row.setInstruction(i.getText());
            // checkbox value will be preset accordingly if instructions is 
            // meant to be only header
            row.setCheckBox(new Boolean(i.isHeaderOnly()));
            row.setHeaderOnly(i.isHeaderOnly());
            row.setComment(i.getComment());
            row.setHelpUrl(i.getHelpUrl());
            
            rows.add(row);
        }
                
        return rows;
        
    } // prepareTableData() -------------------------------------------------

    
    
    public String getActiveCheckList()
    {
        return this.activeCheckList;
        
    } // getActiveCheckList() -----------------------------------------------
    
    
        
    public ArrayList<CheckListRow> getAllRows()
    {
        return this.allRows;
        
    } // getAllRows() -------------------------------------------------------

    

    public int getColumnCount() 
    {
        return header.length;
        
    } // getColumnCount() ---------------------------------------------------

    

    public int getRowCount()
    {
        return allRows.size();
        
    } // getRowCount() ------------------------------------------------------
    
    

    public String getColumnName(int col)
    {
        return header[col];
        
    } // getColumnName() ----------------------------------------------------

    
    
    public Object getValueAt(int row, int col)
    {
        CheckListRow dataRow = allRows.get(row);
        Object r = dataRow.get(col);
        return r;
        
    } // getValueAt() -------------------------------------------------------
    
    
    
    /*
     * Implement this method if table cells are editable.
     */
    public boolean isCellEditable(int row, int col)
    {
        if(editableColumnIndices.contains(col))
        {
            return true;
        }
        else
        {
            return false;
        }
                
    } // isCellEditable() ---------------------------------------------------
    
    
    
    /*
     * Implement this method if your table's data can change.
     */
    public void setValueAt(Object value, int row, int col)
    {
        logger.debug("Setting at row: " + row + " column: " + col +
                     " value: \"" + value.toString() + "\"");

        CheckListRow dataRow = this.allRows.get(row);
        dataRow.set(col, value);
          
        fireTableCellUpdated(row, col);
        
        // problem: for a cell which was lastly edited, this update
        // method has not been called and the newly edited value
        // has not been propagated into the model, thus is lost ...
        // lastly edited cell is sorted out in the begging of
        // CheckListGUI.processCompletedCheckList()
        
    } // setValueAt() -------------------------------------------------------
    
    
    
    /*
     * JTable uses this method to determine the default renderer/editor for
     * each cell.  If we didn't implement this method, then the checkbox
     * column would contain text ("true"/"false"), rather than a check box.
     * ???
     */
    @SuppressWarnings("unchecked")
    public Class getColumnClass(int c)
    {
        return getValueAt(0, c).getClass();
        
    } // getColumnClass() ---------------------------------------------------


    
} // class CheckListTableModel ==============================================




final class CheckListRow
{    
    private static MyLogger logger = MyLogger.getLogger(CheckListRow.class);

    private String instruction = null;
    private Boolean checkBox = null;
    private String comment = null;
    private String helpUrl = null;
    // headerOnly means that default value of checkBox is true and
    // instraction text is rendered in bold font
    private boolean headerOnly = false;
    
    

    public void set(int column, Object value)
    {
    
        switch(column)
        {
        case 0:
            logger.error("CheckListRow.set() column 0 - wrong, " +
                         "instruction is not editable.");
            break;
        case 1:
            this.setCheckBox((Boolean) value);
            break;
        case 2:
            this.setComment((String) value);
            break;
        case 3:
            //logger.error("CheckListRow.set() column 3 (help), " +
            //              "should not have any effect.");
            break;
        }
        
    } // set() --------------------------------------------------------------
    
    
    
    public Object get(int column)
    {
        Object r = null;
        
        switch(column)
        {
            case 0:
                r = this.getInstruction();
                break;
            case 1:
                r = this.getCheckBox();
                break;
            case 2:
                r = this.getComment();
                break;
            case 3:
                r = this.getHelpUrl();
                break;
        }
        
        return r;
        
    } // get() --------------------------------------------------------------

    
    
    public String getInstruction()
    {
        return instruction;
    }
    
    public void setInstruction(String instruction)
    {
        this.instruction = instruction;
    }
    
    public Boolean getCheckBox()
    {
        return checkBox;
    }
    
    public void setCheckBox(Boolean checkBox)
    {
        this.checkBox = checkBox;
    }
    
    public String getComment()
    {
        return comment;
    }
    
    public void setComment(String comment)
    {
        this.comment = comment;
    }
    public String getHelpUrl()
    {
        return helpUrl;
    }
    
    public void setHelpUrl(String helpUrl)
    {
        this.helpUrl = helpUrl;
    }
    
    public boolean isHeaderOnly()
    {
        return headerOnly;
    }
    
    public void setHeaderOnly(boolean headerOnly)
    {
        this.headerOnly = headerOnly;
    }    
    
} // class CheckListRow =====================================================