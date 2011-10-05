/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.functions.DartWordFinder;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.internal.ide.actions.QuickMenuAction;

@SuppressWarnings("restriction")
public abstract class DartQuickMenuAction extends QuickMenuAction {

  private DartEditor fEditor;

  public DartQuickMenuAction(DartEditor editor, String commandId) {
    super(commandId);
    fEditor = editor;
  }

  public DartQuickMenuAction(String commandId) {
    super(commandId);
  }

  protected Point computeMenuLocation(StyledText text) {
    if (fEditor == null || text != fEditor.getViewer().getTextWidget()) {
      return null;
    }
    return computeWordStart();
  }

  private Point computeWordStart() {
    ITextSelection selection = (ITextSelection) fEditor.getSelectionProvider().getSelection();
    IRegion textRegion = DartWordFinder.findWord(fEditor.getViewer().getDocument(),
        selection.getOffset());
    if (textRegion == null) {
      return null;
    }

    IRegion widgetRegion = modelRange2WidgetRange(textRegion);
    if (widgetRegion == null) {
      return null;
    }

    int start = widgetRegion.getOffset();

    StyledText styledText = fEditor.getViewer().getTextWidget();
    Point result = styledText.getLocationAtOffset(start);
    result.y += styledText.getLineHeight(start);

    if (!styledText.getClientArea().contains(result)) {
      return null;
    }
    return result;
  }

  private IRegion modelRange2WidgetRange(IRegion region) {
    ISourceViewer viewer = fEditor.getViewer();
    if (viewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
      return extension.modelRange2WidgetRange(region);
    }

    IRegion visibleRegion = viewer.getVisibleRegion();
    int start = region.getOffset() - visibleRegion.getOffset();
    int end = start + region.getLength();
    if (end > visibleRegion.getLength()) {
      end = visibleRegion.getLength();
    }

    return new Region(start, end - start);
  }
}
