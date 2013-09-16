/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.openon;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Open on definition object
 * 
 * @deprecated Use base support for hyperlink navigation
 */
public class OpenOnDefinition {
  private String fClassName = null;

  private IConfigurationElement fConfigurationElement = null;

  // a hash map of content type Ids (String) that points to lists of
  // parition types (List of Strings)
  // contentTypeId -> List(paritionType, paritionType, partitionType, ...)
  // contentTypeId2 -> List(partitionType, partitionType, ...)
  // ...
  private HashMap fContentTypes = null;
  private String fId = null;

  /**
   * @param id
   * @param class1
   * @param configurationElement
   */
  public OpenOnDefinition(String id, String class1, IConfigurationElement configurationElement) {
    super();
    fId = id;
    fClassName = class1;
    fConfigurationElement = configurationElement;
    fContentTypes = new HashMap();
  }

  public void addContentTypeId(String contentTypeId) {
    if (!fContentTypes.containsKey(contentTypeId))
      fContentTypes.put(contentTypeId, new ArrayList());
  }

  public void addPartitionType(String contentTypeId, String partitionType) {
    if (!fContentTypes.containsKey(contentTypeId))
      fContentTypes.put(contentTypeId, new ArrayList());

    List partitionList = (List) fContentTypes.get(contentTypeId);
    partitionList.add(partitionType);
  }

  /**
   * Creates an extension. If the extension plugin has not been loaded a busy cursor will be
   * activated during the duration of the load.
   * 
   * @param propertyName
   * @return Object
   */
  private Object createExtension(String propertyName) {
    // If plugin has been loaded create extension.
    // Otherwise, show busy cursor then create extension.
    final IConfigurationElement element = getConfigurationElement();
    final String name = propertyName;

    final Object[] result = new Object[1];
    String pluginId = element.getDeclaringExtension().getNamespace();
    Bundle bundle = Platform.getBundle(pluginId);
    if (bundle.getState() == Bundle.ACTIVE) {
      try {
        return element.createExecutableExtension(name);
      } catch (CoreException e) {
        handleCreateExecutableException(result, e);
      }
    } else {
      BusyIndicator.showWhile(null, new Runnable() {
        public void run() {
          try {
            result[0] = element.createExecutableExtension(name);
          } catch (Exception e) {
            handleCreateExecutableException(result, e);
          }
        }
      });
    }
    return result[0];
  }

  /**
   * @return IOpenOn for this definition
   */
  public IOpenOn createOpenOn() {
    IOpenOn openOn = null;

    if (getClassName() != null) {
      openOn = (IOpenOn) createExtension(OpenOnBuilder.ATT_CLASS);
    }

    return openOn;
  }

  /**
   * @return Returns the fClass.
   */
  public String getClassName() {
    return fClassName;
  }

  /**
   * @return Returns the fConfigurationElement.
   */
  public IConfigurationElement getConfigurationElement() {
    return fConfigurationElement;
  }

  /**
   * @return Returns the fContentTypes.
   */
  public HashMap getContentTypes() {
    return fContentTypes;
  }

  /**
   * @return Returns the fId.
   */
  public String getId() {
    return fId;
  }

  /**
   * @param result
   * @param e
   */
  private void handleCreateExecutableException(Object[] result, Throwable e) {
    Logger.logException("Unable to create open on: " + getId(), e); //$NON-NLS-1$
    e.printStackTrace();
    result[0] = null;
  }
}
