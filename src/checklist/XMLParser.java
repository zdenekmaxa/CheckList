package checklist;

import org.xml.sax.XMLReader;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import java.util.LinkedHashMap;

import mylogger.MyLogger;


/**
 * XMLParser - class for reading data from XML file(s)
 * into a CheckList data storage
 * 
 * store attribute contains LinkedHashMap of CheckListData objects. After
 * parsing the XML document, store LinkedHashMap contains all data.
 * 
 * Structure of the XML CheckList document outlined in the comment of the
 * class CheckListData.
 * 
 * @author Zdenek Maxa
 *
 */
public final class XMLParser extends DefaultHandler
{
	private static MyLogger logger = MyLogger.getLogger(XMLParser.class);
	
	
	// main store with all data
	// CheckList title is key in the store
	private LinkedHashMap<String, CheckListData> store = null;
	
	// source XML file the checklists are parsed and loaded from
	private String xmlFileName = null;


	// helper variables for intermediated saving when parsing XML document
	private CheckListData tmpCheckList = null;
	private Instruction tmpInstruction = null;
	// perhaps could remove following twoo ...?
	private StringBuffer tmpInstructionText = null; // instruction text	
	private StringBuffer tmpElogSystemAffected = null;
	

	// current element name - helps to distinguish in the characters() method
	// in which tag we are
	private String currentElement = null;
	
	
	
	
	public XMLParser(String xmlFileName)
	{
		super();
		this.xmlFileName = xmlFileName;
		store = new LinkedHashMap<String, CheckListData>();
		
	} // XMLParser() --------------------------------------------------------

	
	
    public void parse() throws CheckListException
    {
    	logger.debug("Parser initialisation ... ");
    	try
    	{
    		SAXParserFactory spf = SAXParserFactory.newInstance();
    		// localName will be equal to qName
    		spf.setNamespaceAware(true);
    		SAXParser saxLevel1 = spf.newSAXParser();
    		XMLReader parser = saxLevel1.getXMLReader();
    		XMLParserErrorHandler errorHandler = new XMLParserErrorHandler();
    		parser.setErrorHandler(errorHandler);
    		parser.setContentHandler(this);
    		parser.setEntityResolver(this); // for resolveEntity() to be called
    		logger.debug("Parser initialised successfully.");
    		logger.debug("Going to parse document: " + xmlFileName);

    		// this mere call would be enough if not reading from 
    		// input stream
    		// parser.parse(xmlFileName);
    		   
    		// parsing from input stream
    		// resolveEntity() is also called behind the scenes
    		FileInputStream fis = new FileInputStream(xmlFileName);
    		InputSource is = new InputSource(fis);
    		parser.parse(is);
    		
    	}
    	catch(ParserConfigurationException pce)
    	{
    		String m = "Parser initialisation failed: " + pce.getMessage();
    		throw new CheckListException(m);
    	}    	
    	catch(SAXException se)
    	{
    		String m = "Error while parsing document: " + se.getMessage();
    		throw new CheckListException(m);
    	}
    	catch(IOException ioe)
    	{
    		String m = "I/O error while reading document: " + ioe.getMessage();
    		throw new CheckListException(m);
    	}
    	    	           	
    } // parse() ------------------------------------------------------------

    

    /**
     * When reading from stream (parser.parse(stream)), it's necessary to
     * correct entities (files referenced from the main XML file) such
     * as DTD file reference or particular checklists XML files which
     * are included into the main one. Without this method, the files
     * would be looked up in the current directory (as seen in the 
     * original value of systemId). For this method to be called,
     * it's necessary to set: parser.setEntityResolver(this)
     * This method assumes that all included entities are located in
     * the same directory as the main XML file xmlFileName.
     */
    public InputSource resolveEntity(String publicId, String systemId)
    {
        logger.debug("Called resolveEntity() systemId: " + systemId);
        
        InputSource inputSource = null;
        String fileSep = System.getProperty("file.separator");
        String correctPath = new File(xmlFileName).getParent();        
        String requestedFile = new File(systemId).getName();
        String correctFullPath = correctPath + fileSep + requestedFile;
        
        logger.debug("Resolved file: " + correctFullPath);

        try
        {
            FileInputStream is = new FileInputStream(correctFullPath);
            inputSource = new InputSource(is);
        }
        catch(IOException io)
        {
            logger.error("I/O error while reading: " + correctFullPath + " " +
                         "reason: " + io.getMessage());
        }
    
        return inputSource;

    } // resolveEntity() ----------------------------------------------------    
    

    
    public void startDocument() throws SAXException
    {
    	logger.debug("Start parsing document.");
    	
    } // startDocument() ----------------------------------------------------
    
    
    
    public void endDocument() throws SAXException
    {
    	logger.debug("End parsing document.");
    	    	
    } // endDocument() ------------------------------------------------------

    
    
