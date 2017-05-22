package checklist;

import java.util.ArrayList;


/**
 * Class containing CheckList file instructions. Instance of this class
 * corresponds with 1 XML CheckList file. Structure of the class
 * reflects structure of the XML CheckList file, example:
 * 
 * <checklist title="signin-DAQ-HLT">
 *    <instruction PresetComment="preset comment"  helpurl="http://someserver">
 *        instruction text
 *    </instruction>
 *    
 *    <instruction PresetComment="preset comment"  helpurl="http://someserver"
 *                 headerOnly="true">
 *        instruction text
 *    </instruction>
 *    
 *    many instructions per <checklist> element
 *    
 *    <ElogSystemAffected>
 *       DAQ
 *    </ElogSystemAffected>
 *    
 *    could have more <ElogSystemAffected> elements
 *    
 * </checklist>
 * 
 * headerOnly default value is false - instruction is not by default just 
 * header which doesn't have to be fulfilled
 *    
 * @author Zdenek Maxa
 *
 */
public final class CheckListData
{
    // title (name) of the checklist
	private String title = null;
	
	// iterating returns items in the insertion order (ArrayList feature)
	private ArrayList<Instruction> instructions = null;
	private ArrayList<String> elogAffectedSystems = null;
	
	
	
	public CheckListData(String title)
	{
		this.title = title;
		this.instructions = new ArrayList<Instruction>();
		this.elogAffectedSystems = new ArrayList<String>();
						
	} // CheckListData() ----------------------------------------------------
	
	
	
	public String getTitle()
	{
		return this.title;
		
	} // getTitle() ---------------------------------------------------------
	
	
	
	public void addInstruction(Instruction i)
	{
	    this.instructions.add(i);

	} // addInstruction() ---------------------------------------------------
	
	
	
	public ArrayList<Instruction> getInstructions()
	{
	    return this.instructions;
	    
	} // getInstructions() --------------------------------------------------

	
	
	public void addElogAffectedSystem(String newSystemAffected)
	{
	    this.elogAffectedSystems.add(newSystemAffected);
	    
	} // elogAffectedSystems ------------------------------------------------
	
	
	
	public String[] getElogAffectedSystem()
	{
	    
	    String[] r = null;
	    if(! this.elogAffectedSystems.isEmpty())
	    {
	        r = new String[this.elogAffectedSystems.size()];
	        r = (String[]) this.elogAffectedSystems.toArray(r);    
	    }
	    return r;
	    
	} // getElogAffectedSystem() --------------------------------------------
	
	
	
	/**
	 * Size means number of instructions in this checklist data instance.
	 * @return
	 */
	public int size()
	{
	    return this.instructions.size();
	    
	} // size() -------------------------------------------------------------
	
	

} // CheckListData ==========================================================



/**
 * Structure of the <instruction> element.
 */
final class Instruction
{
    // preset instruction comment
    private String comment = null;
    // help URL link
    private String helpUrl = null;
    // indicates if the instruction text is meant as only header in the table,
    // it will appear checked by default
    private boolean headerOnly = false;
    // the text of the instruction
    private String text = null;
    
    
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
    
    
    public String getText()
    {
        return text;
    }
    
    
    public void setText(String text)
    {
        this.text = text;
    }

} // class Instruction ======================================================