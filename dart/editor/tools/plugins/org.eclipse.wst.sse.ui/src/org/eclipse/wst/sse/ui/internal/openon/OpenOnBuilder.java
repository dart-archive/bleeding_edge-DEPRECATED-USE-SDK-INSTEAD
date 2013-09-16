/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.openon;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.sse.ui.internal.extension.RegistryReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads extensions for open on extension point, org.eclipse.wst.sse.ui.extensions.openon
 * 
 * @deprecated Use base support for hyperlink navigation
 */
public class OpenOnBuilder extends RegistryReader {
  public static final String ATT_CLASS = "class"; //$NON-NLS-1$

  public static final String ATT_ID = "id"; //$NON-NLS-1$

  private static OpenOnBuilder fInstance;
  // extension point ID
  public static final String PL_OPENON = "openon"; //$NON-NLS-1$

  public static final String PLUGIN_ID = "org.eclipse.wst.sse.ui"; //$NON-NLS-1$
  public static final String TAG_CONTENT_TYPE_IDENTIFIER = "contenttypeidentifier"; //$NON-NLS-1$

  public static final String TAG_OPENON = "openon"; //$NON-NLS-1$
  public static final String TAG_PARTITION_TYPE = "partitiontype"; //$NON-NLS-1$

  /**
   * returns singleton instance of OpenOnBuilder
   * 
   * @return OpenOnBuilder
   */
  public synchronized static OpenOnBuilder getInstance() {
    if (fInstance == null) {
      fInstance = new OpenOnBuilder();
    }
    return fInstance;
  }

  private String fCurrentContentType;
  private OpenOnDefinition fCurrentOpenOnDefinition = null;

  private List fOpenOnDefs = null;

  protected String targetContributionTag;

  /**
   * Returns the name of the part ID attribute that is expected in the target extension.
   * 
   * @param element
   * @return String
   */
  protected String getId(IConfigurationElement element) {
    String value = element.getAttribute(ATT_ID);
    return value;
  }

  protected String getOpenOnClass(IConfigurationElement element) {
    String value = element.getAttribute(ATT_CLASS);
    return value;
  }

  /**
   * Returns all the open on definition objects
   * 
   * @return
   */
  public OpenOnDefinition[] getOpenOnDefinitions() {
    initCache();
    return (OpenOnDefinition[]) fOpenOnDefs.toArray(new OpenOnDefinition[fOpenOnDefs.size()]);
  }

  /**
   * Returns all the open on definition objects valid for contentType/partitionType
   * 
   * @param contentType
   * @param partitionType
   * @return if either contentType or partitionType is null, null is returned
   */
  public OpenOnDefinition[] getOpenOnDefinitions(String contentType, String partitionType) {
    if (contentType == null || partitionType == null) {
      // should not be able to define an openon without a content type
      // but if it were possible then would need to search all openon
      // definitions for
      // definitions with empty contentType list
      return null;
    }

    // entire list of openon definition objects
    OpenOnDefinition[] allDefs = getOpenOnDefinitions();
    // current list of open on definitions valid for
    // contentType/partitionType
    List defs = new ArrayList();
    // default definitions that should be added to end of list of open on
    // definitions
    List lastDefs = new ArrayList();

    for (int i = 0; i < allDefs.length; ++i) {
      // for each one check if it contains contentType
      List partitions = (List) allDefs[i].getContentTypes().get(contentType);
      if (partitions != null) {
        // this openon definition is valid for all partition types for
        // this content type
        if (partitions.isEmpty()) {
          // this will be added to end of list because this is
          // considered a default openon
          lastDefs.add(allDefs[i]);
        } else {
          // examine the partition types of this openon
          int j = 0; // current index in list of partitions
          boolean added = false; // openon has been added to list
          while (j < partitions.size() && !added) {
            // this openon definition applies to partitionType so
            // add to list of valid openons
            if (partitionType.equals(partitions.get(j))) {
              defs.add(allDefs[i]);
              added = true;
            } else {
              // continue checking to see if this openon
              // definition is valid for current partitionType
              ++j;
            }
          }
        }
      }
    }
    // append the default openon definitions
    defs.addAll(lastDefs);

    // return the list
    return (OpenOnDefinition[]) defs.toArray(new OpenOnDefinition[defs.size()]);
  }

  private void initCache() {
    if (fOpenOnDefs == null) {
      fOpenOnDefs = new ArrayList(0);
      readContributions(TAG_OPENON, PL_OPENON);
    }
  }

  /**
   * Processes element which should be a configuration element specifying a content type for the
   * current open on tag. Assumes that there is a valid current open on definition object.
   * 
   * @param element contenttypeidentifier configuration element
   */
  private void processContentTypeTag(IConfigurationElement element) {
    // add to current openOnDefinition
    String theId = getId(element);

    if (theId != null) {
      fCurrentContentType = theId;
      fCurrentOpenOnDefinition.addContentTypeId(fCurrentContentType);
    } else {
      fCurrentContentType = null;
    }
  }

  /**
   * Processes element which should be a configuration element specifying an open on object. Creates
   * a new open on definition object and adds it to the list of open on definition objects
   * 
   * @param element openon configuration element
   */
  private void processOpenOnTag(IConfigurationElement element) {
    String theId = getId(element);
    String theClass = getOpenOnClass(element);

    if (theId != null && theClass != null) {
      // start building new OpenOnDefinition
      fCurrentOpenOnDefinition = new OpenOnDefinition(theId, theClass, element);
      fOpenOnDefs.add(fCurrentOpenOnDefinition);
    } else {
      fCurrentOpenOnDefinition = null;
    }
  }

  /**
   * Processes element which should be a configuration element specifying a partition type for the
   * current open on/content type tag. Assumes that there is a valid current open on/content type
   * tag.
   * 
   * @param element partitiontype configuration element
   */
  private void processPartitionTypeTag(IConfigurationElement element) {
    // add to current openOnDefinition/contentType
    String theId = getId(element);

    if (theId != null) {
      fCurrentOpenOnDefinition.addPartitionType(fCurrentContentType, theId);
    }
  }

  /**
   * Reads the contributions from the registry for the provided workbench part and the provided
   * extension point ID.
   * 
   * @param tag
   * @param extensionPoint
   */
  protected void readContributions(String tag, String extensionPoint) {
    targetContributionTag = tag;
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    readRegistry(registry, PLUGIN_ID, extensionPoint);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.extension.RegistryReader#readElement(org.eclipse.core.runtime
   * .IConfigurationElement)
   */
  protected boolean readElement(IConfigurationElement element) {
    String tag = element.getName();

    if (tag.equals(targetContributionTag)) {
      processOpenOnTag(element);

      // make sure processing of current open on tag resulted in a
      // current open on definition
      // before continue reading the children
      if (fCurrentOpenOnDefinition != null) {
        readElementChildren(element);
      }
      return true;
    } else if (tag.equals(TAG_CONTENT_TYPE_IDENTIFIER)) {
      processContentTypeTag(element);

      // make sure processing of current content type resulted in a
      // valid content type
      // before reading the children
      if (fCurrentContentType != null) {
        readElementChildren(element);
      }
      return true;
    } else if (tag.equals(TAG_PARTITION_TYPE)) {
      processPartitionTypeTag(element);
      return true;
    }

    return false;
  }
}
