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
package com.google.dart.tools.ui.presentation;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.presentations.defaultpresentation.DefaultTabFolder;
import org.eclipse.ui.internal.presentations.defaultpresentation.DefaultThemeListener;
import org.eclipse.ui.internal.presentations.defaultpresentation.EmptyTabFolder;
import org.eclipse.ui.internal.presentations.util.PresentablePartFolder;
import org.eclipse.ui.internal.presentations.util.TabbedStackPresentation;
import org.eclipse.ui.presentations.AbstractPresentationFactory;
import org.eclipse.ui.presentations.IStackPresentationSite;
import org.eclipse.ui.presentations.StackPresentation;

/**
 * Presentation factory for the Dart Editor.
 */
@SuppressWarnings("restriction")
public class DartEditorPresentationFactory extends AbstractPresentationFactory {

  public DartEditorPresentationFactory() {

  }

  @Override
  public StackPresentation createEditorPresentation(Composite parent, IStackPresentationSite site) {
    DefaultTabFolder folder = new DefaultTabFolder(
        parent,
        SWT.TOP | SWT.BORDER,
        site.supportsState(IStackPresentationSite.STATE_MINIMIZED),
        site.supportsState(IStackPresentationSite.STATE_MAXIMIZED));
    folder.setSimpleTabs(false);

    PresentablePartFolder partFolder = new PresentablePartFolder(folder);

    TabbedStackPresentation result = new TabbedStackPresentation(
        site,
        partFolder,
        new EditorSystemMenu(site));

    DefaultThemeListener themeListener = new DefaultThemeListener(folder, result.getTheme());
    result.getTheme().addListener(themeListener);

    return result;
  }

  @Override
  public StackPresentation createStandaloneViewPresentation(Composite parent,
      IStackPresentationSite site, boolean showTitle) {
    if (showTitle) {
      return createViewPresentation(parent, site);
    }

    EmptyTabFolder folder = new EmptyTabFolder(parent, true);
    TabbedStackPresentation presentation = new TabbedStackPresentation(
        site,
        folder,
        new ViewSystemMenu(site));

    return presentation;

  }

  @Override
  public StackPresentation createViewPresentation(Composite parent, IStackPresentationSite site) {
    DefaultTabFolder folder = new DefaultTabFolder(
        parent,
        SWT.TOP | SWT.BORDER,
        site.supportsState(IStackPresentationSite.STATE_MINIMIZED),
        site.supportsState(IStackPresentationSite.STATE_MAXIMIZED));

    final IPreferenceStore store = PlatformUI.getPreferenceStore();
    final int minimumCharacters = store.getInt(IWorkbenchPreferenceConstants.VIEW_MINIMUM_CHARACTERS);

    if (minimumCharacters >= 0) {
      folder.setMinimumCharacters(minimumCharacters);
    }
    folder.setUnselectedCloseVisible(false);
    folder.setUnselectedImageVisible(true);
    folder.setSimpleTabs(false);

    PresentablePartFolder partFolder = new PresentablePartFolder(folder);

    TabbedStackPresentation result = new TabbedStackPresentation(
        site,
        partFolder,
        new ViewSystemMenu(site));

    DefaultThemeListener themeListener = new DefaultThemeListener(folder, result.getTheme());
    result.getTheme().addListener(themeListener);

    return result;
  }

}
