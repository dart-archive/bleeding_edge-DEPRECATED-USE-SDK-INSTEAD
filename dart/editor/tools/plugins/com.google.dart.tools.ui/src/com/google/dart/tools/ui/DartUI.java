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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.ui.dialogs.TypeSelectionExtension;
import com.google.dart.tools.ui.internal.SharedImages;
import com.google.dart.tools.ui.internal.dialogs.FilteredTypesSelectionDialog;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.text.editor.ExternalCompilationUnitEditorInput;
import com.google.dart.tools.ui.text.IColorManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.texteditor.IDocumentProvider;

import java.net.URL;

/**
 * Central access point for the Dart UI plug-in (id <code>"com.google.dart.tools.ui"</code>). This
 * class provides static methods for:
 * <ul>
 * <li>creating various kinds of selection dialogs to present a collection of Dart elements to the
 * user and let them make a selection.</li>
 * <li>opening a Dart editor on a compilation unit.</li>
 * </ul>
 * <p>
 * This class provides static methods and fields only; it is not intended to be instantiated or
 * subclassed by clients.
 * </p>
 * * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public final class DartUI {

  private static ISharedImages fgSharedImages = null;

  /**
   * The id of the JavaScript plug-in (value <code>"com.google.dart.tools.ui"</code>).
   */
  public static final String ID_PLUGIN = "com.google.dart.tools.ui"; //$NON-NLS-1$

  /**
   * The id of the JavaScript perspective (value
   * <code>"com.google.dart.tools.ui.JavaPerspective"</code>).
   */
  public static final String ID_PERSPECTIVE = "com.google.dart.tools.ui.JavaPerspective"; //$NON-NLS-1$

  /**
   * The id of the JavaScript action set (value
   * <code>"com.google.dart.tools.ui.JavaActionSet"</code>).
   */
  public static final String ID_ACTION_SET = "com.google.dart.tools.ui.JavaActionSet"; //$NON-NLS-1$

  /**
   * The id of the JavaScript Element Creation action set (value
   * <code>"com.google.dart.tools.ui.JavaElementCreationActionSet"</code>).
   */
  public static final String ID_ELEMENT_CREATION_ACTION_SET = "com.google.dart.tools.ui.JavaElementCreationActionSet"; //$NON-NLS-1$

  /**
   * The id of the JavaScript Coding action set (value
   * <code>"com.google.dart.tools.ui.CodingActionSet"</code>).
   */
  public static final String ID_CODING_ACTION_SET = "com.google.dart.tools.ui.CodingActionSet"; //$NON-NLS-1$

  /**
   * The id of the JavaScript action set for open actions (value
   * <code>"com.google.dart.tools.ui.A_OpenActionSet"</code>).
   */
  public static final String ID_OPEN_ACTION_SET = "com.google.dart.tools.ui.A_OpenActionSet"; //$NON-NLS-1$

  /**
   * The id of the JavaScript Search action set (value
   * <code>com.google.dart.tools.ui.SearchActionSet"</code>).
   */
  public static final String ID_SEARCH_ACTION_SET = "com.google.dart.tools.ui.SearchActionSet"; //$NON-NLS-1$

  /**
   * The editor part id of the editor that presents JavaScript compilation units (value
   * <code>"com.google.dart.tools.ui.text.editor.CompilationUnitEditor"</code> ).
   */
  public static final String ID_CU_EDITOR = "com.google.dart.tools.ui.text.editor.CompilationUnitEditor"; //$NON-NLS-1$

  /**
   * The editor part id of the editor that presents JavaScript binary class files (value
   * <code>"com.google.dart.tools.ui.ClassFileEditor"</code>).
   */
  public static final String ID_CF_EDITOR = "com.google.dart.tools.ui.ClassFileEditor"; //$NON-NLS-1$

  /**
   * The editor part id of the default text editor (used in the RCP but not the plugin).
   */
  public static final String ID_DEFAULT_TEXT_EDITOR = "com.google.dart.tools.ui.text.editor.TextEditor";

  /**
   * The editor part id of the code snippet editor (value
   * <code>"com.google.dart.tools.ui.SnippetEditor"</code>).
   */
  public static final String ID_SNIPPET_EDITOR = "com.google.dart.tools.ui.SnippetEditor"; //$NON-NLS-1$

  /**
   * The view part id of the Problems view (value
   * <code>"com.google.dart.tools.ui.ProblemsView"</code>).
   */
  public static final String ID_PROBLEMS = "com.google.dart.tools.ui.ProblemsView"; //$NON-NLS-1$

  /**
   * The view part id of the Call Hierarchy view (value
   * <code>"com.google.dart.tools.ui.callhierarchy.view"</code>).
   */
  public static final String ID_CALL_HIERARCHY = "com.google.dart.tools.ui.callhierarchy.view"; //$NON-NLS-1$

  /**
   * The view part id of the type hierarchy part (value
   * <code>"com.google.dart.tools.ui.TypeHierarchy"</code>).
   * <p>
   * When this id is used to access a view part with <code>IWorkbenchPage.findView</code> or
   * <code>showView</code>, the returned <code>IViewPart</code> can be safely cast to an
   * <code>ITypeHierarchyViewPart</code>.
   * </p>
   * 
   * @see ITypeHierarchyViewPart
   * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
   * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
   */
  public static final String ID_TYPE_HIERARCHY = "com.google.dart.tools.ui.TypeHierarchy"; //$NON-NLS-1$

  /**
   * The view part id of the source (declaration) view (value
   * <code>"com.google.dart.tools.ui.SourceView"</code>).
   * 
   * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
   * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
   */
  public static final String ID_SOURCE_VIEW = "com.google.dart.tools.ui.SourceView"; //$NON-NLS-1$

  /**
   * The view part id of the Files view (value <code>"com.google.dart.tools.ui.FileExplorer"</code>
   * ).
   */
  public static final String ID_FILE_EXPLORER = "com.google.dart.tools.ui.FileExplorer"; //$NON-NLS-1$

  /**
   * The view part id of the Apps view (value <code>"com.google.dart.tools.ui.AppsView"</code>).
   */
  public static final String ID_APPS_VIEW = "com.google.dart.tools.ui.AppsView"; //$NON-NLS-1$

  /**
   * The view part id of the Console view (value <code>"com.google.dart.tools.ui.console"</code>).
   */
  public static final String ID_CONSOLE_VIEW = "com.google.dart.tools.ui.console"; //$NON-NLS-1$

  /**
   * The view part id of the Files view (value <code>"com.google.dart.tools.ui.FilesView"</code> ).
   */
  public static final String ID_FILE_VIEW = "com.google.dart.tools.ui.FilesView"; //$NON-NLS-1$

  /**
   * The view part id of the Tests view (<code>"com.google.dart.tools.ui.DartUnitView"</code>).
   */
  public static final String ID_DARTUNIT_VIEW = "com.google.dart.tools.ui.DartUnitView"; //$NON-NLS-1$

  /**
   * The view part id of the JavaScript Documentation view (value
   * <code>"com.google.dart.tools.ui.JavadocView"</code>).
   * 
   * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
   * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
   */
  public static final String ID_JAVADOC_VIEW = "com.google.dart.tools.ui.JavadocView"; //$NON-NLS-1$

  /**
   * The id of the JavaScript Browsing Perspective (value
   * <code>"com.google.dart.tools.ui.JavaBrowsingPerspective"</code>).
   */
  public static String ID_BROWSING_PERSPECTIVE = "com.google.dart.tools.ui.JavaBrowsingPerspective"; //$NON-NLS-1$

  /**
   * The view part id of the JavaScript Browsing Projects view (value
   * <code>"com.google.dart.tools.ui.ProjectsView"</code>).
   */
  public static String ID_PROJECTS_VIEW = "com.google.dart.tools.ui.ProjectsView"; //$NON-NLS-1$

  /**
   * The view part id of the JavaScript Browsing Packages view (value
   * <code>"com.google.dart.tools.ui.PackagesView"</code>).
   */
  public static String ID_PACKAGES_VIEW = "com.google.dart.tools.ui.PackagesView"; //$NON-NLS-1$

  /**
   * The view part id of the JavaScript Browsing Types view (value
   * <code>"com.google.dart.tools.ui.TypesView"</code>).
   */
  public static String ID_TYPES_VIEW = "com.google.dart.tools.ui.TypesView"; //$NON-NLS-1$

  /**
   * The view part id of the JavaScript Browsing Members view (value
   * <code>"com.google.dart.tools.ui.MembersView"</code>).
   */
  public static String ID_MEMBERS_VIEW = "com.google.dart.tools.ui.MembersView"; //$NON-NLS-1$

  /**
   * Creates a selection dialog that lists all packages of the given JavaScript project. The caller
   * is responsible for opening the dialog with <code>Window.open</code>, and subsequently
   * extracting the selected package (of type <code>IPackageFragment</code>) via
   * <code>SelectionDialog.getResult</code>.
   * 
   * @param parent the parent shell of the dialog to be created
   * @param project the JavaScript project
   * @param style flags defining the style of the dialog; the valid flags are:
   *          <code>IDartElementSearchConstants.CONSIDER_BINARIES</code>, indicating that packages
   *          from binary package fragment roots should be included in addition to those from source
   *          package fragment roots;
   *          <code>IDartElementSearchConstants.CONSIDER_REQUIRED_PROJECTS</code> , indicating that
   *          packages from required projects should be included as well.
   * @return a new selection dialog
   * @exception DartModelException if the selection dialog could not be opened
   */
  public static SelectionDialog createPackageDialog(Shell parent, DartProject project, int style)
      throws DartModelException {
    return createPackageDialog(parent, project, style, ""); //$NON-NLS-1$
  }

  /**
   * Creates a selection dialog that lists all packages of the given JavaScript project. The caller
   * is responsible for opening the dialog with <code>Window.open</code>, and subsequently
   * extracting the selected package (of type <code>IPackageFragment</code>) via
   * <code>SelectionDialog.getResult</code>.
   * 
   * @param parent the parent shell of the dialog to be created
   * @param project the JavaScript project
   * @param style flags defining the style of the dialog; the valid flags are:
   *          <code>IDartElementSearchConstants.CONSIDER_BINARIES</code>, indicating that packages
   *          from binary package fragment roots should be included in addition to those from source
   *          package fragment roots;
   *          <code>IDartElementSearchConstants.CONSIDER_REQUIRED_PROJECTS</code> , indicating that
   *          packages from required projects should be included as well.
   * @param filter the initial pattern to filter the set of packages. For example "com" shows all
   *          packages starting with "com". The meta character '?' representing any character and
   *          '*' representing any string are supported. Clients can pass an empty string if no
   *          filtering is required.
   * @return a new selection dialog
   * @exception DartModelException if the selection dialog could not be opened
   */
  public static SelectionDialog createPackageDialog(Shell parent, DartProject project, int style,
      String filter) throws DartModelException {
    DartX.notYet();
    return null;
    // Assert.isTrue((style | IDartElementSearchConstants.CONSIDER_BINARIES |
    // IDartElementSearchConstants.CONSIDER_REQUIRED_PROJECTS) ==
    // (IDartElementSearchConstants.CONSIDER_BINARIES |
    // IDartElementSearchConstants.CONSIDER_REQUIRED_PROJECTS));
    //
    // IPackageFragmentRoot[] roots = null;
    // if ((style & IDartElementSearchConstants.CONSIDER_REQUIRED_PROJECTS) !=
    // 0) {
    // roots = project.getAllPackageFragmentRoots();
    // } else {
    // roots = project.getPackageFragmentRoots();
    // }
    //
    // List consideredRoots = null;
    // if ((style & IDartElementSearchConstants.CONSIDER_BINARIES) != 0) {
    // consideredRoots = Arrays.asList(roots);
    // } else {
    // consideredRoots = new ArrayList(roots.length);
    // for (int i = 0; i < roots.length; i++) {
    // IPackageFragmentRoot root = roots[i];
    // if (root.getKind() != IPackageFragmentRoot.K_BINARY)
    // consideredRoots.add(root);
    //
    // }
    // }
    //
    // IJavaScriptSearchScope searchScope =
    // SearchEngine.createJavaSearchScope((DartElement[])
    // consideredRoots.toArray(new DartElement[consideredRoots.size()]));
    // BusyIndicatorRunnableContext context = new
    // BusyIndicatorRunnableContext();
    // if (style == 0
    // || style == IDartElementSearchConstants.CONSIDER_REQUIRED_PROJECTS) {
    // return createPackageDialog(parent, context, searchScope, false, true,
    // filter);
    // } else {
    // return createPackageDialog(parent, context, searchScope, false, false,
    // filter);
    // }
  }

  /**
   * Creates a selection dialog that lists all types in the given project. The caller is responsible
   * for opening the dialog with <code>Window.open</code>, and subsequently extracting the selected
   * type(s) (of type <code>Type</code> ) via <code>SelectionDialog.getResult</code>.
   * 
   * @param parent the parent shell of the dialog to be created
   * @param context the runnable context used to show progress when the dialog is being populated
   * @param project the JavaScript project
   * @param style flags defining the style of the dialog; the only valid values are
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_INTERFACES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_ANNOTATION_TYPES</code> ,
   *          <code>IDartElementSearchConstants.CONSIDER_ENUMS</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_ALL_TYPES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES</code>
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS</code> . Please note that
   *          the bitwise OR combination of the elementary constants is not supported.
   * @param multipleSelection <code>true</code> if multiple selection is allowed
   * @return a new selection dialog
   * @exception DartModelException if the selection dialog could not be opened
   */
  public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context,
      IProject project, int style, boolean multipleSelection) throws DartModelException {
    // TODO (pquitslund): update to use project scope once implemented in search core
    //DartSearchScope scope = SearchEngine.createJavaSearchScope(
    //                            new DartProject[] {JavaScriptCore.create(project)});
    SearchScope scope = SearchScopeFactory.createWorkspaceScope();
    return createTypeDialog(parent, context, scope, style, multipleSelection);
  }

  /**
   * Creates a selection dialog that lists all packages under the given package fragment root. The
   * caller is responsible for opening the dialog with <code>Window.open</code>, and subsequently
   * extracting the selected package (of type <code>IPackageFragment</code>) via
   * <code>SelectionDialog.getResult</code>.
   * 
   * @param parent the parent shell of the dialog to be created
   * @param root the package fragment root
   * @return a new selection dialog
   * @exception DartModelException if the selection dialog could not be opened
   */
  // public static SelectionDialog createPackageDialog(Shell parent,
  // IPackageFragmentRoot root) throws DartModelException {
  //    return createPackageDialog(parent, root, ""); //$NON-NLS-1$
  // }

  /**
   * Creates a selection dialog that lists all packages under the given package fragment root. The
   * caller is responsible for opening the dialog with <code>Window.open</code>, and subsequently
   * extracting the selected package (of type <code>IPackageFragment</code>) via
   * <code>SelectionDialog.getResult</code>.
   * 
   * @param parent the parent shell of the dialog to be created
   * @param root the package fragment root
   * @param filter the initial pattern to filter the set of packages. For example "com" shows all
   *          packages starting with "com". The meta character '?' representing any character and
   *          '*' representing any string are supported. Clients can pass an empty string if no
   *          filtering is required.
   * @return a new selection dialog
   * @exception DartModelException if the selection dialog could not be opened
   */
  // public static SelectionDialog createPackageDialog(Shell parent,
  // IPackageFragmentRoot root, String filter) throws DartModelException {
  // IJavaScriptSearchScope scope = SearchEngine.createJavaSearchScope(new
  // DartElement[]{root});
  // BusyIndicatorRunnableContext context = new BusyIndicatorRunnableContext();
  // return createPackageDialog(parent, context, scope, false, true, filter);
  // }

  /**
   * Creates a selection dialog that lists all packages of the given JavaScript search scope. The
   * caller is responsible for opening the dialog with <code>Window.open</code>, and subsequently
   * extracting the selected package (of type <code>IPackageFragment</code>) via
   * <code>SelectionDialog.getResult</code>.
   * 
   * @param parent the parent shell of the dialog to be created
   * @param context the runnable context to run the search in
   * @param scope the scope defining the available packages.
   * @param multipleSelection true if multiple selection is allowed
   * @param removeDuplicates true if only one package is shown per package name
   * @param filter the initial pattern to filter the set of packages. For example "com" shows all
   *          packages starting with "com". The meta character '?' representing any character and
   *          '*' representing any string are supported. Clients can pass an empty string if no
   *          filtering is required.
   * @return a new selection dialog
   */
  // public static SelectionDialog createPackageDialog(Shell parent,
  // IRunnableContext context, IJavaScriptSearchScope scope,
  // boolean multipleSelection, boolean removeDuplicates, String filter) {
  //
  // int flag = removeDuplicates ? PackageSelectionDialog.F_REMOVE_DUPLICATES
  // : 0;
  // PackageSelectionDialog dialog = new PackageSelectionDialog(parent, context,
  // flag, scope);
  // dialog.setFilter(filter);
  // dialog.setIgnoreCase(false);
  // dialog.setMultipleSelection(multipleSelection);
  // return dialog;
  // }

  /**
   * Creates a selection dialog that lists all types in the given scope. The caller is responsible
   * for opening the dialog with <code>Window.open</code>, and subsequently extracting the selected
   * type(s) (of type <code>Type</code> ) via <code>SelectionDialog.getResult</code>.
   * 
   * @param parent the parent shell of the dialog to be created
   * @param context the runnable context used to show progress when the dialog is being populated
   * @param scope the scope that limits which types are included
   * @param style flags defining the style of the dialog; the only valid values are
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_INTERFACES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_ANNOTATION_TYPES</code> ,
   *          <code>IDartElementSearchConstants.CONSIDER_ENUMS</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_ALL_TYPES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES</code>
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS</code> . Please note that
   *          the bitwise OR combination of the elementary constants is not supported.
   * @param multipleSelection <code>true</code> if multiple selection is allowed
   * @return a new selection dialog
   * @exception DartModelException if the selection dialog could not be opened
   */
  public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context,
      SearchScope scope, int style, boolean multipleSelection) throws DartModelException {
    return createTypeDialog(parent, context, scope, style, multipleSelection, "");//$NON-NLS-1$
  }

  /**
   * Creates a selection dialog that lists all types in the given scope. The caller is responsible
   * for opening the dialog with <code>Window.open</code>, and subsequently extracting the selected
   * type(s) (of type <code>Type</code> ) via <code>SelectionDialog.getResult</code>.
   * 
   * @param parent the parent shell of the dialog to be created
   * @param context the runnable context used to show progress when the dialog is being populated
   * @param scope the scope that limits which types are included
   * @param style flags defining the style of the dialog; the only valid values are
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_INTERFACES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_ANNOTATION_TYPES</code> ,
   *          <code>IDartElementSearchConstants.CONSIDER_ENUMS</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_ALL_TYPES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES</code>
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS</code> . Please note that
   *          the bitwise OR combination of the elementary constants is not supported.
   * @param multipleSelection <code>true</code> if multiple selection is allowed
   * @param filter the initial pattern to filter the set of types. For example "Abstract" shows all
   *          types starting with "abstract". The meta character '?' representing any character and
   *          '*' representing any string are supported. Clients can pass an empty string if no
   *          filtering is required.
   * @return a new selection dialog
   * @exception DartModelException if the selection dialog could not be opened
   */
  public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context,
      SearchScope scope, int style, boolean multipleSelection, String filter)
      throws DartModelException {
    return createTypeDialog(parent, context, scope, style, multipleSelection, filter, null);
  }

  /**
   * Creates a selection dialog that lists all types in the given scope. The caller is responsible
   * for opening the dialog with <code>Window.open</code>, and subsequently extracting the selected
   * type(s) (of type <code>Type</code> ) via <code>SelectionDialog.getResult</code>.
   * 
   * @param parent the parent shell of the dialog to be created
   * @param context the runnable context used to show progress when the dialog is being populated
   * @param scope the scope that limits which types are included
   * @param style flags defining the style of the dialog; the only valid values are
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_INTERFACES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_ANNOTATION_TYPES</code> ,
   *          <code>IDartElementSearchConstants.CONSIDER_ENUMS</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_ALL_TYPES</code>,
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES</code>
   *          <code>IDartElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS</code> . Please note that
   *          the bitwise OR combination of the elementary constants is not supported.
   * @param multipleSelection <code>true</code> if multiple selection is allowed
   * @param filter the initial pattern to filter the set of types. For example "Abstract" shows all
   *          types starting with "abstract". The meta character '?' representing any character and
   *          '*' representing any string are supported. Clients can pass an empty string if no
   *          filtering is required.
   * @param extension a user interface extension to the type selection dialog or <code>null</code>
   *          if no extension is desired
   * @return a new selection dialog
   * @exception DartModelException if the selection dialog could not be opened
   */
  public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context,
      SearchScope scope, int style, boolean multipleSelection, String filter,
      TypeSelectionExtension extension) throws DartModelException {
    int elementKinds = 0;
    // TODO (pquitslund): implement search constant kinds
//    if (style == IDartElementSearchConstants.CONSIDER_ALL_TYPES) {
//      elementKinds = IJavaScriptSearchConstants.TYPE;
//    } else if (style == IDartElementSearchConstants.CONSIDER_CLASSES) {
//      elementKinds = IJavaScriptSearchConstants.CLASS;
//    } else {
//      throw new IllegalArgumentException("Invalid style constant."); //$NON-NLS-1$
//    }
    FilteredTypesSelectionDialog dialog = new FilteredTypesSelectionDialog(
        parent,
        multipleSelection,
        context,
        scope,
        elementKinds,
        extension);
    dialog.setMessage(DartUIMessages.JavaUI_defaultDialogMessage);
    dialog.setInitialPattern(filter);
    return dialog;
  }

  /**
   * Returns the color manager the JavaScript UI plug-in which is used to manage any Java-specific
   * colors needed for such things like syntax highlighting.
   * 
   * @return the color manager to be used for JavaScript text viewers
   */
  public static IColorManager getColorManager() {
    return DartToolsPlugin.getDefault().getDartTextTools().getColorManager();
  }

  /**
   * Returns the Dartdoc URL for an element. This returned location doesn't have to exist. Returns
   * <code>null</code> if no Dartdoc location has been attached to the element's library or project.
   * 
   * @param element the element for which the documentation URL is requested.
   * @param includeAnchor If set, the URL contains an anchor for member references
   * @return the Dartdoc URL for the element
   * @throws DartModelException thrown when the element can not be accessed
   */
  public static URL getDartDocLocation(DartElement element, boolean includeAnchor)
      throws DartModelException {
    // TODO(devoncarew):

    return null;
  }

  /**
   * Returns the transfer instance used to copy/paste JavaScript elements to and from the clipboard.
   * Objects managed by this transfer instance are of type <code>DartElement[]</code>. So to access
   * data from the clipboard clients should use the following code snippet:
   * 
   * <pre>
   * DartElement[] elements = (DartElement[]) clipboard.getContents(DartUI.getDartElementClipboardTransfer());
   * </pre>
   * 
   * To put elements into the clipboard use the following snippet:
   * 
   * <pre>
   *    DartElement[] dartElements= ...;
   *    clipboard.setContents(
   *     new Object[] { dartElements },
   *     new Transfer[] { DartUI.getDartElementClipboardTransfer() } );
   * </pre>
   * 
   * @return returns the transfer object used to copy/paste Dart elements to and from the clipboard
   */
  public static Transfer getDartElementClipboardTransfer() {
// TODO (pquitslund): implement cliboard transfer
//    return JavaElementTransfer.getInstance();
    return null;
  }

  /**
   * Returns the DocumentProvider used for JavaScript compilation units.
   * 
   * @return the DocumentProvider for JavaScript compilation units.
   * @see IDocumentProvider
   */
  public static IDocumentProvider getDocumentProvider() {
    return DartToolsPlugin.getDefault().getCompilationUnitDocumentProvider();
  }

  /**
   * Returns the Dart element wrapped by the given editor input.
   * 
   * @param editorInput the editor input
   * @return the Dart element wrapped by <code>editorInput</code> or <code>null</code> if none
   */
  public static DartElement getEditorInputDartElement(IEditorInput editorInput) {
    if (editorInput instanceof ExternalCompilationUnitEditorInput) {
      return ((ExternalCompilationUnitEditorInput) editorInput).getCompilationUnit();
    }
    // Performance: check working copy manager first: this is faster
    DartElement de = DartToolsPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(
        editorInput);
    if (de != null) {
      return de;
    }
    return (DartElement) editorInput.getAdapter(DartElement.class);
  }

