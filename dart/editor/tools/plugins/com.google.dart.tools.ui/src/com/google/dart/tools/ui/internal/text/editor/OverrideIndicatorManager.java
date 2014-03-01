/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.type.InterfaceType;
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
import org.eclipse.ui.texteditor.ITextEditor;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Manages the override and overwrite indicators for the given Dart element and annotation model.
 */
public class OverrideIndicatorManager {

  public static class OverriddenElementFinder extends GeneralizingAstVisitor<Void> {

    private Map<Annotation, Position> annotationMap = new HashMap<Annotation, Position>();

    @Override
    public Void visitMethodDeclaration(MethodDeclaration node) {
      ExecutableElement methodElement = node.getElement();
      if (methodElement != null) {
        Element superElement = findOverriddenElement(methodElement);
        if (superElement == null) {
          return null;
        }
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
        SimpleIdentifier name = node.getName();
        Position position = new Position(name.getOffset(), name.getLength());
        // add override annotation
        annotationMap.put(new OverrideIndicator(superElement, text, isOverride), position);
      }
      return null;
    }

    private void addSupertypesToQueue(List<InterfaceType> queue, ClassElement classElement) {
      InterfaceType[] mixins = classElement.getMixins();
      for (int i = mixins.length - 1; i >= 0; i--) {
        queue.add(mixins[i]);
      }
      InterfaceType superclass = classElement.getSupertype();
      if (superclass != null) {
        queue.add(superclass);
      }
    }

    private MethodElement findOveriddenMethod(MethodElement methodElement) {
      Element parent = methodElement;
      while (parent != null && !(parent instanceof ClassElement)) {
        parent = parent.getEnclosingElement();
      }
      if (parent == null) {
        return null;
      }
      String methodName = methodElement.getDisplayName();
      ClassElement classElement = (ClassElement) parent;
      List<InterfaceType> queue = new LinkedList<InterfaceType>();
      addSupertypesToQueue(queue, classElement);
      while (!queue.isEmpty()) {
        InterfaceType type = queue.remove(0);
        MethodElement overriddenMethod = type.getMethod(methodName);
        if (overriddenMethod != null) {
          if (hasCompatibleParams(methodElement, overriddenMethod)) {
            return overriddenMethod;
          }
        }
        addSupertypesToQueue(queue, type.getElement());
      }
      return null;
    }

    private Element findOverriddenElement(ExecutableElement methodElement) {
      switch (methodElement.getKind()) {
        case METHOD:
          return findOveriddenMethod((MethodElement) methodElement);
      }
      return null;
    }

    private boolean hasCompatibleParams(MethodElement a, MethodElement b) {
      return a.getParameters().length == b.getParameters().length;
    }
  }

  /**
   * Overwrite and override indicator annotation.
   */
  public static class OverrideIndicator extends Annotation {

    private boolean isOverride;
    private Element element;

    OverrideIndicator(Element astElement, String text, boolean isOverride) {
      super(ANNOTATION_TYPE, false, text);
      this.isOverride = isOverride;
      this.element = astElement;
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
        if (element != null) {
          DartUI.openInEditor(element);
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
    // At this time we are only decorating methods that override a method defined in a superclass or mixin.
    return true;
  }

  private final IAnnotationModel annotationModel;
  private final Object annotationModelLockObject;
  private Annotation[] overrideAnnotations;
  private CompilationUnitEditor dartEditor;
  private IDartReconcilingListener reconcileListener = new IDartReconcilingListener() {
    @Override
    public void reconciled(CompilationUnit unit) {
      updateAnnotations(unit, new NullProgressMonitor());
    }
  };

  public OverrideIndicatorManager(IAnnotationModel annotationModel, CompilationUnit ast) {
    Assert.isNotNull(annotationModel);
    this.annotationModel = annotationModel;
    this.annotationModelLockObject = getLockObject(annotationModel);
    // prepare initial annotations
    updateAnnotations(ast, new NullProgressMonitor());
  }

  public void install(ITextEditor editor) {
    Assert.isLegal(editor != null);
    uninstall();
    if (editor instanceof CompilationUnitEditor) {
      dartEditor = (CompilationUnitEditor) editor;
      dartEditor.addReconcileListener(reconcileListener);
    }
  }

  public void uninstall() {
    if (dartEditor != null) {
      dartEditor.removeReconcileListener(reconcileListener);
      dartEditor = null;
    }
  }

  /**
   * Updates the override and implements annotations based on the given AST.
   * 
   * @param ast the compilation unit AST
   * @param progressMonitor the progress monitor
   */
  protected void updateAnnotations(CompilationUnit ast, IProgressMonitor progressMonitor) {
    if (ast == null || progressMonitor.isCanceled()) {
      return;
    }
    // add annotations
    OverriddenElementFinder visitor = new OverriddenElementFinder();
    ast.accept(visitor);
    Map<Annotation, Position> annotationMap = visitor.annotationMap;
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
