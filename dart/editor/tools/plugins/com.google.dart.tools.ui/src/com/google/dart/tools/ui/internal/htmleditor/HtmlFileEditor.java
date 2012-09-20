/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.htmleditor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ITextViewerExtension7;
import org.eclipse.jface.text.TabsToSpacesConverter;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

// TODO(devoncarew): investigate using a more full-featured html editor (WTP?)

/**
 * An editor for HTML files. Defining this editor causes the default double-click action on an html
 * file to open it for editing, instead of opening it in the embedded web browser.
 */
public class HtmlFileEditor extends TextEditor {

  /**
   * Create a new HTML editor.
   */
  public HtmlFileEditor() {
    setRulerContextMenuId("#DartHtmlFileEditorRulerContext"); //$NON-NLS-1$
  }

  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    installTabsToSpacesConverter();
  }

  @Override
  public String getTitleToolTip() {
    if (getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) getEditorInput();

      if (input.getFile().getLocation() != null) {
        return input.getFile().getLocation().toFile().toString();
      }
    }

    return super.getTitleToolTip();
  }

  @Override
  protected void editorContextMenuAboutToShow(IMenuManager menu) {
    // Cut/Copy/Paste actions..
    addAction(menu, ITextEditorActionConstants.UNDO);
    addAction(menu, ITextEditorActionConstants.CUT);
    addAction(menu, ITextEditorActionConstants.COPY);
    addAction(menu, ITextEditorActionConstants.PASTE);
  }

  /**
   * When the preference store changes, the settings for tabs to spaces can be overridden from what
   * we set in {@link #createPartControl(Composite)}, thus this method calls super, and then resets
   * our tabs-to-spaces settings.
   */
  @Override
  protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
    super.handlePreferenceStoreChanged(event);
    installTabsToSpacesConverter();
  }

  @Override
  protected void initializeKeyBindingScopes() {
    setKeyBindingScopes(new String[] {"com.google.dart.tools.ui.dartViewScope"}); //$NON-NLS-1$
  }

  /**
   * This method overrides the implementation in
   * {@link AbstractTextEditor#installTabsToSpacesConverter()} so that the number of spaces is set
   * to <code>2</code>, instead of <code>4</code>.
   */
  @Override
  protected void installTabsToSpacesConverter() {
    SourceViewerConfiguration config = getSourceViewerConfiguration();
    if (config != null && getSourceViewer() instanceof ITextViewerExtension7) {
      TabsToSpacesConverter tabToSpacesConverter = new TabsToSpacesConverter();
      tabToSpacesConverter.setLineTracker(new DefaultLineTracker());
      // TODO(jwren) Revisit and add a preference call for the hard-coded '2':
      tabToSpacesConverter.setNumberOfSpacesPerTab(2);
      ((ITextViewerExtension7) getSourceViewer()).setTabsToSpacesConverter(tabToSpacesConverter);
      updateIndentPrefixes();
    }
  }

  @Override
  protected void rulerContextMenuAboutToShow(IMenuManager menu) {
    super.rulerContextMenuAboutToShow(menu);

    // Remove the Preferences menu item
    menu.remove(ITextEditorActionConstants.RULER_PREFERENCES);
  }

}
