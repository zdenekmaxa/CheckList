package checklist;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;

import javax.swing.JOptionPane;

import mylogger.MyLogger;




/**
 * Main class of the CheckList application.
 * 
 * CheckList application has two main modes of running:
 * 
 * running as stand-alone application - it provides access (via menu) to
 * all available checklist files it finds from the main XML checklist
 * file CheckList.xml
 * 
 * running as invoked from the RunCom application. This scenario requires
 * only one checklist to display and complete and don't have to parse all
 * available checklists (as it was implemented in CheckList-00-00-23 and
 * before) - more over this scenario was causing trouble e.g. one of the 
 * LAr checklists fails at parsing, this way the CheckList fails for
 * everybody because all checklists were parsed upon start-up.
 * 
 * @author Zdenek Maxa
 * 
 */
public final class CheckList
{	
	// logger - takes care of logging to destinations
	private static MyLogger logger = null;
	
	
	// default path to all XML checklist files
	private String checkListsPath = "/det/tdaq/ACR/XMLdata";
	
	// default checklist file, parsing / loading starts from this one
	// when running as a stand-alone application (and no command line
	// argument are provided)
	// if run from RunCom, this variable will be set according to 
	// required checklist file RunCom wants to display, in this case,
	// this checklist is read by default from checkListPath
	private String checkListFile = "CheckList.xml";
	
	// name of desk in RunCom from which CheckList is called (not used
	// when running as a stand-alone application)
	private String deskNameInRunCom = null;
				
	// flag if CheckList was called as a stand-alone application or from RunCom 
	private boolean standAlone = false;

	// CheckList data store - all checklists, instructions and comments use
	// LinkedHashMap rather than HashMap which can't guarantee insertion
	// ordering, main store of data, other classes have reference to this one
	// key: title of the checklist (as defined in the XML checklist file), 
	// values: CheckListData instance corresponds to contents of 1 XML
	// CheckList file
	// to preserve insertion order is useful for instance in CheckListGUI
	// in the class MenuBar - iterates over all keys, and then they are added
	// as menu items - useful to preserve order as they are defined in the main
	// CheckList.xml
	private LinkedHashMap<String, CheckListData> store = null;



	
	
	
	
	private CheckList(boolean standAlone)
	{
		this.standAlone = standAlone;
		
	} // CheckList() --------------------------------------------------------

	
	
	/**
	 * Method which is called from the RunCom application
	 * (not stand-alone running) to display requested checklist.
	 * requestedCheckListFileName should exist in (default) checkListPath
	 */
	public static void showCheckListWindow(String requestedCheckListFileName,
	                                       String deskNameInRunCom)
	{		
		// create instance of CheckList, false - not a stand-alone application		
		CheckList instance = new CheckList(false);

		instance.checkListFile = requestedCheckListFileName;		
		instance.deskNameInRunCom = deskNameInRunCom;
    
		try
		{
		    // should automatically parse only the desired checklist
		    // in requestedCheckListFileName and no other checklist
			instance.initialize();
			CheckListGUI.createAndShowGUI(instance.standAlone,
					                      instance.store,
					                      instance.deskNameInRunCom,
					                      instance.checkListFile);
		}
		catch(CheckListException cle)
		{
			logger.debug(cle.getMessage(), cle);
			logger.error(cle.getMessage());
			String m = "Could not start CheckList.\n" +
			           "See output to console for details.";
			JOptionPane.showMessageDialog(null, m, "CheckList error",
					                      JOptionPane.ERROR_MESSAGE);    			
		}
		
	} // showCheckListWindow() ----------------------------------------------

	
	
