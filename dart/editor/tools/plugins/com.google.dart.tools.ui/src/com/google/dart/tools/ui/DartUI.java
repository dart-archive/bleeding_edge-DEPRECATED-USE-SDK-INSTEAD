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

import com.google.common.base.Charsets;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.source.Source;
import com.google.dart.server.generated.types.Location;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.IFileInfo;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.ResourceMap;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.internal.corext.refactoring.util.DartElementUtil;
import com.google.dart.tools.ui.internal.SharedImages;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.text.IColorManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
 */
public final class DartUI {

  private static ISharedImages fgSharedImages = null;

  /**
   * The id of the Dart UI plug-in (value <code>"com.google.dart.tools.ui"</code>).
   */
  public static final String ID_PLUGIN = "com.google.dart.tools.ui"; //$NON-NLS-1$

  /**
   * The id of the Dart perspective (value <code>"com.google.dart.tools.ui.JavaPerspective"</code>).
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

  public static final String ID_INSPECTOR_VIEW = "com.google.dart.tools.debug.objectInspectorView"; //$NON-NLS-1$

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
   * The maximal length of a document for which we want to support Dart editing features.
   */
  private static final int MAX_DOCUMENT_LENGTH = (int) (1.5 * 1024 * 1024);

  /**
   * The maximal length of a line in a document for which we want to support Dart editing features.
   */
  private static final int MAX_LINE_LENGTH = 1000;

  /**
   * Returns the color manager the Dart UI plug-in which is used to manage any Dart-specific colors
   * needed for such things like syntax highlighting.
   * 
   * @return the color manager to be used for Dart text viewers
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
   * Returns the transfer instance used to copy/paste Dart elements to and from the clipboard.
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
   * Returns the DocumentProvider used for Dart compilation units.
   * 
   * @return the DocumentProvider for Dart compilation units.
   * @see IDocumentProvider
   */
  public static IDocumentProvider getDocumentProvider() {
    return DartToolsPlugin.getDefault().getCompilationUnitDocumentProvider();
  }

