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

import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.dart.IDartReconcilingListener;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages the override and overwrite indicators for the given Java element and annotation model.
 */
class OverrideIndicatorManager implements IDartReconcilingListener {

  /**
   * Overwrite and override indicator annotation.
   */
  class OverrideIndicator extends Annotation {

    private boolean fIsOverwriteIndicator;
    private String fAstNodeKey;

    /**
     * Creates a new override annotation.
     * 
     * @param isOverwriteIndicator <code>true</code> if this annotation is an overwrite indicator,
     *          <code>false</code> otherwise
     * @param text the text associated with this annotation
     * @param key the method binding key
     */
    OverrideIndicator(boolean isOverwriteIndicator, String text, String key) {
      super(ANNOTATION_TYPE, false, text);
      fIsOverwriteIndicator = isOverwriteIndicator;
      fAstNodeKey = key;
    }

    /**
     * Tells whether this is an overwrite or an override indicator.
     * 
     * @return <code>true</code> if this is an overwrite indicator
     */
    public boolean isOverwriteIndicator() {
      return fIsOverwriteIndicator;
    }

    /**
     * Opens and reveals the defining method.
     */
    public void open() {
      DartUnit ast = ASTProvider.getASTProvider().getAST(fJavaElement,
          ASTProvider.WAIT_ACTIVE_ONLY, null);
      if (ast != null) {
        //TODO (pquitslund): implement override decorator in editor ruler
//        DartNode node = ast.findDeclaringNode(fAstNodeKey);
//        if (node instanceof DartMethodDefinition) {
//          try {
//            IFunctionBinding methodBinding = ((DartMethodDefinition) node).resolveBinding();
//            IFunctionBinding definingMethodBinding = Bindings.findOverriddenMethod(
//                methodBinding, true);
//            if (definingMethodBinding != null) {
//              DartElement definingMethod = definingMethodBinding.getElement();
//              if (definingMethod != null) {
//                DartUI.openInEditor(definingMethod, true, true);
//                return;
//              }
//            }
//          } catch (CoreException e) {
//            ExceptionHandler.handle(
//                e,
//                DartEditorMessages.OverrideIndicatorManager_open_error_title,
//                DartEditorMessages.OverrideIndicatorManager_open_error_messageHasLogEntry);
//            return;
//          }
//        }
      }
      String title = DartEditorMessages.OverrideIndicatorManager_open_error_title;
      String message = DartEditorMessages.OverrideIndicatorManager_open_error_message;
      MessageDialog.openError(DartToolsPlugin.getActiveWorkbenchShell(), title, message);
    }
  }

  static final String ANNOTATION_TYPE = "com.google.dart.tools.ui.overrideIndicator"; //$NON-NLS-1$

  private IAnnotationModel fAnnotationModel;
  private Object fAnnotationModelLockObject;
  private Annotation[] fOverrideAnnotations;
  private DartElement fJavaElement;

  public OverrideIndicatorManager(IAnnotationModel annotationModel, DartElement javaElement,
      DartUnit ast) {
    Assert.isNotNull(annotationModel);
    Assert.isNotNull(javaElement);

    fJavaElement = javaElement;
    fAnnotationModel = annotationModel;
    fAnnotationModelLockObject = getLockObject(fAnnotationModel);

    updateAnnotations(ast, new NullProgressMonitor());
  }

  /*
   * @see com.google.dart.tools.ui.functions.java.IJavaReconcilingListener# aboutToBeReconciled()
   */
  @Override
  public void aboutToBeReconciled() {
  }

  /*
   * @see com.google.dart.tools.ui.functions.java.IJavaReconcilingListener#reconciled (DartUnit,
   * boolean, IProgressMonitor)
   */
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

    final Map<Annotation, Position> annotationMap = new HashMap<Annotation, Position>(50);

    DartNodeTraverser<Void> visitor = new DartNodeTraverser<Void>() {
      /*
       * @see org.eclipse.wst.jsdt.core.dom.ASTVisitor#visit(org.eclipse.wst.jsdt
       * .core.dom.FunctionDeclaration)
       */
      @Override
      public Void visitMethodDefinition(DartMethodDefinition node) {
        //TODO (pquitslund): add annotation support for methods
//        IFunctionBinding binding = node.resolveBinding();
//        if (binding != null) {
//          IFunctionBinding definingMethod = Bindings.findOverriddenMethod(
//              binding, true);
//          if (definingMethod != null) {
//
//            ITypeBinding definingType = definingMethod.getDeclaringClass();
//            String qualifiedMethodName = definingType.getQualifiedName()
//                + "." + binding.getName(); //$NON-NLS-1$
//
//            boolean isImplements = JdtFlags.isAbstract(definingMethod);
//            String text;
//            if (isImplements)
//              text = Messages.format(
//                  DartEditorMessages.OverrideIndicatorManager_implements,
//                  qualifiedMethodName);
//            else
//              text = Messages.format(
//                  DartEditorMessages.OverrideIndicatorManager_overrides,
//                  qualifiedMethodName);
//
//            DartIdentifier name = node.getName();
//            Position position = new Position(name.getSourceStart(),
//                name.getSourceLength());
//
//            annotationMap.put(
//                new OverrideIndicator(isImplements, text, binding.getKey()),
//                position);
//
//          }
//        }
        node.visitChildren(this);
        return null;
      }
    };
    ast.accept(visitor);

    if (progressMonitor.isCanceled()) {
      return;
    }

    synchronized (fAnnotationModelLockObject) {
      if (fAnnotationModel instanceof IAnnotationModelExtension) {
        ((IAnnotationModelExtension) fAnnotationModel).replaceAnnotations(fOverrideAnnotations,
            annotationMap);
      } else {
        removeAnnotations();
        Iterator<Map.Entry<Annotation, Position>> iter = annotationMap.entrySet().iterator();
        while (iter.hasNext()) {
          Map.Entry<Annotation, Position> mapEntry = iter.next();
          fAnnotationModel.addAnnotation(mapEntry.getKey(), mapEntry.getValue());
        }
      }
      fOverrideAnnotations = annotationMap.keySet().toArray(
          new Annotation[annotationMap.keySet().size()]);
    }
  }

  /**
   * Removes all override indicators from this manager's annotation model.
   */
  void removeAnnotations() {
    if (fOverrideAnnotations == null) {
      return;
    }

    synchronized (fAnnotationModelLockObject) {
      if (fAnnotationModel instanceof IAnnotationModelExtension) {
        ((IAnnotationModelExtension) fAnnotationModel).replaceAnnotations(fOverrideAnnotations,
            null);
      } else {
        for (int i = 0, length = fOverrideAnnotations.length; i < length; i++) {
          fAnnotationModel.removeAnnotation(fOverrideAnnotations[i]);
        }
      }
      fOverrideAnnotations = null;
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