	private void processCommandLineParameters(String[] args)
	                    throws CheckListException
	{
	
		// command line options definition
		HelpFormatter formatter = new HelpFormatter();
		String helpTitle = "CheckList application help";
		String helpHeaderMsg = "";
		String helpFooterMsg = "";
		
		Options o = new Options();
		
		// define help option (-h, --help)
		Option help = new Option("h", "help", false,
				                 "display this help message");
		o.addOption(help);
		
		// define show checklist (-s --show) option
		String showDescr = "show particular checklist (XML filename) " +
				           "from a given path, the default XML " +
		                   "file is: \"" + checkListFile + "\" " +
		                   "at path: \"" + checkListsPath + "\"";
		Option showCheckList = OptionBuilder.hasArgs(1)
		                       .withArgName("checklist")
		                       .withDescription(showDescr)
		                       .withLongOpt("show").create('s');
		o.addOption(showCheckList);
		
		// define debug option (-d, --debug)
		String debugDescr = "log debug messages of severity level to " +
	                         "stdout, if destination (filename) is set " +
	                         "then also to a file. severity levels are " +
	                         MyLogger.getStringLevels() +
	                         " (INFO is default)";
		Option debug = OptionBuilder.hasArgs(2)
		                            .withArgName("severity destination")
		                            .withDescription(debugDescr)
		                            .withLongOpt("debug").create('d');
		o.addOption(debug);
		
		// define checklist source directory (path with checklist files
		// (-p --path <path>) option
		String resourceDescr = "use <path> to load XML checklists from, " +
		                       "the default path is: " +
		                       "\"" + checkListsPath + "\"";
		Option resource = OptionBuilder.hasArgs(1)
		                               .withArgName("path")
		                               .withDescription(resourceDescr)
		                               .withLongOpt("path").create('p');
		o.addOption(resource);
		 
		
		// process command line options
		CommandLineParser parser = new GnuParser();
		try
		{
			CommandLine l = parser.parse(o, args);
			
			// debug option
			if(l.hasOption('d'))
            {
            	String[] vals = l.getOptionValues('d');
            	// vals[0] - severity level
            	// vals[1] - destination to write logs to
            	try
            	{
            		MyLogger.initialize(vals); // need to check vals
            	}
            	catch(Exception ex)
            	{
            		String m = debug.getLongOpt() + " incorrect options: " +
            		           ex.getMessage();
            		throw new ParseException(m);
            	}
            } 
			// end of debug option
			
			// help option
			if(l.hasOption('h'))
			{
				formatter.printHelp(helpTitle, helpHeaderMsg, o, helpFooterMsg);
				System.exit(0);
			}
			// end of help option
			
			// show option
			if(l.hasOption('s'))
			{
	        	String arg = l.getOptionValue('s');
	        	// argument is a checklist file name to be shown
	        	checkListFile = arg;
			} 
			// end of show option
			
			// path with XML checklist files
			if(l.hasOption('p'))
			{
			    String arg = l.getOptionValue('p');
			    // argument is alternative path to the source XML checklist file,
			    // if it's not specified, the default checklist XML will be read
			    checkListsPath = arg;
			}			
			// end of read option
			
			
		} // try
		catch(ParseException pe)
		{
			formatter.printHelp(helpTitle, helpHeaderMsg, o, helpFooterMsg);
			throw new CheckListException(pe.getMessage());
		}
	
	} // processCommandLineParameters() -------------------------------------
	
	

