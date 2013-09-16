/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.correction;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.extension.RegistryReader;

import java.util.ArrayList;

public class QuickFixRegistryReader extends RegistryReader {
  private static final String QUICKFIXPROCESSOR = "quickFixProcessor"; //$NON-NLS-1$
  private static final String ATT_PROBLEMTYPE = "problemType"; //$NON-NLS-1$

  private static final String ATTRIBUTE = "attribute"; //$NON-NLS-1$
  private static final String ATT_NAME = "name"; //$NON-NLS-1$
  private static final String ATT_VALUE = "value"; //$NON-NLS-1$

  private ArrayList currentAttributeNames;

  private ArrayList currentAttributeValues;

  private QuickFixRegistry quickFixProcessorRegistry;

  protected boolean readElement(IConfigurationElement element) {
    if (element.getName().equals(QUICKFIXPROCESSOR)) {
      readQuickFixProcessorElement(element);
      return true;
    }
    if (element.getName().equals(ATTRIBUTE)) {
      readAttributeElement(element);
      return true;
    }
    return false;
  }

  /**
   * Processes a resolution configuration element.
   */
  private void readQuickFixProcessorElement(IConfigurationElement element) {
    // read type
    String type = element.getAttribute(ATT_PROBLEMTYPE);

    // read attributes and values
    currentAttributeNames = new ArrayList();
    currentAttributeValues = new ArrayList();
    readElementChildren(element);
    String[] attributeNames = (String[]) currentAttributeNames.toArray(new String[currentAttributeNames.size()]);
    String[] attributeValues = (String[]) currentAttributeValues.toArray(new String[currentAttributeValues.size()]);

    // add query to the registry
    AnnotationQuery query = new AnnotationQuery(type, attributeNames);
    AnnotationQueryResult result = new AnnotationQueryResult(attributeValues);
    quickFixProcessorRegistry.addResolutionQuery(query, result, element);
  }

  /**
   * Get the marker help that is defined in the plugin registry and add it to the given marker help
   * registry.
   * <p>
   * Warning: The marker help registry must be passed in because this method is called during the
   * process of setting up the marker help registry and at this time it has not been safely setup
   * with the plugin.
   * </p>
   */
  public void addHelp(QuickFixRegistry registry) {
    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
    quickFixProcessorRegistry = registry;
    readRegistry(extensionRegistry, SSEUIPlugin.ID, QUICKFIXPROCESSOR);
  }

  /**
   * Processes an attribute sub element.
   */
  private void readAttributeElement(IConfigurationElement element) {
    String name = element.getAttribute(ATT_NAME);
    String value = element.getAttribute(ATT_VALUE);
    if (name != null && value != null) {
      currentAttributeNames.add(name);
      currentAttributeValues.add(value);
    }
  }
}
