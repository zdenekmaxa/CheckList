package checklist;

import java.util.ArrayList;

import java.io.IOException;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTable;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.DefaultCellEditor;
import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import mylogger.MyLogger;



/**
 * Rendering, editing of the CheckList data table contents.
 * 
 * @author Zdenek Maxa
 *
 */
@SuppressWarnings("serial")
public final class DataTable extends JTable
{
    private static MyLogger logger = MyLogger.getLogger(DataTable.class);
    
    //declare some table cell preferred sizes
    private static final int PREFERRED_ROW_HEIGHT = 48; 
    private static final int PREFFERED_INSTRUCTION_WIDTH = 500;
    private static final int PREFFERED_CHECK_BOX_WIDTH = 50;
    private static final int PREFERRED_COMMENT_WIDTH = 300;
    private static final int PREFERRED_HELP_WIDTH = 55; 
    
    
    
    
    
    public DataTable(CheckListTableModel model) throws CheckListException
    {
        super(model);
        
        logger.debug("Initializing DataTable instance ...");
        
        int width = PREFFERED_INSTRUCTION_WIDTH + PREFFERED_CHECK_BOX_WIDTH +
                    PREFERRED_COMMENT_WIDTH + PREFERRED_HELP_WIDTH;
        int height = PREFERRED_ROW_HEIGHT * 10;
        
        this.setPreferredScrollableViewportSize(new Dimension(width, height));
        setRowHeight(PREFERRED_ROW_HEIGHT);
        
        // set some cell spacing
        this.setIntercellSpacing(new Dimension(10, 2));
        
        JTableHeader header = this.getTableHeader();
        header.setBackground(Color.LIGHT_GRAY);
        
        
        TableColumn columnInstruction = getColumnModel().getColumn(0);
        TableColumn columnCheckBox = getColumnModel().getColumn(1);
        TableColumn columnComment = getColumnModel().getColumn(2);
        TableColumn columnHelp = getColumnModel().getColumn(3);
        
        columnInstruction.setPreferredWidth(PREFFERED_INSTRUCTION_WIDTH);
        columnCheckBox.setPreferredWidth(PREFFERED_CHECK_BOX_WIDTH);
        columnComment.setPreferredWidth(PREFERRED_COMMENT_WIDTH);
        columnHelp.setPreferredWidth(PREFERRED_HELP_WIDTH);
        
        columnInstruction.setCellRenderer(new CellRenderer());
        // columnInstruction is not editable, no editor necessary
        
        columnComment.setCellRenderer(new CellRenderer());
        columnComment.setCellEditor(new CellEditor(PREFERRED_COMMENT_WIDTH,
                                                   PREFERRED_ROW_HEIGHT));
        
        columnHelp.setCellRenderer(new ButtonRenderer());
        // attaching event listener to ButtonRenderer doesn't work
        // no matter whether a column is editable or not, need editor ...
        columnHelp.setCellEditor(new ButtonEditor(new JCheckBox(), model));
        
        
    } // DataTable() --------------------------------------------------------
    
    
    
    public static void setComponentColor(Component c, JTable table, 
                                         boolean isSelected, int row)
    {
        if(isSelected)
        {
            c.setForeground(table.getSelectionForeground());
            c.setBackground(table.getSelectionBackground());
        } 
        else
        {
            if(row % 2 == 0)
            {
                c.setForeground(table.getForeground());
                c.setBackground(new Color(225, 249, 237));                
            }
            else
            {
                c.setForeground(table.getForeground());
                c.setBackground(new Color(178, 212, 196));                
            }
        }
        
    } // setComponentColor() ------------------------------------------------
    

} // class DataTable ========================================================



@SuppressWarnings("serial")
class CellRenderer extends JTextArea implements TableCellRenderer
{
    
