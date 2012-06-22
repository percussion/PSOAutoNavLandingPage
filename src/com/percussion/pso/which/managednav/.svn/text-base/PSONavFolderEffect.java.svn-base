/******************************************************************************
 *
 * [ PSONavFolderEffect.java ]
 *
 *
 ******************************************************************************/
package com.percussion.pso.which.managednav;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.cms.objectstore.PSAaRelationshipList;
import com.percussion.cms.objectstore.PSActiveAssemblyProcessorProxy;
import com.percussion.cms.objectstore.PSComponentSummaries;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.PSRelationshipProcessorProxy;
import com.percussion.consulting.utils.PSORelationshipHelper;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.fastforward.managednav.PSNavAbstractEffect;
import com.percussion.fastforward.managednav.PSNavConfig;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.fastforward.managednav.PSNavFolder;
import com.percussion.fastforward.managednav.PSNavFolderUtils;
import com.percussion.fastforward.managednav.PSNavProxyFactory;
import com.percussion.fastforward.managednav.PSNavRelationshipInfo;
import com.percussion.fastforward.managednav.PSNavUtil;
import com.percussion.relationship.IPSEffect;
import com.percussion.relationship.IPSExecutionContext;
import com.percussion.relationship.PSEffectResult;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.cache.PSCacheProxy;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSContentTypeMgr;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.PSContentMgrConfig;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.util.PSXMLDomUtil;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.assembly.IPSAssemblyWs;
import com.percussion.webservices.assembly.PSAssemblyWsLocator;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;

/**
 * A relationship effect for managing folders and navons. This effect is
 * designed for the <code>Folder Content</code> relationhip.
 * <p>
 * There are several different events that this effect must handle
 * <ul>
 * <li>A new folder is added to a folder</li>
 * <li>A new navon is added a folder</li>
 * <li>A Navon is removed from a folder</li>
 * <li>A folder is removed</li>
 * </ul>
 * <p>
 * New navons are added only when the folder above them already has a navon in
 * it. When a navon exists in a child folder, these navons are connected to the
 * current navon by an Active Assembly relationship.
 * <p>
 * When folders or navons are removed, these relationships are removed as well.
 * <p>
 * All navons added by this effect inherit the community of the navon above them
 * in the hierarchy, not the community of the calling user
 * <p>
 * This effect is designed to run as the <code>rxserver</code> user.
 * <p>
 *
 */
public class PSONavFolderEffect extends PSNavAbstractEffect implements IPSEffect
{
    private HashMap m_landingPageProps = null;
    private IPSGuidManager guidMgr = PSGuidManagerLocator.getGuidMgr();
    private IPSContentMgr cmgr = PSContentMgrLocator.getContentMgr();
    private IPSContentWs cws = PSContentWsLocator.getContentWebservice();

  /**
    * Initializes the effect.
    *
    * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef,
    *      java.io.File)
    */
    public void init(IPSExtensionDef arg0, File arg1)
         throws PSExtensionException
    {
      super.init(arg0, arg1);

      m_log = Logger.getLogger(this.getClass());

      // have to initialise this here as Eclipse/Ant complain otherwise
      // don't ask me why as it compiles fine normally!!!!
      try {
        ms_config = PSNavConfig.getInstance();
      } catch (PSNavException ne) {
       throw new PSExtensionException("Cannot get Nav Config",ne.getMessage());
      }

      m_log.debug("intialising...");

      try {
        m_landingPageProps = getLandingPageCombinations();
      }
      catch (PSExtensionException pse)
      {
        // Rethrow
           throw pse;
      }
      catch (Exception ex)
      {
        throw new PSExtensionException(
          IPSExtensionErrors.BAD_PUBLISH_CONTENT_INITIALIZATION_DATA,
          ex.getMessage());
      }

      m_log.debug("intialised...");

    }

