/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.extension.RegistryReader;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple generic ID to class to mapping. Loads a specified class defined in a configuration element
 * with the matching type and target ID. Example plugin.xml section: &lt;extension
 * point=&quot;org.eclipse.wst.sse.ui.editorConfiguration&quot;&gt;contentoutlineconfiguration
 * target=&quot;org.eclipse.wst.sse.dtd.core.dtdsource&quot;
 * class=&quot;org.eclipse.wst.sse.ui.dtd.views
 * .contentoutline.DTDContentOutlineConfiguration&quot;/&gt; &lt;/extension&gt; Used in code by
 * getConfiguration(&quot;contentoutlineconfiguration&quot;,
 * &quot;org.eclipse.wst.dtd.ui.StructuredTextEditorDTD&quot;);
 */
public class ExtendedConfigurationBuilder extends RegistryReader {
  /**
   * Extension type to pass into getConfigurations to get content outline configuration
   */
  public static final String CONTENTOUTLINECONFIGURATION = "contentOutlineConfiguration"; //$NON-NLS-1$
  /**
   * Extension type to pass into getConfigurations to get property sheet configuration
   */
  public static final String PROPERTYSHEETCONFIGURATION = "propertySheetConfiguration"; //$NON-NLS-1$
  /**
   * Extension type to pass into getConfigurations to get source viewer configuration
   */
  public static final String SOURCEVIEWERCONFIGURATION = "sourceViewerConfiguration"; //$NON-NLS-1$
  /**
   * Extension type to pass into getConfigurations to get documentation text hover
   */
  public static final String DOCUMENTATIONTEXTHOVER = "documentationTextHover"; //$NON-NLS-1$
  /**
   * Extension type to pass into getConfigurations to get double click strategy
   */
  public static final String DOUBLECLICKSTRATEGY = "doubleClickStrategy"; //$NON-NLS-1$
  /**
   * Extension type to pass into getConfigurations to get quick outline configuration
   */
  public static final String QUICKOUTLINECONFIGURATION = "quickOutlineConfiguration"; //$NON-NLS-1$

  private static final String ATT_CLASS = "class"; //$NON-NLS-1$
  private static final String ATT_TARGET = "target"; //$NON-NLS-1$
  private static final String ATT_TYPE = "type"; //$NON-NLS-1$
  private static final String CONFIGURATION = "provisionalConfiguration"; //$NON-NLS-1$
  private static Map configurationMap = null;
  private final static boolean debugTime = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.ui/extendedconfigurationbuilder/time")); //$NON-NLS-1$  //$NON-NLS-2$
  private static final String DEFINITION = "provisionalDefinition"; //$NON-NLS-1$
  private static final String EP_EXTENDEDCONFIGURATION = "editorConfiguration"; //$NON-NLS-1$
  private static ExtendedConfigurationBuilder instance = null;
  public static final String VALUE = "value"; //$NON-NLS-1$

  /**
   * Creates an extension. If the extension plugin has not been loaded a busy cursor will be
   * activated during the duration of the load.
   * 
   * @param element the config element defining the extension
   * @param classAttribute the name of the attribute carrying the class
   * @returns the extension object if successful. If an error occurs when createing executable
   *          extension, the exception is logged, and null returned.
   */
  static Object createExtension(final IConfigurationElement element, final String classAttribute,
      final String targetID) {
    final Object[] result = new Object[1];
    String pluginId = element.getDeclaringExtension().getNamespace();
    Bundle bundle = Platform.getBundle(pluginId);
    if (bundle.getState() == Bundle.ACTIVE) {
      try {
        result[0] = element.createExecutableExtension(classAttribute);
      } catch (Exception e) {
        // catch and log ANY exception while creating the extension
        Logger.logException("error loading class " + classAttribute + " for " + targetID, e); //$NON-NLS-1$ //$NON-NLS-2$
      }
    } else {
      BusyIndicator.showWhile(null, new Runnable() {
        public void run() {
          try {
            result[0] = element.createExecutableExtension(classAttribute);
          } catch (Exception e) {
            // catch and log ANY exception from extension point
            Logger.logException("error loading class " + classAttribute + " for " + targetID, e); //$NON-NLS-1$ //$NON-NLS-2$
          }
        }
      });
    }
    return result[0];
  }

  public synchronized static ExtendedConfigurationBuilder getInstance() {
    if (instance == null)
      instance = new ExtendedConfigurationBuilder();
    return instance;
  }

  long time0 = 0;

  private ExtendedConfigurationBuilder() {
    super();
  }

