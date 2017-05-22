package checklist;


import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JOptionPane;

import mylogger.MyLogger;

/**
 * The only class of the CheckList application which interacts with RunCom.
 * The is no API dependency on RunCom. This class call() method will try to
 * call RunCom method which modifies particular desk setting (e.g.
 * if desk checklist was completed when changing state to ready or when
 * setting DQ checked status to checked.
 * RunCom method: setDeskDetailsApprovedByCheckList()
 * Works via Java Reflection which only relies that RunCom class is accessible
 * which it should be since CheckList was called from RunCom ...
 * 
 * @author Zdenek Maxa
 */
public final class RunComCaller
{
    private static MyLogger logger = MyLogger.getLogger(RunComCaller.class);
    
    private static final String CLASS_NAME = "runcom.RunCom";
    private static final String METHOD_NAME = "setDeskDetailsApprovedByCheckList";
    
    private static CheckListGUI guiInstance = null;
    
    
    
    
    /**
     * Call back RunCom upon completed checklist
     * 
     * @param checkListFileName - completed checklist filename 
     * @param deskNameInRunCom - name of the desk in RunCom which invoked
     *                           CheckList
     * @param guiInstance
     */
    public static void call(String checkListFileName, String deskNameInRunCom,
                            CheckListGUI guiInstance)
    {
        logger.info("Trying to load RunCom class and set desk details on \"" +
                     deskNameInRunCom + "\" subsystem desk, completed " +
                     "checklist: \"" + checkListFileName + "\" calling: " +
                     CLASS_NAME + "." + METHOD_NAME + "()");
        
        RunComCaller.guiInstance = guiInstance;
        
        String m = null;

        try 
        {
            // RunCom class should be accessible ... 
            Class<?> checkListClass = Class.forName(CLASS_NAME);
            // array is list of parameter types of the invoked method
            Method method = null;
            method = checkListClass.getDeclaredMethod(METHOD_NAME,
                                 new Class[] { String.class, String.class });
            // invokes method on the specified object (which is null since the
            // method which is called is static) with one String argument            
            method.invoke(null, checkListFileName, deskNameInRunCom);
          }
          catch(ClassNotFoundException cnf) 
          {
              m = "Could not call RunCom application because RunCom " +
                  "class is not available.";
              logger.debug(m, cnf);
              handleError(m);
          }
          catch(IllegalAccessException iae) 
          {
              m = "Could not call RunCom because RunCom class " +
                  "could not be accessed.";
              logger.debug(m, iae);
              handleError(m);
          }
          catch(NoSuchMethodException nsme) 
          {
              m = "Could not call RunCom because method " +
                  CLASS_NAME + "." + METHOD_NAME + " is not available.";
              logger.debug(m, nsme);
              handleError(m);
          }
          catch(InvocationTargetException ite)
          {
              m = "Could not call RunCom because method " +
                  CLASS_NAME + "." + METHOD_NAME + " could not be called.";
              logger.debug(m, ite);
              handleError(m);
          }          
          catch(Throwable t)
          {
        	  m = "Unknown exception ocurred, reason: " + t.getMessage();
        	  logger.debug(m, t);
        	  handleError(m);
          }
        
    } // call() -------------------------------------------------------------
    
    
    
    private static void handleError(String message)
    {
        logger.error(message);
        JOptionPane.showMessageDialog(RunComCaller.guiInstance,
                                      message, "CheckList error",
                                      JOptionPane.ERROR_MESSAGE);
        
    } // handleError() ------------------------------------------------------

} // class RunComCaller =====================================================