	/**
	 * Method for submitting log messages into ATLOG
	 * The message parameters must be the same as on the web form (the elog
	 * command line client sometimes gives spurious error messages).
	 * The attributes which are provided in a list box on the web form
	 * must be in the exact form including spaces.
	 * The testing schema (different log name and port is state below).
	 * The web ATLOG (elog): https://pcatdwww.cern.ch/elog/ATLOG_TEST_W
	 * When testing the elog command on the command line, the attributes
	 * with spaces must be provided as for instance:
	 * -a "Message Type=Default Message Type"
	 * 
	 * @param message
	 * @param activeCheckList - checklist which has just been fulfilled
	 * @param systemAffected - list of SystemAffected elog parameter, taken
	 *     from the checklist (must be in the exact form as on the web form)
	 *     if it's null, "System Affected=Other" is provided, could have one
	 *     or more items - for more items - each is in a single tag
	 *     
	 * Testing on the command line, this command works:
	 * /usr/local/bin/elog -h pc-atlas-www.cern.ch -p 8096 -l ATLOG_TEST 
	       -u reljicd trrean18 -a valid=valid -a User=reljicd -a Rem_IP=127.0.1.1
	       -a "Author=Checklist Entry" -a "Message Type=Default Message Type"
	       -a Status=closed -a Subject=DQcheck-TRT -a "System Affected=ID Gen. \(IC\)"
	       -a "System Affected=TRT" test
	   Message successfully transmitted, ID=329
	 */
	protected static void submitIntoATLog(String message, String activeCheckList,
	                                      String[] systemAffected)
	{		
		String elogValid = "valid=valid";
		String elogUser = "User=reljicd";
		String hostAddress = "unknown";
		
		try
		{
			hostAddress = (InetAddress.getLocalHost()).getHostAddress();
	    }
		catch(UnknownHostException uhe)
		{
			String m = "The default host address of localhost was not found.";
			logger.error(m);
		}
				
	    String elogRemIp = "Rem_IP=" + hostAddress;
        String elogAuthor = "Author=Checklist Entry";
        String elogSystem = "Message Type=Default Message Type";
        String elogStatus = "Status=closed";
        String elogSystemAffected = "System Affected=Other";
        String elogSubject = "Subject=" + activeCheckList;

        // command must by supplied in form of array - String doesn't work
        String[] command = {
                "/usr/local/bin/elog",
                "-h", "pc-atlas-www.cern.ch",
                "-p", "8100", // production elog
                // "-p", "8096", // testing elog
                "-l", "ATLAS", // production elog
                // "-l", "ATLOG_TEST", // testing elog
                "-u", "reljicd", "trrean18",
                "-a", elogValid,
                "-a", elogUser,
                "-a", elogRemIp,
                "-a", elogAuthor,
                "-a", elogSystem,
                "-a", elogStatus,        
                "-a", elogSubject };
        // SystemAffected and the message will be added later
        
        // if there are any systemAffected (coming from XML checklist, use them)
        // otherwise default system affected value other is used automatically:
        // String elogSystemAffected = "System Affected=Other";
        if(systemAffected != null)
        {
            elogSystemAffected = "System Affected=";
            for(int i = 0; i < systemAffected.length; i++)
            {
                elogSystemAffected += systemAffected[i];
                // add elog "System Affected" parameter separator if there are
                // more system affected items and don't add it to the last one
                if(systemAffected.length > 1 && i < systemAffected.length - 1)
                {
                    elogSystemAffected += " | ";
                }
            }
        }

        // final command array size:
        // all items from command + 3 (-a "System Affected=some_system(s) msg")
        int finalCommandSize = command.length + 2 + 1;
        String[] commandFinal = new String[finalCommandSize];
        System.arraycopy(command, 0, commandFinal, 0, command.length);
        
        commandFinal[finalCommandSize - 3] = "-a";
        commandFinal[finalCommandSize - 2] = elogSystemAffected;
        commandFinal[finalCommandSize - 1] = message;
        
        logger.warn("Going to execute ATLOG (elog) inserting command ...");
        String commandDebug = "";
        for(int i = 0; i < commandFinal.length; i++)
        {
            commandDebug += "\"" + commandFinal[i] + "\"" + " ";
        }
        logger.debug("ATLOG (elog) command: \"" + commandDebug + "\"");
        
        
        try
        {
            String ls_str = "";
            Process ls_proc = Runtime.getRuntime().exec(commandFinal); // must be array
        
			// get the command output
			InputStreamReader isr = new InputStreamReader(ls_proc.getInputStream());
	        BufferedReader ls_in = new BufferedReader(isr);
	        
	        logger.warn("Getting elog inserting command output ...");
	        try
	        {
	        	while((ls_str = ls_in.readLine()) != null)
	        	{
	        		logger.warn(ls_str);
	            }
	        }
	        catch(IOException ioe)
	        {
	        	logger.error("Error getting elog command output, reason: " +
	        			     ioe.getMessage());
	        }
	        finally
	        {
	        	ls_in.close();
	        	isr.close();
	        }
	        
	        logger.warn("Executing elog inserting command finished.");
		}
	    catch(IOException ioe)
	    {
	    	String m = "Error executing elog inserting command, reason: " +
	    	            ioe.getMessage();
	    	logger.error(m);	    	
			JOptionPane.showMessageDialog(null, m, "CheckList elog error",
					                      JOptionPane.ERROR_MESSAGE);    
	    }

	} // submitIntoATLog() --------------------------------------------------
		
	
	
	private void initialize() throws CheckListException
	{
		// logger should either be initialized from
		// processCommandLineParameters() or will be using default settings
		logger = MyLogger.getLogger(CheckList.class);		
		
		logger.info("CheckList class initialised, going to parse XML ... ");
		logger.info("CheckList path: \"" + checkListsPath + "\" " +
		            "CheckList file: \"" + checkListFile + "\"");

		String fullPath = checkListsPath + System.getProperty("file.separator") +
		                  checkListFile;
		
		XMLParser parser = new XMLParser(fullPath);
		parser.parse(); // throws CheckListException
		store = parser.getCheckListStore();		
		
	} // initialize() -------------------------------------------------------
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		
		// create instance of CheckList, true - stand alone application
		// i.e. it's not called from within the RunCom application		
		CheckList instance = new CheckList(true);

		try
		{
			instance.processCommandLineParameters(args);
		}
		catch(CheckListException cle)
		{
			// cle.printStackTrace();
			System.out.println(cle.getMessage());
			System.exit(1);
		}
		
		try
		{
			instance.initialize();
		}
		catch(CheckListException cle)
		{
			logger.debug(cle.getMessage(), cle);
			logger.error(cle.getMessage());
			logger.error("Could not start CheckList, exit.");
			System.exit(1);
		}

		
		// use checkListFile to display checklist (if it is the main
		// checklist CheckList.xml (default value, no --show <file> was
		// specified, then the CheckList window will be empty and user can
		// select from menu which checklist to bring up		
        try
        {
            CheckListGUI.createAndShowGUI(instance.standAlone,
            		                      instance.store, null, null);
        }
        catch(CheckListException cle)
        {
            logger.debug(cle.getMessage(), cle);
            logger.error(cle.getMessage());            
        }

	} // main() -------------------------------------------------------------

	
	
} // class CheckList ========================================================