    /**
     * startElement() and endElement() are the most important parsing methods.
     */
    public void startElement(String namespaceURI, String localName, String qName,
    		                 Attributes attrs) throws SAXException
	{        
    	// check that local name (localName) is equal to qualified name (qName)
    	if(! localName.equals(qName))
    	{
        	logger.error("Parsing, start element: localName: " +
        			     localName + "  qName: " + qName + " local " +
        			     "name and qualified name are not equal, error.");
    	}
    	
		if(attrs != null)
		{
			for(int i = 0; i < attrs.getLength(); i++)
			{
				// check that attribute's local name is equal to qualified name
				if(! attrs.getLocalName(i).equals(attrs.getQName(i)))
				{
					logger.error("Parsing attributes attrs.getLocalName(): " +
							     attrs.getLocalName(i) + "  attrs.getQName(): " +
							     attrs.getQName(i) + "  attribute's local " +
							     "name and qualified name are not equal, error.");
				}
				
				// use attribute's qualified name
				String aName = attrs.getQName(i);
				
				if("checklist".equals(localName) && "title".equals(aName))
				{
					// start parsing <checklist> element
				    // e.g. <checklist title="signin-DAQ">
					String title = attrs.getValue(i);
					logger.info("checklist title: \"" + title + "\"");
					tmpCheckList = new CheckListData(title);
					tmpInstruction = new Instruction();
					tmpInstructionText = new StringBuffer();
					tmpElogSystemAffected = new StringBuffer();
				}
				if("instruction".equals(localName) && "PresetComment".equals(aName))
				{
					// parsing <instruction> element, PresetComment attribute
					String comment = attrs.getValue(i);
					tmpInstruction.setComment(comment);
				}
				if("instruction".equals(localName) && "helpurl".equals(aName))
				{
					// parsing <instruction> element, helpurl attribute
					String url = attrs.getValue(i);
					tmpInstruction.setHelpUrl(url);
				}
				if("instruction".equals(localName) && "headerOnly".equals(aName))
				{
				    // parsing <instruction> element, headerOnly attribute
				    String s = attrs.getValue(i);
				    boolean headerOnly = Boolean.parseBoolean(s);
				    tmpInstruction.setHeaderOnly(headerOnly);
				}
			}
		}

		// this element doesn't have any attributes
		// tmpElogSystemAffected if this variable doesn't exist, could
		// perhaps remove the whole block
        if("ElogSystemAffected".equals(localName))
        {
            tmpElogSystemAffected = new StringBuffer();
        }
		
		currentElement = localName;
	
	} // startElement() -----------------------------------------------------

        

	public void endElement(String namespaceURI, String localName,
			               String qName) throws SAXException
	{
    
		if("checklist".equals(localName))
		{
		    // parsing reached </checklist>
			store.put(tmpCheckList.getTitle(), tmpCheckList);
			tmpCheckList = null;
			tmpInstruction = null;
		}
		if("instruction".equals(localName))
		{
		    // parsing reached </instruction>
		    tmpInstruction.setText(tmpInstructionText.toString());
			tmpCheckList.addInstruction(tmpInstruction);
			tmpInstructionText.setLength(0); // erase previous instruction content
			tmpInstruction = new Instruction(); // prepare for the one that follow
		}
		if("ElogSystemAffected".equals(localName))
		{
		    // parsing reached </ElogSystemAffected>
		    // should be one item of system affected
		    String s = tmpElogSystemAffected.toString().trim();
		    tmpCheckList.addElogAffectedSystem(s);
		    logger.debug("Elog SystemAffected for this checklist: \"" + s + "\"");
		    tmpElogSystemAffected.setLength(0); // erase previous elog systemaffected
		}
		
	} // endElement() -------------------------------------------------------

	

	/**
	 * This method reads in the content within the tags, i.e. not attributes
	 * of an element.
	 */
    public void characters(char buf[], int offset, int len) throws SAXException
    {
        // need to have currentElement to be able to distinguish in which
        // element we are in
        
        String s = new String(buf, offset, len);
        
        if("instruction".equals(currentElement))
        {    
            tmpInstructionText.append(s);            
        }
        if("ElogSystemAffected".equals(currentElement))
        {
            tmpElogSystemAffected.append(s);
        }
        
    } // characters() -------------------------------------------------------    

    

    public LinkedHashMap<String, CheckListData> getCheckListStore()
    {
    	return store;
    	
    } // getCheckListStore() ------------------------------------------------

    
} // class XMLParser ========================================================




class XMLParserErrorHandler implements ErrorHandler
{
	private static MyLogger logger = 
		MyLogger.getLogger(XMLParserErrorHandler.class);
	
	
	private String getFormattedMessage(SAXParseException spe)
	{
		String r = spe.getSystemId() + "  line: " + spe.getLineNumber() +
		           "  column: " + spe.getColumnNumber() + "  reason: " +
		           spe.getMessage();
		return r;
		
	} // getMessage() -------------------------------------------------------
	
	
	
	public void warning(SAXParseException spe)
	{
		String msg = getFormattedMessage(spe);
		logger.warn(msg);
		
	} // warning() ----------------------------------------------------------

	
	
	public void error(SAXParseException spe) throws SAXException
	{
		String msg = "error: " + getFormattedMessage(spe);
		throw new SAXException(msg);
		
	} // error() ------------------------------------------------------------
	
	
	public void fatalError(SAXParseException spe) throws SAXException
	{
		String msg = "fatal error: " + getFormattedMessage(spe);
		throw new SAXException(msg);
		
	} // fatalError() -------------------------------------------------------
	
	
} // class XMLParserErrorHandler ============================================
