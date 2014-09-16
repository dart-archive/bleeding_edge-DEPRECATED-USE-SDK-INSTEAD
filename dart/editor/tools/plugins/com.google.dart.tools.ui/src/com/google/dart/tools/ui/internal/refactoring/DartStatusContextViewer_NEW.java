/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext_NEW;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.text.editor.DartSourceViewer;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;
import com.google.dart.tools.ui.text.DartTextTools;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.ui.refactoring.TextStatusContextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

public class DartStatusContextViewer_NEW extends TextStatusContextViewer {
  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    final SourceViewer viewer = getSourceViewer();
    viewer.unconfigure();
    IPreferenceStore store = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    viewer.configure(new DartSourceViewerConfiguration(
        DartToolsPlugin.getDefault().getDartTextTools().getColorManager(),
        store,
        null,
        null));
    viewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
  }

  @Override
  public void setInput(RefactoringStatusContext context) {
    if (context instanceof DartStatusContext_NEW) {
      DartStatusContext_NEW sc = (DartStatusContext_NEW) context;
      String content = sc.getContent();
      IDocument document = newDocument(content);
      setInput(document, sc.getRegion());
      updateTitle(sc);
    }
  }

  @Override
  protected SourceViewer createSourceViewer(Composite parent) {
    IPreferenceStore store = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    // prepare line number ruler
    CompositeRuler ruler;
    {
      ruler = new CompositeRuler();
      LineNumberRulerColumn lnrc = new LineNumberRulerColumn();
      ruler.addDecorator(0, lnrc);
    }
    // create viewer
    DartSourceViewer viewer = new DartSourceViewer(parent, ruler, null, false, SWT.LEFT_TO_RIGHT
        | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION, store);
    // set font (it is somehow reset when we set a ruler)
    {
      Font font = JFaceResources.getFont(FontPreferencePage.EDITOR_FONT_KEY);
      viewer.getTextWidget().setFont(font);
    }
    // done
    return viewer;
  }

  private IDocument newDocument(String source) {
    IDocument result = new Document(source);
    DartTextTools textTools = DartToolsPlugin.getDefault().getDartTextTools();
    textTools.setupJavaDocumentPartitioner(result);
    return result;
  }
}