   /**
    * Tests whether the effect should allow the operation to continue.
    *
    * @param params the effect parameters specified in the workbench. Not
    *    used in this effect.
    * @param req the callers request context, not <code>null</code>.
    * @param excontext the execution context determines which event caused this
    *    effect to run, not <code>null</code>.
    * @param result the result object tells the effect processor whether the
    *    event is allowed to continue or not, not <code>null</code>.
    * @throws PSExtensionProcessingException
    * @throws PSParameterMismatchException
    *
    */
   public void test(Object[] params, IPSRequestContext req,
      IPSExecutionContext excontext, PSEffectResult result)
      throws PSExtensionProcessingException, PSParameterMismatchException
   {
      if (req == null)
         throw new IllegalArgumentException("req cannot be null");

      if (excontext == null)
         throw new IllegalArgumentException("excontext cannot be null");

      if (result == null)
         throw new IllegalArgumentException("result cannot be null");

      try
      {
         String userName =
            req.getUserContextInformation("User/Name", "").toString();
         m_log.debug("User name " + userName);

         if (isExclusive(req))
         {
            m_log.debug("TEST - exclusion flag detected");
            result.setSuccess();
            return;
         }

         ms_config = PSNavConfig.getInstance(req);

         PSRelationship currRel = excontext.getCurrentRelationship();

         PSNavRelationshipInfo currentInfo;
         try
         {
            currentInfo = new PSNavRelationshipInfo(currRel, req);
         }
         catch (Exception ex)
         {
            m_log.warn("Unable to load relationship info rid is "
                  + currRel.getId(), ex);
            result.setSuccess();
            return;
         }

         String operation = String.valueOf(excontext.getContextType());
         m_log.debug("Test Current " + currentInfo.toString() +
            "\n Operation " + operation);

         if (excontext.isPostConstruction())
            handleTestNew(req, currentInfo, result);
         else
            result.setSuccess();
      }
      catch (Exception ex)
      {
         m_log.error(this.getClass().getName(), ex);
         result.setError(new PSExtensionProcessingException(this.getClass()
               .getName(), ex));
      }

      return;
   }

   /**
    * Processes the actual operation.
    *
    * @param params the array of Effect parameters specified in the workbench.
    *           Not used in this effect.
    * @param req the callers request context.
    * @param excontext the execution context specifies which event is being
    *           processed.
    * @param result the result block determines whether the event has been
    *           handled successfully.
    *
    * @see com.percussion.relationship.IPSEffect#attempt(java.lang.Object[],
    *      com.percussion.server.IPSRequestContext,
    *      com.percussion.relationship.IPSExecutionContext,
    *      com.percussion.relationship.PSEffectResult)
    */
   public void attempt(Object[] params, IPSRequestContext req,
         IPSExecutionContext excontext, PSEffectResult result)
         throws PSExtensionProcessingException, PSParameterMismatchException
   {
      try
      {
         if (isExclusive(req))
         {
            m_log.debug("ATTEMPT = exclusion flag detected");
            result.setSuccess();
            return;
         }

         ms_config = PSNavConfig.getInstance(req);

         PSRelationship currRel = excontext.getCurrentRelationship();

         PSNavRelationshipInfo currentInfo;
         try
         {
            currentInfo = new PSNavRelationshipInfo(currRel, req);
         }
         catch (Exception ex)
         {
            m_log.warn("Unable to load relationship info rid is "
                  + currRel.getId(), ex);
            result.setSuccess();
            return;
         }

         String operation = String.valueOf(excontext.getContextType());
         m_log.debug("Attempt Current " + currentInfo.toString() + "\n Operation "
               + operation);
         result.setSuccess();
         if (excontext.isPostConstruction())
         {
            handleAttemptNew(req, currentInfo, result);
         }
         else if (excontext.isPostDestruction())
         {
            handleAttemptDestroy(req, currentInfo);
         }
      }
      catch (Exception ex)
      {
         m_log.error(this.getClass().getName(), ex);

         result.setError(new PSExtensionProcessingException(this.getClass()
               .getName(), ex));
      }

      return;
   }

