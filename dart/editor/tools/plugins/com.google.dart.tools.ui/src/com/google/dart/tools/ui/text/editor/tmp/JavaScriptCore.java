/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.text.editor.tmp;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartPreferenceConstants;

import org.eclipse.core.runtime.Plugin;

public class JavaScriptCore extends DartCore {

  /**
   * The identifier for the JavaScript validator (value
   * <code>"org.eclipse.wst.jsdt.core.javascriptValidator"</code>).
   */
  public static final String BUILDER_ID = PLUGIN_ID + ".dartValidator"; //$NON-NLS-1$
  public static final String VERSION_1_5 = "1.5"; //$NON-NLS-1$
  /**
   * Possible configurable option ID.
   * 
   * @see #getDefaultOptions()
   */
  public static final String COMPILER_TASK_TAGS = PLUGIN_ID + ".compiler.taskTags"; //$NON-NLS-1$
  /**
   * Default task tag
   */
  public static final String DEFAULT_TASK_TAGS = "TODO,FIXME,XXX"; //$NON-NLS-1$
  /**
   * Value of the content-type for JavaScript source files. Use this value to retrieve the
   * JavaScript content type from the content type manager, and to add new JavaScript-like
   * extensions to this content type.
   * 
   * @see org.eclipse.core.runtime.content.IContentTypeManager#getContentType(String)
   * @see #getJavaScriptLikeExtensions()
   */
  public static final String JAVA_SOURCE_CONTENT_TYPE = JavaScriptCore.PLUGIN_ID + ".jsSource"; //$NON-NLS-1$
  /**
   * Possible configurable option ID.
   * 
   * @see #getDefaultOptions()
   */
  public static final String COMPILER_TASK_PRIORITIES = PLUGIN_ID + ".compiler.taskPriorities"; //$NON-NLS-1$
  /**
   * Default task priority
   */
  public static final String DEFAULT_TASK_PRIORITIES = "NORMAL,HIGH,NORMAL"; //$NON-NLS-1$
  /**
   * Possible configurable option ID.
   * 
   * @see #getDefaultOptions()
   */
  public static final String COMPILER_TASK_CASE_SENSITIVE = PLUGIN_ID
      + ".compiler.taskCaseSensitive"; //$NON-NLS-1$
  /**
   * Possible configurable option value.
   * 
   * @see #getDefaultOptions()
   */
  public static final String CODEASSIST_CAMEL_CASE_MATCH = DartPreferenceConstants.CODEASSIST_CAMEL_CASE_MATCH;

  public static final String CODEASSIST_FIELD_PREFIXES = DartPreferenceConstants.CODEASSIST_FIELD_PREFIXES;

  public static final String CODEASSIST_FIELD_SUFFIXES = DartPreferenceConstants.CODEASSIST_FIELD_SUFFIXES;

  public static final String CODEASSIST_VISIBILITY_CHECK = DartPreferenceConstants.CODEASSIST_VISIBILITY_CHECK;

  public static final String COMPILER_COMPLIANCE = null;

  public static final String COMPILER_DOC_COMMENT_SUPPORT = null;

  public static final String COMPILER_PB_INVALID_IMPORT = null;

  public static final String COMPILER_PB_UNREACHABLE_CODE = null;

  public static final String COMPILER_SOURCE = PLUGIN_ID + ".compiler.source"; //$NON-NLS-1$

  public static final String CORE_ENCODING = null;

  public static final String COMPUTE = "compute"; //$NON-NLS-1$

  public static final String DO_NOT_INSERT = "do not insert"; //$NON-NLS-1$

  public static final String ENABLED = DartPreferenceConstants.ENABLED;

  public static final String ERROR = "error"; //$NON-NLS-1$

  public static final String INSERT = "insert"; //$NON-NLS-1$

  public static final String SPACE = "space"; //$NON-NLS-1$

  public static final String TAB = "tab"; //$NON-NLS-1$

  public static final String VERSION_1_4 = "1.4"; //$NON-NLS-1$

  public static final String NATURE_ID = DartCore.DART_PROJECT_NATURE;

  public static Plugin getJavaScriptCore() {

    return DartCore.getPlugin();

  }

}
