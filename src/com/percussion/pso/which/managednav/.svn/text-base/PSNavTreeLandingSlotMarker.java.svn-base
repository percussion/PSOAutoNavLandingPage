
/* *****************************************************************************
*
* [ PSNavTreeLandingSlotMarker.java ]
* 
* COPYRIGHT (c) 1999 - 2005 by Percussion Software, Inc., Woburn, MA USA.
* All rights reserved. This material contains unpublished, copyrighted
* work including confidential and proprietary information of Percussion.
*
******************************************************************************/
package com.percussion.pso.which.managednav;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.percussion.cms.objectstore.PSActiveAssemblyProcessorProxy;
import com.percussion.cms.objectstore.PSComponentSummaries;
import com.percussion.cms.objectstore.PSSlotType;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.fastforward.managednav.PSNavConfig;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.fastforward.managednav.PSNavProxyFactory;
import com.percussion.fastforward.managednav.PSNavUtil;
import com.percussion.fastforward.managednav.PSNavon;
import com.percussion.fastforward.managednav.PSNavonType;
import com.percussion.server.IPSRequestContext;
import com.percussion.xml.PSXmlTreeWalker;

/**
 * <p>This exit is to be used along with the NavTreeLink extension for
 * generation of a navigation tree for a specific navon.  When this extension
 * processes subsequent to NavTreeLink, it will walk down the navtree
 * and check the info-url for each "ancestor" node.  If it determines that
 * the navon has content in a specified slot, it will mark the navon element
 * with a special attribute set to "yes" which can then be leveraged in XSLT
 * processing.</p>
 * 
 * <p>The purpose of this extension is to allow for links in custom slots on
 * the Landing Page variants to propogate down the ancestor tree and appear on each
 * child navon.  It can of course be used for other appropriate logic built
 * into the XSL stylesheets that process the result document.</p>
 * 
 * <p>This should process after the NavTreeLink extension. It can be used
 * multiple times to create a marker for more than one slot.</p>
 *
 */
