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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.internal.text.IJavaThemeConstants;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings;
import com.google.dart.tools.ui.text.IDartColorConstants;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Preference constants used in the Dart-UI preference store. Clients should only read the Dart-UI
 * preference store using these values. Clients are not allowed to modify the preference store
 * programmatically. Provisional API: This class/interface is part of an interim API that is still
 * under development and expected to change significantly before reaching stability. It is being
 * made available at this early stage to solicit feedback from pioneering adopters on the
 * understanding that any code that uses this API will almost certainly be broken (repeatedly) as
 * the API evolves.
 */
public class PreferenceConstants {

  /**
   * Default editor tab display width.
   */
  public static final int EDITOR_DEFAULT_TAB_WIDTH = 2;

  /**
   * A named preference that controls return type rendering of methods in the UI.
   * <p>
   * Value is of type <code>Boolean</code>: if <code>true</code> return types are rendered
   * </p>
   */
  public static final String APPEARANCE_METHOD_RETURNTYPE = "com.google.dart.tools.ui.methodreturntype";//$NON-NLS-1$

  /**
   * A named preference that controls type parameter rendering of methods in the UI.
   * <p>
   * Value is of type <code>Boolean</code>: if <code>true</code> return types are rendered
   * </p>
   */
  public static final String APPEARANCE_METHOD_TYPEPARAMETERS = "com.google.dart.tools.ui.methodtypeparametesr";//$NON-NLS-1$

  /**
   * A named preference that controls if quick assist light bulbs are shown.
   * <p>
   * Value is of type <code>Boolean</code>: if <code>true</code> light bulbs are shown for quick
   * assists.
   * </p>
   */
  public static final String EDITOR_QUICKASSIST_LIGHTBULB = "org.eclipse.wst.jsdt.quickassist.lightbulb"; //$NON-NLS-1$

  /**
   * A named preference that defines the pattern used for package name compression.
   * <p>
   * Value is of type <code>String</code>. For example for the given package name
   * 'org.eclipse.wst.jsdt' pattern '.' will compress it to '..jdt', '1~' to 'o~.e~.jdt'.
   * </p>
   */
  public static final String APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW = "PackagesView.pkgNamePatternForPackagesView";//$NON-NLS-1$

  /**
   * A named preference that controls if package name compression is turned on or off.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @see #APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW
   */
  public static final String APPEARANCE_COMPRESS_PACKAGE_NAMES = "com.google.dart.tools.ui.compresspackagenames";//$NON-NLS-1$

  /**
   * A named preference that holds the methods or types whose methods are by default expanded with
   * constructors in the Call Hierarchy.
   * <p>
   * Value is of type <code>String</code>: semicolon separated list of fully qualified type names
   * appended with ".*" or "." + method name.
   * </p>
   */
  public static final String PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS_MEMBERS = "CallHierarchy.defaultExpandWithConstructorsMembers"; //$NON-NLS-1$

  /**
   * A named preference that defines how member elements are ordered by the JavaScript views using
   * the <code>DartElementSorter</code>.
   * <p>
   * Value is of type <code>String</code>: A comma separated list of the following entries. Each
   * entry must be in the list, no duplication. List order defines the sort order.
   * <ul>
   * <li><b>T</b>: Types</li>
   * <li><b>C</b>: Constructors</li>
   * <li><b>I</b>: Initializers</li>
   * <li><b>M</b>: Methods</li>
   * <li><b>F</b>: Fields</li>
   * <li><b>SI</b>: Static Initializers</li>
   * <li><b>SM</b>: Static Methods</li>
   * <li><b>SF</b>: Static Fields</li>
   * </ul>
   * </p>
   */
  public static final String APPEARANCE_MEMBER_SORT_ORDER = "outlinesortoption"; //$NON-NLS-1$

  /**
   * A named preference that defines how member elements are ordered by visibility in the JavaScript
   * views using the <code>DartElementSorter</code> .
   * <p>
   * Value is of type <code>String</code>: A comma separated list of the following entries. Each
   * entry must be in the list, no duplication. List order defines the sort order.
   * <ul>
   * <li><b>B</b>: Public</li>
   * <li><b>V</b>: Private</li>
   * <li><b>R</b>: Protected</li>
   * <li><b>D</b>: Default</li>
   * </ul>
   * </p>
   */
  public static final String APPEARANCE_VISIBILITY_SORT_ORDER = "com.google.dart.tools.ui.visibility.order"; //$NON-NLS-1$

  /**
   * A named preferences that controls if JavaScript elements are also sorted by visibility.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER = "com.google.dart.tools.ui.enable.visibility.order"; //$NON-NLS-1$

  /**
   * A named preference that controls category rendering of JavaScript elements in the UI.
   * <p>
   * Value is of type <code>Boolean</code>: if <code>true</code> category is rendered
   * </p>
   */
  public static final String APPEARANCE_CATEGORY = "com.google.dart.tools.ui.category";//$NON-NLS-1$

  /**
   * The symbolic font name for the font used to display Javadoc (value
   * <code>&quot;com.google.dart.tools.ui.javadocfont&quot;</code>).
   */
  public final static String APPEARANCE_JAVADOC_FONT = "com.google.dart.tools.ui.javadocfont"; //$NON-NLS-1$

  /**
   * A named preference that controls if prefix removal during setter/getter generation is turned on
   * or off.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated Use JavaScriptCore preference store (key JavaScriptCore. CODEASSIST_FIELD_PREFIXES
   *             and CODEASSIST_STATIC_FIELD_PREFIXES)
   */
  @Deprecated
  public static final String CODEGEN_USE_GETTERSETTER_PREFIX = "com.google.dart.tools.ui.gettersetter.prefix.enable";//$NON-NLS-1$

  /**
   * A named preference that holds a list of prefixes to be removed from a local variable to compute
   * setter and getter names.
   * <p>
   * Value is of type <code>String</code>: comma separated list of prefixed
   * </p>
   * 
   * @deprecated Use JavaScriptCore preference store (key JavaScriptCore. CODEASSIST_FIELD_PREFIXES
   *             and CODEASSIST_STATIC_FIELD_PREFIXES)
   */
  @Deprecated
  public static final String CODEGEN_GETTERSETTER_PREFIX = "com.google.dart.tools.ui.gettersetter.prefix.list";//$NON-NLS-1$

  /**
   * A named preference that controls if suffix removal during setter/getter generation is turned on
   * or off.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated Use JavaScriptCore preference store (key JavaScriptCore. CODEASSIST_FIELD_PREFIXES
   *             and CODEASSIST_STATIC_FIELD_PREFIXES)
   */
  @Deprecated
  public static final String CODEGEN_USE_GETTERSETTER_SUFFIX = "com.google.dart.tools.ui.gettersetter.suffix.enable";//$NON-NLS-1$

  /**
   * A named preference that holds a list of suffixes to be removed from a local variable to compute
   * setter and getter names.
   * <p>
   * Value is of type <code>String</code>: comma separated list of suffixes
   * </p>
   * 
   * @deprecated Use setting from JavaScriptCore preference store (key JavaScriptCore.
   *             CODEASSIST_FIELD_SUFFIXES and CODEASSIST_STATIC_FIELD_SUFFIXES)
   */
  @Deprecated
  public static final String CODEGEN_GETTERSETTER_SUFFIX = "com.google.dart.tools.ui.gettersetter.suffix.list"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the keyword "this" will be added automatically to
   * field accesses in generated methods.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String CODEGEN_KEYWORD_THIS = "com.google.dart.tools.ui.keywordthis"; //$NON-NLS-1$

  /**
   * A named preference that controls whether to use the prefix "is" or the prefix "get" for
   * automatically created getters which return a boolean field.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String CODEGEN_IS_FOR_GETTERS = "com.google.dart.tools.ui.gettersetter.use.is"; //$NON-NLS-1$

  /**
   * A named preference that defines the preferred variable names for exceptions in catch clauses.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public static final String CODEGEN_EXCEPTION_VAR_NAME = "com.google.dart.tools.ui.exception.name"; //$NON-NLS-1$

  /**
   * A named preference that controls if comment stubs will be added automatically to newly created
   * types and methods.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String CODEGEN_ADD_COMMENTS = "com.google.dart.tools.ui.javadoc"; //$NON-NLS-1$

  /**
   * A named preference that controls whether to add a override annotation for newly created methods
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String CODEGEN_USE_OVERRIDE_ANNOTATION = "com.google.dart.tools.ui.overrideannotation"; //$NON-NLS-1$

  /**
   * A named preference that holds a list of semicolon separated fully qualified type names with
   * wild card characters.
   */
  public static final String TYPEFILTER_ENABLED = "com.google.dart.tools.ui.typefilter.enabled"; //$NON-NLS-1$

  /**
   * A named preference that holds a list of semicolon separated fully qualified type names with
   * wild card characters.
   */
  public static final String TYPEFILTER_DISABLED = "com.google.dart.tools.ui.typefilter.disabled"; //$NON-NLS-1$

  /**
   * A named preference that holds a list of semicolon separated package names. The list specifies
   * the import order used by the "Organize Imports" operation.
   * <p>
   * Value is of type <code>String</code>: semicolon separated list of package names
   * </p>
   */
  public static final String ORGIMPORTS_IMPORTORDER = "com.google.dart.tools.ui.importorder"; //$NON-NLS-1$

  /**
   * A named preference that specifies the number of imports added before a star-import declaration
   * is used.
   * <p>
   * Value is of type <code>Integer</code>: positive value specifying the number of non star-import
   * is used
   * </p>
   */
  public static final String ORGIMPORTS_ONDEMANDTHRESHOLD = "com.google.dart.tools.ui.ondemandthreshold"; //$NON-NLS-1$

  /**
   * A named preference that specifies the number of static imports added before a star-import
   * declaration is used.
   * <p>
   * Value is of type <code>Integer</code>: positive value specifying the number of non star-import
   * is used
   * </p>
   */
  public static final String ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD = "com.google.dart.tools.ui.staticondemandthreshold"; //$NON-NLS-1$

  /**
   * A named preferences that controls if types that start with a lower case letters get added by
   * the "Organize Import" operation.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String ORGIMPORTS_IGNORELOWERCASE = "com.google.dart.tools.ui.ignorelowercasenames"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the projects view's selection is linked to the active
   * editor.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String LINK_BROWSING_PROJECTS_TO_EDITOR = "com.google.dart.tools.ui.browsing.projectstoeditor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the packages view's selection is linked to the active
   * editor.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String LINK_BROWSING_PACKAGES_TO_EDITOR = "com.google.dart.tools.ui.browsing.packagestoeditor"; //$NON-NLS-1$

  /* supertype preferences */
  public static final String SUPER_TYPE_CONTAINER = "com.google.dart.tools.ui.superType.container"; //$NON-NLS-1$

  public static final String SUPER_TYPE_NAME = "com.google.dart.tools.ui.superType.name"; //$NON-NLS-1$
  /**
   * A named preference that controls whether the types view's selection is linked to the active
   * editor.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String LINK_BROWSING_TYPES_TO_EDITOR = "com.google.dart.tools.ui.browsing.typestoeditor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the members view's selection is linked to the active
   * editor.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String LINK_BROWSING_MEMBERS_TO_EDITOR = "com.google.dart.tools.ui.browsing.memberstoeditor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether new projects are generated using source and output
   * folder.
   * <p>
   * Value is of type <code>Boolean</code>. if <code>true</code> new projects are created with a
   * source and output folder. If <code>false</code> source and output folder equals to the project.
   * </p>
   */
  public static final String SRCBIN_FOLDERS_IN_NEWPROJ = "com.google.dart.tools.ui.wizards.srcBinFoldersInNewProjects"; //$NON-NLS-1$
  /**
   * A named preference that specifies the source folder name used when creating a new JavaScript
   * project. Value is inactive if <code>SRCBIN_FOLDERS_IN_NEWPROJ</code> is set to
   * <code>false</code>.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   * 
   * @see #SRCBIN_FOLDERS_IN_NEWPROJ
   */
  public static final String SRCBIN_SRCNAME = "com.google.dart.tools.ui.wizards.srcBinFoldersSrcName"; //$NON-NLS-1$