    public CellRenderer()
    {
        this.setLineWrap(true); // do wrap lines
        this.setWrapStyleWord(true); // wrap lines at word boundaries
        
    } // CellRenderer() -----------------------------------------------------
    
    
   
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row, int column)
    {
        String s = value != null ? value.toString() : "";
        this.setText(s);

        // table row height sizing issue, bug in JTable+TextArea returning
        // correct preferred size, more on:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4446522
        // seems to be a bug still in Java 1.6
        // these two lines set the size implicitly and remove the problem
        // so that rows needing to accommodate longer instructions get
        // resized
        this.setSize(table.getColumnModel().getColumn(column).getWidth(), 0);
        this.getUI().getRootView(this).setSize(this.getWidth(), 0f);        
        
        // check if it's the first column (Instructions)
        if(column == 0)
        {
            // now getPreferredSize() should return correct values
            int prefRowHeight = (int) this.getPreferredSize().getHeight();
            int currRowHeight = table.getRowHeight(row);

            // adjust height row height to display longer instruction
            if(currRowHeight < prefRowHeight)
            {
                table.setRowHeight(row, prefRowHeight + 10); // plus some extra
            }         
        }
        
        DataTable.setComponentColor(this, table, isSelected, row);
        
        // set tooltip for columns to display cell's contents if necessary
        // String toolTip = value.toString();
        // this.setToolTipText("row: " + row + " col: " +
        //                     column + "  " + toolTip);        
        // this.setToolTipText(toolTip);
        
        // if the instruction is meant as header, render the instruction,
        // i.e. the first column, in bold font
        CheckListTableModel model = (CheckListTableModel) table.getModel();
        ArrayList<CheckListRow> allRows = model.getAllRows();
        boolean isHeader = allRows.get(row).isHeaderOnly();
        if(column == 0 && isHeader)
        {
            Font f = this.getFont();
            this.setFont(f.deriveFont(Font.BOLD));
        }
        else
        {
            // need to check font for the other cells - since JTable reuses
            // instances of cell renderers, once it's set to BOLD, it is BOLD
            // for all the next to come
            Font f = this.getFont();
            this.setFont(f.deriveFont(Font.PLAIN));
        }
        
        return this;
        
    } // getTableCellRendererComponent() ------------------------------------
    
       
} // class CellRenderer =====================================================



@SuppressWarnings("serial")
class CellEditor extends AbstractCellEditor implements TableCellEditor
{
    private JTextArea commentTextArea;
    private JScrollPane scrollPane;
    
    
    public CellEditor(int preferredWidth, int  preferredHeight) 
    {
        commentTextArea = new JTextArea();
        commentTextArea.setLineWrap(true); // do wrap lines
        commentTextArea.setWrapStyleWord(true); // wrap lines at word boundaries
        scrollPane = new JScrollPane(commentTextArea);
        Dimension size = new Dimension(preferredWidth, preferredHeight);
        scrollPane.setPreferredSize(size);
        // scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
    } // CellEditor() -------------------------------------------------------
    
    
    
    /**
     * Method defined by AbstractCellEditor
     */
    public Object getCellEditorValue() 
    {
        return commentTextArea.getText();
        
    } // getCellEditorValue() -----------------------------------------------
    
    
    
    /**
     * Method defined by TableCellEditor
     */
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column)
    {
  
        DataTable.setComponentColor(commentTextArea, table, isSelected, row);        
        String s = value != null ? value.toString() : "";
        commentTextArea.setText(s);
        
        return scrollPane;
        
    } // getTableCellEditorComponent() --------------------------------------
    
    
    public boolean stopCellEditing()
    {
        return super.stopCellEditing();
        
    } // stopCellEditing() -------------------------------------------------
    
    
    
    protected void fireEditingStopped()
    {
        super.fireEditingStopped();
        
    } // fireEditingStopped() -----------------------------------------------
    
    
} // class CellEditor =======================================================



/**
 * Class is "used" to render a table cell (not when mouse is clicked in a
 * cell), used at columnHelp.setCellRenderer( ... )
 */
@SuppressWarnings("serial")
class ButtonRenderer extends JPanel implements TableCellRenderer
{
    private TableButton button = null;
    
    
    public ButtonRenderer() 
    {
        super();
        
        button = new TableButton();        
        this.add(button);        
                
    } // ButtonRenderer() ---------------------------------------------------
  
    
    
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row, int column) 
    {
        
        DataTable.setComponentColor(this, table, isSelected, row);
 
        // set the question mark on the button
        button.setText("?");
        
        // setting tooltip - url the button points to (value)
        String s = value != null ? value.toString() : ""; 
        this.setToolTipText("".equals(s) ? "<empty>" : s);
        
        return this;
        
    } // getTableCellRendererComponent() ------------------------------------
    
} // class ButtonRenderer ===================================================



/**
 * Class is "used" when mouse is clicked on a table cell, that is set at
 * columnHelp.setCellEditor( ... )
 */
