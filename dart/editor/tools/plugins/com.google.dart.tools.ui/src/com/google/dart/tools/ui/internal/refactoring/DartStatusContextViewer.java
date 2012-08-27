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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.base.DartStringStatusContext;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.editor.DartSourceViewer;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;
import com.google.dart.tools.ui.text.DartTextTools;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.ui.refactoring.TextStatusContextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class DartStatusContextViewer extends TextStatusContextViewer {

  private static IRegion createRegion(SourceRange range) {
    return new Region(range.getOffset(), range.getLength());
  }

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
    if (context instanceof DartStatusContext) {
      DartStatusContext jsc = (DartStatusContext) context;
      IDocument document = null;
      {
        CompilationUnit cunit = jsc.getCompilationUnit();
        if (cunit.isWorkingCopy()) {
          try {
            document = newDocument(cunit.getSource());
          } catch (DartModelException e) {
            // document is null which is a valid input.
          }
        } else {
          IEditorInput editorInput = new FileEditorInput((IFile) cunit.getResource());
          document = getDocument(
              DartToolsPlugin.getDefault().getCompilationUnitDocumentProvider(),
              editorInput);
        }
        if (document == null) {
          document = new Document(RefactoringMessages.DartStatusContextViewer_no_source_available);
        }
        updateTitle(cunit);
      }
      setInput(document, createRegion(jsc.getSourceRange()));
    } else if (context instanceof DartStringStatusContext) {
      updateTitle(null);
      DartStringStatusContext sc = (DartStringStatusContext) context;
      setInput(newDocument(sc.getSource()), createRegion(sc.getSourceRange()));
    }
  }

  @Override
  protected SourceViewer createSourceViewer(Composite parent) {
    IPreferenceStore store = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    return new DartSourceViewer(parent, null, null, false, SWT.LEFT_TO_RIGHT | SWT.V_SCROLL
        | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION, store);
  }

  private IDocument getDocument(IDocumentProvider provider, IEditorInput input) {
    if (input == null) {
      return null;
    }
    IDocument result = null;
    try {
      provider.connect(input);
      result = provider.getDocument(input);
    } catch (CoreException e) {
    } finally {
      provider.disconnect(input);
    }
    return result;
  }

  private IDocument newDocument(String source) {
    IDocument result = new Document(source);
    DartTextTools textTools = DartToolsPlugin.getDefault().getDartTextTools();
    textTools.setupJavaDocumentPartitioner(result);
    return result;
  }
}
