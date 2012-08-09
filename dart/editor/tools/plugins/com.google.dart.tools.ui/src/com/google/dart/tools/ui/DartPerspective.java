/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * The Dart Tooling for Eclipse perspective.
 */
public class DartPerspective implements IPerspectiveFactory {
  private static final String DEBUGGER_VIEW_ID = "com.google.dart.tools.debug.debuggerView"; //$NON-NLS-1$
  private static final String BREAKPOINTS_VIEW_ID = "com.google.dart.tools.debug.breakpointsView"; //$NON-NLS-1$

  private static final String BR = "bottomRight"; //$NON-NLS-1$
  private static final String TL = "topLeft"; //$NON-NLS-1$
  private static final String TR = "topRight"; //$NON-NLS-1$
  private static final String OUTLINE_FOLDER = "outlineFolder"; //$NON-NLS-1$

//  private static final String BL = "bottomLeft"; //$NON-NLS-1$
  private static final String WIZARD_NEW_MODULE = "com.google.dart.project.wizard"; //$NON-NLS-1$
  private static final String WIZARD_NEW_FILE = "org.eclipse.ui.wizards.new.file"; //$NON-NLS-1$
  private static final String WIZARD_NEW_FOLDER = "org.eclipse.ui.wizards.new.folder"; //$NON-NLS-1$
  private static final String WIZARD_NEW_TEXT = "org.eclipse.ui.editors.wizards.UntitledTextFileWizard"; //$NON-NLS-1$

  public DartPerspective() {
  }

  @Override
  public void createInitialLayout(IPageLayout layout) {
    String editorArea = layout.getEditorArea();

    // Top left: Project Explorer view
    IFolderLayout topLeft = layout.createFolder(TL, IPageLayout.LEFT, 0.25f, editorArea);

    topLeft.addView(DartUI.ID_FILE_EXPLORER);
    topLeft.addPlaceholder(DartUI.ID_APPS_VIEW);

    // Bottom left: Outline view and Property Sheet view
    IPlaceholderFolderLayout outlinefolder = layout.createPlaceholderFolder(
        OUTLINE_FOLDER,
        IPageLayout.BOTTOM,
        0.50f,
        TL);
    outlinefolder.addPlaceholder(IPageLayout.ID_OUTLINE);
    outlinefolder.addPlaceholder(IPageLayout.ID_PROP_SHEET);
    outlinefolder.addPlaceholder(DartUI.ID_DARTUNIT_VIEW);

    // Bottom right: info views
    IFolderLayout outputfolder = layout.createFolder(BR, IPageLayout.BOTTOM, 0.75f, editorArea);
    outputfolder.addView(DartUI.ID_PROBLEMS);
    //outputfolder.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
    outputfolder.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
    outputfolder.addPlaceholder(DartUI.ID_CONSOLE_VIEW);
    outputfolder.addPlaceholder(IPageLayout.ID_TASK_LIST);
    outputfolder.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);

    // Top right: Debugger View
    IPlaceholderFolderLayout debuggerFolder = layout.createPlaceholderFolder(
        TR,
        IPageLayout.RIGHT,
        0.70f,
        editorArea);
    debuggerFolder.addPlaceholder(DEBUGGER_VIEW_ID);
    debuggerFolder.addPlaceholder(BREAKPOINTS_VIEW_ID);

    layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

    //layout.addShowViewShortcut(NewSearchUI.SEARCH_VIEW_ID);
    layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
    layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
    layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
    layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
    layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);

    // new actions - wizards
    layout.addNewWizardShortcut(WIZARD_NEW_MODULE);
    layout.addNewWizardShortcut(WIZARD_NEW_FOLDER);
    layout.addNewWizardShortcut(WIZARD_NEW_FILE);
    layout.addNewWizardShortcut(WIZARD_NEW_TEXT);
  }
}
