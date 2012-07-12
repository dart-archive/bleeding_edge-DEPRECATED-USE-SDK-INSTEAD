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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.common.collect.Maps;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.resolver.MethodNodeElement;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.utilities.ast.DartElementLocator;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.text.dart.IDartReconcilingListener;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages the override and overwrite indicators for the given Dart element and annotation model.
 */
class OverrideIndicatorManager implements IDartReconcilingListener {
  /**
   * Overwrite and override indicator annotation.
   */
  class OverrideIndicator extends Annotation {
    private final boolean isOverride;
    private final DartElement dartElement;

    OverrideIndicator(Element astElement, String text, boolean isOverride) {
      super(ANNOTATION_TYPE, false, text);
      this.isOverride = isOverride;
      // TODO(scheglov) we (probably) can optimize this, don't compiler Element's, but also don't
      // create DartElement. We need to remember enough lightweight information to do this.
      {
        CompilationUnit dartUnit = managerDartElement.getAncestor(CompilationUnit.class);
        DartElementLocator locator = new DartElementLocator(dartUnit, astElement);
        dartElement = locator.getFoundElement();
      }
    }

    /**
     * @return <code>true</code> if replacing of the exiting element, or <code>false</code> if new
     *         implementation of abstract element.
     */
    public boolean isOverride() {
      return isOverride;
    }

    /**
     * Opens and reveals the defining element.
     */
    public void open() {
      try {
        if (dartElement != null) {
          DartUI.openInEditor(dartElement);
        } else {
          String title = DartEditorMessages.OverrideIndicatorManager_open_error_title;
          String message = DartEditorMessages.OverrideIndicatorManager_open_error_message;
          MessageDialog.openError(DartToolsPlugin.getActiveWorkbenchShell(), title, message);
        }
      } catch (CoreException e) {
        ExceptionHandler.handle(
            e,
            DartEditorMessages.OverrideIndicatorManager_open_error_title,
            DartEditorMessages.OverrideIndicatorManager_open_error_messageHasLogEntry);
      }
    }
  }

  static final String ANNOTATION_TYPE = "com.google.dart.tools.ui.overrideIndicator"; //$NON-NLS-1$

  /**
   * @return <code>true</code> if given {@link Element} has implementation.
   */
  private static boolean hasImplementation(Element element) {
    if (element instanceof MethodElement) {
      return ((MethodElement) element).hasBody();
    }
    return true;
  }

  private final DartElement managerDartElement;
  private final IAnnotationModel annotationModel;
  private final Object annotationModelLockObject;
  private Annotation[] overrideAnnotations;

  public OverrideIndicatorManager(IAnnotationModel annotationModel, DartElement dartElement,
      DartUnit ast) {
    Assert.isNotNull(annotationModel);
    Assert.isNotNull(dartElement);
    // assign to fields
    this.managerDartElement = dartElement;
    this.annotationModel = annotationModel;
    this.annotationModelLockObject = getLockObject(annotationModel);
    // prepare initial annotations
    updateAnnotations(ast, new NullProgressMonitor());
  }

  @Override
  public void aboutToBeReconciled() {
  }

  @Override
  public void reconciled(DartUnit ast, boolean forced, IProgressMonitor progressMonitor) {
    updateAnnotations(ast, progressMonitor);
  }

  /**
   * Updates the override and implements annotations based on the given AST.
   * 
   * @param ast the compilation unit AST
   * @param progressMonitor the progress monitor
   */
  protected void updateAnnotations(DartUnit ast, IProgressMonitor progressMonitor) {
    if (ast == null || progressMonitor.isCanceled()) {
      return;
    }
    // add annotations
    final Map<Annotation, Position> annotationMap = Maps.newHashMapWithExpectedSize(50);
    ASTVisitor<Void> visitor = new ASTVisitor<Void>() {
      @Override
      public Void visitMethodDefinition(DartMethodDefinition node) {
        MethodNodeElement methodElement = node.getElement();
        if (methodElement != null) {
          for (Element superElement : methodElement.getOverridden()) {
            boolean isOverride = hasImplementation(superElement);
            // prepare "super" method name
            String qualifiedMethodName = MessageFormat.format(
                "{0}.{1}",
                superElement.getEnclosingElement().getName(),
                superElement.getName());
            // prepare text
            String text;
            if (isOverride) {
              text = Messages.format(
                  DartEditorMessages.OverrideIndicatorManager_overrides,
                  qualifiedMethodName);
            } else {
              text = Messages.format(
                  DartEditorMessages.OverrideIndicatorManager_implements,
                  qualifiedMethodName);
            }
            // prepare "super" name of implemented
            DartExpression name = node.getName();
            Position position = new Position(
                name.getSourceInfo().getOffset(),
                name.getSourceInfo().getLength());
            // add override annotation
            annotationMap.put(new OverrideIndicator(superElement, text, isOverride), position);
          }
        }
        node.visitChildren(this);
        return null;
      }
    };
    ast.accept(visitor);
    // may be already cancelled
    if (progressMonitor.isCanceled()) {
      return;
    }
    // add annotations to the model
    synchronized (annotationModelLockObject) {
      if (annotationModel instanceof IAnnotationModelExtension) {
        ((IAnnotationModelExtension) annotationModel).replaceAnnotations(
            overrideAnnotations,
            annotationMap);
      } else {
        removeAnnotations();
        Iterator<Map.Entry<Annotation, Position>> iter = annotationMap.entrySet().iterator();
        while (iter.hasNext()) {
          Map.Entry<Annotation, Position> mapEntry = iter.next();
          annotationModel.addAnnotation(mapEntry.getKey(), mapEntry.getValue());
        }
      }
      overrideAnnotations = annotationMap.keySet().toArray(
          new Annotation[annotationMap.keySet().size()]);
    }
  }

  /**
   * Removes all override indicators from this manager's annotation model.
   */
  void removeAnnotations() {
    if (overrideAnnotations == null) {
      return;
    }
    // remove annotations from the model
    synchronized (annotationModelLockObject) {
      if (annotationModel instanceof IAnnotationModelExtension) {
        ((IAnnotationModelExtension) annotationModel).replaceAnnotations(overrideAnnotations, null);
      } else {
        for (int i = 0, length = overrideAnnotations.length; i < length; i++) {
          annotationModel.removeAnnotation(overrideAnnotations[i]);
        }
      }
      overrideAnnotations = null;
    }
  }

  /**
   * Returns the lock object for the given annotation model.
   * 
   * @param annotationModel the annotation model
   * @return the annotation model's lock object
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
}