   /**
    * Handles the <code>test</code> event when a new item has been created.
    *
    * @param req the parent request context, assumed not <code>null</code>.
    * @param currentInfo the information about the relationship being processed,
    *    assumed not <code>null</code>.
    * @param result the result objects to return to the caller, assumed not
    *    <code>null</code>.
    * @throws PSNavException when any error occurs.
    */
   private void handleTestNew(IPSRequestContext req,
      PSNavRelationshipInfo currentInfo, PSEffectResult result)
      throws PSNavException
   {
      PSComponentSummary dependent = currentInfo.getDependent();
      if (dependent.isItem() && PSNavUtil.isNavType(req, dependent))
      {
         m_log.debug("test inserting new navon");

         PSComponentSummary owner = currentInfo.getOwner();
         PSComponentSummary currentNavon =
            PSNavFolderUtils.getChildNavonSummary(req, owner);
         if (currentNavon != null)
         {
            PSNavConfig config = PSNavConfig.getInstance();

            String folderName = owner.getName();
            String currentItemName = currentNavon.getName();
            String currentItemType = PSNavUtil.isNavonItem(req, currentNavon) ?
               config.getPropertyString(PSNavConfig.NAVON_CONTENT_TYPE) :
               config.getPropertyString(PSNavConfig.NAVTREE_CONTENT_TYPE);

            String[] args =
            {
               folderName,
               currentItemName,
               currentItemType,
               config.getPropertyString(PSNavConfig.NAVON_CONTENT_TYPE),
               config.getPropertyString(PSNavConfig.NAVTREE_CONTENT_TYPE)
            };

            MessageFormat formatter = new MessageFormat(MSG_ALREADY_EXISTS);
            String errMsg = formatter.format(args);
            m_log.warn(errMsg);
            result.setError(errMsg);
            return;
         }
         else
            m_log.debug("no child navon found in folder " + owner.getName());
      }
      else
      {
         m_log.debug("ignore this event");
         result.setRecurseDependents(false);
      }

      result.setSuccess();
   }

   /**
    * Handles the <code>attempt</code> method when a new item has been
    * created.
    *
    * @param req the parent request
    * @param currentInfo information about the relationship that caused this
    *           event.
    * @param result the result block to return to the caller.
    * @throws PSNavException when an error occurs.
    */
   private void handleAttemptNew(IPSRequestContext req,
         PSNavRelationshipInfo currentInfo, PSEffectResult result)
         throws PSNavException
   {

      m_log.debug("Entering handleAttemptNew....");

      PSComponentSummary dependent = currentInfo.getDependent();
      if (dependent.isFolder())
      {
         m_log.debug("isFolder....");

         handleAttemptNewFolder(req, currentInfo, result);
      }
      else
      {
         long depType = dependent.getContentTypeId();
         
    	 int dependType = (int) depType;
         if (dependType == ms_config.getNavonType())
         {
            m_log.debug("isNavon....");

            handleAttemptNewNavon(req, currentInfo, result);
         }
         else if (dependType == ms_config.getNavTreeType())
         {
            m_log.debug("isNavTree....");

            handleAttemptNewNavTree(req, currentInfo, result);
         }
         else
         {
            m_log.debug("is potential landing page....");
            // check that a properties file was found
            if (m_landingPageProps != null) {

              /* check the config data to see if this item is a type
                 that could be added as a landing page */
                Integer contentTypeId = new Integer (dependType);
                
                IPSGuid guid = dependent.getContentTypeGUID();
                List ctList = Collections.singletonList(guid);
                m_log.debug("the guid of the content type of the item being created is: " + guid);
                
                String contentTypeName = "";
             
                try
                {              	
                	IPSContentTypeMgr ctmgr = PSContentMgrLocator.getContentMgr();
                	m_log.debug("created content type manager...");
                	//Load the content type using the Content Type ID provided
                	List<IPSNodeDefinition> ctNodes = ctmgr.loadNodeDefinitions(ctList);
                	m_log.debug("loaded the node definitions for cts...");
                	for (int i = 0; i < ctNodes.size(); i++)
                	{
                		IPSNodeDefinition contentTypeNode = ctNodes.get(i);
                		m_log.debug("iteration " + i + " through the CT nodes: ctInternalName: " 
                				+ contentTypeNode.getInternalName() + " -- ctName: " + contentTypeNode.getName()
                				+ " -- ct:Label: " + contentTypeNode.getLabel());
                		
                		//Find the name of the Content Type if it is the same as the one in the config file
                		if (contentTypeNode.getGUID().longValue() == guid.longValue())
                		{
                			contentTypeName = contentTypeNode.getInternalName();
                			m_log.debug("content type with id (" + guid.getUUID() + ")'s name is: " + contentTypeName);
                		}
                	}
                }
                catch(RepositoryException cex)
                {
                	m_log.debug("Exception while accessing repository to load Node definitions... error: " + cex.fillInStackTrace());
                }
                
                //Get the variant from the config data
                String varName = (String) m_landingPageProps.get(contentTypeId);
                m_log.debug("variantname: " + varName);
                
                if (varName != null && contentTypeName != "" ) {
                  m_log.debug("content type is potential landing page");
                  handleAttemptNewLandingPage(req, currentInfo, result, varName, contentTypeName);

              } else {

                m_log.debug("ignore this event, not Navon, NavTree or potential landing page");
                result.setSuccess();

              }
            }
         }
      }

      m_log.debug("Exiting handleAttemptNew....");

   }

