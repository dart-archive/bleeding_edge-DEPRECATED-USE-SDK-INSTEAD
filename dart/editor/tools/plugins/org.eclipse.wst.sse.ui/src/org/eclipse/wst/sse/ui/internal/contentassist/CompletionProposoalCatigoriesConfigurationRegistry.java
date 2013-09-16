/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentassist;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationReader;
import org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A registry for all extensions to the
 * <code>org.eclipse.wst.sse.ui.completionProposalCategoriesConfiguration</code> extension point.
 * </p>
 */
public final class CompletionProposoalCatigoriesConfigurationRegistry {
  /** The extension schema name of the extension point */
  private static final String EXTENSION_POINT = "completionProposalCategoriesConfiguration"; //$NON-NLS-1$

  /** The extension schema name of categories properties child elements */
  private static final String ELEM_CATEGORIES_PROPERTIES = "categoriesConfiguration"; //$NON-NLS-1$

  /** The extension schema name of the content type id attribute */
  private static final String ATTR_CONTENT_TYPE_ID = "contentTypeID"; //$NON-NLS-1$

  /** The extension schema name of the class attribute */
  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  /** The singleton instance. */
  private static CompletionProposoalCatigoriesConfigurationRegistry fgSingleton = null;

  /** <code>true</code> if this registry has been loaded. */
  private boolean fLoaded;

  /**
   * <code>{@link Map}<{@link String}, {@link ICompletionProposalCategoriesConfigurationReader}></code>
   * <ul>
   * <li><b>key:</b> Content Type ID</li>
   * <li><b>value:</b> Categories Properties</li>
   */
  private Map fPropertiesByContentTypeID;

  /**
   * @return the singleton instance of this registry
   */
  public static synchronized CompletionProposoalCatigoriesConfigurationRegistry getDefault() {
    if (fgSingleton == null) {
      fgSingleton = new CompletionProposoalCatigoriesConfigurationRegistry();
    }

    return fgSingleton;
  }

  /**
   * Private constructor to create singleton instance
   */
  private CompletionProposoalCatigoriesConfigurationRegistry() {
    this.fLoaded = false;
    this.fPropertiesByContentTypeID = new HashMap();
  }

  /**
   * @param contentTypeID get the {@link ICompletionProposalCategoriesConfigurationReader}
   *          associated with the given content type
   * @return the {@link ICompletionProposalCategoriesConfigurationReader} associated with the given
   *         content type, or <code>null</code> if one does not exist.
   */
  public ICompletionProposalCategoriesConfigurationReader getReadableConfiguration(
      String contentTypeID) {
    this.ensureLoaded();
    return (ICompletionProposalCategoriesConfigurationReader) this.fPropertiesByContentTypeID.get(contentTypeID);
  }

  /**
   * @param contentTypeID get the {@link ICompletionProposalCategoriesConfigurationWriter}
   *          associated with the given content type
   * @return the {@link ICompletionProposalCategoriesConfigurationWriter} associated with the given
   *         content type, or <code>null</code> if one does not exist.
   */
  public ICompletionProposalCategoriesConfigurationWriter getWritableConfiguration(
      String contentTypeID) {
    this.ensureLoaded();

    ICompletionProposalCategoriesConfigurationReader reader = getReadableConfiguration(contentTypeID);
    ICompletionProposalCategoriesConfigurationWriter writer = null;
    if (reader instanceof ICompletionProposalCategoriesConfigurationWriter) {
      writer = (ICompletionProposalCategoriesConfigurationWriter) reader;
    }

    return writer;
  }

  /**
   * Ensures the <code>org.eclipse.wst.sse.ui.completionProposalCategoriesConfiguration</code>
   * extensions have been loaded
   */
  private void ensureLoaded() {
    if (!this.fLoaded) {
      this.load();
      this.fLoaded = true;
    }
  }

  /**
   * Loads the <code>org.eclipse.wst.sse.ui.completionProposalCategoriesConfiguration</code>
   * extensions
   */
  private void load() {
    //get the extensions
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    List extensionElements = new ArrayList(Arrays.asList(registry.getConfigurationElementsFor(
        SSEUIPlugin.ID, EXTENSION_POINT)));

    //for each element get its attributes and add it to the map of properties
    Iterator elementsIter = extensionElements.iterator();
    while (elementsIter.hasNext()) {
      IConfigurationElement element = (IConfigurationElement) elementsIter.next();

      try {
        if (element.getName().equals(ELEM_CATEGORIES_PROPERTIES)) {
          String contentTypeID = element.getAttribute(ATTR_CONTENT_TYPE_ID);
          ContentAssistUtils.checkExtensionAttributeNotNull(contentTypeID, ATTR_CONTENT_TYPE_ID,
              element);

          String pageClass = element.getAttribute(ATTR_CLASS);
          ContentAssistUtils.checkExtensionAttributeNotNull(pageClass, ATTR_CLASS, element);

          ICompletionProposalCategoriesConfigurationReader props = (ICompletionProposalCategoriesConfigurationReader) element.createExecutableExtension(ATTR_CLASS);

          if (!this.fPropertiesByContentTypeID.containsKey(contentTypeID)) {
            this.fPropertiesByContentTypeID.put(contentTypeID, props);
          } else {
            Logger.log(Logger.ERROR, "Extension " + element.getDeclaringExtension() + //$NON-NLS-1$
                " is attempting to to define itself as the proposal cateigories" + //$NON-NLS-1$
                " configuration for content type " + contentTypeID + " when another" + //$NON-NLS-1$ //$NON-NLS-2$
                " extensions has already done so."); //$NON-NLS-1$
          }
        } else {
          //extension specified element that is not valid for this extension
          Logger.log(Logger.WARNING, "The element " + element + " is not valid for the" + //$NON-NLS-1$ //$NON-NLS-2$
              "org.eclipse.wst.sse.ui.completionProposalCategoriesConfiguration" + //$NON-NLS-1$
              " extension point.  Only " + ELEM_CATEGORIES_PROPERTIES + //$NON-NLS-1$
              " elements are valid."); //$NON-NLS-1$
        }
      } catch (InvalidRegistryObjectException x) {
        /*
         * Element is not valid any longer as the contributing plug-in was unloaded or for some
         * other reason.
         */
        String message = "The extension ''" + element.toString() + "'' has become invalid."; //$NON-NLS-1$ //$NON-NLS-2$
        IStatus status = new Status(IStatus.WARNING, SSEUIPlugin.ID, IStatus.OK, message, x);
        Logger.log(status);
      } catch (CoreException x) {
        Logger.log(x.getStatus());
      }
    }
  }
}
