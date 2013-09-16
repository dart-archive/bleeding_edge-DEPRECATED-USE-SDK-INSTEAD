/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile.validator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.extension.RegistryReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Based off of TransferBuilder. Reads the extension point for
 * <code>org.eclipse.wst.sse.ui.extensions.sourcevalidation</code>
 */
public class ValidatorBuilder extends RegistryReader {

  public static final String ATT_CLASS = "class"; //$NON-NLS-1$
  public static final String ATT_ID = "id"; //$NON-NLS-1$
  public static final String ATT_SCOPE = "scope"; //$NON-NLS-1$

  public static final ValidatorMetaData[] EMTPY_VMD_ARRAY = new ValidatorMetaData[0];

  // extension point ID
  public static final String PL_SOURCE_VALIDATION = "sourcevalidation"; //$NON-NLS-1$

  public static final String PLUGIN_ID = "org.eclipse.wst.sse.ui"; //$NON-NLS-1$
  public static final String TAG_CONTENT_TYPE_IDENTIFIER = "contentTypeIdentifier"; //$NON-NLS-1$
  public static final String TAG_PARTITION_TYPE = "partitionType"; //$NON-NLS-1$

  public static final String TAG_VALIDATOR = "validator"; //$NON-NLS-1$

  public static final String TRACE_FILTER = "reconcile_validator"; //$NON-NLS-1$

  public static final String TRUE = "true"; //$NON-NLS-1$
  private String fCurrentCTID;
  private ValidatorMetaData fCurrentVMD = null;
  private List fVmds = new ArrayList();
  protected String targetContributionTag;
  protected String targetID;

  private final String UNKNOWN = "???"; //$NON-NLS-1$

  /**
   * Returns the name of the part ID attribute that is expected in the target extension.
   * 
   * @param element
   * @return String
   */
  protected String getID(IConfigurationElement element) {
    String value = element.getAttribute(ATT_ID);
    return value != null ? value : UNKNOWN; //$NON-NLS-1$
  }

  protected String getValidatorClass(IConfigurationElement element) {
    String value = element.getAttribute(ATT_CLASS);
    return value != null ? value : UNKNOWN; //$NON-NLS-1$
  }

  /**
   * @param editorId
   * @return Transfer[]
   */
  public ValidatorMetaData[] getValidatorMetaData(String editorId) {
    readContributions(editorId, TAG_VALIDATOR, PL_SOURCE_VALIDATION);
    return (ValidatorMetaData[]) fVmds.toArray(new ValidatorMetaData[fVmds.size()]);
  }

  protected String getValidatorScope(IConfigurationElement element) {
    String value = element.getAttribute(ATT_SCOPE);
    return value != null ? value : UNKNOWN; //$NON-NLS-1$
  }

  /**
   * Reads the contributions from the registry for the provided workbench part and the provided
   * extension point ID.
   * 
   * @param id
   * @param tag
   * @param extensionPoint
   */
  protected void readContributions(String id, String tag, String extensionPoint) {
    targetID = id;
    targetContributionTag = tag;
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    readRegistry(registry, PLUGIN_ID, extensionPoint);
  }

  protected boolean readElement(IConfigurationElement element) {
    String tag = element.getName();
    //ie. targetContributionTag == validator
    if (tag.equals(targetContributionTag)) {
      String vId = getID(element);
      String vClass = getValidatorClass(element);
      String vScope = getValidatorScope(element);

      if (vId == null) {
        // This is not of interest to us - don't go deeper
        return true;
      }
      // start building a VMD
      fCurrentVMD = new ValidatorMetaData(element, vId, vClass, vScope);
      fVmds.add(fCurrentVMD);

      if (Logger.isTracing(ValidatorBuilder.TRACE_FILTER))
        System.out.println("added reconcile validator: " + vId + ":" + vClass + ":" + vScope); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    } else if (tag.equals(TAG_CONTENT_TYPE_IDENTIFIER)) {
      // add to current VMD
      fCurrentCTID = getID(element);
      fCurrentVMD.addContentTypeId(fCurrentCTID);
    } else if (tag.equals(TAG_PARTITION_TYPE)) {
      // add to current VMD
      String partitionType = getID(element);
      fCurrentVMD.addParitionType(fCurrentCTID, partitionType);

      return true;
    } else {
      return false;
    }

    readElementChildren(element);
    return true;
  }
}