  /**
   * A named preference that specifies the output folder name used when creating a new JavaScript
   * project. Value is inactive if <code>SRCBIN_FOLDERS_IN_NEWPROJ</code> is set to
   * <code>false</code>.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   * 
   * @deprecated - there is no output
   * @see #SRCBIN_FOLDERS_IN_NEWPROJ
   */
  @Deprecated
  public static final String SRCBIN_BINNAME = "com.google.dart.tools.ui.wizards.srcBinFoldersBinName"; //$NON-NLS-1$

  /**
   * A named preference that holds a list of possible JRE libraries used by the New JavaScript
   * Project wizard. A library consists of a description and an arbitrary number of
   * <code>IIncludePathEntry</code>s, that will represent the JRE on the new project's class path.
   * <p>
   * Value is of type <code>String</code>: a semicolon separated list of encoded JRE libraries.
   * <code>NEWPROJECT_JRELIBRARY_INDEX
   * </code> defines the currently used library. Clients should use the method
   * <code>encodeJRELibrary</code> to encode a JRE library into a string and the methods
   * <code>decodeJRELibraryDescription(String)</code> and <code>
   * decodeJRELibraryClasspathEntries(String)</code> to decode the description and the array of
   * class path entries from an encoded string.
   * </p>
   * 
   * @see #NEWPROJECT_JRELIBRARY_INDEX
   * @see #encodeJRELibrary(String, IIncludePathEntry[])
   * @see #decodeJRELibraryDescription(String)
   * @see #decodeJRELibraryClasspathEntries(String)
   */
  public static final String NEWPROJECT_JRELIBRARY_LIST = "com.google.dart.tools.ui.wizards.jre.list"; //$NON-NLS-1$

  /**
   * A named preferences that specifies the current active JRE library.
   * <p>
   * Value is of type <code>Integer</code>: an index into the list of possible JRE libraries.
   * </p>
   * 
   * @see #NEWPROJECT_JRELIBRARY_LIST
   */
  public static final String NEWPROJECT_JRELIBRARY_INDEX = "com.google.dart.tools.ui.wizards.jre.index"; //$NON-NLS-1$

  /**
   * A named preference that controls the behavior when double clicking on a container in the
   * packages view.
   * <p>
   * Value is of type <code>String</code>: possible values are <code>
   * DOUBLE_CLICK_GOES_INTO</code> or <code>
   * DOUBLE_CLICK_EXPANDS</code>.
   * </p>
   * 
   * @see #DOUBLE_CLICK_EXPANDS
   * @see #DOUBLE_CLICK_GOES_INTO
   */
  public static final String DOUBLE_CLICK = "packageview.doubleclick"; //$NON-NLS-1$

  /**
   * A string value used by the named preference <code>DOUBLE_CLICK</code>.
   * 
   * @see #DOUBLE_CLICK
   */
  public static final String DOUBLE_CLICK_GOES_INTO = "packageview.gointo"; //$NON-NLS-1$

  /**
   * A string value used by the named preference <code>DOUBLE_CLICK</code>.
   * 
   * @see #DOUBLE_CLICK
   */
  public static final String DOUBLE_CLICK_EXPANDS = "packageview.doubleclick.expands"; //$NON-NLS-1$

  /**
   * A named preference that controls whether JavaScript views update their presentation while
   * editing or when saving the content of an editor.
   * <p>
   * Value is of type <code>String</code>: possible values are <code>
   * UPDATE_ON_SAVE</code> or <code>
   * UPDATE_WHILE_EDITING</code>.
   * </p>
   * 
   * @see #UPDATE_ON_SAVE
   * @see #UPDATE_WHILE_EDITING
   * @deprecated Since 3.0, views now always update while editing
   */
  @Deprecated
  public static final String UPDATE_JAVA_VIEWS = "DartUI.update"; //$NON-NLS-1$

  /**
   * A string value used by the named preference <code>UPDATE_JAVA_VIEWS</code>
   * 
   * @see #UPDATE_JAVA_VIEWS
   * @deprecated Since 3.0, views now always update while editing
   */
  @Deprecated
  public static final String UPDATE_ON_SAVE = "DartUI.update.onSave"; //$NON-NLS-1$

  /**
   * A string value used by the named preference <code>UPDATE_JAVA_VIEWS</code>
   * 
   * @see #UPDATE_JAVA_VIEWS
   * @deprecated Since 3.0, views now always update while editing
   */
  @Deprecated
  public static final String UPDATE_WHILE_EDITING = "DartUI.update.whileEditing"; //$NON-NLS-1$

  /**
   * A named preference that holds the path of the Javadoc command used by the Javadoc creation
   * wizard.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public static final String JAVADOC_COMMAND = "command"; //$NON-NLS-1$

  /**
   * A named preference that defines whether the hint to make hover sticky should be shown.
   * 
   * @see DartUI
   * @deprecated As of 3.3, replaced by
   *             {@link AbstractDecoratedTextEditorPreferenceConstants#EDITOR_SHOW_TEXT_HOVER_AFFORDANCE}
   */
  @Deprecated
  public static final String EDITOR_SHOW_TEXT_HOVER_AFFORDANCE = "PreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE"; //$NON-NLS-1$

  /**
   * A named preference that defines the key for the hover modifiers.
   * 
   * @see DartUI
   */
  public static final String EDITOR_TEXT_HOVER_MODIFIERS = "hoverModifiers"; //$NON-NLS-1$

  /**
   * A named preference that defines the key for the hover modifier state masks. The value is only
   * used if the value of <code>EDITOR_TEXT_HOVER_MODIFIERS</code> cannot be resolved to valid SWT
   * modifier bits.
   * 
   * @see DartUI
   * @see #EDITOR_TEXT_HOVER_MODIFIERS
   */
  public static final String EDITOR_TEXT_HOVER_MODIFIER_MASKS = "hoverModifierMasks"; //$NON-NLS-1$

  /**
   * The id of the best match hover contributed for extension point
   * <code>javaEditorTextHovers</code>.
   */
  public static final String ID_BESTMATCH_HOVER = "com.google.dart.tools.ui.BestMatchHover"; //$NON-NLS-1$

  /**
   * The id of the source code hover contributed for extension point
   * <code>javaEditorTextHovers</code>.
   */
  public static final String ID_SOURCE_HOVER = "com.google.dart.tools.ui.JavaSourceHover"; //$NON-NLS-1$

  /**
   * The id of the javadoc hover contributed for extension point <code>javaEditorTextHovers</code>.
   */
  public static final String ID_JAVADOC_HOVER = "com.google.dart.tools.ui.JavadocHover"; //$NON-NLS-1$

  /**
   * A named preference that controls whether bracket matching highlighting is turned on or off.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to highlight matching brackets.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_MATCHING_BRACKETS_COLOR = "matchingBracketsColor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the current line highlighting is turned on or off.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by
   *             {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants}
   */
  @Deprecated
  public final static String EDITOR_CURRENT_LINE = "currentLine"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to highlight the current line.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated as of 3.0 replaced by
   *             {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants}
   */
  @Deprecated
  public final static String EDITOR_CURRENT_LINE_COLOR = "currentLineColor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the print margin is turned on or off.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by
   *             {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants}
   */
  @Deprecated
  public final static String EDITOR_PRINT_MARGIN = "printMargin"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render the print margin.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated as of 3.0 replaced by
   *             {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants}
   */
  @Deprecated
  public final static String EDITOR_PRINT_MARGIN_COLOR = "printMarginColor"; //$NON-NLS-1$

