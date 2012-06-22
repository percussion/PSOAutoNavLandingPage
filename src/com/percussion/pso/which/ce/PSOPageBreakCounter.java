package com.percussion.pso.which.ce;

import java.io.File;

import org.apache.log4j.Logger;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSRequestPreProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequestValidationException;

/**
* This pre-exit extracts a request parameter from the key specified by the 
* source exit parameter, parses through for a count of string values, and returns
* the count to the key specified by the destination exit parameter
*/
public class PSOPageBreakCounter implements IPSRequestPreProcessor
{
	
  private static String PAGE_BREAK_STRING = "<span class=\"pagetitle\">";

  Logger m_log = Logger.getLogger(this.getClass());
	
  public PSOPageBreakCounter()
  {
     // nothing to do
  }

  
  // see IPSRequestPreProcessor
  public void preProcessRequest(Object[] params, IPSRequestContext request)
        throws PSAuthorizationException, PSRequestValidationException,
        PSParameterMismatchException, PSExtensionProcessingException
  {
     // expects two string parameters   
     String sourceName = getParameter(params, 0);
     String destinationName = getParameter(params, 1);
     String offSet = getParameter(params, 2);
     String vSource = "";
     int vCount=0;
     int vOffSet=0;
     
     if (offSet != null )
     {
    	 vOffSet = Integer.parseInt(offSet);
     }
     
     //get source body field object from request
     Object o = request.getParameterObject(sourceName);
     if (o != null)
    	//Convert source body field object to string
    	vSource = o.toString();
     
     	m_log.debug("getSourceParam: String=" + vSource);
     	//Count occurances of substring
     	vCount = countOccurances(PAGE_BREAK_STRING, vSource, vOffSet, true);
     	
     	m_log.debug("setDestParam: Count=" + vCount);
     	
     	//Convert returned value to a String
     	String endCount = String.valueOf(vCount);
     	
     	//Set new request parameter (defined by the destination field name) to the value of the no. of occurances
        request.setParameter(destinationName, endCount);
  }


  // see IPSRequestPreProcessor
  public void init(IPSExtensionDef def, File codeRoot)
        throws PSExtensionException
  {
     // nothing to do
  }


  /**
   * Get a parameter from the parameter array, and return it as a string.
   *
   * @param params array of parameter objects from the calling function.
   * @param index the integer index into the parameters
   * 
   * @return a not-null, not-empty string which is the value of the parameter
   * @throws PSParameterMismatchException if the parameter is missing or empty
   **/
  private static String getParameter(Object[] params, int index)
        throws PSParameterMismatchException
  {
     if (params.length < index + 1 || null == params[index] ||
           params[index].toString().trim().length() == 0)
     {
        throw new PSParameterMismatchException(PSOPageBreakCounter.class +
              ": Missing exit parameter");
     }
     else
     {
        return params[index].toString().trim();
     }
  }

  /**
   * 
   * @param subString - subString to search for - uses Static Variable defined at top 
   * @param mainString - mainString to search through - uses Source Body value
   * @return an int value of the count
   */
  public static int countOccurances(String subString, String mainString, int vOffSet, boolean matchreoccur)
  {
		int i = 0;
		for(int index = 0; (index = mainString.indexOf(subString, index)) != -1; i++)
		    index += matchreoccur?1:subString.length();
		i = i + vOffSet;
		return i;
  }

}
