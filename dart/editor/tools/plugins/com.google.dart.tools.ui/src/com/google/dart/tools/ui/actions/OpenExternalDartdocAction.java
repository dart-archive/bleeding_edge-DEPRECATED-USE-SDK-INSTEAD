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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.callhierarchy.CallHierarchy;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This action opens an external Dartdoc link.
 */
public class OpenExternalDartdocAction extends SelectionDispatchAction {

  private DartEditor editor;

  private DartElement selectedElement = null;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor internal
   */
  public OpenExternalDartdocAction(DartEditor editor) {
    this(editor.getEditorSite());
    this.editor = editor;
  }

  /**
   * Creates a new <code>OpenExternalDartdocAction</code>. The action requires that the selection
   * provided by the site's selection provider is of type
   * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param site the site providing context information for this action
   */
  public OpenExternalDartdocAction(IWorkbenchSite site) {
    super(site);
    setText(ActionMessages.OpenExternalDartdocAction_label);
    setDescription(ActionMessages.OpenExternalDartdocAction_description);
    setToolTipText(ActionMessages.OpenExternalDartdocAction_tooltip);
  }

  @Override
  public void run(ITextSelection selection) {
    selectedElement = getDartElementToOpen(selection);
    if (selectedElement == null) {
      return;
    }
    Type type = getTypeParent(selectedElement);
    DartLibrary library = getDartLibraryParent(selectedElement);
    if (library == null) {
      return;
    }
    String libraryName = library.getElementName();
    libraryName = libraryName.replace(':', '_');
    if (type != null) {
      String classNameHTML = type.getElementName();
      classNameHTML = classNameHTML.substring(0, classNameHTML.length()) + ".html";
      openDocsInBrowser("http://api.dartlang.org/docs/continuous/" + libraryName + '/'
          + classNameHTML);
    } else {
      openDocsInBrowser("http://api.dartlang.org/docs/continuous/" + libraryName + ".html");
    }

  }

  /**
   * Notifies this action that the given selection has changed. This default implementation sets the
   * action's enablement state to <code>false</code>.
   * 
   * @param selection the new selection
   */
  @Override
  public void selectionChanged(ISelection selection) {
    // overridden to not set enabled to false
  }

  /**
   * This is called by {@link OpenExternalDartdocAction} each time the {@link ActionContext} is
   * changed. This is what enables or disables the action in the context menu in the Editor.
   */
  public void updateEnabled(ISelection selection) {
    if (selection instanceof ITextSelection) {
      selectedElement = getDartElementToOpen((ITextSelection) selection);
      setEnabled(selectedElement != null);
    } else {
      setEnabled(false);
    }
  }

  /**
   * Given some {@link DartElement}, this method returns the parent {@link CompilationUnit}, if
   * there is no such parent, <code>null</code> is returned.
   * 
   * @param dartElement some contained {@link DartElement}
   * @return the parent {@link CompilationUnit}, or <code>null</code>
   */
  private CompilationUnit getCompilationUnitParent(DartElement dartElement) {
    DartElement parent = dartElement;
    while (parent != null && parent.getElementType() != DartElement.COMPILATION_UNIT) {
      parent = parent.getParent();
    }
    if (parent == null) {
      return null;
    } else {
      return (CompilationUnit) parent;
    }
  }

  /**
   * Given some {@link ITextSelection}, this method returns the {@link CompilationUnit}
   * {@link DartElement} that is a non-null {@link ExternalCompilationUnitImpl}.
   */
  private DartElement getDartElementToOpen(ITextSelection selection) {
    CompilationUnit input = SelectionConverter.getInputAsCompilationUnit(editor);
    if (input == null || selection == null) {
      return null;
    }
    if (!ActionUtil.isProcessable(getShell(), input)) {
      return null;
    }
    try {
      DartElement[] elements = SelectionConverter.codeResolve(editor);
      if (elements == null) {
        return null;
      }
      List<DartElement> candidates = new ArrayList<DartElement>(elements.length);
      for (int i = 0; i < elements.length; i++) {
        DartElement element = elements[i];
        if (CallHierarchy.isPossibleInputElement(element)) {
          candidates.add(element);
        }
      }
      if (candidates.isEmpty()) {
        DartElement enclosingMethod = getEnclosingMethod(input, selection);
        if (enclosingMethod != null) {
          candidates.add(enclosingMethod);
        }
      }
      if (candidates.size() > 0) {
        DartElement dartElement = candidates.get(0);
        CompilationUnit cu = getCompilationUnitParent(dartElement);
        if (cu instanceof ExternalCompilationUnitImpl) {
          return dartElement;
        }
      }
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }

  /**
   * Given some {@link DartElement}, this method returns the parent {@link DartLibrary}, if there is
   * no such parent, <code>null</code> is returned.
   * 
   * @param dartElement some contained {@link DartElement}
   * @return the parent {@link DartLibrary}, or <code>null</code>
   */
  private DartLibrary getDartLibraryParent(DartElement element) {
    DartElement parent = element;
    while (parent != null && parent.getElementType() != DartElement.LIBRARY) {
      parent = parent.getParent();
    }
    if (parent == null) {
      return null;
    } else {
      return (DartLibrary) parent;
    }
  }

  private DartElement getEnclosingMethod(CompilationUnit input, ITextSelection selection) {
    try {
      DartElement enclosingElement = input.getElementAt(selection.getOffset());
      if (enclosingElement instanceof Method || enclosingElement instanceof Field) {
        // opening on the enclosing type would be too confusing (since the type resolves to the constructors)
        return enclosingElement;
      }
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }

  /**
   * Given some {@link DartElement}, this method returns the parent {@link Type}, if there is no
   * such parent, <code>null</code> is returned.
   * 
   * @param dartElement some contained {@link DartElement}
   * @return the parent {@link Type}, or <code>null</code>
   */
  private Type getTypeParent(DartElement dartElement) {
    DartElement parent = dartElement;
    while (parent != null && parent.getElementType() != DartElement.TYPE) {
      parent = parent.getParent();
    }
    if (parent == null) {
      return null;
    } else {
      return (Type) parent;
    }
  }

  //TODO(jwren) Open in Browser functionality should be a shared utility in the product
  private void openDocsInBrowser(String url) {
    if (url == null || url.isEmpty()) {
      return;
    }
    IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
    try {
      IWebBrowser browser = support.getExternalBrowser();
      browser.openURL(new URL(url));
    } catch (MalformedURLException e) {
      DartToolsPlugin.log(e);
    } catch (PartInitException e) {
      DartToolsPlugin.log(e);
    }
  }

}