  /**
   * Print margin column. Integer value.
   * 
   * @deprecated as of 3.0 replaced by
   *             {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants}
   */
  @Deprecated
  public final static String EDITOR_PRINT_MARGIN_COLUMN = "printMarginColumn"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used for the find/replace scope.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated As of 3.2, use {@link AbstractTextEditor#PREFERENCE_COLOR_FIND_SCOPE} instead}
   */
  @Deprecated
  public final static String EDITOR_FIND_SCOPE_COLOR = AbstractTextEditor.PREFERENCE_COLOR_FIND_SCOPE;

  /**
   * A named preference that specifies if the editor uses spaces for tabs.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code>spaces instead of tabs are used in
   * the editor. If <code>false</code> the editor inserts a tab character when pressing the tab key.
   * </p>
   * 
   * @deprecated As of 3.1 replaced by the formatter setting defined in
   *             {@link com.google.dart.tools.ui.text.formatter.DefaultCodeFormatterConstants#FORMATTER_TAB_CHAR}
   */
  @Deprecated
  public final static String EDITOR_SPACES_FOR_TABS = "spacesForTabs"; //$NON-NLS-1$

  /**
   * A named preference that holds the number of spaces used per tab in the editor.
   * <p>
   * Value is of type <code>Integer</code>: positive integer value specifying the number of spaces
   * per tab.
   * </p>
   * 
   * @deprecated As of 3.0 replaced by
   *             {@link AbstractDecoratedTextEditorPreferenceConstants#EDITOR_TAB_WIDTH}
   */
  @Deprecated
  public final static String EDITOR_TAB_WIDTH = "com.google.dart.tools.ui.editor.tab.width"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the editor should remove trailing whitespace on saves.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_REMOVE_TRAILING_WS = DartCore.PLUGIN_ID
      + ".removeTrailingWhitespace";

  /**
   * A named preference that controls whether the outline view selection should stay in sync with
   * with the element at the current cursor position.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE = "DartEditor.SyncOutlineOnCursorMove"; //$NON-NLS-1$

  /**
   * A named preference that controls if correction indicators are shown in the UI.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_CORRECTION_INDICATION = "DartEditor.ShowTemporaryProblem"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the editor shows problem indicators in text (squiggly
   * lines).
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_PROBLEM_INDICATION = "problemIndication"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render problem indicators.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see #EDITOR_PROBLEM_INDICATION
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_PROBLEM_INDICATION_COLOR = "problemIndicationColor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the editor shows warning indicators in text (squiggly
   * lines).
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_WARNING_INDICATION = "warningIndication"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render warning indicators.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see #EDITOR_WARNING_INDICATION
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_WARNING_INDICATION_COLOR = "warningIndicationColor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the editor shows task indicators in text (squiggly
   * lines).
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_TASK_INDICATION = "taskIndication"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render task indicators.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see #EDITOR_TASK_INDICATION
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_TASK_INDICATION_COLOR = "taskIndicationColor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the editor shows bookmark indicators in text (squiggly
   * lines).
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_BOOKMARK_INDICATION = "bookmarkIndication"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render bookmark indicators.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see #EDITOR_BOOKMARK_INDICATION
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_BOOKMARK_INDICATION_COLOR = "bookmarkIndicationColor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the editor shows search indicators in text (squiggly
   * lines).
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_SEARCH_RESULT_INDICATION = "searchResultIndication"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render search indicators.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see #EDITOR_SEARCH_RESULT_INDICATION
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_SEARCH_RESULT_INDICATION_COLOR = "searchResultIndicationColor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the editor shows unknown indicators in text (squiggly
   * lines).
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_UNKNOWN_INDICATION = "othersIndication"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render unknown indicators.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see #EDITOR_UNKNOWN_INDICATION
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_UNKNOWN_INDICATION_COLOR = "othersIndicationColor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the overview ruler shows error indicators.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER = "errorIndicationInOverviewRuler"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the overview ruler shows warning indicators.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER = "warningIndicationInOverviewRuler"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the overview ruler shows task indicators.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER = "taskIndicationInOverviewRuler"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the overview ruler shows bookmark indicators.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER = "bookmarkIndicationInOverviewRuler"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the overview ruler shows search result indicators.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER = "searchResultIndicationInOverviewRuler"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the overview ruler shows unknown indicators.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by {@link org.eclipse.ui.texteditor.MarkerAnnotationPreferences}
   */
  @Deprecated
  public final static String EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER = "othersIndicationInOverviewRuler"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'close strings' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_CLOSE_STRINGS = "closeStrings"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'wrap strings' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_WRAP_STRINGS = "wrapStrings"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'escape strings' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_ESCAPE_STRINGS = "escapeStrings"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'close brackets' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_CLOSE_BRACKETS = "closeBrackets"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'close braces' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_CLOSE_BRACES = "closeBraces"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'close JavaScript docs' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_CLOSE_JAVADOCS = "closeJavaDocs"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'add JavaDoc tags' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_ADD_JAVADOC_TAGS = "addJavaDocTags"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'format Javadoc tags' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_FORMAT_JAVADOCS = "autoFormatJavaDocs"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'smart paste' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_SMART_PASTE = "smartPaste"; //$NON-NLS-1$

  /**
   * A named preference that controls whether 'paste' should update the imports.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_IMPORTS_ON_PASTE = "importsOnPaste"; //$NON-NLS-1$

  /**
   * A named preference that controls whether the 'smart home-end' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.3 replaced by
   *             {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants#EDITOR_SMART_HOME_END}
   */
  @Deprecated
  public final static String EDITOR_SMART_HOME_END = AbstractTextEditor.PREFERENCE_NAVIGATION_SMART_HOME_END;

  /**
   * A named preference that controls whether the 'sub-word navigation' feature is enabled.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_SUB_WORD_NAVIGATION = "subWordNavigation"; //$NON-NLS-1$

  /**
   * A named preference that controls if temporary problems are evaluated and shown in the UI.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_EVALUTE_TEMPORARY_PROBLEMS = "handleTemporaryProblems"; //$NON-NLS-1$

  /**
   * A named preference that controls if the overview ruler is shown in the UI.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by
   *             {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants}
   */
  @Deprecated
  public final static String EDITOR_OVERVIEW_RULER = "overviewRuler"; //$NON-NLS-1$

  /**
   * A named preference that controls if the line number ruler is shown in the UI.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.0 replaced by
   *             {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants}
   */
  @Deprecated
  public final static String EDITOR_LINE_NUMBER_RULER = "lineNumberRuler"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render line numbers inside the line number
   * ruler.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @see #EDITOR_LINE_NUMBER_RULER
   * @deprecated as of 3.0 replaced by
   *             {@link org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants}
   */
  @Deprecated
  public final static String EDITOR_LINE_NUMBER_RULER_COLOR = "lineNumberColor"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render linked positions inside code templates.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @deprecated not used any longer as the linked positions are displayed as annotations
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  @Deprecated
  public final static String EDITOR_LINKED_POSITION_COLOR = "linkedPositionColor"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used as the text foreground. This value has not effect
   * if the system default color is used.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated As of 3.1, replaced by {@link AbstractTextEditor#PREFERENCE_COLOR_FOREGROUND}
   */
  @Deprecated
  public final static String EDITOR_FOREGROUND_COLOR = AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND;

  /**
   * A named preference that describes if the system default foreground color is used as the text
   * foreground.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated As of 3.1, replaced by
   *             {@link AbstractTextEditor#PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT}
   */
  @Deprecated
  public final static String EDITOR_FOREGROUND_DEFAULT_COLOR = AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT;

  /**
   * A named preference that holds the color used as the text background. This value has not effect
   * if the system default color is used.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated As of 3.1, replaced by {@link AbstractTextEditor#PREFERENCE_COLOR_BACKGROUND}
   */
  @Deprecated
  public final static String EDITOR_BACKGROUND_COLOR = AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND;

  /**
   * A named preference that describes if the system default background color is used as the text
   * background.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated As of 3.1, replaced by
   *             {@link AbstractTextEditor#PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT}
   */
  @Deprecated
  public final static String EDITOR_BACKGROUND_DEFAULT_COLOR = AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT;

  /**
   * Preference key suffix for bold text style preference keys.
   */
  public static final String EDITOR_BOLD_SUFFIX = "_bold"; //$NON-NLS-1$

  /**
   * Preference key suffix for italic text style preference keys.
   */
  public static final String EDITOR_ITALIC_SUFFIX = "_italic"; //$NON-NLS-1$

  /**
   * Preference key suffix for strikethrough text style preference keys.
   */
  public static final String EDITOR_STRIKETHROUGH_SUFFIX = "_strikethrough"; //$NON-NLS-1$

  /**
   * Preference key suffix for underline text style preference keys.
   */
  public static final String EDITOR_UNDERLINE_SUFFIX = "_underline"; //$NON-NLS-1$

  /**
   * A named preference that holds the color used to render multi-line comments.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_MULTI_LINE_COMMENT_COLOR = IDartColorConstants.JAVA_MULTI_LINE_COMMENT;

  /**
   * The symbolic font name for the JavaScript editor text font (value
   * <code>&quot;com.google.dart.tools.ui.editors.textfont&quot;</code>).
   */
  public final static String EDITOR_TEXT_FONT = "com.google.dart.tools.ui.editors.textfont"; //$NON-NLS-1$

  /**
   * A named preference that controls whether multi-line comments are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> multi-line comments are rendered in
   * bold. If <code>false</code> the are rendered using no font style attribute.
   * </p>
   */
  public final static String EDITOR_MULTI_LINE_COMMENT_BOLD = IDartColorConstants.JAVA_MULTI_LINE_COMMENT
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether multi-line comments are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> multi-line comments are rendered in
   * italic. If <code>false</code> the are rendered using no italic font style attribute.
   * </p>
   */
  public final static String EDITOR_MULTI_LINE_COMMENT_ITALIC = IDartColorConstants.JAVA_MULTI_LINE_COMMENT
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether multi-line comments are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> multi-line comments are rendered in
   * strikethrough. If <code>false</code> the are rendered using no strikethrough font style
   * attribute.
   * </p>
   */
  public final static String EDITOR_MULTI_LINE_COMMENT_STRIKETHROUGH = IDartColorConstants.JAVA_MULTI_LINE_COMMENT
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether multi-line comments are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> multi-line comments are rendered in
   * underline. If <code>false</code> the are rendered using no underline font style attribute.
   * </p>
   */
  public final static String EDITOR_MULTI_LINE_COMMENT_UNDERLINE = IDartColorConstants.JAVA_MULTI_LINE_COMMENT
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render single line comments.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_SINGLE_LINE_COMMENT_COLOR = IDartColorConstants.JAVA_SINGLE_LINE_COMMENT;

  /**
   * A named preference that controls whether single line comments are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> single line comments are rendered
   * in bold. If <code>false</code> the are rendered using no font style attribute.
   * </p>
   */
  public final static String EDITOR_SINGLE_LINE_COMMENT_BOLD = IDartColorConstants.JAVA_SINGLE_LINE_COMMENT
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether single line comments are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> single line comments are rendered
   * in italic. If <code>false</code> the are rendered using no italic font style attribute.
   * </p>
   */
  public final static String EDITOR_SINGLE_LINE_COMMENT_ITALIC = IDartColorConstants.JAVA_SINGLE_LINE_COMMENT
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether single line comments are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> single line comments are rendered
   * in strikethrough. If <code>false</code> the are rendered using no italic font style attribute.
   * </p>
   */
  public final static String EDITOR_SINGLE_LINE_COMMENT_STRIKETHROUGH = IDartColorConstants.JAVA_SINGLE_LINE_COMMENT
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether single line comments are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> single line comments are rendered
   * in underline. If <code>false</code> the are rendered using no italic font style attribute.
   * </p>
   */
  public final static String EDITOR_SINGLE_LINE_COMMENT_UNDERLINE = IDartColorConstants.JAVA_SINGLE_LINE_COMMENT
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render JavaScript keywords.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_DART_KEYWORD_COLOR = IDartColorConstants.JAVA_KEYWORD;

  /**
   * A named preference that controls whether keywords are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_KEYWORD_BOLD = IDartColorConstants.JAVA_KEYWORD
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether keywords are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_KEYWORD_ITALIC = IDartColorConstants.JAVA_KEYWORD
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether keywords are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_KEYWORD_STRIKETHROUGH = IDartColorConstants.JAVA_KEYWORD
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether keywords are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_KEYWORD_UNDERLINE = IDartColorConstants.JAVA_KEYWORD
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render string constants.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_STRING_COLOR = IDartColorConstants.JAVA_STRING;

  /**
   * A named preference that holds the color used to render multi-line string constants.
   */

  public final static String EDITOR_MULTI_LINE_STRING_COLOR = IDartColorConstants.DART_MULTI_LINE_STRING;

  /**
   * A named preference that controls whether string constants are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_STRING_BOLD = IDartColorConstants.JAVA_STRING
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether string constants are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_STRING_ITALIC = IDartColorConstants.JAVA_STRING
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether string constants are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_STRING_STRIKETHROUGH = IDartColorConstants.JAVA_STRING
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether string constants are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_STRING_UNDERLINE = IDartColorConstants.JAVA_STRING
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render method names.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated the method name highlighting has been replaced by a semantic highlighting, see
   *             {@link com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings#METHOD}
   */
  @Deprecated
  public final static String EDITOR_JAVA_METHOD_NAME_COLOR = IDartColorConstants.JAVA_METHOD_NAME;

  /**
   * A named preference that controls whether method names are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated the method name highlighting has been replaced by a semantic highlighting, see
   *             {@link com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings#METHOD}
   */
  @Deprecated
  public final static String EDITOR_JAVA_METHOD_NAME_BOLD = IDartColorConstants.JAVA_METHOD_NAME
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether method names are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated the method name highlighting has been replaced by a semantic highlighting, see
   *             {@link com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings#METHOD}
   */
  @Deprecated
  public final static String EDITOR_JAVA_METHOD_NAME_ITALIC = IDartColorConstants.JAVA_METHOD_NAME
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that holds the color used to render the 'return' keyword.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_DART_KEYWORD_RETURN_COLOR = IDartColorConstants.JAVA_KEYWORD_RETURN;

  /**
   * A named preference that controls whether 'return' keyword is rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_KEYWORD_RETURN_BOLD = IDartColorConstants.JAVA_KEYWORD_RETURN
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether 'return' keyword is rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_KEYWORD_RETURN_ITALIC = IDartColorConstants.JAVA_KEYWORD_RETURN
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether 'return' keyword is rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_KEYWORD_RETURN_STRIKETHROUGH = IDartColorConstants.JAVA_KEYWORD_RETURN
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether 'return' keyword is rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_KEYWORD_RETURN_UNDERLINE = IDartColorConstants.JAVA_KEYWORD_RETURN
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render operators.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_DART_OPERATOR_COLOR = IDartColorConstants.JAVA_OPERATOR;

  /**
   * A named preference that controls whether operators are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_OPERATOR_BOLD = IDartColorConstants.JAVA_OPERATOR
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether operators are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_OPERATOR_ITALIC = IDartColorConstants.JAVA_OPERATOR
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether operators are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_OPERATOR_STRIKETHROUGH = IDartColorConstants.JAVA_OPERATOR
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether operators are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_OPERATOR_UNDERLINE = IDartColorConstants.JAVA_OPERATOR
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render brackets.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_DART_BRACKET_COLOR = IDartColorConstants.JAVA_BRACKET;

  /**
   * A named preference that controls whether brackets are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_BRACKET_BOLD = IDartColorConstants.JAVA_BRACKET
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether brackets are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_BRACKET_ITALIC = IDartColorConstants.JAVA_BRACKET
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether brackets are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_BRACKET_STRIKETHROUGH = IDartColorConstants.JAVA_BRACKET
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether brackets are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_BRACKET_UNDERLINE = IDartColorConstants.JAVA_BRACKET
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render annotations.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated the annotation highlighting has been replaced by a semantic highlighting, see
   *             {@link com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings#ANNOTATION}
   */
  @Deprecated
  public final static String EDITOR_JAVA_ANNOTATION_COLOR = IDartColorConstants.JAVA_ANNOTATION;

  /**
   * A named preference that controls whether annotations are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated the annotation highlighting has been replaced by a semantic highlighting, see
   *             {@link com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings#ANNOTATION}
   */
  @Deprecated
  public final static String EDITOR_JAVA_ANNOTATION_BOLD = IDartColorConstants.JAVA_ANNOTATION
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether annotations are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated the annotation highlighting has been replaced by a semantic highlighting, see
   *             {@link com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings#ANNOTATION}
   */
  @Deprecated
  public final static String EDITOR_JAVA_ANNOTATION_ITALIC = IDartColorConstants.JAVA_ANNOTATION
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether annotations are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated the annotation highlighting has been replaced by a semantic highlighting, see
   *             {@link com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings#ANNOTATION}
   */
  @Deprecated
  public final static String EDITOR_JAVA_ANNOTATION_STRIKETHROUGH = IDartColorConstants.JAVA_ANNOTATION
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether annotations are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated the annotation highlighting has been replaced by a semantic highlighting, see
   *             {@link com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings#ANNOTATION}
   */
  @Deprecated
  public final static String EDITOR_JAVA_ANNOTATION_UNDERLINE = IDartColorConstants.JAVA_ANNOTATION
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render JavaScript default text.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_DART_DEFAULT_COLOR = IDartColorConstants.JAVA_DEFAULT;

  /**
   * A named preference that controls whether JavaScript default text is rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_DEFAULT_BOLD = IDartColorConstants.JAVA_DEFAULT
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether JavaScript default text is rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_DEFAULT_ITALIC = IDartColorConstants.JAVA_DEFAULT
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether JavaScript default text is rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_DEFAULT_STRIKETHROUGH = IDartColorConstants.JAVA_DEFAULT
      + EDITOR_STRIKETHROUGH_SUFFIX;
  /**
   * A named preference that controls whether JavaScript default text is rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVA_DEFAULT_UNDERLINE = IDartColorConstants.JAVA_DEFAULT
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render task tags.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_TASK_TAG_COLOR = IDartColorConstants.TASK_TAG;

  /**
   * A named preference that controls whether task tags are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_TASK_TAG_BOLD = IDartColorConstants.TASK_TAG
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether task tags are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_TASK_TAG_ITALIC = IDartColorConstants.TASK_TAG
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether task tags are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_TASK_TAG_STRIKETHROUGH = IDartColorConstants.TASK_TAG
      + EDITOR_STRIKETHROUGH_SUFFIX;
  /**
   * A named preference that controls whether task tags are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_TASK_TAG_UNDERLINE = IDartColorConstants.TASK_TAG
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render javadoc keywords.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_DARTDOC_KEYWORD_COLOR = IDartColorConstants.JAVADOC_KEYWORD;

  /**
   * A named preference that controls whether javadoc keywords are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_KEYWORD_BOLD = IDartColorConstants.JAVADOC_KEYWORD
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether javadoc keywords are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_KEYWORD_ITALIC = IDartColorConstants.JAVADOC_KEYWORD
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether javadoc keywords are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_KEYWORD_STRIKETHROUGH = IDartColorConstants.JAVADOC_KEYWORD
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether javadoc keywords are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_KEYWORD_UNDERLINE = IDartColorConstants.JAVADOC_KEYWORD
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render javadoc tags.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_DARTDOC_TAG_COLOR = IDartColorConstants.JAVADOC_TAG;

  /**
   * A named preference that controls whether javadoc tags are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_TAG_BOLD = IDartColorConstants.JAVADOC_TAG
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether javadoc tags are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_TAG_ITALIC = IDartColorConstants.JAVADOC_TAG
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether javadoc tags are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_TAG_STRIKETHROUGH = IDartColorConstants.JAVADOC_TAG
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether javadoc tags are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_TAG_UNDERLINE = IDartColorConstants.JAVADOC_TAG
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render javadoc links.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_DARTDOC_LINKS_COLOR = IDartColorConstants.JAVADOC_LINK;

  /**
   * A named preference that controls whether javadoc links are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_LINKS_BOLD = IDartColorConstants.JAVADOC_LINK
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether javadoc links are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_LINKS_ITALIC = IDartColorConstants.JAVADOC_LINK
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether javadoc links are rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_LINKS_STRIKETHROUGH = IDartColorConstants.JAVADOC_LINK
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether javadoc links are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_LINKS_UNDERLINE = IDartColorConstants.JAVADOC_LINK
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render javadoc default text.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_DARTDOC_DEFAULT_COLOR = IDartColorConstants.JAVADOC_DEFAULT;

  /**
   * A named preference that controls whether javadoc default text is rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_DEFAULT_BOLD = IDartColorConstants.JAVADOC_DEFAULT
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether javadoc default text is rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_DEFAULT_ITALIC = IDartColorConstants.JAVADOC_DEFAULT
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether javadoc default text is rendered in strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_DEFAULT_STRIKETHROUGH = IDartColorConstants.JAVADOC_DEFAULT
      + EDITOR_STRIKETHROUGH_SUFFIX;
  /**
   * A named preference that controls whether javadoc default text is rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String EDITOR_JAVADOC_DEFAULT_UNDERLINE = IDartColorConstants.JAVADOC_DEFAULT
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used for 'linked-mode' underline.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   * @deprecated As of 3.1, replaced by
   *             {@link AbstractDecoratedTextEditorPreferenceConstants#EDITOR_HYPERLINK_COLOR}
   */
  @Deprecated
  public final static String EDITOR_LINK_COLOR = "linkColor"; //$NON-NLS-1$

  /**
   * A named preference that controls whether hover tool tips in the editor are turned on or off.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_SHOW_HOVER = "com.google.dart.tools.ui.editor.showHover"; //$NON-NLS-1$

  /**
   * A named preference that defines the hover shown when no control key is pressed.
   * <p>
   * Value is of type <code>String</code>: possible values are <code>
   * EDITOR_NO_HOVER_CONFIGURED_ID</code> or <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the
   * hover id of a hover contributed as <code>javaEditorTextHovers</code>.
   * </p>
   * 
   * @see #EDITOR_NO_HOVER_CONFIGURED_ID
   * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
   * @see DartUI
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_NONE_HOVER = "noneHover"; //$NON-NLS-1$

  /**
   * A named preference that defines the hover shown when the <code>CTRL</code> modifier key is
   * pressed.
   * <p>
   * Value is of type <code>String</code>: possible values are <code>
   * EDITOR_NO_HOVER_CONFIGURED_ID</code> or <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the
   * hover id of a hover contributed as <code>javaEditorTextHovers</code>.
   * </p>
   * 
   * @see #EDITOR_NO_HOVER_CONFIGURED_ID
   * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
   * @see DartUI
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_CTRL_HOVER = "ctrlHover"; //$NON-NLS-1$

  /**
   * A named preference that defines the hover shown when the <code>SHIFT</code> modifier key is
   * pressed.
   * <p>
   * Value is of type <code>String</code>: possible values are <code>
   * EDITOR_NO_HOVER_CONFIGURED_ID</code> or <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the
   * hover id of a hover contributed as <code>javaEditorTextHovers</code>.
   * </p>
   * 
   * @see #EDITOR_NO_HOVER_CONFIGURED_ID
   * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
   * @see DartUI ID_*_HOVER
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_SHIFT_HOVER = "shiftHover"; //$NON-NLS-1$

  /**
   * A named preference that defines the hover shown when the <code>CTRL + ALT</code> modifier keys
   * is pressed.
   * <p>
   * Value is of type <code>String</code>: possible values are <code>
   * EDITOR_NO_HOVER_CONFIGURED_ID</code> or <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the
   * hover id of a hover contributed as <code>javaEditorTextHovers</code>.
   * </p>
   * 
   * @see #EDITOR_NO_HOVER_CONFIGURED_ID
   * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
   * @see DartUI ID_*_HOVER
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_CTRL_ALT_HOVER = "ctrlAltHover"; //$NON-NLS-1$

  /**
   * A named preference that defines the hover shown when the <code>CTRL + ALT + SHIFT</code>
   * modifier keys is pressed.
   * <p>
   * Value is of type <code>String</code>: possible values are <code>
   * EDITOR_NO_HOVER_CONFIGURED_ID</code> or <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the
   * hover id of a hover contributed as <code>javaEditorTextHovers</code>.
   * </p>
   * 
   * @see #EDITOR_NO_HOVER_CONFIGURED_ID
   * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
   * @see DartUI ID_*_HOVER
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_CTRL_ALT_SHIFT_HOVER = "ctrlAltShiftHover"; //$NON-NLS-1$

  /**
   * A named preference that defines the hover shown when the <code>CTRL + SHIFT</code> modifier
   * keys is pressed.
   * <p>
   * Value is of type <code>String</code>: possible values are <code>
   * EDITOR_NO_HOVER_CONFIGURED_ID</code> or <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the
   * hover id of a hover contributed as <code>javaEditorTextHovers</code>.
   * </p>
   * 
   * @see #EDITOR_NO_HOVER_CONFIGURED_ID
   * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
   * @see DartUI ID_*_HOVER
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_CTRL_SHIFT_HOVER = "ctrlShiftHover"; //$NON-NLS-1$

  /**
   * A named preference that defines the hover shown when the <code>ALT</code> modifier key is
   * pressed.
   * <p>
   * Value is of type <code>String</code>: possible values are <code>
   * EDITOR_NO_HOVER_CONFIGURED_ID</code>, <code>EDITOR_DEFAULT_HOVER_CONFIGURED_ID</code> or the
   * hover id of a hover contributed as <code>javaEditorTextHovers</code>.
   * </p>
   * 
   * @see #EDITOR_NO_HOVER_CONFIGURED_ID
   * @see #EDITOR_DEFAULT_HOVER_CONFIGURED_ID
   * @see DartUI ID_*_HOVER
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_ALT_SHIFT_HOVER = "altShiftHover"; //$NON-NLS-1$

  /**
   * A string value used by the named preferences for hover configuration to describe that no hover
   * should be shown for the given key modifiers.
   * 
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_NO_HOVER_CONFIGURED_ID = "noHoverConfiguredId"; //$NON-NLS-1$

  /**
   * A string value used by the named preferences for hover configuration to describe that the
   * default hover should be shown for the given key modifiers. The default hover is described by
   * the <code>EDITOR_DEFAULT_HOVER</code> property.
   * 
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_DEFAULT_HOVER_CONFIGURED_ID = "defaultHoverConfiguredId"; //$NON-NLS-1$

  /**
   * A named preference that defines the hover named the 'default hover'. Value is of type
   * <code>String
   * </code>: possible values are <code>
   * EDITOR_NO_HOVER_CONFIGURED_ID</code> or the hover id of a hover contributed as
   * <code>javaEditorTextHovers
   * </code>. </p>
   * 
   * @deprecated As of 3.0, replaced by {@link #EDITOR_TEXT_HOVER_MODIFIERS}
   */
  @Deprecated
  public static final String EDITOR_DEFAULT_HOVER = "defaultHover"; //$NON-NLS-1$

  /**
   * A named preference that controls if segmented view (show selected element only) is turned on or
   * off.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_SHOW_SEGMENTS = "com.google.dart.tools.ui.editor.showSegments"; //$NON-NLS-1$

  /**
   * A named preference that controls if browser like links are turned on or off.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated As of 3.1, replaced by
   *             {@link AbstractDecoratedTextEditorPreferenceConstants#EDITOR_HYPERLINKS_ENABLED}
   */
  @Deprecated
  public static final String EDITOR_BROWSER_LIKE_LINKS = "browserLikeLinks"; //$NON-NLS-1$

  /**
   * A named preference that controls the key modifier for browser like links.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   * 
   * @deprecated As of 3.1, replaced by
   *             {@link AbstractDecoratedTextEditorPreferenceConstants#EDITOR_HYPERLINK_KEY_MODIFIER}
   */
  @Deprecated
  public static final String EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER = "browserLikeLinksKeyModifier"; //$NON-NLS-1$

  /**
   * A named preference that controls the key modifier mask for browser like links. The value is
   * only used if the value of <code>EDITOR_BROWSER_LIKE_LINKS</code> cannot be resolved to valid
   * SWT modifier bits.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   * 
   * @see #EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER
   * @deprecated As of 3.1, replaced by
   *             {@link AbstractDecoratedTextEditorPreferenceConstants#EDITOR_HYPERLINK_KEY_MODIFIER_MASK}
   */
  @Deprecated
  public static final String EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER_MASK = "browserLikeLinksKeyModifierMask"; //$NON-NLS-1$

  /**
   * A named preference that controls whether occurrences are marked in the editor.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_OCCURRENCES = "markOccurrences"; //$NON-NLS-1$

  /**
   * A named preference that controls whether occurrences are sticky in the editor.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_STICKY_OCCURRENCES = "stickyOccurrences"; //$NON-NLS-1$

  /**
   * A named preference that controls whether type occurrences are marked. Only valid if
   * {@link #EDITOR_MARK_OCCURRENCES} is <code>true</code>.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_TYPE_OCCURRENCES = "markTypeOccurrences"; //$NON-NLS-1$

  /**
   * A named preference that controls whether method occurrences are marked. Only valid if
   * {@link #EDITOR_MARK_OCCURRENCES} is <code>true</code>.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_METHOD_OCCURRENCES = "markMethodOccurrences"; //$NON-NLS-1$

  /**
   * A named preference that controls whether non-constant field occurrences are marked. Only valid
   * if {@link #EDITOR_MARK_OCCURRENCES} is <code>true</code> .
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_FIELD_OCCURRENCES = "markFieldOccurrences"; //$NON-NLS-1$
  /**
   * A named preference that controls whether constant (static final) occurrences are marked. Only
   * valid if {@link #EDITOR_MARK_OCCURRENCES} is <code>true</code>.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_CONSTANT_OCCURRENCES = "markConstantOccurrences"; //$NON-NLS-1$
  /**
   * A named preference that controls whether local variable occurrences are marked. Only valid if
   * {@link #EDITOR_MARK_OCCURRENCES} is <code>true</code> .
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_LOCAL_VARIABLE_OCCURRENCES = "markLocalVariableOccurrences"; //$NON-NLS-1$

  /**
   * A named preference that controls whether exception occurrences are marked. Only valid if
   * {@link #EDITOR_MARK_OCCURRENCES} is <code>true</code>.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_EXCEPTION_OCCURRENCES = "markExceptionOccurrences"; //$NON-NLS-1$

  /**
   * A named preference that controls whether method exit points are marked. Only valid if
   * {@link #EDITOR_MARK_OCCURRENCES} is <code>true</code>.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_METHOD_EXIT_POINTS = "markMethodExitPoints"; //$NON-NLS-1$

  /**
   * A named preference that controls whether targets for of <code>break</code> and <code>
   * continue</code> statements are marked. Only valid if {@link #EDITOR_MARK_OCCURRENCES} is
   * <code>true
   * </code>.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_BREAK_CONTINUE_TARGETS = "markBreakContinueTargets"; //$NON-NLS-1$

  /**
   * A named preference that controls whether method exit points are marked. Only valid if
   * {@link #EDITOR_MARK_OCCURRENCES} is <code>true</code>.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_MARK_IMPLEMENTORS = "markImplementors"; //$NON-NLS-1$

  /**
   * A named preference prefix for semantic highlighting preferences.
   */
  public static final String EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX = "semanticHighlighting."; //$NON-NLS-1$

  /**
   * A named preference that controls if semantic highlighting is enabled.
   * <p>
   * Value is of type <code>Boolean</code>:<code>true</code> if enabled.
   * </p>
   * 
   * @deprecated As of 3.1, this preference is not used or set any longer; see
   *             {@link SemanticHighlightings#affectsEnablement(IPreferenceStore, org.eclipse.jface.util.PropertyChangeEvent)}
   */
  @Deprecated
  public static final String EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED = EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
      + "enabled"; //$NON-NLS-1$

  /**
   * A named preference suffix that controls a semantic highlighting's color.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public static final String EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX = ".color"; //$NON-NLS-1$

  /**
   * A named preference suffix that controls if semantic highlighting has the text attribute bold.
   * <p>
   * Value is of type <code>Boolean</code>: <code>true</code> if bold.
   * </p>
   */
  public static final String EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX = ".bold"; //$NON-NLS-1$

  /**
   * A named preference suffix that controls if semantic highlighting has the text attribute italic.
   * <p>
   * Value is of type <code>Boolean</code>: <code>true</code> if italic.
   * </p>
   */
  public static final String EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX = ".italic"; //$NON-NLS-1$

  /**
   * A named preference suffix that controls if semantic highlighting has the text attribute
   * strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>: <code>true</code> if strikethrough.
   * </p>
   */
  public static final String EDITOR_SEMANTIC_HIGHLIGHTING_STRIKETHROUGH_SUFFIX = ".strikethrough"; //$NON-NLS-1$

  /**
   * A named preference suffix that controls if semantic highlighting has the text attribute
   * underline.
   * <p>
   * Value is of type <code>Boolean</code>: <code>true</code> if underline.
   * </p>
   */
  public static final String EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX = ".underline"; //$NON-NLS-1$

  /**
   * A named preference suffix that controls if semantic highlighting is enabled.
   * <p>
   * Value is of type <code>Boolean</code>: <code>true</code> if enabled.
   * </p>
   */
  public static final String EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX = ".enabled"; //$NON-NLS-1$

  /**
   * A named preference that controls disabling of the overwrite mode.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @deprecated as of 3.1 replaced by
   *             {@link AbstractDecoratedTextEditorPreferenceConstants#EDITOR_DISABLE_OVERWRITE_MODE}
   */
  @Deprecated
  public static final String EDITOR_DISABLE_OVERWRITE_MODE = "disable_overwrite_mode"; //$NON-NLS-1$

  /**
   * A named preference that controls the "smart semicolon" smart typing handler.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_SMART_SEMICOLON = "smart_semicolon"; //$NON-NLS-1$

  /**
   * A named preference that controls the smart backspace behavior.
   * <p>
   * Value is of type <code>Boolean</code>.
   */
  public static final String EDITOR_SMART_BACKSPACE = "smart_backspace"; //$NON-NLS-1$

  /**
   * A named preference that controls the "smart opening brace" smart typing handler.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_SMART_OPENING_BRACE = "smart_opening_brace"; //$NON-NLS-1$

  /**
   * A named preference that controls the smart tab behavior.
   * <p>
   * Value is of type <code>Boolean</code>.
   */
  public static final String EDITOR_SMART_TAB = "smart_tab"; //$NON-NLS-1$

  /**
   * A named preference that controls whether words containing digits should be skipped during spell
   * checking.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String SPELLING_IGNORE_DIGITS = "spelling_ignore_digits"; //$NON-NLS-1$

  /**
   * A named preference that controls whether mixed case words should be skipped during spell
   * checking.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String SPELLING_IGNORE_MIXED = "spelling_ignore_mixed"; //$NON-NLS-1$

  /**
   * A named preference that controls whether sentence capitalization should be ignored during spell
   * checking.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String SPELLING_IGNORE_SENTENCE = "spelling_ignore_sentence"; //$NON-NLS-1$

  /**
   * A named preference that controls whether upper case words should be skipped during spell
   * checking.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String SPELLING_IGNORE_UPPER = "spelling_ignore_upper"; //$NON-NLS-1$

  /**
   * A named preference that controls whether URLs should be ignored during spell checking.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String SPELLING_IGNORE_URLS = "spelling_ignore_urls"; //$NON-NLS-1$

  /**
   * A named preference that controls whether single letters should be ignored during spell
   * checking.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String SPELLING_IGNORE_SINGLE_LETTERS = "spelling_ignore_single_letters"; //$NON-NLS-1$

  /**
   * A named preference that controls whether '&' in JavaScript properties files are ignored.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String SPELLING_IGNORE_AMPERSAND_IN_PROPERTIES = "spelling_ignore_ampersand_in_properties"; //$NON-NLS-1$

  /**
   * A named preference that controls whether non-letters at word boundaries should be ignored
   * during spell checking.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String SPELLING_IGNORE_NON_LETTERS = "spelling_ignore_non_letters"; //$NON-NLS-1$

  /**
   * A named preference that controls the locale used for spell checking.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public final static String SPELLING_LOCALE = "spelling_locale"; //$NON-NLS-1$

  /**
   * A named preference that controls the number of proposals offered during spell checking.
   * <p>
   * Value is of type <code>Integer</code>.
   * </p>
   */
  public final static String SPELLING_PROPOSAL_THRESHOLD = "spelling_proposal_threshold"; //$NON-NLS-1$

  /**
   * A named preference that specifies the workspace user dictionary.
   * <p>
   * Value is of type <code>Integer</code>.
   * </p>
   */
  public final static String SPELLING_USER_DICTIONARY = "spelling_user_dictionary"; //$NON-NLS-1$

  /**
   * A named preference that specifies encoding of the workspace user dictionary.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public final static String SPELLING_USER_DICTIONARY_ENCODING = "spelling_user_dictionary_encoding"; //$NON-NLS-1$

  /**
   * A named preference that specifies whether spelling dictionaries are available to content
   * assist. <strong>Note:</strong> This is currently not supported because the spelling engine
   * cannot return word proposals but only correction proposals.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String SPELLING_ENABLE_CONTENTASSIST = "spelling_enable_contentassist"; //$NON-NLS-1$

  /**
   * A named preference that controls if the JavaScript code assist gets auto activated.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String CODEASSIST_AUTOACTIVATION = "content_assist_autoactivation"; //$NON-NLS-1$

  /**
   * A name preference that holds the auto activation delay time in milliseconds.
   * <p>
   * Value is of type <code>Integer</code>.
   * </p>
   */
  public final static String CODEASSIST_AUTOACTIVATION_DELAY = "content_assist_autoactivation_delay"; //$NON-NLS-1$

  /**
   * A named preference that controls if code assist contains only visible proposals.
   * <p>
   * Value is of type <code>Boolean</code>. if <code>true</code> code assist only contains visible
   * members. If <code>false</code> all members are included.
   * </p>
   */
  public final static String CODEASSIST_SHOW_VISIBLE_PROPOSALS = "content_assist_show_visible_proposals"; //$NON-NLS-1$

  /**
   * A named preference that controls if the JavaScript code assist inserts a proposal automatically
   * if only one proposal is available.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String CODEASSIST_AUTOINSERT = "content_assist_autoinsert"; //$NON-NLS-1$

  /**
   * A named preference that controls if the JavaScript code assist adds import statements.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String CODEASSIST_ADDIMPORT = "content_assist_add_import"; //$NON-NLS-1$

  /**
   * A named preference that controls if the JavaScript code assist only inserts completions. If set
   * to false the proposals can also _replace_ code.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String CODEASSIST_INSERT_COMPLETION = "content_assist_insert_completion"; //$NON-NLS-1$	

  /**
   * A named preference that controls whether code assist proposals filtering is case sensitive or
   * not.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String CODEASSIST_CASE_SENSITIVITY = "content_assist_case_sensitivity"; //$NON-NLS-1$

  /**
   * A named preference that defines if code assist proposals are sorted in alphabetical order.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> that are sorted in alphabetical
   * order. If <code>false</code> that are unsorted.
   * </p>
   * 
   * @deprecated use {@link #CODEASSIST_SORTER} instead
   */
  @Deprecated
  public final static String CODEASSIST_ORDER_PROPOSALS = "content_assist_order_proposals"; //$NON-NLS-1$

  /**
   * A named preference that controls if argument names are filled in when a method is selected from
   * as list of code assist proposal.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String CODEASSIST_FILL_ARGUMENT_NAMES = "content_assist_fill_method_arguments"; //$NON-NLS-1$

  /**
   * A named preference that controls if method arguments are guessed when a method is selected from
   * as list of code assist proposal.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String CODEASSIST_GUESS_METHOD_ARGUMENTS = "content_assist_guess_method_arguments"; //$NON-NLS-1$

  /**
   * A named preference that holds the characters that auto activate code assist in JavaScript code.
   * <p>
   * Value is of type <code>String</code>. All characters that trigger auto code assist in
   * JavaScript code.
   * </p>
   */
  public final static String CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA = "content_assist_autoactivation_triggers_java"; //$NON-NLS-1$

  /**
   * A named preference that holds the characters that auto activate code assist in Javadoc.
   * <p>
   * Value is of type <code>String</code>. All characters that trigger auto code assist in Javadoc.
   * </p>
   */
  public final static String CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC = "content_assist_autoactivation_triggers_javadoc"; //$NON-NLS-1$

  /**
   * A named preference that holds the background color used in the code assist selection dialog.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String CODEASSIST_PROPOSALS_BACKGROUND = "content_assist_proposals_background"; //$NON-NLS-1$

  /**
   * A named preference that holds the foreground color used in the code assist selection dialog.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String CODEASSIST_PROPOSALS_FOREGROUND = "content_assist_proposals_foreground"; //$NON-NLS-1$

  /**
   * A named preference that holds the background color used for parameter hints.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String CODEASSIST_PARAMETERS_BACKGROUND = "content_assist_parameters_background"; //$NON-NLS-1$

  /**
   * A named preference that holds the foreground color used in the code assist selection dialog.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String CODEASSIST_PARAMETERS_FOREGROUND = "content_assist_parameters_foreground"; //$NON-NLS-1$

  /**
   * A named preference that holds the background color used in the code assist selection dialog to
   * mark replaced code.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String CODEASSIST_REPLACEMENT_BACKGROUND = "content_assist_completion_replacement_background"; //$NON-NLS-1$

  /**
   * A named preference that holds the foreground color used in the code assist selection dialog to
   * mark replaced code.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String CODEASSIST_REPLACEMENT_FOREGROUND = "content_assist_completion_replacement_foreground"; //$NON-NLS-1$

  /**
   * A named preference that holds the favorite static members.
   * <p>
   * Value is of type <code>String</code>: semicolon separated list of favorites.
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String CODEASSIST_FAVORITE_STATIC_MEMBERS = "content_assist_favorite_static_members"; //$NON-NLS-1$

  /**
   * A named preference that controls the behavior of the refactoring wizard for showing the error
   * page.
   * <p>
   * Value is of type <code>String</code>. Valid values are: <code>REFACTOR_FATAL_SEVERITY</code>,
   * <code>REFACTOR_ERROR_SEVERITY</code>, <code>REFACTOR_WARNING_SEVERITY</code>
   * <code>REFACTOR_INFO_SEVERITY</code>, <code>REFACTOR_OK_SEVERITY</code>.
   * </p>
   * 
   * @see #REFACTOR_FATAL_SEVERITY
   * @see #REFACTOR_ERROR_SEVERITY
   * @see #REFACTOR_WARNING_SEVERITY
   * @see #REFACTOR_INFO_SEVERITY
   * @see #REFACTOR_OK_SEVERITY
   * @deprecated Use method
   *             {@link org.eclipse.ltk.core.refactoring.RefactoringCore#getConditionCheckingFailedSeverity()}
   *             .
   */
  @Deprecated
  public static final String REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD = "Refactoring.ErrorPage.severityThreshold"; //$NON-NLS-1$

  /**
   * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>
   * .
   * 
   * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
   * @deprecated Use constant {@link org.eclipse.ltk.core.refactoring.RefactoringStatus#FATAL}
   */
  @Deprecated
  public static final String REFACTOR_FATAL_SEVERITY = "4"; //$NON-NLS-1$

  /**
   * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>
   * .
   * 
   * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
   * @deprecated Use constant {@link org.eclipse.ltk.core.refactoring.RefactoringStatus#ERROR}
   */
  @Deprecated
  public static final String REFACTOR_ERROR_SEVERITY = "3"; //$NON-NLS-1$

  /**
   * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>
   * .
   * 
   * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
   * @deprecated Use constant {@link org.eclipse.ltk.core.refactoring.RefactoringStatus#WARNING}
   */
  @Deprecated
  public static final String REFACTOR_WARNING_SEVERITY = "2"; //$NON-NLS-1$

  /**
   * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>
   * .
   * 
   * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
   * @deprecated Use constant {@link org.eclipse.ltk.core.refactoring.RefactoringStatus#INFO}
   */
  @Deprecated
  public static final String REFACTOR_INFO_SEVERITY = "1"; //$NON-NLS-1$

  /**
   * A string value used by the named preference <code>REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD</code>
   * .
   * 
   * @see #REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD
   * @deprecated Use constant {@link org.eclipse.ltk.core.refactoring.RefactoringStatus#OK}
   */
  @Deprecated
  public static final String REFACTOR_OK_SEVERITY = "0"; //$NON-NLS-1$

  /**
   * A named preference that controls whether all dirty editors are automatically saved before a
   * refactoring is executed.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String REFACTOR_SAVE_ALL_EDITORS = "Refactoring.savealleditors"; //$NON-NLS-1$

  /**
   * A named preference that controls whether certain refactorings use a lightweight UI when started
   * from a JavaScript editor.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * <p>
   * Note: this is work in progress and may change any time
   * </p>
   */
  public static final String REFACTOR_LIGHTWEIGHT = "Refactor.lightweight"; //$NON-NLS-1$

  /**
   * A named preference that controls a reduced search menu is used in the JavaScript editors.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String SEARCH_USE_REDUCED_MENU = "Search.usereducemenu"; //$NON-NLS-1$

  /**
   * A named preference that controls if the JavaScript Browsing views are linked to the active
   * editor.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String BROWSING_LINK_VIEW_TO_EDITOR = "com.google.dart.tools.ui.browsing.linktoeditor"; //$NON-NLS-1$

  /**
   * A named preference that controls the layout of the JavaScript Browsing views vertically.
   * Boolean value.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> the views are stacked vertical. If
   * <code>false
   * </code> they are stacked horizontal.
   * </p>
   */
  public static final String BROWSING_STACK_VERTICALLY = "com.google.dart.tools.ui.browsing.stackVertically"; //$NON-NLS-1$

  /**
   * A named preference that controls if templates are formatted when applied.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String TEMPLATES_USE_CODEFORMATTER = "com.google.dart.tools.ui.template.format"; //$NON-NLS-1$

  /**
   * A named preference that controls which profile is used by the code formatter.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public static final String FORMATTER_PROFILE = "formatter_profile"; //$NON-NLS-1$

  /**
   * A named preference that controls whether annotation roll over is used or not.
   * <p>
   * Value is of type <code>Boolean</code>. If <code>true</code> the annotation ruler column uses a
   * roll over to display multiple annotations
   * </p>
   */
  public static final String EDITOR_ANNOTATION_ROLL_OVER = "editor_annotation_roll_over"; //$NON-NLS-1$

  /**
   * A named preference that controls if content assist inserts the common prefix of all proposals
   * before presenting choices.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public final static String CODEASSIST_PREFIX_COMPLETION = "content_assist_prefix_completion"; //$NON-NLS-1$

  /**
   * A named preference that controls which completion proposal categories have been excluded from
   * the default proposal list.
   * <p>
   * Value is of type <code>String</code>, a "\0"-separated list of identifiers.
   * </p>
   */
  public static final String CODEASSIST_EXCLUDED_CATEGORIES = "content_assist_disabled_computers"; //$NON-NLS-1$

  /**
   * A named preference that controls which the order of the specific code assist commands.
   * <p>
   * Value is of type <code>String</code>, a "\0"-separated list of identifiers.
   * </p>
   */
  public static final String CODEASSIST_CATEGORY_ORDER = "content_assist_category_order"; //$NON-NLS-1$

  /**
   * A named preference that controls whether folding is enabled in the JavaScript editor.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_FOLDING_ENABLED = "editor_folding_enabled"; //$NON-NLS-1$

  /**
   * A named preference that stores the configured folding provider.
   * <p>
   * Value is of type <code>String</code>.
   * </p>
   */
  public static final String EDITOR_FOLDING_PROVIDER = "editor_folding_provider"; //$NON-NLS-1$

  /**
   * A named preference that stores the value for Javadoc folding for the default folding provider.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_FOLDING_JAVADOC = "editor_folding_default_javadoc"; //$NON-NLS-1$

  /**
   * A named preference that stores the value for inner type folding for the default folding
   * provider.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_FOLDING_INNERTYPES = "editor_folding_default_innertypes"; //$NON-NLS-1$

  /**
   * A named preference that stores the value for method folding for the default folding provider.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_FOLDING_METHODS = "editor_folding_default_methods"; //$NON-NLS-1$

  /**
   * A named preference that stores the value for imports folding for the default folding provider.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_FOLDING_IMPORTS = "editor_folding_default_imports"; //$NON-NLS-1$

  /**
   * A named preference that stores the value for header comment folding for the default folding
   * provider.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String EDITOR_FOLDING_HEADERS = "editor_folding_default_headers"; //$NON-NLS-1$

  public static final String EDITOR_FOLDING_CLASSES = "editor_folding_default_classes"; //$NON-NLS-1$
  public static final String EDITOR_FOLDING_FUNCTIONS = "editor_folding_default_functions"; //$NON-NLS-1$

  /**
   * The symbolic font name for the JavaScript properties file editor text font (value
   * <code>&quot;com.google.dart.tools.ui.PropertiesFileEditor.textfont&quot;</code> ).
   * 
   * @deprecated - we don't expose this editor
   */
  @Deprecated
  public static final String PROPERTIES_FILE_EDITOR_TEXT_FONT = "com.google.dart.tools.ui.PropertiesFileEditor.textfont"; //$NON-NLS-1$

  // ---------- Properties File Editor ----------

  /**
   * A named preference that holds the color used to render keys in a properties file.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public static final String PROPERTIES_FILE_COLORING_KEY = IDartColorConstants.PROPERTIES_FILE_COLORING_KEY;

  /**
   * A named preference that controls whether keys in a properties file are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_KEY_BOLD = PROPERTIES_FILE_COLORING_KEY
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether keys in a properties file are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_KEY_ITALIC = PROPERTIES_FILE_COLORING_KEY
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether keys in a properties file are rendered in
   * strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_KEY_STRIKETHROUGH = PROPERTIES_FILE_COLORING_KEY
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether keys in a properties file are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_KEY_UNDERLINE = PROPERTIES_FILE_COLORING_KEY
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render comments in a properties file.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public static final String PROPERTIES_FILE_COLORING_COMMENT = IDartColorConstants.PROPERTIES_FILE_COLORING_COMMENT;

  /**
   * A named preference that controls whether comments in a properties file are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_COMMENT_BOLD = PROPERTIES_FILE_COLORING_COMMENT
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether comments in a properties file are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_COMMENT_ITALIC = PROPERTIES_FILE_COLORING_COMMENT
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether comments in a properties file are rendered in
   * strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_COMMENT_STRIKETHROUGH = PROPERTIES_FILE_COLORING_COMMENT
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether comments in a properties file are rendered in
   * underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_COMMENT_UNDERLINE = PROPERTIES_FILE_COLORING_COMMENT
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render values in a properties file.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public static final String PROPERTIES_FILE_COLORING_VALUE = IDartColorConstants.PROPERTIES_FILE_COLORING_VALUE;

  /**
   * A named preference that controls whether values in a properties file are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_VALUE_BOLD = PROPERTIES_FILE_COLORING_VALUE
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether values in a properties file are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_VALUE_ITALIC = PROPERTIES_FILE_COLORING_VALUE
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether values in a properties file are rendered in
   * strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_VALUE_STRIKETHROUGH = PROPERTIES_FILE_COLORING_VALUE
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether values in a properties file are rendered in underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_VALUE_UNDERLINE = PROPERTIES_FILE_COLORING_VALUE
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render assignments in a properties file.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public static final String PROPERTIES_FILE_COLORING_ASSIGNMENT = IDartColorConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT;

  /**
   * A named preference that controls whether assignments in a properties file are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_ASSIGNMENT_BOLD = PROPERTIES_FILE_COLORING_ASSIGNMENT
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether assignments in a properties file are rendered in
   * italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_ASSIGNMENT_ITALIC = PROPERTIES_FILE_COLORING_ASSIGNMENT
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether assignments in a properties file are rendered in
   * strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_ASSIGNMENT_STRIKETHROUGH = PROPERTIES_FILE_COLORING_ASSIGNMENT
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether assignments in a properties file are rendered in
   * underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_ASSIGNMENT_UNDERLINE = PROPERTIES_FILE_COLORING_ASSIGNMENT
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that holds the color used to render arguments in a properties file.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public static final String PROPERTIES_FILE_COLORING_ARGUMENT = IDartColorConstants.PROPERTIES_FILE_COLORING_ARGUMENT;

  /**
   * A named preference that controls whether arguments in a properties file are rendered in bold.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_ARGUMENT_BOLD = PROPERTIES_FILE_COLORING_ARGUMENT
      + EDITOR_BOLD_SUFFIX;

  /**
   * A named preference that controls whether arguments in a properties file are rendered in italic.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_ARGUMENT_ITALIC = PROPERTIES_FILE_COLORING_ARGUMENT
      + EDITOR_ITALIC_SUFFIX;

  /**
   * A named preference that controls whether arguments in a properties file are rendered in
   * strikethrough.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_ARGUMENT_STRIKETHROUGH = PROPERTIES_FILE_COLORING_ARGUMENT
      + EDITOR_STRIKETHROUGH_SUFFIX;

  /**
   * A named preference that controls whether arguments in a properties file are rendered in
   * underline.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   */
  public static final String PROPERTIES_FILE_COLORING_ARGUMENT_UNDERLINE = PROPERTIES_FILE_COLORING_ARGUMENT
      + EDITOR_UNDERLINE_SUFFIX;

  /**
   * A named preference that stores the content assist LRU history
   * <p>
   * Value is an XML encoded version of the history.
   * </p>
   * 
   * @see org.eclipse.wst.jsdt.internal.ui.text.java.ContentAssistHistory#load(org.eclipse.core.runtime.Preferences,
   *      String)
   */
  public static final String CODEASSIST_LRU_HISTORY = "content_assist_lru_history"; //$NON-NLS-1$

  /**
   * A named preference that stores the content assist sorter id.
   * <p>
   * Value is a {@link String}.
   * </p>
   * 
   * @see org.eclipse.wst.jsdt.internal.ui.text.java.ProposalSorterRegistry
   */
  public static final String CODEASSIST_SORTER = "content_assist_sorter"; //$NON-NLS-1$

  /**
   * A named preference that holds the source hover background color.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_SOURCE_HOVER_BACKGROUND_COLOR = "sourceHoverBackgroundColor"; //$NON-NLS-1$

  /**
   * A named preference that tells whether to use the system default color (
   * {@link SWT#COLOR_INFO_BACKGROUND}) for the source hover background color.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String EDITOR_SOURCE_HOVER_BACKGROUND_COLOR_SYSTEM_DEFAULT = "sourceHoverBackgroundColor.SystemDefault"; //$NON-NLS-1$

  /**
   * The symbolic font name for the Dart views text font (value
   * <code>&quot;com.google.dart.tools.ui.views.textfont&quot;</code>).
   */
  public final static String VIEW_TEXT_FONT = "com.google.dart.tools.ui.views.textfont"; //$NON-NLS-1$

  /**
   * A named preference that tells whether to perform code transforms when formatting.
   */
  public static final String FORMATTER_PERFORM_TRANSFORMS = "com.google.dart.tools.ui.performTransforms";

  /**
   * Decodes an encoded JRE library and returns its class path entries.
   * 
   * @param encodedLibrary the encoded library
   * @return the array of classpath entries of an encoded JRE library.
   * @see #encodeJRELibrary(String, IIncludePathEntry[])
   */
//  public static IIncludePathEntry[] decodeJRELibraryClasspathEntries(
//      String encodedLibrary) {
//    return NewJavaProjectPreferencePage.decodeJRELibraryClasspathEntries(encodedLibrary);
//  }

  /**
   * Decodes an encoded JRE library and returns its description string.
   * 
   * @param encodedLibrary the encoded library
   * @return the description of an encoded JRE library
   * @see #encodeJRELibrary(String, IIncludePathEntry[])
   */
//  public static String decodeJRELibraryDescription(String encodedLibrary) {
//    return NewJavaProjectPreferencePage.decodeJRELibraryDescription(encodedLibrary);
//  }

  /**
   * Encodes a JRE library to be used in the named preference
   * <code>NEWPROJECT_JRELIBRARY_LIST</code>.
   * 
   * @param description a string value describing the JRE library. The description is used to
   *          identify the JDR library in the UI
   * @param entries an array of classpath entries to be encoded
   * @return the encoded string.
   */
//  public static String encodeJRELibrary(String description,
//      IIncludePathEntry[] entries) {
//    return NewJavaProjectPreferencePage.encodeJRELibrary(description, entries);
//  }

  /**
   * Returns the current configuration for the JRE to be used as default in new JavaScript projects.
   * This is a convenience method to access the named preference
   * <code>NEWPROJECT_JRELIBRARY_LIST</code> with the index defined by
   * <code> NEWPROJECT_JRELIBRARY_INDEX</code> .
   * 
   * @return the current default set of class path entries
   * @see #NEWPROJECT_JRELIBRARY_LIST
   * @see #NEWPROJECT_JRELIBRARY_INDEX
   */
//  public static IIncludePathEntry[] getDefaultJRELibrary() {
//    return NewJavaProjectPreferencePage.getDefaultJRELibrary();
//  }

  /**
   * Returns the value for the given key in the given context.
   * 
   * @param key The preference key
   * @param project The current context or <code>null</code> if no context is available and the
   *          workspace setting should be taken. Note that passing <code>null</code> should be
   *          avoided.
   * @return Returns the current value for the string.
   */
  @SuppressWarnings("deprecation")
  public static String getPreference(String key, DartProject project) {
    String val;
    if (project != null) {
      val = new ProjectScope(project.getProject()).getNode(DartUI.ID_PLUGIN).get(key, null);
      if (val != null) {
        return val;
      }
    }
    val = new InstanceScope().getNode(DartUI.ID_PLUGIN).get(key, null);
    if (val != null) {
      return val;
    }
    return new DefaultScope().getNode(DartUI.ID_PLUGIN).get(key, null);
  }

  /**
   * Returns the Dart-UI preference store.
   * 
   * @return the Dart-UI preference store
   */
  public static IPreferenceStore getPreferenceStore() {
    return DartToolsPlugin.getDefault().getPreferenceStore();
  }

  /**
   * Initializes the given preference store with the default values.
   * 
   * @param store the preference store to be initialized
   */
  @SuppressWarnings("deprecation")
  public static void initializeDefaultValues(IPreferenceStore store) {
    ColorRegistry registry = getColorRegistry();
    store.setDefault(PreferenceConstants.EDITOR_SHOW_SEGMENTS, false);

    // JavaBasePreferencePage
    store.setDefault(PreferenceConstants.DOUBLE_CLICK, PreferenceConstants.DOUBLE_CLICK_EXPANDS);
    store.setDefault(
        PreferenceConstants.UPDATE_JAVA_VIEWS,
        PreferenceConstants.UPDATE_WHILE_EDITING);
    store.setToDefault(PreferenceConstants.UPDATE_JAVA_VIEWS); // clear
                                                               // preference,
                                                               // update on save
                                                               // not supported
                                                               // anymore

    store.setDefault(PreferenceConstants.LINK_BROWSING_PROJECTS_TO_EDITOR, true);
    store.setDefault(PreferenceConstants.LINK_BROWSING_PACKAGES_TO_EDITOR, true);
    store.setDefault(PreferenceConstants.LINK_BROWSING_TYPES_TO_EDITOR, true);
    store.setDefault(PreferenceConstants.LINK_BROWSING_MEMBERS_TO_EDITOR, true);

    store.setDefault(PreferenceConstants.SEARCH_USE_REDUCED_MENU, true);

    // AppearancePreferencePage
    store.setDefault(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
    store.setDefault(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE, false);
    store.setDefault(PreferenceConstants.APPEARANCE_METHOD_TYPEPARAMETERS, true);
    store.setDefault(PreferenceConstants.APPEARANCE_CATEGORY, false);
    store.setDefault(PreferenceConstants.BROWSING_STACK_VERTICALLY, false);
    store.setDefault(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW, ""); //$NON-NLS-1$

    // ImportOrganizePreferencePage
    store.setDefault(PreferenceConstants.ORGIMPORTS_IMPORTORDER, "java;javax;org;com"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD, 99);
    store.setDefault(PreferenceConstants.ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD, 99);
    store.setDefault(PreferenceConstants.ORGIMPORTS_IGNORELOWERCASE, true);

    // TypeFilterPreferencePage
    store.setDefault(PreferenceConstants.TYPEFILTER_ENABLED, ""); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.TYPEFILTER_DISABLED, ""); //$NON-NLS-1$

    // ClasspathVariablesPreferencePage
    // CodeFormatterPreferencePage
    // CompilerPreferencePage
    // no initialization needed

    // RefactoringPreferencePage
    store.setDefault(
        PreferenceConstants.REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD,
        PreferenceConstants.REFACTOR_WARNING_SEVERITY);
    store.setDefault(PreferenceConstants.REFACTOR_SAVE_ALL_EDITORS, false);
    store.setDefault(PreferenceConstants.REFACTOR_LIGHTWEIGHT, true);

    // TemplatePreferencePage
    store.setDefault(PreferenceConstants.TEMPLATES_USE_CODEFORMATTER, true);

    // CodeGenerationPreferencePage
    // compatibility code
    if (store.getBoolean(PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX)) {
      String prefix = store.getString(PreferenceConstants.CODEGEN_GETTERSETTER_PREFIX);
      if (prefix.length() > 0) {
        DartCore.getPlugin().getPluginPreferences().setValue(
            JavaScriptCore.CODEASSIST_FIELD_PREFIXES,
            prefix);
        store.setToDefault(PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX);
        store.setToDefault(PreferenceConstants.CODEGEN_GETTERSETTER_PREFIX);
      }
    }
    if (store.getBoolean(PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX)) {
      String suffix = store.getString(PreferenceConstants.CODEGEN_GETTERSETTER_SUFFIX);
      if (suffix.length() > 0) {
        DartCore.getPlugin().getPluginPreferences().setValue(
            JavaScriptCore.CODEASSIST_FIELD_SUFFIXES,
            suffix);
        store.setToDefault(PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX);
        store.setToDefault(PreferenceConstants.CODEGEN_GETTERSETTER_SUFFIX);
      }
    }
    store.setDefault(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);
    store.setDefault(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, true);
    store.setDefault(PreferenceConstants.CODEGEN_EXCEPTION_VAR_NAME, "e"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
    store.setDefault(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, true);

    // MembersOrderPreferencePage
    store.setDefault(PreferenceConstants.APPEARANCE_MEMBER_SORT_ORDER, "T,SF,SI,SM,F,I,C,M"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.APPEARANCE_VISIBILITY_SORT_ORDER, "B,V,R,D"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, false);

    // JavaEditorPreferencePage
    store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS, true);
    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR,
        findRGB(
            registry,
            IJavaThemeConstants.EDITOR_MATCHING_BRACKETS_COLOR,
            new RGB(192, 192, 192)));

    store.setDefault(PreferenceConstants.EDITOR_CORRECTION_INDICATION, true);
    store.setDefault(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE, true);

    store.setDefault(PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS, true);

    PreferenceConverter.setDefault(
        store,
        PreferenceConstants.EDITOR_LINKED_POSITION_COLOR,
        new RGB(121, 121, 121));

    store.setDefault(
        AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH,
        EDITOR_DEFAULT_TAB_WIDTH);
    store.setDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, true);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_MULTI_LINE_COMMENT_COLOR, new RGB(63, 127, 95)));

    store.setDefault(PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR,
        findRGB(
            registry,
            IJavaThemeConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR,
            new RGB(63, 127, 95)));

    store.setDefault(PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_DART_KEYWORD_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_JAVA_KEYWORD_COLOR, new RGB(127, 0, 85)));
    store.setDefault(PreferenceConstants.EDITOR_JAVA_KEYWORD_BOLD, true);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_KEYWORD_ITALIC, false);

    PreferenceConverter.setDefault(
        store,
        PreferenceConstants.EDITOR_JAVA_ANNOTATION_COLOR,
        new RGB(100, 100, 100));
    store.setDefault(PreferenceConstants.EDITOR_JAVA_ANNOTATION_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_ANNOTATION_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_STRING_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_STRING_COLOR, new RGB(42, 0, 255)));

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_MULTI_LINE_STRING_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_MULTI_LINE_STRING_COLOR, new RGB(42, 0, 255)));

    store.setDefault(PreferenceConstants.EDITOR_STRING_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_STRING_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_DART_DEFAULT_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_JAVA_DEFAULT_COLOR, new RGB(0, 0, 0)));
    store.setDefault(PreferenceConstants.EDITOR_JAVA_DEFAULT_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_DEFAULT_ITALIC, false);

    setDefaultAndFireEvent(store, PreferenceConstants.EDITOR_JAVA_METHOD_NAME_COLOR, new RGB(
        0,
        0,
        0));
    store.setDefault(PreferenceConstants.EDITOR_JAVA_METHOD_NAME_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_METHOD_NAME_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_DART_KEYWORD_RETURN_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_JAVA_KEYWORD_RETURN_COLOR, new RGB(127, 0, 85)));
    store.setDefault(PreferenceConstants.EDITOR_JAVA_KEYWORD_RETURN_BOLD, true);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_KEYWORD_RETURN_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_DART_OPERATOR_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_JAVA_OPERATOR_COLOR, new RGB(0, 0, 0)));
    store.setDefault(PreferenceConstants.EDITOR_JAVA_OPERATOR_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_OPERATOR_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_DART_BRACKET_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_JAVA_BRACKET_COLOR, new RGB(0, 0, 0)));
    store.setDefault(PreferenceConstants.EDITOR_JAVA_BRACKET_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVA_BRACKET_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_TASK_TAG_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_TASK_TAG_COLOR, new RGB(127, 159, 191)));
    store.setDefault(PreferenceConstants.EDITOR_TASK_TAG_BOLD, true);
    store.setDefault(PreferenceConstants.EDITOR_TASK_TAG_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_DARTDOC_KEYWORD_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_JAVADOC_KEYWORD_COLOR, new RGB(127, 159, 191)));
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_KEYWORD_BOLD, true);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_KEYWORD_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_DARTDOC_TAG_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_JAVADOC_TAG_COLOR, new RGB(127, 127, 159)));
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_TAG_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_TAG_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_DARTDOC_LINKS_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_JAVADOC_LINKS_COLOR, new RGB(63, 63, 191)));
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_LINKS_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_LINKS_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.EDITOR_DARTDOC_DEFAULT_COLOR,
        findRGB(registry, IJavaThemeConstants.EDITOR_JAVADOC_DEFAULT_COLOR, new RGB(63, 95, 191)));
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_DEFAULT_BOLD, false);
    store.setDefault(PreferenceConstants.EDITOR_JAVADOC_DEFAULT_ITALIC, false);

    store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION, true);
    store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY, 80); // was 300

    store.setDefault(PreferenceConstants.CODEASSIST_AUTOINSERT, false);
    setDefaultAndFireEvent(
        store,
        PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND,
        findRGB(registry, IJavaThemeConstants.CODEASSIST_PROPOSALS_BACKGROUND, new RGB(
            255,
            255,
            255)));
    setDefaultAndFireEvent(
        store,
        PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND,
        findRGB(registry, IJavaThemeConstants.CODEASSIST_PROPOSALS_FOREGROUND, new RGB(0, 0, 0)));
    setDefaultAndFireEvent(
        store,
        PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND,
        findRGB(registry, IJavaThemeConstants.CODEASSIST_PARAMETERS_BACKGROUND, new RGB(
            255,
            255,
            255)));
    setDefaultAndFireEvent(
        store,
        PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND,
        findRGB(registry, IJavaThemeConstants.CODEASSIST_PARAMETERS_FOREGROUND, new RGB(0, 0, 0)));
    setDefaultAndFireEvent(
        store,
        PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND,
        findRGB(registry, IJavaThemeConstants.CODEASSIST_REPLACEMENT_BACKGROUND, new RGB(
            255,
            255,
            0)));
    setDefaultAndFireEvent(
        store,
        PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND,
        findRGB(registry, IJavaThemeConstants.CODEASSIST_REPLACEMENT_FOREGROUND, new RGB(255, 0, 0)));
    store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, "."); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, "@#"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, true);
    store.setDefault(PreferenceConstants.CODEASSIST_CASE_SENSITIVITY, false);
    store.setDefault(PreferenceConstants.CODEASSIST_ADDIMPORT, false);
    store.setDefault(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, true);
    store.setDefault(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
    store.setDefault(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);
    store.setDefault(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, false);
    // can't believe embedded null is used as a delimiter
    store.setDefault(
        PreferenceConstants.CODEASSIST_EXCLUDED_CATEGORIES,
        "com.google.dart.tools.ui.spellingProposalCategory\0com.google.dart.tools.ui.textProposalCategory\0"); //$NON-NLS-1$
    store.setDefault(
        PreferenceConstants.CODEASSIST_CATEGORY_ORDER,
        "com.google.dart.tools.ui.spellingProposalCategory:65545\0com.google.dart.tools.ui.javaTypeProposalCategory:65540\0com.google.dart.tools.ui.javaNoTypeProposalCategory:65539\0com.google.dart.tools.ui.textProposalCategory:65541\0com.google.dart.tools.ui.templateProposalCategory:2\0"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_LRU_HISTORY, ""); //$NON-NLS-1$
    store.setDefault(
        PreferenceConstants.CODEASSIST_SORTER,
        "com.google.dart.tools.ui.RelevanceSorter"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, ""); //$NON-NLS-1$

    store.setDefault(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION, true);
    store.setDefault(PreferenceConstants.EDITOR_SMART_PASTE, true);
    store.setDefault(PreferenceConstants.EDITOR_IMPORTS_ON_PASTE, true);
    store.setDefault(PreferenceConstants.EDITOR_CLOSE_STRINGS, true);
    store.setDefault(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);
    store.setDefault(PreferenceConstants.EDITOR_CLOSE_BRACES, true);
    store.setDefault(PreferenceConstants.EDITOR_CLOSE_JAVADOCS, true);
    store.setDefault(PreferenceConstants.EDITOR_WRAP_STRINGS, true);
    store.setDefault(PreferenceConstants.EDITOR_ESCAPE_STRINGS, false);
    store.setDefault(PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS, true);
    store.setDefault(PreferenceConstants.EDITOR_FORMAT_JAVADOCS, false);

    store.setDefault(PreferenceConstants.EDITOR_REMOVE_TRAILING_WS, false);

    int sourceHoverModifier = SWT.MOD2;
    String sourceHoverModifierName = Action.findModifierString(sourceHoverModifier); // Shift
    int nlsHoverModifier = SWT.MOD1 + SWT.MOD3;
    String nlsHoverModifierName = Action.findModifierString(SWT.MOD1)
        + "+" + Action.findModifierString(SWT.MOD3); // Ctrl + Alt //$NON-NLS-1$
    store.setDefault(
        PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIERS,
        "com.google.dart.tools.ui.BestMatchHover;0;com.google.dart.tools.ui.JavaSourceHover;" + sourceHoverModifierName + ";com.google.dart.tools.ui.NLSStringHover;" + nlsHoverModifierName); //$NON-NLS-1$ //$NON-NLS-2$
    store.setDefault(
        PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIER_MASKS,
        "com.google.dart.tools.ui.BestMatchHover;0;com.google.dart.tools.ui.JavaSourceHover;" + sourceHoverModifier + ";com.google.dart.tools.ui.NLSStringHover;" + nlsHoverModifier); //$NON-NLS-1$ //$NON-NLS-2$

    store.setDefault(PreferenceConstants.EDITOR_SMART_TAB, true);
    store.setDefault(PreferenceConstants.EDITOR_SMART_BACKSPACE, true);
    store.setDefault(PreferenceConstants.EDITOR_ANNOTATION_ROLL_OVER, false);

    store.setDefault(EDITOR_SOURCE_HOVER_BACKGROUND_COLOR_SYSTEM_DEFAULT, true);

    DartX.todo();
//    store.setDefault(PreferenceConstants.FORMATTER_PROFILE,
//        FormatterProfileManager.DEFAULT_PROFILE);

    // mark occurrences
    boolean disableMarks = DartCoreDebug.DISABLE_MARK_OCCURRENCES;
    store.setDefault(PreferenceConstants.EDITOR_MARK_OCCURRENCES, !disableMarks);
    store.setDefault(PreferenceConstants.EDITOR_STICKY_OCCURRENCES, false);
    store.setDefault(PreferenceConstants.EDITOR_MARK_TYPE_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_METHOD_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_CONSTANT_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_FIELD_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_LOCAL_VARIABLE_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_EXCEPTION_OCCURRENCES, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_METHOD_EXIT_POINTS, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_BREAK_CONTINUE_TARGETS, true);
    store.setDefault(PreferenceConstants.EDITOR_MARK_IMPLEMENTORS, true);

    // folding
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_ENABLED, true);
    store.setDefault(
        PreferenceConstants.EDITOR_FOLDING_PROVIDER,
        "com.google.dart.tools.ui.text.defaultFoldingProvider"); //$NON-NLS-1$
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_JAVADOC, false);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_INNERTYPES, false);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_METHODS, false);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_IMPORTS, false);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_HEADERS, true);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_CLASSES, false);
    store.setDefault(PreferenceConstants.EDITOR_FOLDING_FUNCTIONS, false);

    // properties file editor
    setDefaultAndFireEvent(
        store,
        PreferenceConstants.PROPERTIES_FILE_COLORING_KEY,
        findRGB(registry, IJavaThemeConstants.PROPERTIES_FILE_COLORING_KEY, new RGB(0, 0, 0)));
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_KEY_BOLD, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_KEY_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE,
        findRGB(registry, IJavaThemeConstants.PROPERTIES_FILE_COLORING_VALUE, new RGB(42, 0, 255)));
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE_BOLD, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT,
        findRGB(registry, IJavaThemeConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT, new RGB(0, 0, 0)));
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT_BOLD, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT,
        findRGB(
            registry,
            IJavaThemeConstants.PROPERTIES_FILE_COLORING_ARGUMENT,
            new RGB(127, 0, 85)));
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT_BOLD, true);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT_ITALIC, false);

    setDefaultAndFireEvent(
        store,
        PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT,
        findRGB(
            registry,
            IJavaThemeConstants.PROPERTIES_FILE_COLORING_COMMENT,
            new RGB(63, 127, 95)));
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT_BOLD, false);
    store.setDefault(PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT_ITALIC, false);

    store.setDefault(PreferenceConstants.PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS_MEMBERS, ""); //$NON-NLS-1$

    // semantic highlighting
    SemanticHighlightings.initDefaults(store);

    // do more complicated stuff
    DartX.todo();
