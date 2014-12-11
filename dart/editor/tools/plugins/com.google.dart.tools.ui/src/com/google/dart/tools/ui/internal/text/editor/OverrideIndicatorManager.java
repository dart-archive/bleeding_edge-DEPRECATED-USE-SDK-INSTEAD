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

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.server.generated.types.OverriddenMember;
import com.google.dart.server.generated.types.OverrideMember;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.AnalysisServerOverridesListener;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.text.dart.IDartReconcilingListener;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.Assert;
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
import java.util.Set;

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
      if (methodElement.isStatic()) {
        return null;
      }
      // prepare enclosing ClassElement
      if (!(methodElement.getEnclosingElement() instanceof ClassElement)) {
        return null;
      }
      ClassElement classElement = methodElement.getEnclosingElement();
      // check super classes
      String methodName = methodElement.getDisplayName();
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

    private Element findOverriddenElement(ExecutableElement element) {
      ElementKind kind = element.getKind();
      if (kind == ElementKind.METHOD) {
        return findOveriddenMethod((MethodElement) element);
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
    private com.google.dart.server.generated.types.Element element;
    private Element element_OLD;

    OverrideIndicator(com.google.dart.server.generated.types.Element element, String text,
        boolean isOverride) {
      super(ANNOTATION_TYPE, false, text);
      this.isOverride = isOverride;
      this.element = element;
    }

    OverrideIndicator(Element astElement, String text, boolean isOverride) {
      super(ANNOTATION_TYPE, false, text);
      this.isOverride = isOverride;
      this.element_OLD = astElement;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof OverrideIndicator) {
        OverrideIndicator other = (OverrideIndicator) obj;
        return other.isOverride == isOverride && Objects.equal(other.element, element)
            && Objects.equal(other.element_OLD, element_OLD);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(isOverride, element, element_OLD);
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
          DartUI.openInEditor(element, true);
        } else if (element_OLD != null) {
          DartUI.openInEditor(element_OLD);
        } else {
          String title = DartEditorMessages.OverrideIndicatorManager_open_error_title;
          String message = DartEditorMessages.OverrideIndicatorManager_open_error_message;
          MessageDialog.openError(DartToolsPlugin.getActiveWorkbenchShell(), title, message);
        }
      } catch (Exception e) {
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
  private Set<Annotation> overrideAnnotations = Sets.newHashSet();
  private CompilationUnitEditor dartEditor;
  private String file;
  private IDartReconcilingListener reconcileListener = new IDartReconcilingListener() {
    @Override
    public void reconciled(CompilationUnit unit) {
      updateAnnotations(unit, new NullProgressMonitor());
    }
  };

  private AnalysisServerOverridesListener overridesListener = new AnalysisServerOverridesListener() {
    @Override
    public void computedHighlights(String _file, OverrideMember[] overrides) {
      if (Objects.equal(file, _file)) {
        updateAnnotations(overrides);
      }
    }
  };

  public OverrideIndicatorManager(IAnnotationModel annotationModel) {
    Assert.isLegal(DartCoreDebug.ENABLE_ANALYSIS_SERVER);
    Assert.isNotNull(annotationModel);
    this.annotationModel = annotationModel;
    this.annotationModelLockObject = getLockObject(annotationModel);
  }

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
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        file = dartEditor.getInputFilePath();
        DartCore.getAnalysisServerData().addOverridesListener(file, overridesListener);
      } else {
        dartEditor.addReconcileListener(reconcileListener);
      }
    }
  }

  public void uninstall() {
    if (dartEditor != null) {
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        DartCore.getAnalysisServerData().removeOverridesListener(file, overridesListener);
      } else {
        dartEditor.removeReconcileListener(reconcileListener);
      }
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
    updateAnnotations(annotationMap);
  }

  /**
   * Updates the override and implements annotations based on the given {@link OverrideMember}s.
   */
  protected void updateAnnotations(OverrideMember[] overrides) {
    // add annotations
    Map<Annotation, Position> annotationMap = Maps.newHashMap();
    for (OverrideMember override : overrides) {
      OverriddenMember superclassMember = override.getSuperclassMember();
      List<OverriddenMember> interfaceMembers = override.getInterfaceMembers();
      // prepare target
      boolean isOverride = true;
      com.google.dart.server.generated.types.Element element = null;
      String text = null;
      if (superclassMember != null) {
        element = superclassMember.getElement();
        String memberName = MessageFormat.format(
            "{0}.{1}",
            superclassMember.getClassName(),
            superclassMember.getElement().getName());
        text = Messages.format(DartEditorMessages.OverrideIndicatorManager_overrides, memberName);
      } else if (!interfaceMembers.isEmpty()) {
        isOverride = false;
        OverriddenMember interfaceMember = interfaceMembers.get(0);
        element = interfaceMember.getElement();
        String memberName = MessageFormat.format(
            "{0}.{1}",
            interfaceMember.getClassName(),
            interfaceMember.getElement().getName());
        text = Messages.format(DartEditorMessages.OverrideIndicatorManager_implements, memberName);
      }
      // shouldn't happen
      if (element == null) {
        continue;
      }
      // add override annotation
      Position position = new Position(override.getOffset(), override.getLength());
      annotationMap.put(new OverrideIndicator(element, text, isOverride), position);
    }
    // add annotations to the model
    updateAnnotations(annotationMap);
  }

  /**
   * Removes all override indicators from this manager's annotation model.
   */
  void removeAnnotations() {
    if (overrideAnnotations.isEmpty()) {
      return;
    }
    // remove annotations from the model
    synchronized (annotationModelLockObject) {
      if (annotationModel instanceof IAnnotationModelExtension) {
        ((IAnnotationModelExtension) annotationModel).replaceAnnotations(
            overrideAnnotations.toArray(new Annotation[overrideAnnotations.size()]),
            null);
      } else {
        for (Annotation annotation : overrideAnnotations) {
          annotationModel.removeAnnotation(annotation);
        }
      }
      overrideAnnotations = Sets.newHashSet();
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

  private void updateAnnotations(Map<Annotation, Position> annotationMap) {
    synchronized (annotationModelLockObject) {
      Set<Annotation> newAnnotations = Sets.newHashSet(annotationMap.keySet());
      if (annotationModel instanceof IAnnotationModelExtension) {
        Set<Annotation> removedAnnotations = Sets.newHashSet();
        for (Annotation annotation : overrideAnnotations) {
          Position oldPosition = annotationModel.getPosition(annotation);
          // if the same position for the same annotation, ignore it
          Position newPosition = annotationMap.get(annotation);
          if (oldPosition.equals(newPosition)) {
            annotationMap.remove(annotation);
            continue;
          }
          // otherwise, replace the annotation
          removedAnnotations.add(annotation);
          annotationMap.remove(annotation);
        }
        // update model only if there are changes
        if (!removedAnnotations.isEmpty() || !annotationMap.isEmpty()) {
          ((IAnnotationModelExtension) annotationModel).replaceAnnotations(
              removedAnnotations.toArray(new Annotation[removedAnnotations.size()]),
              annotationMap);
        }
      } else {
        removeAnnotations();
        Iterator<Map.Entry<Annotation, Position>> iter = annotationMap.entrySet().iterator();
        while (iter.hasNext()) {
          Map.Entry<Annotation, Position> mapEntry = iter.next();
          annotationModel.addAnnotation(mapEntry.getKey(), mapEntry.getValue());
        }
      }
      overrideAnnotations = newAnnotations;
    }
  }
}
