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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.model.CompilationUnit;

import java.util.Iterator;

/**
 * Interface of annotations representing markers and problems.
 * 
 * @see org.eclipse.core.resources.IMarker
 * @see org.eclipse.Problem.jsdt.core.compiler.IProblem
 */
public interface IJavaAnnotation {

  /**
   * Adds the given annotation to the list of annotations which are overlaid by this annotations.
   * 
   * @param annotation the problem annotation
   */
  void addOverlaid(IJavaAnnotation annotation);

  /**
   * Returns the problem arguments or <code>null</code> if no problem arguments can be evaluated.
   * 
   * @return returns the problem arguments or <code>null</code> if no problem arguments can be
   *         evaluated.
   */
  String[] getArguments();

  /**
   * Returns the compilation unit corresponding to the document on which the annotation is set or
   * <code>null</code> if no corresponding compilation unit exists.
   */
  CompilationUnit getCompilationUnit();

  /**
   * Returns the problem id or <code>-1</code> if no problem id can be evaluated.
   * 
   * @return returns the problem id or <code>-1</code>
   */
  int getId();

  /**
   * Returns the marker type associated to this problem or <code>null<code> if no marker type
   * can be evaluated. See also {@link org.eclipse.wst.jsdt.core.compiler.CategorizedProblem#getMarkerType()}.
   * 
   * @return the type of the marker which would be associated to the problem or <code>null<code> if
   *         no marker type can be evaluated.
   */
  String getMarkerType();

  /**
   * Returns an iterator for iterating over the annotation which are overlaid by this annotation.
   * 
   * @return an iterator over the overlaid annotations
   */
  Iterator getOverlaidIterator();

  /**
   * Returns the overlay of this annotation.
   * 
   * @return the annotation's overlay
   */
  IJavaAnnotation getOverlay();

  /**
   * @see org.eclipse.jface.text.source.Annotation#getText()
   */
  String getText();

  /**
   * @see org.eclipse.jface.text.source.Annotation#getType()
   */
  String getType();

  /**
   * Returns whether this annotation is overlaid.
   * 
   * @return <code>true</code> if overlaid
   */
  boolean hasOverlay();

  /**
   * @see org.eclipse.jface.text.source.Annotation#isMarkedDeleted()
   */
  boolean isMarkedDeleted();

  /**
   * @see org.eclipse.jface.text.source.Annotation#isPersistent()
   */
  boolean isPersistent();

  /**
   * Tells whether this annotation is a problem annotation.
   * 
   * @return <code>true</code> if it is a problem annotation
   */
  boolean isProblem();

  /**
   * Removes the given annotation from the list of annotations which are overlaid by this
   * annotation.
   * 
   * @param annotation the problem annotation
   */
  void removeOverlaid(IJavaAnnotation annotation);
}