//    NewJavaProjectPreferencePage.initDefaults(store);

    // reset preferences that are not settable by editor any longer
    // see AbstractDecoratedTextEditorPreferenceConstants
    store.setToDefault(EDITOR_SMART_HOME_END); // global
    store.setToDefault(EDITOR_LINE_NUMBER_RULER); // global
    store.setToDefault(EDITOR_LINE_NUMBER_RULER_COLOR); // global
    store.setToDefault(EDITOR_OVERVIEW_RULER); // removed -> true
    store.setToDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS); // accessibility

    store.setToDefault(PreferenceConstants.EDITOR_CURRENT_LINE); // global
    store.setToDefault(PreferenceConstants.EDITOR_CURRENT_LINE_COLOR); // global

    store.setToDefault(PreferenceConstants.EDITOR_PRINT_MARGIN); // global
    store.setToDefault(PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN); // global
    store.setToDefault(PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR); // global

    store.setToDefault(PreferenceConstants.EDITOR_FOREGROUND_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_BACKGROUND_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR); // global
    store.setToDefault(PreferenceConstants.EDITOR_FIND_SCOPE_COLOR); // global
    store.setToDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR); // global
    store.setToDefault(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR); // global

    store.setToDefault(PreferenceConstants.EDITOR_DISABLE_OVERWRITE_MODE); // global

    store.setToDefault(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED); // removed

    store.setToDefault(PreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE); // global

    // Code Clean Up
    DartX.todo();
  }

  /**
   * Returns the RGB for the given key in the given color registry.
   * 
   * @param registry the color registry
   * @param key the key for the constant in the registry
   * @param defaultRGB the default RGB if no entry is found
   * @return RGB the RGB
   */
  private static RGB findRGB(ColorRegistry registry, String key, RGB defaultRGB) {
    if (registry == null) {
      return defaultRGB;
    }
    RGB rgb = registry.getRGB(key);
    if (rgb != null) {
      return rgb;
    }
    return defaultRGB;
  }

  /**
   * Return the system color registry, or <code>null</code> if the registry cannot be accessed for
   * some reason (such as the workbench not existing when running headless tests).
   * 
   * @return the system color registry
   */
  private static ColorRegistry getColorRegistry() {
    try {
      return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
    } catch (Exception exception) {
      DartToolsPlugin.log("INFO: Could not access the workbench", exception);
      return null;
    }
  }

  /**
   * Sets the default value and fires a property change event if necessary.
   * 
   * @param store the preference store
   * @param key the preference key
   * @param newValue the new value
   */
  private static void setDefaultAndFireEvent(IPreferenceStore store, String key, RGB newValue) {
    try {
      RGB oldValue = null;
      if (store.isDefault(key)) {
        oldValue = PreferenceConverter.getDefaultColor(store, key);
      }

      PreferenceConverter.setDefault(store, key, newValue);

      if (oldValue != null && !oldValue.equals(newValue)) {
        store.firePropertyChangeEvent(key, oldValue, newValue);
      }
    } catch (Throwable exception) {
      DartToolsPlugin.log("INFO: Could not get the default value for the color preference named "
          + key, exception);
    }
  }

  private PreferenceConstants() {
  }

}