  public static Color getEditorBackground(IPreferenceStore prefs, Display display) {
    return prefs.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT) ? null
        : createColor(prefs, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, display);
  }

  public static Color getEditorForeground(IPreferenceStore prefs, Display display) {
    return prefs.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT) ? null
        : createColor(prefs, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND, display);
  }

  /**
   * Returns the Dart element wrapped by the given editor input.
   * 
   * @param editorInput the editor input
   * @return the Dart element wrapped by <code>editorInput</code> or <code>null</code> if none
   */
  public static Element getEditorInputDartElement2(IEditorInput editorInput) {

    //TODO (pquitslund): add support for external compilation units

//    if (editorInput instanceof ExternalCompilationUnitEditorInput) {
//      return ((ExternalCompilationUnitEditorInput) editorInput).getCompilationUnit();
//    }

    //TODO (pquitslund): consider doing this without going through the adapter service

    return (Element) editorInput.getAdapter(Element.class);
  }

  public static Color getEditorSelectionBackground(IPreferenceStore prefs, Display display) {
    return prefs.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR)
        ? null : createColor(
            prefs,
            AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR,
            display);
  }

  public static Color getEditorSelectionForeground(IPreferenceStore prefs, Display display) {
    return prefs.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR)
        ? null : createColor(
            prefs,
            AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR,
            display);
  }

  /**
   * @return the {@link IFile} with {@link #element} to open, may be {@code null}.
   */
  public static IFile getElementFile(Element element) {
    ResourceMap map = getResourceMap(element);
    Source source = element.getSource();
    return getSourceFile(map, source);
  }

  /**
   * Returns the shared images for the Dart UI.
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
   * @return the {@link IFile} to open for ""path, may be {@code null}.
   */
  public static IFile getSourceFile(String source) {
    File javaFile = new File(source);
    return ResourceUtil.getFile(javaFile);
  }

  /**
   * @return the {@link IFile} to open for the {@link Source} in given context, may be {@code null}.
   */
  public static IFile getSourceFile(String contextId, Source source) {
    ResourceMap map = getResourceMap(contextId);
    return getSourceFile(map, source);
  }

  public static Color getViewerBackground(IPreferenceStore prefs, Display display) {
    // TODO(messick) Use a color identifier distinct from the editor.
    return prefs.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT) ? null
        : createColor(prefs, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND, display);
  }

  public static Color getViewerForeground(IPreferenceStore prefs, Display display) {
    // TODO(messick) Use a color identifier distinct from the editor.
    return prefs.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT) ? null
        : createColor(prefs, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND, display);
  }

  public static Color getViewerSelectionBackground(IPreferenceStore prefs, Display display) {
    // TODO(messick) Use a color identifier distinct from the editor.
    return prefs.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR)
        ? null : createColor(
            prefs,
            AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR,
            display);
  }

  public static Color getViewerSelectionForeground(IPreferenceStore prefs, Display display) {
    // TODO(messick) Use a color identifier distinct from the editor.
    return prefs.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR)
        ? null : createColor(
            prefs,
            AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR,
            display);
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
   * Returns the working copy manager for the Dart UI plug-in.
   * 
   * @return the working copy manager for the Dart UI plug-in
   */
  public static IWorkingCopyManager getWorkingCopyManager() {
    return DartToolsPlugin.getDefault().getWorkingCopyManager();
  }

  /**
   * Eclipse StyledText becomes very slow (practically hangs) when it attempts to render a long line
   * with many style ranges. We need to check for this and avoid creating styles.
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=17204
   * <p>
   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=196731
   */
  public static boolean isTooComplexDartDocument(IDocument document) {
    if (document == null) {
      return false;
    }
    if (document.getLength() > MAX_DOCUMENT_LENGTH) {
      return true;
    }
    int numberOfLines = document.getNumberOfLines();
    for (int i = 0; i < numberOfLines; i++) {
      try {
        if (document.getLineLength(i) > MAX_LINE_LENGTH) {
          return true;
        }
      } catch (BadLocationException e) {
      }
    }
    return false;
  }

  /**
   * Checks if the given Dart file will be too complex for the opening it in the Dart editor.
   */
  public static boolean isTooComplexDartFile(IFile resource) {
    IPath resourceLoc = resource.getLocation();
    if (resourceLoc == null) {
      return false;
    }
    // should not be too long
    File file = resourceLoc.toFile();
    if (file.length() > MAX_DOCUMENT_LENGTH) {
      return true;
    }
    // check line length
    try {
      Reader reader = new InputStreamReader(new FileInputStream(file), Charsets.UTF_8);
      BufferedReader br = new BufferedReader(reader);
      try {
        String line = br.readLine();
        if (line.length() > MAX_LINE_LENGTH) {
          return true;
        }
      } finally {
        br.close();
      }
    } catch (Throwable e) {
      return true;
    }
    // OK
    return false;
  }

  public static IEditorPart openFilePath(String path, boolean activate) throws PartInitException,
      DartModelException {
    File javaFile = new File(path);
    IFile file = ResourceUtil.getFile(javaFile);
    if (file != null) {
      return openFile(file, activate);
    }
    // fallback for SDK, which sources are is not a IFile(s)
    return EditorUtility.openInEditor(javaFile, activate);
  }

  /**
   * Opens an editor at the given position in the active page.
   */
  public static IEditorPart openFilePath(String filePath, boolean activate, int offset, int length)
      throws Exception {
    IEditorPart editor = openFilePath(filePath, activate);
    EditorUtility.revealInEditor(editor, offset, length);
    return editor;
  }

  /**
   * Opens an editor with {@link com.google.dart.server.Element}.
   * 
   * @param target the {@link NavigationTarget} to open in reveal
   * @return the opened editor or {@code null} if by some reason editor was not opened
   */
  public static IEditorPart openInEditor(com.google.dart.server.Element target, boolean activate)
      throws Exception {
    if (target == null) {
      return null;
    }
    Location location = target.getLocation();
    String path = location.getFile();
    IEditorPart editor = openFilePath(path, activate);
    if (editor != null) {
      int offset = location.getOffset();
      int length = location.getLength();
      EditorUtility.revealInEditor(editor, offset, length);
    }
    return editor;
  }

  /**
   * Opens an editor with {@link Element} in context of the given {@link DartEditor}.
   * 
   * @param contextEditor the {@link DartEditor} to use as context, may be {@code null}.
   * @param element the {@link Element} to open in reveal.
   * @return the opened editor or {@code null} if by some reason editor was not opened.
   */
  public static IEditorPart openInEditor(DartEditor contextEditor, Element element, boolean activate)
      throws Exception {
    IFile contextFile = contextEditor != null ? contextEditor.getInputResourceFile() : null;
    return openInEditor(contextFile, element, activate);
  }

  /**
   * Opens an editor on the given Dart element in the active page. Valid elements are all Dart
   * elements that are {@link SourceReference}. For elements inside a compilation unit, the parent
   * is opened in the editor is opened and the element revealed. If there already is an open Dart
   * editor for the given element, it is returned.
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
   * Opens an editor on the given Dart element in the active page. Valid elements are all Dart
   * elements that are {@link SourceReference}. For elements inside a compilation unit, the parent
   * is opened in the editor is opened. If there already is an open Dart editor for the given
   * element, it is returned.
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
   * Opens an editor on the given Dart element in the active page. Valid elements are all Dart
   * elements that are {@link SourceReference}. For elements inside a compilation unit, the parent
   * is opened in the editor is opened and the element revealed. If there already is an open Dart
   * editor for the given element, it is returned.
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
  public static IEditorPart openInEditor(Element element) throws DartModelException,
      PartInitException {
    element = DartElementUtil.getVariableIfSyntheticAccessor(element);
    element = DartElementUtil.getAccessorIfSyntheticVariable(element);
    element = DartElementUtil.getExplicitIfSyntheticImplicitConstructor(element);
    element = DartElementUtil.getClassIfSyntheticDefaultConstructor(element);
    return openInEditor(element, true, true);
  }

  /**
   * Opens an editor on the given Dart element in the active page. Valid elements are all Dart
   * elements that are {@link SourceReference}. For elements inside a compilation unit, the parent
   * is opened in the editor is opened. If there already is an open Dart editor for the given
   * element, it is returned.
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
  public static IEditorPart openInEditor(Element element, boolean activate, boolean reveal)
      throws DartModelException, PartInitException {
    if (element == null) {
      return null;
    }
    ResourceMap map = getResourceMap(element);
    Source source = element.getSource();
    IEditorPart editor = openSource(map, source, activate);
    if (reveal) {
      EditorUtility.revealInEditor(editor, element);
    }
    return editor;
  }

  /**
   * Opens an editor with {@link Element} in context of the given {@link IFile}.
   * 
   * @param context the {@link IResource} to open {@link Element} in, may be {@code null}.
   * @param element the {@link Element} to open in reveal.
   * @return the opened editor or {@code null} if by some reason editor was not opened.
   */
  public static IEditorPart openInEditor(IResource context, Element element, boolean activate)
      throws Exception {
    if (element == null) {
      return null;
    }
    IFile file = getElementFile(element);
    String name = element.getDisplayName();
    int nameOffset = element.getNameOffset();
    int nameLength = name != null ? name.length() : 0;
    return openFile(file, activate, nameOffset, nameLength);
  }

  /**
   * Opens an editor at the given position in the active page.
   */
  public static IEditorPart openInEditor(IResource context, Source source, boolean activate,
      int offset, int length) throws Exception {
    ResourceMap map = getResourceMap(context);
    return openSource(map, source, activate, offset, length);
  }

  /**
   * Opens an editor at the given position in the active page.
   */
  public static IEditorPart openInEditor(String contextId, Source source, boolean activate)
      throws Exception {
    ResourceMap map = getResourceMap(contextId);
    return openSource(map, source, activate);
  }

  /**
   * Opens an editor at the given position in the active page.
   */
  public static IEditorPart openInEditor(String contextId, Source source, boolean activate,
      int offset, int length) throws Exception {
    IEditorPart editor = openInEditor(contextId, source, activate);
    EditorUtility.revealInEditor(editor, offset, length);
    return editor;
  }

  /**
   * Reveals the given Dart element in the given editor. If the element is not an instance of
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

  /**
   * A shared method to show the warning about "too complex" Dart file.
   */
  public static void showTooComplexDartFileWarning(IEditorPart editor) {
    if (editor != null) {
      editor.getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(
          "This file is too complex to open it in the Dart editor.");
    }
  }

  private static Color createColor(IPreferenceStore store, String key, Display display) {
    // TODO(messick) Remove display or use it instead of color manager
    RGB rgb = null;
    if (store.contains(key)) {
      if (store.isDefault(key)) {
        rgb = PreferenceConverter.getDefaultColor(store, key);
      } else {
        rgb = PreferenceConverter.getColor(store, key);
      }
      if (rgb != null) {
        return getColorManager().getColor(rgb);
      }
    }
    return null;
  }

  private static ResourceMap getResourceMap(Element element) {
    if (element == null) {
      return null;
    }
    AnalysisContext context = element.getContext();
    return DartCore.getProjectManager().getResourceMap(context);
  }

  private static ResourceMap getResourceMap(IResource context) {
    if (context == null) {
      return null;
    }
    return DartCore.getProjectManager().getResourceMap(context);
  }

  private static ResourceMap getResourceMap(String contextId) {
    if (contextId == null) {
      return null;
    }
    return DartCore.getProjectManager().getResourceMap(contextId);
  }

  /**
   * @return the {@link IFile} to open for the {@link Source}, may be {@code null}.
   */
  private static IFile getSourceFile(ResourceMap map, Source source) {
    ProjectManager projectManager = DartCore.getProjectManager();
    // fallback
    if (map == null) {
      return (IFile) projectManager.getResource(source);
    }
    // use ResourceMap
    IFile file = map.getResource(source);
    // self-reference tweak
    if (file != null) {
      IResource resource = projectManager.getResource(source);
      if (resource instanceof IFile) {
        // on Windows, check if there is an resource in project/lib in workspace that is 
        // the same as the one in packages. If so, open the local source and not packages
        if (DartCore.isWindows()
            && resource.getFullPath().toString().contains(DartCore.PACKAGES_DIRECTORY_PATH)) {

          String path = resource.getFullPath().toString();
          int index = path.indexOf(DartCore.PACKAGES_DIRECTORY_PATH);
          String packageUri = DartCore.PACKAGE_SCHEME_SPEC
              + path.substring(index + DartCore.PACKAGES_DIRECTORY_PATH.length());

          IFileInfo info = DartCore.getProjectManager().resolveUriToFileInfo(
              map.getResource(),
              packageUri);
          if (info != null && info.getResource() != null) {
            return info.getResource();
          }
        }

        file = (IFile) resource;
      }
    }
    return file;
  }

  private static IEditorPart openFile(IFile file, boolean activate) throws PartInitException,
      DartModelException {
    if (file == null) {
      return null;
    }
    return EditorUtility.openInEditor(file, activate);
  }

  private static IEditorPart openFile(IFile file, boolean activate, int offset, int length)
      throws Exception {
    IEditorPart editor = openFile(file, activate);
    if (editor != null) {
      EditorUtility.revealInEditor(editor, offset, length);
    }
    return editor;
  }

  private static IEditorPart openSource(ResourceMap map, Source source, boolean activate)
      throws PartInitException, DartModelException {
    IFile file = getSourceFile(map, source);
    if (file != null) {
      return openFile(file, activate);
    }
    // fallback for SDK, which sources are is not a IFile(s)
    return EditorUtility.openInEditor(source, activate);
  }

  private static IEditorPart openSource(ResourceMap map, Source source, boolean activate,
      int offset, int length) throws Exception {
    IEditorPart editor = openSource(map, source, activate);
    if (editor != null) {
      EditorUtility.revealInEditor(editor, offset, length);
    }
    return editor;
  }

  private DartUI() {
    // prevent instantiation of DartUI.
  }

}
