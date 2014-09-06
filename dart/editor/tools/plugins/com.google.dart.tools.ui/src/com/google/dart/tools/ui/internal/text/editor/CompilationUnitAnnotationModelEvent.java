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

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.MarkerAnnotation;

/**
 * Event sent out by changes of the compilation unit annotation model.
 */
public class CompilationUnitAnnotationModelEvent extends AnnotationModelEvent {

  private boolean fIncludesProblemMarkerAnnotations;
  private IResource fUnderlyingResource;

  /**
   * Constructor for CompilationUnitAnnotationModelEvent.
   * 
   * @param model
   * @param underlyingResource The annotation model's underlying resource
   */
  public CompilationUnitAnnotationModelEvent(IAnnotationModel model, IResource underlyingResource) {
    super(model);
    fUnderlyingResource = underlyingResource;
    fIncludesProblemMarkerAnnotations = false;
  }

  /*
   * @see org.eclipse.jface.text.source.AnnotationModelEvent#annotationAdded(org.
   * eclipse.jface.text.source.Annotation)
   */
  @Override
  public void annotationAdded(Annotation annotation) {
    super.annotationAdded(annotation);
    testIfProblemMarker(annotation);
  }

  /*
   * @see org.eclipse.jface.text.source.AnnotationModelEvent#annotationChanged(org
   * .eclipse.jface.text.source.Annotation)
   */
  @Override
  public void annotationChanged(Annotation annotation) {
    testIfProblemMarker(annotation);
    super.annotationChanged(annotation);
  }

  /*
   * @see org.eclipse.jface.text.source.AnnotationModelEvent#annotationRemoved(org
   * .eclipse.jface.text.source.Annotation)
   */
  @Override
  public void annotationRemoved(Annotation annotation) {
    super.annotationRemoved(annotation);
    testIfProblemMarker(annotation);
  }

  /*
   * @see org.eclipse.jface.text.source.AnnotationModelEvent#annotationRemoved(org
   * .eclipse.jface.text.source.Annotation, org.eclipse.jface.text.Position)
   */
  @Override
  public void annotationRemoved(Annotation annotation, Position position) {
    super.annotationRemoved(annotation, position);
    testIfProblemMarker(annotation);
  }

  /**
   * Returns the annotation model's underlying resource
   */
  public IResource getUnderlyingResource() {
    return fUnderlyingResource;
  }

  /**
   * Returns whether the change included problem marker annotations.
   * 
   * @return <code>true</code> if the change included marker annotations
   */
  public boolean includesProblemMarkerAnnotationChanges() {
    return fIncludesProblemMarkerAnnotations;
  }

  private void testIfProblemMarker(Annotation annotation) {
    if (fIncludesProblemMarkerAnnotations) {
      return;
    }
    if (annotation instanceof MarkerAnnotation) {
      try {
        IMarker marker = ((MarkerAnnotation) annotation).getMarker();
        if (!marker.exists() || marker.isSubtypeOf(IMarker.PROBLEM)) {
          fIncludesProblemMarkerAnnotations = true;
        }
      } catch (CoreException e) {
        DartToolsPlugin.log(e);
      }
    }
  }

}
