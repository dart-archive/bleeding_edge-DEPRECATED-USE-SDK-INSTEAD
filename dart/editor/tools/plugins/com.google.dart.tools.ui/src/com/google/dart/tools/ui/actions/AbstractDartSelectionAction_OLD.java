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
package com.google.dart.tools.ui.actions;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.tools.ui.internal.refactoring.RefactoringUtils;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.text.editor.LightNodeElement;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Abstract class selection-based actions.
 */
public abstract class AbstractDartSelectionAction_OLD extends InstrumentedSelectionDispatchAction {
  /**
   * Waits for background analysis, to be sure that {@link AssistContext} will stay actual. Then
   * requests {@link AssistContext} from the given {@link DartSelection}.
   * 
   * @return the {@link AssistContext}, may be {@code null} if waiting of background analysis was
   *         cancelled, or {@link DartSelection} has no context.
   */
  protected static AssistContext getContextAfterBuild(DartSelection selection) {
    if (!RefactoringUtils.waitReadyForRefactoring()) {
      return null;
    }
    return selection.getContext();
  }

  /**
   * @return the {@link IFile} corresponding to the given {@link DartSelection}. May be {@code null}
   *         .
   */
  protected static IFile getSelectionContextFile(DartSelection selection) {
    DartEditor editor = selection.getEditor();
    if (editor != null) {
      return editor.getInputResourceFile();
    }
    return null;
  }

  /**
   * @return the {@link IFile} corresponding to the given {@link IStructuredSelection}. May be
   *         {@code null}.
   */
  protected static IFile getSelectionContextFile(IStructuredSelection selection) {
    if (selection.size() == 1) {
      Object object = selection.getFirstElement();
      if (object instanceof LightNodeElement) {
        return ((LightNodeElement) object).getContextFile();
      }
    }
    return null;
  }

  /**
   * @return the {@link Element} covered by the given {@link DartSelection}, may be {@code null}.
   */
  protected static Element getSelectionElement(DartSelection selection) {
    AssistContext context = selection.getContext();
    if (context != null) {
      return context.getCoveredElement();
    }
    return null;
  }

  /**
   * @return the only {@link Element} in the given {@link IStructuredSelection}. May be {@code null}
   *         .
   */
  protected static Element getSelectionElement(IStructuredSelection selection) {
    if (selection.size() == 1) {
      Object object = selection.getFirstElement();
      if (object instanceof Element) {
        return (Element) object;
      }
      if (object instanceof LightNodeElement) {
        return ((LightNodeElement) object).getElement();
      }
    }
    return null;
  }

  /**
   * @return the {@link AstNode} covered by the given {@link DartSelection}, may be {@code null}.
   */
  protected static AstNode getSelectionNode(DartSelection selection) {
    AssistContext context = selection.getContext();
    if (context != null) {
      return context.getCoveredNode();
    }
    return null;
  }

  /**
   * @return {@code true} if given {@link AstNode} and {@link Element} are interesting in broad
   *         meaning, i.e. we can do something with it - open, find, etc.
   */
  protected static boolean isInterestingElement(AstNode node, Element element) {
    // no node - probably impossible
    if (node == null) {
      return false;
    }
    // no element - not resolved, or not resolvable node
    if (element == null) {
      return false;
    }
    // LibraryElement, bad selection in the most cases
    if (element instanceof LibraryElement) {
      if (node != null && node.getAncestor(NamespaceDirective.class) != null) {
      } else {
        return false;
      }
    }
    // CompilationUnit, bad selection in the most cases
    if (element instanceof CompilationUnitElement) {
      if (node != null
          && (node.getAncestor(PartDirective.class) != null || node.getAncestor(PartOfDirective.class) != null)) {
        // OK, unit reference in "part" 
      } else {
        // cursor outside of any node
        return false;
      }
    }
    // OK
    return true;
  }

  /**
   * @return {@code true} if {@link Element} covered by the given {@link DartSelection} is
   *         interesting in broad meaning, i.e. we can do something with it - open, find, etc.
   */
  protected static boolean isInterestingElementSelected(DartSelection selection) {
    AstNode node = getSelectionNode(selection);
    Element element = getSelectionElement(selection);
    return isInterestingElement(node, element);
  }

  protected final DartEditor editor;

  public AbstractDartSelectionAction_OLD(DartEditor editor) {
    super(editor.getEditorSite());
    this.editor = editor;
    init();
  }

  public AbstractDartSelectionAction_OLD(IWorkbenchSite site) {
    super(site);
    this.editor = null;
    init();
  }

  /**
   * Called once by the constructors to initialize label, tooltip, image. To be overridden by
   * subclasses.
   */
  protected abstract void init();
}
