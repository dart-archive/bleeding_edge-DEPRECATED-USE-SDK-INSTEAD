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

package com.google.dart.tools.ui.internal.text.editor;

import com.google.common.collect.Maps;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.internal.text.functions.DartWordFinder;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MarkOccurrencesManager_OLD {
  private static final String TYPE = "com.google.dart.tools.ui.occurrences";

  private final DartEditor editor;
  private final DartSourceViewer viewer;
  private final IEditorInput editorInput;

  private ISelectionChangedListener occurrencesResponder;
  private ISelection fForcedMarkOccurrencesSelection;
  private Annotation[] fOccurrenceAnnotations;
  private IRegion fMarkOccurrenceTargetRegion;

  public MarkOccurrencesManager_OLD(DartEditor _editor, DartSourceViewer viewer) {
    this.editor = _editor;
    this.viewer = viewer;
    this.editorInput = editor.getEditorInput();
    // track selection
    occurrencesResponder = new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if (selection instanceof ITextSelection) {
          fForcedMarkOccurrencesSelection = selection;
          updateOccurrenceAnnotations((ITextSelection) selection, editor.getInputUnit());
        }
      }
    };
    viewer.getSelectionProvider();
    editor.addDartSelectionListener(occurrencesResponder);
    // force update
    {
      fForcedMarkOccurrencesSelection = editor.getSelectionProvider().getSelection();
      if (editor.getInputUnit() != null) {
        updateOccurrenceAnnotations(
            (ITextSelection) fForcedMarkOccurrencesSelection,
            editor.getInputUnit());
      }
    }
  }

  public void dispose() {
    if (occurrencesResponder != null) {
      editor.removeDartSelectionListener(occurrencesResponder);
      occurrencesResponder = null;
    }
    removeOccurrenceAnnotations();
  }

  private void addAnnotations(IDocument document, Position[] positions) {
    // prepare annotations
    Map<Annotation, Position> annotationMap = Maps.newHashMap();
    for (Position position : positions) {
      String message;
      try {
        message = document.get(position.offset, position.length);
      } catch (BadLocationException ex) {
        continue;
      }
      annotationMap.put(new Annotation(TYPE, false, message), position);
    }
    // prepare IAnnotationModel
    IAnnotationModel annotationModel = getAnnotationModel();
    if (annotationModel == null) {
      return;
    }
    // add annotations
    synchronized (getLockObject(annotationModel)) {
      if (annotationModel instanceof IAnnotationModelExtension) {
        IAnnotationModelExtension extension = (IAnnotationModelExtension) annotationModel;
        extension.replaceAnnotations(fOccurrenceAnnotations, annotationMap);
      } else {
        removeOccurrenceAnnotations();
        for (Entry<Annotation, Position> entry : annotationMap.entrySet()) {
          Annotation annotation = entry.getKey();
          Position position = entry.getValue();
          annotationModel.addAnnotation(annotation, position);
        }
      }
      fOccurrenceAnnotations = annotationMap.keySet().toArray(
          new Annotation[annotationMap.keySet().size()]);
    }
  }

  private IAnnotationModel getAnnotationModel() {
    IDocumentProvider documentProvider = editor.getDocumentProvider();
    if (documentProvider == null) {
      return null;
    }
    return documentProvider.getAnnotationModel(editorInput);
  }

  /**
   * Returns the lock object for the given annotation model.
   */
  private Object getLockObject(IAnnotationModel annotationModel) {
    if (annotationModel instanceof ISynchronizable) {
      Object lock = ((ISynchronizable) annotationModel).getLockObject();
      if (lock != null) {
        return lock;
      }
    }
    return annotationModel;
  }

  private void removeOccurrenceAnnotations() {
    // prepare IAnnotationModel
    IAnnotationModel annotationModel = getAnnotationModel();
    if (annotationModel == null) {
      return;
    }
    // do remove
    synchronized (getLockObject(annotationModel)) {
      IAnnotationModelExtension extension = (IAnnotationModelExtension) annotationModel;
      if (annotationModel instanceof IAnnotationModelExtension) {
        extension.replaceAnnotations(fOccurrenceAnnotations, null);
      } else {
        for (Annotation annotation : fOccurrenceAnnotations) {
          annotationModel.removeAnnotation(annotation);
        }
      }
      fOccurrenceAnnotations = null;
    }
  }

  /**
   * Updates the occurrences annotations based on the current selection.
   */
  private void updateOccurrenceAnnotations(ITextSelection selection, CompilationUnit unit) {
    if (unit == null || selection == null) {
      return;
    }

    IDocument document = viewer.getDocument();
    if (document == null) {
      return;
    }

    if (document instanceof IDocumentExtension4) {
      int offset = selection.getOffset();
      IRegion markOccurrenceTargetRegion = fMarkOccurrenceTargetRegion;
      if (markOccurrenceTargetRegion != null) {
        if (markOccurrenceTargetRegion.getOffset() <= offset
            && offset <= markOccurrenceTargetRegion.getOffset()
                + markOccurrenceTargetRegion.getLength()) {
          if (selection.getLength() > 0
              && selection.getLength() != fMarkOccurrenceTargetRegion.getLength()) {
            removeOccurrenceAnnotations();
          }
          return;
        }
      }
      fMarkOccurrenceTargetRegion = DartWordFinder.findWord(document, offset);
      if (selection.getLength() > 0
          && selection.getLength() != fMarkOccurrenceTargetRegion.getLength()) {
        removeOccurrenceAnnotations();
        return;
      }
    }

    DartX.todo("marking");
    Collection<AstNode> matches = null;

    NodeLocator locator = new NodeLocator(selection.getOffset(), selection.getOffset()
        + selection.getLength());
    AstNode selectedNode = locator.searchWithin(unit);

    if (matches == null && selectedNode != null) {
      if (selectedNode instanceof SimpleIdentifier) {
        SimpleIdentifier ident = (SimpleIdentifier) selectedNode;
        matches = com.google.dart.engine.services.util.NameOccurrencesFinder.findIn(ident, unit);
      }
    }
    if (matches == null || matches.size() == 0) {
      removeOccurrenceAnnotations();
      return;
    }

    Position[] positions = new Position[matches.size()];
    {
      int i = 0;
      for (Iterator<AstNode> each = matches.iterator(); each.hasNext();) {
        AstNode currentNode = each.next();
        positions[i++] = new Position(currentNode.getOffset(), currentNode.getLength());
      }
    }

    // Add occurrence annotations
    addAnnotations(document, positions);
  }
}