   /**
    * Called when the new item being inserted is a potential new landing page.
    *
    * @param req the parent request context
    * @param currentInfo information about the relationship that caused this
    *           event.
    * @param result the result block to return to the caller.
    * @param templateName the name of the template used to get the AssemblyTemplate
    * @param contentTypeName then name of the contenttype used to load the Assembly Templates
    * @throws PSNavException when an error occurs.
    */
   private void handleAttemptNewLandingPage(IPSRequestContext req,
         PSNavRelationshipInfo currentInfo, PSEffectResult result, String templateName, String contentTypeName)
         throws PSNavException
   {
      m_log.debug("Entering handleAttemptNewLandingPage....");

      PSComponentSummary folder = currentInfo.getOwner();
      PSComponentSummary navon = PSNavFolderUtils.getChildNavonSummary(req, folder);
      PSComponentSummary item = currentInfo.getDependent();
      PSLocator itemLoc = item.getCurrentLocator();

      /* check the number of items in the folder */
      // do I need to do this? - no!

      m_log.debug("does the folder contain a Navon?");

      /* check that the folder is Navon */
      if (navon == null)
      { //there's no navon
         m_log.debug("folder has no Navon");
         result.setSuccess();
         return; // we are done
      }

      m_log.debug("folder has a Navon");

      PSLocator navonLoc = navon.getCurrentLocator();

      m_log.debug("setup the RelationshipProcessorProxy");

      /* check if Navon nav_landing_page slot is populated */
      PSNavProxyFactory pf = PSNavProxyFactory.getInstance(req);
      PSRelationshipProcessorProxy relProxy = pf.getRelProxy();

      m_log.debug("setup the RelationshipFilter");

      PSRelationshipFilter filter = new PSRelationshipFilter();
      filter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_ACTIVE_ASSEMBLY);
      filter.setOwner(navonLoc);
      // standard nav_landing_page slotid - there is a better way to lok this up
      filter.setProperty("sys_slotid","510");
      filter.setCommunityFiltering(false);

      try
      {
        PSComponentSummaries resultSet = relProxy.getSummaries(filter, false);

        m_log.debug(resultSet.size() + " ComponentSummaries found");

        if (resultSet.size() == 0) {

          m_log.debug("no slot content found");

          PSORelationshipHelper helper = new PSORelationshipHelper(req);

          //Load the templates and select the IPSAssemblyTemplate with templateName
          IPSAssemblyWs aws = PSAssemblyWsLocator.getAssemblyWebservice();
          List<PSAssemblyTemplateWs> allTemplates = aws.loadAssemblyTemplates(templateName, contentTypeName);
          m_log.debug("loading template with templatename: " + templateName + " - contenttypeName: " + contentTypeName);

          IPSAssemblyTemplate assemblyTemplate = allTemplates.get(0).getTemplate();
          m_log.debug("got assembly template: " + assemblyTemplate.getName());
          
          IPSTemplateService ts = PSAssemblyServiceLocator.getAssemblyService();
          
          //Load the IPSTemplateSlot used as the landing page slot on the navon
          IPSTemplateSlot landingPageSlot = null; 
          try 
          {
        	  landingPageSlot = ts.findSlotByName("rffNavLandingPage");
        	  m_log.debug("got slot with name: " + landingPageSlot.getName());
          }
          catch (PSAssemblyException aex)
          {
        	  m_log.debug("The slot - rffNavLandingPage was not found: " + aex.fillInStackTrace());
          }

         //Create the relationship to add the new item to the navon into the appropriate slot using the right template
         PSAaRelationship aaRel = new PSAaRelationship(navonLoc, itemLoc, landingPageSlot, assemblyTemplate);
         m_log.debug("created relationship...");
         
         PSActiveAssemblyProcessorProxy aaProxy = pf.getAaProxy();
         
         m_log.debug("adding relationship to list...");
         PSAaRelationshipList aaList = new PSAaRelationshipList();
         
         m_log.debug("add to list " + aaList.toString());
         aaList.add(aaRel);

         //Add the relationship to the slot
         aaProxy.addSlotRelationships(aaList, -1);
         m_log.debug("item added to nav_landing_page slot");

        }

        }
        catch (PSCmsException ce)
        {
           throw new PSNavException(PSNavFolderUtils.class.getName(), ce);
        }

