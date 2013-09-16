/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.extensions;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.MultiPageEditorSite;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationPointCalculator {
  public static final String DESIGN = ".design"; //$NON-NLS-1$
  public static final String SOURCE = ".source"; //$NON-NLS-1$

  public static String[] getConfigurationPoints(IEditorPart part, String contentType,
      String subContext, Class rootClass) {
    ConfigurationPointCalculator calculator = new ConfigurationPointCalculator();
    calculator.setContentType(contentType);
    calculator.setPart(part);
    calculator.setRootClass(rootClass);
    calculator.setSubContext(subContext);
    return calculator.getConfigurationPoints();
  }

  protected String fContentType = null;
  protected IWorkbenchPart fPart = null;

  protected Class fRootClass = null;
  protected String fSubContext = null;

  /**
	 * 
	 */
  public ConfigurationPointCalculator() {
    super();
  }

  public String[] getConfigurationPoints() {
    List points = new ArrayList(2);

    IWorkbenchPartSite site = null;
    if (fPart != null) {
      site = fPart.getSite();
      if (site != null) {
        String id = site.getId();
        if (id != null && id.length() > 0 && !id.equals(fRootClass.getName()))
          points.add(id);
      }
      if (site instanceof MultiPageEditorSite) {
        String multipageID = ((MultiPageEditorSite) site).getMultiPageEditor().getSite().getId();
        if (!points.contains(multipageID))
          points.add(multipageID);
        String sourcePageID = ((MultiPageEditorSite) site).getMultiPageEditor().getSite().getId()
            + ".source"; //$NON-NLS-1$
        if (!points.contains(sourcePageID))
          points.add(sourcePageID);
      }
      if (site instanceof MultiPageEditorSite) {
        String multipageClassName = ((MultiPageEditorSite) site).getMultiPageEditor().getClass().getName();
        if (!points.contains(multipageClassName))
          points.add(multipageClassName);
      }
      Class editorClass = fPart.getClass();
      while (editorClass != null && fRootClass != null && !editorClass.equals(fRootClass)) {
        if (!points.contains(editorClass.getName()))
          points.add(editorClass.getName());
        editorClass = editorClass.getSuperclass();
      }
    }

    if (fContentType != null) {
      IContentType contentType = Platform.getContentTypeManager().getContentType(fContentType);
      while (contentType != null && !contentType.getId().equals(IContentTypeManager.CT_TEXT)) {
        if (!points.contains(contentType.getId()))
          points.add(contentType.getId());
        contentType = contentType.getBaseType();
      }
    }

    if (fRootClass != null && !points.contains(fRootClass.getName()))
      points.add(fRootClass.getName());
    return (String[]) points.toArray(new String[0]);
  }

  /**
   * @return Returns the contentType.
   */
  public String getContentType() {
    return fContentType;
  }

  /**
   * @return Returns the part.
   */
  public IWorkbenchPart getPart() {
    return fPart;
  }

  /**
   * @return Returns the rootClass.
   */
  public Class getRootClass() {
    return fRootClass;
  }

  /**
   * @return Returns the subContext.
   */
  public String getSubContext() {
    return fSubContext;
  }

  /**
   * @param contentType The contentType to set.
   */
  public void setContentType(String contentType) {
    fContentType = contentType;
  }

  /**
   * @param part The part to set.
   */
  public void setPart(IWorkbenchPart part) {
    fPart = part;
  }

  /**
   * @param rootClass The rootClass to set.
   */
  public void setRootClass(Class rootClass) {
    fRootClass = rootClass;
  }

  /**
   * @param subContext The subContext to set.
   */
  public void setSubContext(String subContext) {
    fSubContext = subContext;
  }

}
