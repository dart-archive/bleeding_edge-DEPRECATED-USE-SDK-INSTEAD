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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.server.Element;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

/**
 * Provides a Dart element to be displayed in by an information presenter.
 */
public class DartElementProvider implements IInformationProvider, IInformationProviderExtension {

  private DartEditor fEditor;
  private boolean fUseCodeResolve;

  public DartElementProvider(IEditorPart editor) {
    fUseCodeResolve = false;
    if (editor instanceof DartEditor) {
      fEditor = (DartEditor) editor;
    }
  }

  public DartElementProvider(IEditorPart editor, boolean useCodeResolve) {
    this(editor);
    fUseCodeResolve = useCodeResolve;
  }

  /*
   * @see IInformationProvider#getInformation(ITextViewer, IRegion)
   */
  @Override
  public String getInformation(ITextViewer textViewer, IRegion subject) {
    return getInformation2(textViewer, subject).toString();
  }

  /*
   * @see IInformationProviderExtension#getElement(ITextViewer, IRegion)
   */
  @Override
  public Object getInformation2(ITextViewer textViewer, IRegion subject) {
    if (fEditor == null) {
      return null;
    }

    try {
      if (fUseCodeResolve) {
        IStructuredSelection sel = SelectionConverter.getStructuredSelection(fEditor);
        if (!sel.isEmpty()) {
          return sel.getFirstElement();
        }
      }

      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        String file = fEditor.getInputFilePath();
        int offset = subject.getOffset();
        Element[] targets = NewSelectionConverter.getNavigationTargets(file, offset);
        if (targets.length == 0) {
          return null;
        }
        return targets[0];
      } else {
        return NewSelectionConverter.getElementAtOffset(fEditor);
      }

    } catch (DartModelException e) {
      return null;
    }
  }

  /*
   * @see IInformationProvider#getSubject(ITextViewer, int)
   */
  @Override
  public IRegion getSubject(ITextViewer textViewer, int offset) {
    if (textViewer != null && fEditor != null) {
      IRegion region = DartWordFinder.findWord(textViewer.getDocument(), offset);
      if (region != null) {
        return region;
      } else {
        return new Region(offset, 0);
      }
    }
    return null;
  }
}
