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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.callhierarchy.CallHierarchy;
import com.google.dart.tools.ui.internal.filesview.IDartNode;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This action opens an external Dartdoc link.
 */
public class OpenExternalDartdocAction_OLD extends InstrumentedSelectionDispatchAction {

  private DartEditor editor;

  private DartElement selectedElement = null;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor internal
   */
  public OpenExternalDartdocAction_OLD(DartEditor editor) {
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
  public OpenExternalDartdocAction_OLD(IWorkbenchSite site) {
    super(site);
    setText(ActionMessages.OpenExternalDartdocAction_label);
    setDescription(ActionMessages.OpenExternalDartdocAction_description);
    setToolTipText(ActionMessages.OpenExternalDartdocAction_tooltip);
  }

  /**
   * Notifies this action that the given selection has changed. This method overrides the default
   * implementation to prevent the action's enablement state from being set to {@code false}.
   */
  @Override
  public void selectionChanged(ISelection selection) {
    // overridden to not set enabled to false
  }

  /**
   * This is called by {@link OpenExternalDartdocAction_OLD} each time the {@link ActionContext} is
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

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    if (selection == null || selection.isEmpty()) {
      return;
    }
    String libraryName = null;
    boolean browseSdkLibDocs = false;
    Object object = selection.getFirstElement();
    if (object instanceof IDartNode) {
      object = ((IDartNode) object).getFileStore();
    }
    if (object instanceof IFileStore) {
      IFileStore fileStore = (IFileStore) object;
      try {
        object = fileStore.toLocalFile(0, null);
      } catch (CoreException e) {
        DartCore.logError("Failed to convert to local file", e);
      }
    }
    if (object instanceof File) {
      IPath path = new Path(((File) object).getAbsolutePath());
      File sdkLibDir = DartSdkManager.getManager().getSdk().getLibraryDirectory();
      IPath sdkLibPath = new Path(sdkLibDir.getAbsolutePath());
      if (sdkLibPath.equals(path)) {
        browseSdkLibDocs = true;
      } else if (sdkLibPath.isPrefixOf(path)) {
        libraryName = "dart:" + path.segment(sdkLibPath.segmentCount());
      }
    }
    if (libraryName != null || browseSdkLibDocs) {
      browseDartDoc(libraryName, null, instrumentation);
    }
  }

  @Override
  protected void doRun(ITextSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    selectedElement = getDartElementToOpen(selection);

    if (selectedElement == null) {
      instrumentation.metric("Problem", "selectedElement was null");
      return;
    }

    DartLibrary library = getDartLibraryParent(selectedElement);
    if (library == null) {
      instrumentation.metric("Problem", "library was null");
      return;
    }
    String libraryName = library.getElementName();

    Type type = getTypeParent(selectedElement);
    String className = type != null ? type.getElementName() : null;

    browseDartDoc(libraryName, className, instrumentation);
  }

  /**
   * Open an external browser on the Dart doc for the specified library and class.
   * 
   * @param libraryName the library name (not {@code null})
   * @param className the class name or {@code null} to display library Dart doc
   */
  private void browseDartDoc(String libraryName, String className,
      UIInstrumentationBuilder instrumentation) {
    String url = "http://api.dartlang.org/";
    if (libraryName != null) {
      url += libraryName.replace(':', '_');
      if (className != null) {
        url += '/' + className;
      }
      url += ".html";
    }

    instrumentation.metric("ClickTarget", className != null ? "type" : "library");
    instrumentation.data("LibraryName", libraryName).data("ClassNameHtml", className);
    instrumentation.data("Url", url);

    ExternalBrowserUtil.openInExternalBrowser(url);
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

}
