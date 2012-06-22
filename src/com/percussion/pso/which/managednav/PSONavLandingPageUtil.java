package com.percussion.pso.which.managednav;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import com.percussion.cms.objectstore.PSContentTypeVariant;
import com.percussion.cms.objectstore.PSContentTypeVariantSet;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.PSRelationshipProcessorProxy;
import com.percussion.cms.objectstore.PSSlotType;
import com.percussion.cms.objectstore.PSSlotTypeSet;
import com.percussion.consulting.utils.PSORelationshipHelper;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.IPSRequestPreProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.fastforward.managednav.PSNavConfig;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.fastforward.managednav.PSNavProxyFactory;
import com.percussion.pso.which.ce.PSOPageBreakCounter;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequestValidationException;
import com.percussion.util.PSXMLDomUtil;

/**
 * A utility class for adding content to the Navon nav_landing_page slot after
 * it has been created in the folder.
 * <p>
 *
 */

public class PSONavLandingPageUtil extends PSDefaultExtension implements
		IPSRequestPreProcessor {

	private HashMap m_landingPageProps = null;

	Logger m_log = null;

	/**
	 * Initializes the class.
	 *
	 * @see com.percussion.extension.IPSExtension#init(com.percussion.extension.IPSExtensionDef,
	 *      java.io.File)
	 */
	public void init(IPSExtensionDef arg0, File arg1)
			throws PSExtensionException {

		m_log = Logger.getLogger(this.getClass());

		// have to initialise this here as Eclipse/Ant complain otherwise
		// don't ask me why as it compiles fine normally!!!!
		try {
			ms_config = PSNavConfig.getInstance();
		} catch (PSNavException ne) {
			throw new PSExtensionException("Cannot get Nav Config", ne
					.getMessage());
		}

		m_log.info("intialising..." + this.getClass().getName());

		try {
			m_landingPageProps = getLandingPageCombinations();
		} catch (PSExtensionException pse) {
			// Rethrow
			throw pse;
		} catch (Exception ex) {
			throw new PSExtensionException(
					IPSExtensionErrors.BAD_PUBLISH_CONTENT_INITIALIZATION_DATA,
					ex.getMessage());
		}

		m_log.info("intialised..." + this.getClass().getName());

	}

	public void preProcessRequest(Object[] params, IPSRequestContext request)
			throws PSAuthorizationException, PSRequestValidationException,
			PSParameterMismatchException, PSExtensionProcessingException {

		m_log.info("entering preProcessRequest....");
		String folderParam = getParameter(params, 0);
		int folderId = Integer.parseInt(folderParam);

		PSLocator rootFolderLocator = new PSLocator(folderId);

		String actionParam = "info"; 
		actionParam = getParameter(params, 1);
		
		// call the method to process the folders
		processFolder(rootFolderLocator, actionParam, request);
		m_log.info("exiting preProcessRequest....");

	}

	protected void processFolder(PSLocator folder, String actionParam, IPSRequestContext request) {

		m_log.info("entering processFolder....");

		//Create New Relationship filter
		PSRelationshipFilter relFilter = new PSRelationshipFilter();
		m_log.debug("Initialising Relationship Filter");

		relFilter.setOwner(folder);
		m_log.debug("Set OwnerID of Relationship Filter: " + folder);

		relFilter.setName(PSRelationshipFilter.FILTER_NAME_FOLDER_CONTENT);
		m_log.debug("Set Relationship Filter Type: "
				+ PSRelationshipFilter.FILTER_NAME_FOLDER_CONTENT);

		relFilter.setCommunityFiltering(false);
		m_log.debug("Set Community Filtering = Off");

		try {
			//Initialise PSRelationshipProcessorProxy
			PSRelationshipProcessorProxy m_relProxy = null;
			m_relProxy = new PSRelationshipProcessorProxy(
					PSRelationshipProcessorProxy.PROCTYPE_SERVERLOCAL, request);

			PSComponentSummaries summary = m_relProxy.getSummaries(relFilter,
					false);
			int itemCount = summary.size();
			m_log.info("No. of Items in the folder of ID:" + folder.getId()
					+ " : " + itemCount);

			if (itemCount > 0) {

				//Get an Iterator of Component Summaries of returned search Items
				Iterator items = summary.getSummaries();

				Object item = null;
				PSComponentSummary itemPCS = null;
				int itemContentTypeId;

				List navonItems = new ArrayList();
				List folderItems = new ArrayList();
				List contentItems = new ArrayList();

				int navonItemCount = 0;
				int folderItemCount = 0;
				int contentItemCount = 0;

				// Iterate through the folder items
				while (items.hasNext()) {

					m_log.debug("iterating through the folder items...");

					item = items.next(); // Next item as an Object.
					itemPCS = (PSComponentSummary) item; // Type-cast item to a PSComponentSummary
					long itemCtId = itemPCS.getContentTypeId();
					itemContentTypeId = (int) itemCtId;

					m_log.debug("--> Item ID: " + itemPCS.getContentId()); //
					m_log.debug("--> Item ContentType ID: "	+ itemContentTypeId);

					if (itemContentTypeId == ms_config.getNavonType()
							|| itemContentTypeId == ms_config.getNavTreeType()) {

						// the item is a navon or navtree
						m_log.debug("folder item is a navon...");
						navonItems.add(item);

					} else if (itemPCS.isFolder() == true) {

						// the items is a folder
						m_log.debug("folder item is a folder...");
						folderItems.add(item);

					} else {

						// the item is 'content' 
						m_log.debug("folder item is content...");
						PSOComponentSummaryWrapper wrapper = new PSOComponentSummaryWrapper(
								itemPCS);
						contentItems.add(wrapper);

					}

				}

				navonItemCount = navonItems.size();
				folderItemCount = folderItems.size();
				contentItemCount = contentItems.size();

				m_log.info(navonItemCount + " navon items found");
				m_log.info(folderItemCount + " folders found");
				m_log.info(contentItemCount + " content items found");
				
				// tidy up variables that will be re-used
				itemContentTypeId = 0;
				summary = null;

				if (navonItemCount > 0 && contentItemCount > 0) {

					// sort the content items into ascending date order
					m_log.debug("sort the content items...");
					Collections.sort(contentItems);

					// find out if the nav_landing_page slot is empty
					item = null;
					Iterator iNavonItems = navonItems.iterator();
					PSLocator navonLoc = null;
					PSNavProxyFactory pf = null;
					boolean landingPage = true; // if no nav content is found then we skip the processing

					while (iNavonItems.hasNext()) {

						m_log.info("iterating through the navon items...");

						item = iNavonItems.next(); // Next item as an Object.
						itemPCS = (PSComponentSummary) item; // Type-cast item to a PSComponentSummary 	

						navonLoc = itemPCS.getCurrentLocator();

						m_log.debug("setup the RelationshipProcessorProxy");

						/* check if Navon nav_landing_page slot is populated */
						try {
							pf = PSNavProxyFactory.getInstance(request);
						} catch (PSNavException ne) {
							m_log.info("Error with NavProxyFactory");
						}
						m_relProxy = pf.getRelProxy();

						m_log.debug("setup the RelationshipFilter");

						PSRelationshipFilter filter = new PSRelationshipFilter();
						filter
								.setCategory(PSRelationshipFilter.FILTER_CATEGORY_ACTIVE_ASSEMBLY);
						filter.setOwner(navonLoc);
						// standard nav_landing_page slotid - there is a better way to look this up
						filter.setProperty("sys_slotid", "310");
						filter.setCommunityFiltering(false);

						summary = m_relProxy.getSummaries(filter, false);

						m_log.debug(summary.size()
								+ " ComponentSummaries found");

						if (summary.size() == 0) {

							m_log.info("NO NAV_LANDING_PAGE SLOT CONTENT FOUND");
							landingPage = false;

						} else {

							m_log.info("NAV_LANDING_PAGE SLOT CONTENT FOUND");
							landingPage = true;

						}

					}

					
					PSOComponentSummaryWrapper itemPCSW = null;

					if (landingPage == false) {
						//	if the nav_landing_page slot is empty, then find any candidate content

						m_log.info("is potential landing page?....");
						// check that a properties file was found

						if (m_landingPageProps != null) {

							// loop through the content items in the folder
							// these have been sorted into the 'natural' ascending order
							Iterator iContentItems = contentItems.iterator();

							while (iContentItems.hasNext()) {

								m_log.info("iterating through folder content....");

								item = iContentItems.next(); // Next item as an Object.
								itemPCSW = (PSOComponentSummaryWrapper) item; // Type-cast item to a PSComponentSummary
								long itemCtId = itemPCSW.getItem().getContentTypeId();
								itemContentTypeId = (int) itemCtId;
								Integer iContentTypeId = new Integer(
										itemContentTypeId);

								m_log.info("--> Item ID: "
										+ itemPCSW.getItem().getContentId());
								m_log.info("--> Item ContentType ID: "
										+ itemContentTypeId);
								m_log.info("--> Item Content Created Date: "
										+ itemPCSW.getItem()
												.getContentCreatedDate());

								/* check the config data to see if this item is a type
								 that could be added as a landing page */
								Integer varId = (Integer) m_landingPageProps
										.get(iContentTypeId);

								if (varId != null) {

									int variantId = varId.intValue();
									PSLocator itemLoc = itemPCSW.getItem()
											.getCurrentLocator();

									m_log.info("variantid: " + variantId);
									m_log.info("content type is potential landing page");
									
									if (actionParam.equals("execute")) {
										
										m_log.info("attempting to add content to slot");

										try {
	
											// add the item to the slot
											PSORelationshipHelper helper = new PSORelationshipHelper(
													request);
	
											// need navLinkVariant (PSContentTypeVariant) and landingPageSlot (PSSlotType)
											PSSlotTypeSet slotSet = helper
													.getSlotSet();
	
											PSSlotType landingPageSlot = slotSet
													.getSlotTypeByName("nav_landing_page");
	
											PSContentTypeVariantSet variantSet = helper
													.getVariantSet();
	
											PSContentTypeVariant navLinkVariant = variantSet
													.getContentVariantById(variantId);
	
											PSRelationshipConfig aaConfig = m_relProxy
													.getConfig(PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
	
											/* populate with the nav_landing_page slot with the content item being added to the folder */
											PSAaRelationship aaRel = new PSAaRelationship(
													navonLoc, itemLoc,
													landingPageSlot,
													navLinkVariant, aaConfig);
											PSActiveAssemblyProcessorProxy aaProxy = pf
													.getAaProxy();
	
											PSAaRelationshipList aaList = new PSAaRelationshipList();
											m_log.debug("add to list " + aaList.toString());
											aaList.add(aaRel);
											 
											aaProxy.addSlotRelationships(aaList, -1);
											m_log.info("ITEM ADDED TO NAV_LANDING_PAGE SLOT");
	 
										} catch (PSUnknownNodeTypeException unte) {
											m_log
													.info("Error with RelationshipHelper");
										}
										
									} else {
										
										m_log.info("ITEM WOULD BE ADDED TO NAV_LANDING_PAGE SLOT HERE");
										
									}

									// break out of the while clause
									// we have completed out work here
									break;

								} else {

									m_log.info("ignore this event, not Navon, NavTree or potential landing page");

								}

							}

						}

					}
					
					m_relProxy = null;

				}

				if (folderItemCount > 0) {

					// finally find any child folders and check these
					item = null;
					Iterator iFolderItems = folderItems.iterator();

					while (iFolderItems.hasNext()) {

						m_log.info("iterating through the folder items...");

						item = iFolderItems.next(); // Next item as an Object.
						itemPCS = (PSComponentSummary) item; // Type-cast item to a PSComponentSummary

						m_log.info("--> Item ID: " + itemPCS.getContentId()); //
						m_log.info("--> Item ContentType ID: "
								+ itemPCS.getContentTypeId());

						// recurse by calling this method with a new folder locator
						PSLocator childFolder = itemPCS.getCurrentLocator();
						m_log.info("found child, about to recurse...");
						processFolder(childFolder, actionParam, request);
						m_log.info("child recursion finished...");

					}

				}
				
				navonItemCount = 0;
				folderItemCount = 0;
				contentItemCount = 0;

			}

		} catch (PSCmsException e) {
			m_log.info("Error with Relationship Filter");
		}

		m_log.info("exiting processFolder....");
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
			throws PSParameterMismatchException {
		if (params.length < index + 1 || null == params[index]
				|| params[index].toString().trim().length() == 0) {
			throw new PSParameterMismatchException(PSOPageBreakCounter.class
					+ ": Missing exit parameter");
		} else {
			return params[index].toString().trim();
		}
	}

	/**
	 * Load the landing page combinations from the file.
	 * @return the landing page combinations as an object.
	 * @throws IOException when there is an error reading the file
	 * @throws FileNotFoundException when the file does not exist
	 */
	private HashMap getLandingPageCombinations() throws PSExtensionException,
			SAXException, IOException, ParserConfigurationException {
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
			NodeList elements = configfile
					.getElementsByTagName(LPC_COMBINATION);

			// Each node should have two subelements
			int len = elements.getLength();

			m_log.debug("found " + len + " child elements....");

			rval = new HashMap(len);
			//int[] rval = new int[len];

			for (int i = 0; i < len; i++) {

				m_log.debug("looping through elements....");

				Element el = (Element) elements.item(i);
				Element type = (Element) PSXMLDomUtil.findFirstNamedChildNode(
						el, TYPE);
				Element variant = (Element) PSXMLDomUtil
						.findFirstNamedChildNode(el, VARIANT);

				if (type == null) {
					throw new PSExtensionException(0,
							"Missing type in specification file");
				}

				if (variant == null) {
					throw new PSExtensionException(0,
							"Missing variant in specification file");
				}

				m_log.debug("created Element(s)....");

				Integer contentTypeId = null;
				Integer variantId = null;

				try {
					contentTypeId = new Integer(PSXMLDomUtil
							.getElementData(type));
					//contentTypeId =
					//   Integer.parseInt(PSXMLDomUtil.getElementData(type));
				} catch (Throwable th) {
					throw new PSExtensionException(
							IPSExtensionErrors.BAD_PUBLISH_CONTENT_FILE_DATA,
							"Error while parsing value for workflow id");
				}

				m_log.debug("contentTypeid: " + contentTypeId);

				try {
					variantId = new Integer(PSXMLDomUtil
							.getElementData(variant));
					//variantId = Integer.parseInt(PSXMLDomUtil.getElementData(variant));
				} catch (Throwable th) {
					throw new PSExtensionException(
							IPSExtensionErrors.BAD_PUBLISH_CONTENT_FILE_DATA,
							"Error while parsing value for the variant id");
				}

				m_log.debug("variantId: " + variantId);

				///rval[contentTypeId] = variantId;
				rval.put(contentTypeId, variantId);

				m_log.debug("added to HashMap...");

			}

		}

		m_log.debug("Exiting getLandingPageCombinations....");

		return rval;
	}

	private class PSOComponentSummaryWrapper implements Comparable {

		PSComponentSummary item = null;

		public PSOComponentSummaryWrapper(PSComponentSummary item) {

			m_log.debug("constructing PSOComponentSummaryWrapper...");

			this.item = item;

			m_log.debug("finished constructing PSOComponentSummaryWrapper...");
		}

		public PSComponentSummary getItem() {

			return item;

		}

		public int compareTo(Object o) {

			//m_log.debug("entering PSOComponentSummaryWrapper.compareTo()...");
			m_log.debug("item content created date: " + item.getContentCreatedDate());

			PSOComponentSummaryWrapper comp = null;
			try {
				comp = (PSOComponentSummaryWrapper) o;
			} catch (ClassCastException cce) {
				m_log
						.debug("ClassCastException within PSOComponentSummaryWrapper.compareTo()");
			}

			m_log.debug("comparison object content created date: " + comp.getItem().getContentCreatedDate());

			return item.getContentCreatedDate().compareTo(
					comp.getItem().getContentCreatedDate());
			//return item.getContentCreatedDate().compareTo(o);  

		}

	}

	/**
	 * The relative path of the transitionValidate.xml file.
	 */
	private static final String LPC_CONFIG_FILE = "rxconfig/Server/landingPageCombinations.xml";

	/*
	 * Defined strings to search for child nodes
	 */
	private static final String LPC_COMBINATION = "PSONavFolderEffect";

	private static final String TYPE = "PSXContentTypeId";

	private static final String VARIANT = "PSXVariantId";

	/**
	 * the configuration object for the navigation system.
	 */
	static private PSNavConfig ms_config;

}