public class PSNavTreeLandingSlotMarker extends PSDefaultExtension
   implements IPSResultDocumentProcessor
{
    private static PSNavConfig config = null;
    private static Logger log = Logger.getLogger(PSNavTreeLandingSlotMarker.class); 
   /**
    * This extension never modifies the stylesheet.
    */
   public boolean canModifyStyleSheet()
   {
      return false;
   }   
      
	// Constants
	private static final String CLASSNAME = PSNavTreeLandingSlotMarker.class.getName();
	
	// Constructor
	public PSNavTreeLandingSlotMarker() {
	}
	
	/**
	 * @param params the parameters passed in:
	 * <table border="1" cellpadding="3" cellspacing="0">
	 * <tr><td>markerName</td><td>java.lang.String</td><td>Name of the attribute to create on appropriate navon elements</td></tr>
	 * <tr><td>slotName</td><td>java.lang.String</td><td>Name of the slot for which the exit should check for content</td></tr>
	 * </table>
	 * <p>
	 * @param request the request context object
	 * @param doc the result XML document
	 * @throws PSExtensionProcessingException
	 * </p>
    */   
	
	public Document processResultDocument(Object[] params,
			IPSRequestContext request, Document doc)
	throws PSParameterMismatchException, PSExtensionProcessingException
	{
      String markerName = null;
      String slotName = null;
            
      // first validate the exit parameters
      if (params.length < 1 || params[0] == null ||
            params[0].toString().trim().length() == 0)
      {
         throw new PSParameterMismatchException(MSG_MISSING_PARAM1);
      }
      else
      {
         markerName = params[0].toString().trim();
         request.printTraceMessage("Marker Name is " + markerName);     
      }
      if (params.length < 2 || params[1] == null ||
            params[1].toString().trim().length() == 0)
      {
         throw new PSParameterMismatchException(MSG_MISSING_PARAM2);
      }
      else
      {
         slotName = params[1].toString().trim();
         request.printTraceMessage("Slot Name to check is " + slotName);      
      }
      
      try
      {
            config = PSNavConfig.getInstance(request); 
            
            PSSlotType ourSlot = config.getAllSlots().getSlotTypeByName(slotName); 
            request.printTraceMessage("ourSlot="+ourSlot);
         // initialize navRoot as the document root, <navTree>
         Element navRoot = doc.getDocumentElement();
         if(navRoot != null)
         {  
        	request.printTraceMessage("navRoot is not Null");
        	 
            PSXmlTreeWalker walker = new PSXmlTreeWalker(navRoot);   
            // set navRoot to the first <navon> which should have 
            navRoot = walker.getNextElement(PSNavon.XML_ELEMENT_NAME, 
                   PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN);
            	String contentId = navRoot.getAttribute(PSNavon.XML_ATTR_CONTENTID);
            	request.printTraceMessage("Root content id is="+contentId);
               walkTreeForContent(request, navRoot, ourSlot, markerName);
            }
		}
		catch (Exception e)
		{            
			log.error("Unexpected Exception " +
					e.getLocalizedMessage() ,e);
			throw new PSExtensionProcessingException(CLASSNAME, e);
		}		
		
		return doc;
	}
	

   private void walkTreeForContent(IPSRequestContext req, Element node,
         PSSlotType slot, String attribName) throws PSNavException
   {
      String relation = node.getAttribute(PSNavon.XML_ATTR_TYPE);
      log.debug("Relationship is " + relation);
      /* get landing page node*/
      PSXmlTreeWalker subWalkerlanding = new PSXmlTreeWalker(node);
      Element landing_node = subWalkerlanding.getNextElement(
         PSNavon.XML_ELEMENT_LANDINGPAGE,
         PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN); 
     
     
      if (relation.equalsIgnoreCase(PSNavonType.TYPENAME_SELF))
      {
         checkItemForContent(req, landing_node, slot, attribName);
      }
      if (relation.equalsIgnoreCase(PSNavonType.TYPENAME_ANCESTOR) ||
          relation.equalsIgnoreCase(PSNavonType.TYPENAME_ROOT) )
      {
         checkItemForContent(req, landing_node, slot, attribName);
         PSXmlTreeWalker subWalker = new PSXmlTreeWalker(node);
         Element subnode = subWalker.getNextElement(PSNavon.XML_ELEMENT_NAME,PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN); 
         while(subnode != null)
         {
            walkTreeForContent(req, subnode, slot, attribName); 
            subnode = subWalker.getNextElement(PSNavon.XML_ELEMENT_NAME, PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS);            
         }
      } 
   }
   
   
	
   private boolean checkItemForContent(IPSRequestContext req, Element node,
         PSSlotType slot, String attribName) throws PSNavException
   {
      int authType = PSNavUtil.getAuthType(req);
      
      
      if (node == null)
      {
         log.warn("No Landing Page for node");
         return false;
      }
      String contentId = node.getAttribute(PSNavon.XML_ATTR_CONTENTID);

      if (contentId == null || contentId.trim().length() == 0)
      {
         log.warn("Navon node has no content id");
         return false;
      }
      String revision = node.getAttribute(PSNavon.XML_ATTR_REVISION);
      if (revision == null || revision.trim().length() == 0)
      {
         log.warn("Navon node has no revision");
         return false;
      }
      try
      {
    	 
         PSLocator loc = new PSLocator(contentId, revision);
         PSNavProxyFactory pf = PSNavProxyFactory.getInstance(req);
         PSActiveAssemblyProcessorProxy aaproxy = pf.getAaProxy();
         PSComponentSummaries cs = aaproxy.getSlotItems(loc, slot, authType);

         if(cs!= null && cs.size() > 0)
         {
            node.setAttribute(attribName, "Yes"); 
            return true; 
         }
      } catch (Exception e)
      {
         log.error("Unexpected exception in " + CLASSNAME, e);
         throw new PSNavException(e);
      }
      return false;
   }
	/**
	 * Error message used when parameters are missing
	 */
	private static final String MSG_MISSING_PARAM1  =
		"Missing markerName parameter.";
	
	/**
	 * Error message used when parameters are missing
	 */
	private static final String MSG_MISSING_PARAM2  =
		"Missing slotName parameter.";
	
	/**
	 * Error message used if <info-url> value is empty or null
	 */
	private static final String MSG_INVALID_INFOURL  =
		"An <info-url> value is empty or null.";

	/**
	 * Error message used if the request to the nav info doc
	 * was invalid
	 */
	private static final String MSG_INVALID_REQUEST =
		"The <info-url> does not point to a valid request.";
	/**
	 * Error message used if the request to the nav info doc
	 * was invalid
	 */
	private static final String MSG_EMPTY_INFODOC =
		"The navinfolink document is empty.";
   

}