@SuppressWarnings("serial")
class ButtonEditor extends DefaultCellEditor  
{
    private static MyLogger logger = MyLogger.getLogger(ButtonEditor.class);

    protected TableButton button = null;
    private String label = null;
    private boolean isPushed = false;
    private int buttonRow = 0;
    
    private CheckListTableModel model = null;
    

    public ButtonEditor(JCheckBox checkBox, CheckListTableModel model) 
 
    {
        super(checkBox);
        this.model = model;
        button = new TableButton();
        
        button.setActionListener(new ActionListener() 
        {
            public void actionPerformed(ActionEvent e) 
            {
                fireEditingStopped();
            }
        });
        
    } // ButtonEditor() -----------------------------------------------------
    
    

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 int row, int column)
    {

        // set the question mark on the button
        label = "?";
        button.setText(label);
        
        // value is the URL, that appears in the button tooltip already
        
        isPushed = true;
        buttonRow = row;
        return button;
        
    } // getTableCellEditorComponent() --------------------------------------
    
    

    public Object getCellEditorValue()
    {
        if(isPushed)
        {
            try
            {
                String currentCheckList = model.getActiveCheckList();
                ArrayList<CheckListRow> allRows = model.getAllRows();
                String urlToShow = allRows.get(buttonRow).getHelpUrl();
                
                if("".equals(urlToShow))
                {
                    String m = "Checklist: \"" + currentCheckList + "\", row " +
                                buttonRow + " has empty URL.";
                    logger.error(m);
                    JOptionPane.showMessageDialog(null, m, "CheckList error",
                                                  JOptionPane.ERROR_MESSAGE);                
                }
                else
                {
                    logger.debug("URL to show in browser: " + urlToShow);
                    logger.debug("Launching web browser ...");
                    showInBrowser(urlToShow);
                }
            }
            catch(Exception ex)
            {
                // this exception should actually never occur
                // must be null pointer or index out of bound ...
                logger.error(ex.getMessage(), ex);
                JOptionPane.showMessageDialog(null, ex.getMessage(),
                                              "CheckList error",
                                              JOptionPane.ERROR_MESSAGE);
            }
        } // if(isPushed)

        isPushed = false;
        return new String(label);
    
    } // getCellEditorValue() -----------------------------------------------
    
  
    
    public boolean stopCellEditing() 
    {
        isPushed = false;
        return super.stopCellEditing();
        
    } // stopCellEditing() -------------------------------------------------
    
    
    
    protected void fireEditingStopped()
    {
        super.fireEditingStopped();
        
    } // fireEditingStopped() -----------------------------------------------
    
    
    
    private static void showInBrowser(String url)
    {
        
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();
        
        try
        {
            if(os.indexOf("win") >= 0)
            {
                // we are on MS Windows
                String[] cmd = new String[4];
                cmd[0] = "cmd.exe";
                cmd[1] = "/C";
                cmd[2] = "start";
                cmd[3] = url;
                rt.exec(cmd);
            }
            else if(os.indexOf("mac") >= 0)
            {
                // we are on Mac
                rt.exec("open " + url);
            }
            else
            {
                rt.exec("konqueror " + url);
            }
        }
        catch(IOException ioe)
        {
            logger.error("IO Exception occured while trying to launch web " +
                         "browser on operating system: " + os);
        }

    } // showInBrowser() ----------------------------------------------------

    
} // class ButtonEditor =====================================================



@SuppressWarnings("serial")
class TableButton extends JPanel
{
    private JButton button = null;
    
    
    public TableButton()
    {
        this.setLayout(new FlowLayout());
        // this.setLayout(new BorderLayout());
        JPanel innerPanel = new JPanel();
        // innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
        innerPanel.setLayout(new BorderLayout());
        // Border border = new LineBorder(innerPanel.getBackground(), 5, true);
        // innerPanel.setBorder(border);
        
        button = new JButton();
        button.setOpaque(true);
        
        innerPanel.add(button, BorderLayout.CENTER);
        this.add(innerPanel);
        
    } // TableButton() ------------------------------------------------------
    
    
    
    protected void setText(String text)
    {
        button.setText(text);
        
    } // setText() ----------------------------------------------------------
    
    
    
    protected void setActionListener(ActionListener al)
    {
        button.addActionListener(al);
        
    } // setActionListener() ------------------------------------------------
    

} // class TableButton ======================================================