//TODO (pquitslund): implement when we have dart doc
//  /**
//   * Returns the Javadoc base URL for an element. The base location contains the
//   * index file. This location doesn't have to exist. Returns <code>null</code>
//   * if no javadoc location has been attached to the element's library or
//   * project. Example of a returned URL is
//   * <i>http://www.junit.org/junit/javadoc</i>.
//   *
//   * @param element the element for which the documentation URL is requested.
//   * @return the base location
//   * @throws DartModelException thrown when the element can not be accessed
//   */
//  public static URL getJSdocBaseLocation(DartElement element)
//      throws DartModelException {
//    return JavaDocLocations.getJavadocBaseLocation(element);
//  }
//
//  /**
//   * Returns the Javadoc URL for an element. Example of a returned URL is
//   * <i>http://www.junit.org/junit/javadoc/junit/extensions/TestSetup.html</i>.
//   * This returned location doesn't have to exist. Returns <code>null</code> if
//   * no javadoc location has been attached to the element's library or project.
//   *
//   * @param element the element for which the documentation URL is requested.
//   * @param includeAnchor If set, the URL contains an anchor for member
//   *          references:
//   *          <i>http://www.junit.org/junit/javadoc/junit/extensions/
//   *          TestSetup.html#run(junit.framework.TestResult)</i>. Note that this
//   *          involves type resolving and is a more expensive call than without
//   *          anchor.
//   * @return the Javadoc URL for the element
//   * @throws DartModelException thrown when the element can not be accessed
//   */
//  public static URL getJSdocLocation(DartElement element, boolean includeAnchor)
//      throws DartModelException {
//    return JavaDocLocations.getJavadocLocation(element, includeAnchor);
//  }
//
//  /**
//   * Returns the Javadoc location for library's classpath entry or
//   * <code>null</code> if no location is available. Note that only classpath
//   * entries of kind {@link IIncludePathEntry#CPE_LIBRARY} and
//   * {@link IIncludePathEntry#CPE_VARIABLE} support Javadoc locations.
//   *
//   * @param entry the classpath entry to get the Javadoc location for
//   * @return the Javadoc location or<code>null</code> if no Javadoc location is
//   *         available
//   * @throws IllegalArgumentException Thrown when the entry is <code>null</code>
//   *           or not of kind {@link IIncludePathEntry#CPE_LIBRARY} or
//   *           {@link IIncludePathEntry#CPE_VARIABLE}.
//   */
//  public static URL getLibraryJSdocLocation(IIncludePathEntry entry) {
//    return JavaDocLocations.getLibraryJavadocLocation(entry);
//  }
//
//  /**
//   * Returns the Javadoc location for a JavaScript project or <code>null</code>
//   * if no location is available. This location is used for all types located in
//   * the project's source folders.
//   *
//   * @param project the project
//   * @return the Javadoc location for a JavaScript project or <code>null</code>
//   */
//  public static URL getProjectJSdocLocation(DartProject project) {
//    return JavaDocLocations.getProjectJavadocLocation(project);
//  }
//
//  /**
//   * Sets the Javadoc location for a JavaScript project. This location is used
//   * for all types located in the project's source folders.
//   *
//   * @param project the project
//   * @param url the Javadoc location to set. This location should contain
//   *          index.html and a file 'package-list'. <code>null</code> clears the
//   *          current documentation location.
//   */
//  public static void setProjectJSdocLocation(DartProject project, URL url) {
//    JavaDocLocations.setProjectJavadocLocation(project, url);
//  }

  /**
   * Returns the shared images for the JavaScript UI.
   * 
   * @return the shared images manager
   */
  public static ISharedImages getSharedImages() {
    if (fgSharedImages == null) {
      fgSharedImages = new SharedImages();
    }

    return fgSharedImages;
  }

  /**
   * Returns the working copy manager for the JavaScript UI plug-in.
   * 
   * @return the working copy manager for the JavaScript UI plug-in
   */
  public static IWorkingCopyManager getWorkingCopyManager() {
    return DartToolsPlugin.getDefault().getWorkingCopyManager();
  }

  /**
   * Opens an editor on the given JavaScript element in the active page. Valid elements are all
   * JavaScript elements that are {@link SourceReference}. For elements inside a compilation unit or
   * class file, the parent is opened in the editor is opened and the element revealed. If there
   * already is an open JavaScript editor for the given element, it is returned.
   * 
   * @param element the input element; either a compilation unit ( <code>CompilationUnit</code>) or
   *          a class file ( <code>IClassFile</code>) or source references inside.
   * @return returns the editor part of the opened editor or <code>null</code> if the element is not
   *         a {@link SourceReference} or the file was opened in an external editor.
   * @exception PartInitException if the editor could not be initialized or no workbench page is
   *              active
   * @exception DartModelException if this element does not exist or if an exception occurs while
   *              accessing its underlying resource
   */
  public static IEditorPart openInEditor(DartElement element) throws DartModelException,
      PartInitException {
    return openInEditor(element, true, true);
  }

  /**
   * Opens an editor on the given JavaScript element in the active page. Valid elements are all
   * JavaScript elements that are {@link SourceReference}. For elements inside a compilation unit or
   * class file, the parent is opened in the editor is opened. If there already is an open
   * JavaScript editor for the given element, it is returned.
   * 
   * @param element the input element; either a compilation unit ( <code>CompilationUnit</code>) or
   *          a class file ( <code>IClassFile</code>) or source references inside.
   * @param activate if set, the editor will be activated.
   * @param reveal if set, the element will be revealed.
   * @return returns the editor part of the opened editor or <code>null</code> if the element is not
   *         a {@link SourceReference} or the file was opened in an external editor.
   * @exception PartInitException if the editor could not be initialized or no workbench page is
   *              active
   * @exception DartModelException if this element does not exist or if an exception occurs while
   *              accessing its underlying resource
   */
  public static IEditorPart openInEditor(DartElement element, boolean activate, boolean reveal)
      throws DartModelException, PartInitException {
    if (!(element instanceof SourceReference)) {
      return null;
    }
    IEditorPart part = EditorUtility.openInEditor(element, activate);
    if (reveal && part != null) {
      EditorUtility.revealInEditor(part, element);
    }
    return part;
  }

  /**
   * Reveals the given JavaScript element in the given editor. If the element is not an instance of
   * <code>SourceReference</code> this method result in a NOP. If it is a source reference no
   * checking is done if the editor displays a compilation unit or class file that contains the
   * source reference element. The editor simply reveals the source range denoted by the given
   * element.
   * 
   * @param part the editor displaying a compilation unit or class file
   * @param element the element to be revealed
   */
  public static void revealInEditor(IEditorPart part, DartElement element) {
    EditorUtility.revealInEditor(part, element);
  }

  private DartUI() {
    // prevent instantiation of DartUI.
  }

}