  private List createConfigurations(List configurations, String extensionType, String targetID) {
    if (configurations == null)
      return new ArrayList(0);
    List result = new ArrayList(1);
    for (int i = 0; i < configurations.size(); i++) {
      IConfigurationElement element = (IConfigurationElement) configurations.get(i);
      if ((element.getName().equals(extensionType) || (element.getName().equals(CONFIGURATION) && extensionType.equals(element.getAttribute(ATT_TYPE))))) {
        String[] targets = StringUtils.unpack(element.getAttribute(ATT_TARGET));
        for (int j = 0; j < targets.length; j++) {
          if (targetID.equals(targets[j].trim())) {
            Object o = createExtension(element, ATT_CLASS, targetID);
            if (o != null) {
              result.add(o);
            }
          }
        }
      }
    }
    return result;
  }

  private IConfigurationElement[] findConfigurationElements(List configurations,
      String extensionType, String targetID) {
    if (configurations == null)
      return new IConfigurationElement[0];
    List result = new ArrayList(1);
    for (int i = 0; i < configurations.size(); i++) {
      IConfigurationElement element = (IConfigurationElement) configurations.get(i);
      if ((element.getName().equals(extensionType) || (element.getName().equals(DEFINITION) && extensionType.equals(element.getAttribute(ATT_TYPE))))) {
        String[] targets = StringUtils.unpack(element.getAttribute(ATT_TARGET));
        for (int j = 0; j < targets.length; j++) {
          if (targetID.equals(targets[j].trim())) {
            result.add(element);
          }
        }
      }
    }
    return (IConfigurationElement[]) result.toArray(new IConfigurationElement[0]);
  }

  /**
   * Returns a configuration for the given extensionType matching the targetID, if one is available.
   * If more than one configuration is defined, the first one found is returned.
   * 
   * @param extensionType
   * @param targetID
   * @return a configuration object, if one was defined
   */
  public Object getConfiguration(String extensionType, String targetID) {
    if (targetID == null || targetID.length() == 0)
      return null;
    List configurations = getConfigurations(extensionType, targetID);
    if (configurations.isEmpty())
      return null;
    return configurations.get(0);
  }

  /**
   * Returns all configurations for the given extensionType matching the targetID, if any are
   * available.
   * 
   * @param extensionType
   * @param targetID
   * @return a List of configuration objects, which may or may not be empty
   */
  public List getConfigurations(String extensionType, String targetID) {
    if (targetID == null || targetID.length() == 0)
      return new ArrayList(0);
    if (configurationMap == null) {
      configurationMap = new HashMap(0);
      synchronized (configurationMap) {
        readRegistry(Platform.getExtensionRegistry(), SSEUIPlugin.ID, EP_EXTENDEDCONFIGURATION);
        if (debugTime) {
          System.out.println(getClass().getName()
              + "#readRegistry():  " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
          time0 = System.currentTimeMillis();
        }
      }
    }
    List extensions = (List) configurationMap.get(extensionType);
    List configurations = createConfigurations(extensions, extensionType, targetID);
    if (debugTime) {
      if (!configurations.isEmpty())
        System.out.println(getClass().getName()
            + "#getConfiguration(" + extensionType + ", " + targetID + "): configurations loaded in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      else
        System.out.println(getClass().getName()
            + "#getConfiguration(" + extensionType + ", " + targetID + "): ran in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
    return configurations;
  }

  /**
   * Returns all declared definitions for the given extensionType matching the targetID, if any are
   * available.
   * 
   * @param extensionType
   * @param targetID
   * @return An array containing the definitions, empty if none were declared
   */
  public String[] getDefinitions(String extensionType, String targetID) {
    if (targetID == null || targetID.length() == 0)
      return new String[0];
    if (debugTime) {
      time0 = System.currentTimeMillis();
    }
    if (configurationMap == null) {
      configurationMap = new HashMap(0);
      synchronized (configurationMap) {
        readRegistry(Platform.getExtensionRegistry(), SSEUIPlugin.ID, EP_EXTENDEDCONFIGURATION);
        if (debugTime) {
          System.out.println(getClass().getName()
              + "#readRegistry():  " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
          time0 = System.currentTimeMillis();
        }
      }
    }
    List definitions = (List) configurationMap.get(extensionType);
    IConfigurationElement[] elements = findConfigurationElements(definitions, extensionType,
        targetID);
    String[] values = new String[elements.length];
    for (int i = 0; i < values.length; i++) {
      values[i] = elements[i].getAttribute(VALUE);
    }
    if (debugTime) {
      if (values.length > 0)
        System.out.println(getClass().getName()
            + "#getDefinitions(" + extensionType + ", " + targetID + "): definition loaded in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      else
        System.out.println(getClass().getName()
            + "#getDefinitions(" + extensionType + ", " + targetID + "): ran in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
    return values;
  }

  protected boolean readElement(IConfigurationElement element) {
    String name = element.getName();
    if (name.equals(CONFIGURATION) || name.equals(DEFINITION))
      name = element.getAttribute(ATT_TYPE);
    List configurations = (List) configurationMap.get(name);
    if (configurations == null) {
      configurations = new ArrayList(1);
      configurationMap.put(name, configurations);
    }
    configurations.add(element);
    return true;
  }
}
