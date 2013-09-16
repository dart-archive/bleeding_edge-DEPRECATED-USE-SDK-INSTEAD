/*******************************************************************************
 * Copyright (c) 2005, 2010 Standards for Technology in Automotive Retail All rights reserved. This
 * program and the accompanying materials are made available under the terms of the Eclipse Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver - bug 213883 - initial api
 * IBM Corporation - bug 271619 - Move and rename XML perspective definition
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.xml.ui.internal.IProductConstants;
import org.eclipse.wst.xml.ui.internal.ProductProperties;

/**
 * @author dcarver
 */
public class XMLPerspectiveFactory implements IPerspectiveFactory {

//	private static final String SEARCH_VIEW_ID = "org.eclipse.search.ui.views.SearchView"; //$NON-NLS-1$
//	private static final String XPATH_VIEW_ID = "org.eclipse.wst.xml.views.XPathView"; //$NON-NLS-1$
//	private static final String SNIPPETS_VIEW_ID = "org.eclipse.wst.common.snippets.internal.ui.SnippetsView"; //$NON-NLS-1$
//	private static final String TEXTEDITOR_TEMPLATES_VIEW_ID = "org.eclipse.ui.texteditor.TemplatesView"; //$NON-NLS-1$
//	private static final String ID_CONSOLE_VIEW = "org.eclipse.ui.console.ConsoleView"; //$NON-NLS-1$

  private static String EXPLORER_VIEW_ID = "org.eclipse.ui.navigator.ProjectExplorer"; //$NON-NLS-1$

  public XMLPerspectiveFactory() {
    String viewerID = ProductProperties.getProperty(IProductConstants.PERSPECTIVE_EXPLORER_VIEW);
    if (viewerID != null) {
      // verify that the view actually exists
      if (PlatformUI.getWorkbench().getViewRegistry().find(viewerID) != null) {
        EXPLORER_VIEW_ID = viewerID;
      }
    }
  }

  /**
   * Creates the initial layout. This is what the layout is reset to when the Reset Perspective is
   * selected. It takes as input a IPageLayout object.
   * 
   * @param layout
   */
  public void createInitialLayout(IPageLayout layout) {
    // Get the Editor Area
//		String editorArea = layout.getEditorArea();

    // Turn on the Editor Area
    layout.setEditorAreaVisible(true);
    layout.setFixed(false);

    layout.addShowViewShortcut(EXPLORER_VIEW_ID);

    IFolderLayout topLeft = layout.createFolder(
        "topLeft", IPageLayout.LEFT, 0.23f, layout.getEditorArea());//$NON-NLS-1$
    topLeft.addView(EXPLORER_VIEW_ID);

    // Create the areas of the layout with their initial views
//		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.23, editorArea); //$NON-NLS-1$
//		left.addView(IPageLayout.ID_PROJECT_EXPLORER);
//
//		IFolderLayout bottomLeft = layout.createFolder("bottom-left", IPageLayout.BOTTOM, (float) 0.50, "left"); //$NON-NLS-1$ //$NON-NLS-2$
//		bottomLeft.addView(XPATH_VIEW_ID); //$NON-NLS-1$
//
//		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.70, editorArea); //$NON-NLS-1$
//		right.addView(IPageLayout.ID_OUTLINE);
//		
//		IFolderLayout bottomRight = layout.createFolder("bottom-right", IPageLayout.BOTTOM, (float) 0.60, "right"); //$NON-NLS-1$//$NON-NLS-2$
//		bottomRight.addView(TEXTEDITOR_TEMPLATES_VIEW_ID);
//
//		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.75, editorArea); //$NON-NLS-1$
//		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
//		bottom.addView(IPageLayout.ID_PROP_SHEET);
//		bottom.addView(ID_CONSOLE_VIEW);
//		bottom.addView(SNIPPETS_VIEW_ID);
//
//		bottom.addPlaceholder(SEARCH_VIEW_ID);
  }

}