      flushall();

      m_log.debug("Exiting handleAttemptNewLandingPage....");
      result.setSuccess();
   }


   /**
    * Called when the new item being inserted is a folder.
    *
    * @param req the parent request context
    * @param currentInfo information about the relationship that caused this
    *           event.
    * @param result the result block to return to the caller.
    * @throws PSNavException when an error occurs.
    */
   private void handleAttemptNewFolder(IPSRequestContext req,
         PSNavRelationshipInfo currentInfo, PSEffectResult result)
         throws PSNavException
   {
      m_log.debug("inserting new folder");
      PSComponentSummary currentFolder = currentInfo.getDependent();
      PSComponentSummary parentFolder = currentInfo.getOwner();

      PSComponentSummary parentNavon = PSNavFolderUtils.getChildNavonSummary(
            req, parentFolder);
      if (parentNavon == null)
      { //there's no parent navon
         m_log.debug("parent folder has no Navon");
         result.setSuccess();
         return; // we are done
      }
      else
      {
         m_log.debug("parent navon is " + parentNavon.getName());
      }

      int parentCommunity = parentNavon.getCommunityId();
      m_log.debug("parent community is " + parentCommunity);
      PSComponentSummary currentNavon = PSNavFolderUtils.getChildNavonSummary(
            req, currentFolder);
      if (currentNavon == null)
      {
         setExclusive(req, true);
         m_log.debug("creating new Navon");
         currentNavon = PSNavFolderUtils.addNavonToFolder(req, currentFolder,
               parentCommunity);
         setExclusive(req, false);
      }
      else
      {
         PSNavFolderUtils.removeNavonParents(req, currentNavon
               .getCurrentLocator());
      }
      PSNavFolderUtils.addNavonSubmenu(req, parentNavon.getCurrentLocator(),
            currentNavon.getCurrentLocator());
      flushall();
   }

   /**
    * Called when the new item being inserted is a navon.
    *
    * @param req the parent request context
    * @param currentInfo information about the relationship that caused this
    *           event.
    * @param result the result block to return to the caller.
    * @throws PSNavException when an error occurs.
    */
   private void handleAttemptNewNavon(IPSRequestContext req,
         PSNavRelationshipInfo currentInfo, PSEffectResult result)
         throws PSNavException
   {
      m_log.debug("inserting new navon");
      PSComponentSummary folder = currentInfo.getOwner();
      PSComponentSummary navon = currentInfo.getDependent();
      m_log
            .debug("navon is "
                  + String.valueOf(navon.getCurrentLocator().getId()) + " rev "
                  + String.valueOf(navon.getCurrentLocator().getRevision()));
      PSComponentSummary myParent = PSNavFolderUtils.getParentFolder(req,
            folder);
      if (myParent != null)
      {
         PSNavFolder parentNavFolder = PSNavFolderUtils.getNavParentFolder(req,
               myParent, false);

         if (parentNavFolder != null)
         { //There is a Navon in the folder above this one
            m_log.debug("found parent folder with Navon");
            PSNavFolderUtils.addNavonSubmenu(req, parentNavFolder
                  .getNavonSummary().getCurrentLocator(), navon
                  .getCurrentLocator());
         }
      }
      m_log
            .debug("navon is "
                  + String.valueOf(navon.getCurrentLocator().getId()) + " rev "
                  + String.valueOf(navon.getCurrentLocator().getRevision()));
      boolean propFlag = isPropagate(req, navon.getCurrentLocator());
      setExclusive(req, true);
      PSNavFolderUtils.processSubFolders(req, folder, navon, propFlag);
      setExclusive(req, false);

      flushall();
      m_log.debug("done with new Navon");
      result.setSuccess();
   }

   /**
    * Called when the new item being inserted is a NavTree.
    *
    * @param req the parent request context
    * @param currentInfo information about the relationship that caused this
    *           event.
    * @param result the result block to return to the caller.
    * @throws PSNavException when an error occurs.
    */
   private void handleAttemptNewNavTree(IPSRequestContext req,
         PSNavRelationshipInfo currentInfo, PSEffectResult result)
         throws PSNavException
   {
      PSComponentSummary folder = currentInfo.getOwner();
      PSComponentSummary navon = currentInfo.getDependent();
      m_log.debug("inserting new navtree");
      
      m_log.debug("checking propogate...");
      boolean propFlag = isPropagate(req, navon.getCurrentLocator());
      m_log.debug("Using propagate: " + propFlag);
      
      setExclusive(req, true);
      m_log.debug("Processing subfolders...");
      PSNavFolderUtils.processSubFolders(req, folder, navon, propFlag);
      m_log.debug("Done with subfoldres...");
      setExclusive(req, false);

      flushall();
      m_log.debug("done with new navtree");
      result.setSuccess();
   }

   /**
    * Handles the <code>attempt</code> method for any destroy event.
    *
    * @param req the parent request, assumed not <code>null</code>.
    * @param currentInfo information about the current relationship, assumed
    *    not <code>null</code>.
    * @throws PSNavException when any error occurs.
    */
   private void handleAttemptDestroy(IPSRequestContext req,
      PSNavRelationshipInfo currentInfo) throws PSNavException
   {
      PSNavConfig config = PSNavConfig.getInstance(req);

      PSComponentSummary parentFolder = currentInfo.getOwner();
      PSComponentSummary dependent = currentInfo.getDependent();
      if (dependent.getType() == PSComponentSummary.TYPE_FOLDER)
      {
         m_log.debug("removing folder from folder");
         PSComponentSummary parentNavon = PSNavFolderUtils.getChildNavonSummary(
            req, parentFolder);
         if (parentNavon == null)
         {
            m_log.debug("parent folder has no Navon");
            return;
         }
         else
            m_log.debug("parent navon is " + parentNavon.getName());

         PSComponentSummary navon = PSNavFolderUtils.getChildNavonSummary(req,
            dependent);
         if (navon == null)
         {
            m_log.debug("child Navon not found");
            return;
         }
         else
            m_log.debug("child Navon is " + navon.getName());

         PSNavFolderUtils.removeNavonChild(req, parentNavon.getCurrentLocator(),
            navon.getCurrentLocator());
      }
      else if (dependent.getContentTypeId() == config.getNavonType())
      {
         m_log.debug("removing Navon from folder");
         m_log.debug("child navon is " + dependent.getName());

         PSNavFolderUtils.removeNavonParents(req,
            dependent.getCurrentLocator());
      }

      flushall();
   }

   /**
    * Flushes the assembly cache. This method is here primarily to encapsulate
    * any exceptions thrown from the <code>PSCacheProxy</code>.
    *
    * @throws PSNavException
    */
   private void flushall() throws PSNavException
   {
      try
      {
         PSCacheProxy.flushAssemblers(null, null, null, null);
        //PSCacheProxy.flushFolderCache(); 
      }
      catch (Exception ex)
      {
         throw new PSNavException(ex);
      }
      m_log.debug("cache flushed");
   }

   /**
    * Determines if this Navon or NavTree has the "propagate" flag set.
    *
    * @param req the parent request context.
    * @param navonLoc the locator for the current navon.
    * @return <code>true</code> if the propagate check box was checked.
    * @throws PSNavException when any error occurs.
    */
   private boolean isPropagate(IPSRequestContext req, PSLocator navonLoc)
         throws PSNavException
   {
      PSNavConfig config = PSNavConfig.getInstance(req);
      m_log.debug("getting nav component summary...");
      try
      {
         PSComponentSummary navonSummary = PSNavUtil.getItemSummary(req,
               navonLoc);
         m_log.debug("got component summary id: " + navonSummary.getContentId());
//       The next line needs to be modified to get the JSR node instead of XML 
//		 Document tdoc = PSNavUtil.getNavonDocument(req, navonSummary);
         
         IPSGuid guid = guidMgr.makeGuid(navonLoc);
         m_log.debug("created the guid");
         
         PSContentMgrConfig cmgrConfig = new PSContentMgrConfig();
         
         //List<IPSGuid> finditems = new Collections().singletonList(guid);
         List<IPSGuid> finditems = Collections.singletonList(guid);
         
         //finditems.add(guid);
         
         List<Node> items = cmgr.findItemsByGUID(finditems, cmgrConfig);
         m_log.debug("found the item of id:" + navonLoc.getId());
         
         Node theitem = items.get(0);
         
         String propValue;

         m_log.debug("using rx:no_propagate");
         propValue = theitem.getProperty("rx:no_propagate").getString();
         m_log.debug("got nav propfield: " + propValue);
         
         if (propValue != null)
         {
            m_log.debug("propagate field returns " + propValue);
            if (propValue.equalsIgnoreCase("1"))
            {
               m_log.debug("propagate is TRUE");
               return true;
            }
         }
      }
      catch (PSNavException e)
      {	 m_log.debug("found the exception...");
         throw (PSNavException) e.fillInStackTrace();
      }
      catch (Exception ex)
      {
         m_log.error(this.getClass(), ex);
         throw new PSNavException(this.getClass().getName(), ex);
      }
      return false;
   }


   /**
    * Load the landing page combinations from the file.
    * @return the landing page combinations as an object.
    * @throws IOException when there is an error reading the file
    * @throws FileNotFoundException when the file does not exist
    */
   private HashMap getLandingPageCombinations()
      throws
         PSExtensionException,
         SAXException,
         IOException,
         ParserConfigurationException
   {
      m_log.debug("Entering getLandingPageCombinations....");

      HashMap rval = null;

      DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = fact.newDocumentBuilder();
      Document configfile = null;

      try {
        configfile = builder.parse(LPC_CONFIG_FILE);
      } catch (IOException ie) {
        m_log.debug("Cannot find the landing page properties file....");
      }

      if (configfile != null) {

        m_log.debug("DocumentBuilder created....");

        // Find child elements
        NodeList elements = configfile.getElementsByTagName(LPC_COMBINATION);


        // Each node should have two subelements
        int len = elements.getLength();

        m_log.debug("found " + len + " child elements....");

        rval = new HashMap(len);
        //int[] rval = new int[len];

        for (int i = 0; i < len; i++)
        {

           m_log.debug("looping through elements....");

           Element el = (Element) elements.item(i);
           Element type =
              (Element) PSXMLDomUtil.findFirstNamedChildNode(el, TYPE);
           Element variant =
              (Element) PSXMLDomUtil.findFirstNamedChildNode(el, VARIANT);

           if (type == null)
           {
              throw new PSExtensionException(
                 0,
                 "Missing type in specification file");
           }

           if (variant == null)
           {
              throw new PSExtensionException(
                 0,
                 "Missing variant in specification file");
           }

           m_log.debug("created Element(s)....");

           Integer contentTypeId = null;
           String templateName = null;

           try
           {
              contentTypeId = new Integer (PSXMLDomUtil.getElementData(type));
              //contentTypeId =
              //   Integer.parseInt(PSXMLDomUtil.getElementData(type));
           }
           catch (Throwable th)
           {
              throw new PSExtensionException(
                 IPSExtensionErrors.BAD_PUBLISH_CONTENT_FILE_DATA,
                 "Error while parsing value for workflow id");
           }

           m_log.debug("contentTypeid: " + contentTypeId);

           try
           {
              templateName = new String (PSXMLDomUtil.getElementData(variant));
              //variantId = Integer.parseInt(PSXMLDomUtil.getElementData(variant));
           }
           catch (Throwable th)
           {
              throw new PSExtensionException(
                 IPSExtensionErrors.BAD_PUBLISH_CONTENT_FILE_DATA,
                 "Error while parsing value for the variant id");
           }

           m_log.debug("variantId: " + templateName);

           ///rval[contentTypeId] = variantId;
           rval.put(contentTypeId, templateName);

           m_log.debug("added to HashMap...");

        }

      }

      m_log.debug("Exiting getLandingPageCombinations....");

      return rval;
   }

   /**
    * The relative path of the transitionValidate.xml file.
    */
   private static final String LPC_CONFIG_FILE =
      "rxconfig/Server/landingPageCombinations.xml";

   /*
    * Defined strings to search for child nodes
    */
   private static final String LPC_COMBINATION = "PSONavFolderEffect";
   private static final String TYPE = "PSXContentTypeId";
   private static final String VARIANT = "PSXTemplateName";

   /**
    * the configuration object for the navigation system.
    */
   static private PSNavConfig ms_config;

   /**
    * This message is displayed to the end-user if he tries to add a
    * <code>Navon</code> or <code>NavonTree</code> item to a folder which
    * already contains an object of either type.
    */
   private static final String MSG_ALREADY_EXISTS = "Folder \"{0}\" already " +
      "contains item \"{1}\" of type \"{2}\". Items of type \"{3}\" or " +
      "\"{4}\" cannot co-exist in a Folder.";;
}