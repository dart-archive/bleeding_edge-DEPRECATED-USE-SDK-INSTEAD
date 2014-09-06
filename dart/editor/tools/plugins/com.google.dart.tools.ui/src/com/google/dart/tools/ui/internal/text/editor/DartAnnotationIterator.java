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
package com.google.dart.tools.ui.internal.text.editor;

import org.eclipse.jface.text.quickassist.IQuickFixableAnnotation;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import java.util.Collections;
import java.util.Iterator;

/**
 * Filters problems based on their types.
 */
@SuppressWarnings("rawtypes")
public class DartAnnotationIterator implements Iterator {

  private Iterator fIterator;
  private Annotation fNext;
  private boolean fSkipIrrelevants;
  private boolean fReturnAllAnnotations;

  /**
   * Equivalent to <code>DartAnnotationIterator(model, skipIrrelevants, false)</code>.
   */
  public DartAnnotationIterator(IAnnotationModel model, boolean skipIrrelevants) {
    this(model, skipIrrelevants, false);
  }

  /**
   * Returns a new DartAnnotationIterator.
   * 
   * @param model the annotation model
   * @param skipIrrelevants whether to skip irrelevant annotations
   * @param returnAllAnnotations Whether to return non IJavaAnnotations as well
   */
  public DartAnnotationIterator(IAnnotationModel model, boolean skipIrrelevants,
      boolean returnAllAnnotations) {
    fReturnAllAnnotations = returnAllAnnotations;
    if (model != null) {
      fIterator = model.getAnnotationIterator();
    } else {
      fIterator = Collections.EMPTY_LIST.iterator();
    }
    fSkipIrrelevants = skipIrrelevants;
    skip();
  }

  /*
   * @see Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    return fNext != null;
  }

  /*
   * @see Iterator#next()
   */
  @Override
  public Object next() {
    try {
      return fNext;
    } finally {
      skip();
    }
  }

  /*
   * @see Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  private void skip() {
    while (fIterator.hasNext()) {
      Annotation next = (Annotation) fIterator.next();
      if (next instanceof IQuickFixableAnnotation) {
        if (fSkipIrrelevants) {
          if (!next.isMarkedDeleted()) {
            fNext = next;
            return;
          }
        } else {
          fNext = next;
          return;
        }
      } else if (fReturnAllAnnotations) {
        fNext = next;
        return;
      }
    }
    fNext = null;
  }
}
