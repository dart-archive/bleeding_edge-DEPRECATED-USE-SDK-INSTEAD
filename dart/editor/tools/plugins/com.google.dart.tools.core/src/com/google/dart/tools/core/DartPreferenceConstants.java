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
package com.google.dart.tools.core;

/**
 * The interface <code>DartPreferenceConstants</code> defines constants used to access the
 * preferences defined by {@link DartCore}, specifically those accessed using the methods
 * {@link DartCore#getOptions()}, {@link DartCore#getOption(String)},
 * {@link DartCore#getDefaultOptions()} and {@link DartCore#setOptions(java.util.Hashtable)}.
 */
public interface DartPreferenceConstants {
  /**
   * Code assist option ID: Define the Prefixes for Argument Name.
   * <p>
   * When the prefixes is non empty, completion for argument name will begin with one of the
   * proposed prefixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd><code>"com.google.dart.tools.core.codeComplete.argumentPrefixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_ARGUMENT_PREFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.argumentPrefixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Define the Suffixes for Argument Name.
   * <p>
   * When the suffixes is non empty, completion for argument name will end with one of the proposed
   * suffixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd><code>"com.google.dart.tools.core.codeComplete.argumentSuffixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code> where <code>&lt;suffix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_ARGUMENT_SUFFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.argumentSuffixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Activate Camel Case Sensitive Completion.
   * <p>
   * When enabled, completion shows proposals whose name match the CamelCase pattern.
   * <dl>
   * <dt>Option id:</dt>
   * <dd><code>"com.google.dart.tools.core.codeComplete.camelCaseMatch"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "enabled", "disabled" }</code></dd>
   * <dt>Default:</dt>
   * <dd><code>"enabled"</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_CAMEL_CASE_MATCH = DartCore.PLUGIN_ID
      + ".codeComplete.camelCaseMatch"; //$NON-NLS-1$

  /**
   * Code assist option ID: Activate Deprecation Sensitive Completion.
   * <p>
   * When enabled, completion doesn't propose deprecated members and types.
   * <dl>
   * <dt>Option id:</dt>
   * <dd><code>"com.google.dart.tools.core.codeComplete.deprecationCheck"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "enabled", "disabled" }</code></dd>
   * <dt>Default:</dt>
   * <dd><code>"disabled"</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  // TODO Does Dart support the notion of deprecated code?
  public static final String CODEASSIST_DEPRECATION_CHECK = DartCore.PLUGIN_ID
      + ".codeComplete.deprecationCheck"; //$NON-NLS-1$

  /**
   * Code assist option ID: Activate Discouraged Reference Sensitive Completion.
   * <p>
   * When enabled, completion doesn't propose elements which match a discouraged reference rule.
   * <dl>
   * <dt>Option id:</dt>
   * <dd>
   * <code>"com.google.dart.tools.core.codeComplete.discouragedReferenceCheck"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "enabled", "disabled" }</code></dd>
   * <dt>Default:</dt>
   * <dd><code>"disabled"</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_DISCOURAGED_REFERENCE_CHECK = DartCore.PLUGIN_ID
      + ".codeComplete.discouragedReferenceCheck"; //$NON-NLS-1$

  /**
   * Code assist option ID: Define the Prefixes for Field Name.
   * <p>
   * When the prefixes is non empty, completion for field name will begin with one of the proposed
   * prefixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd><code>"com.google.dart.tools.core.codeComplete.fieldPrefixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_FIELD_PREFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.fieldPrefixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Define the Suffixes for Field Name.
   * <p>
   * When the suffixes is non empty, completion for field name will end with one of the proposed
   * suffixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd><code>"com.google.dart.tools.core.codeComplete.fieldSuffixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code> where <code>&lt;suffix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_FIELD_SUFFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.fieldSuffixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Activate Forbidden Reference Sensitive Completion.
   * <p>
   * When enabled, completion doesn't propose elements which match a forbidden reference rule.
   * <dl>
   * <dt>Option id:</dt>
   * <dd>
   * <code>"com.google.dart.tools.core.codeComplete.forbiddenReferenceCheck"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "enabled", "disabled" }</code></dd>
   * <dt>Default:</dt>
   * <dd><code>"enabled"</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_FORBIDDEN_REFERENCE_CHECK = DartCore.PLUGIN_ID
      + ".codeComplete.forbiddenReferenceCheck"; //$NON-NLS-1$

  /**
   * Code assist option ID: Automatic Qualification of Implicit Members.
   * <p>
   * When active, completion automatically qualifies completion on implicit field references and
   * message expressions.
   * <dl>
   * <dt>Option id:</dt>
   * <dd>
   * <code>"com.google.dart.tools.core.codeComplete.forceImplicitQualification"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "enabled", "disabled" }</code></dd>
   * <dt>Default:</dt>
   * <dd><code>"disabled"</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_IMPLICIT_QUALIFICATION = DartCore.PLUGIN_ID
      + ".codeComplete.forceImplicitQualification"; //$NON-NLS-1$

  /**
   * Code assist option ID: Define the Prefixes for Local Variable Name.
   * <p>
   * When the prefixes is non empty, completion for local variable name will begin with one of the
   * proposed prefixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd><code>"com.google.dart.tools.core.codeComplete.localPrefixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_LOCAL_PREFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.localPrefixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Define the Suffixes for Local Variable Name.
   * <p>
   * When the suffixes is non empty, completion for local variable name will end with one of the
   * proposed suffixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd><code>"com.google.dart.tools.core.codeComplete.localSuffixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code> where <code>&lt;suffix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_LOCAL_SUFFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.localSuffixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Define the Prefixes for Static Field Name.
   * <p>
   * When the prefixes is non empty, completion for static field name will begin with one of the
   * proposed prefixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd>
   * <code>"com.google.dart.tools.core.codeComplete.staticFieldPrefixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_STATIC_FIELD_PREFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.staticFieldPrefixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Define the Suffixes for Static Field Name.
   * <p>
   * When the suffixes is non empty, completion for static field name will end with one of the
   * proposed suffixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd>
   * <code>"com.google.dart.tools.core.codeComplete.staticFieldSuffixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code>< where <code>&lt;suffix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_STATIC_FIELD_SUFFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.staticFieldSuffixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Define the Prefixes for Static Final Field Name.
   * <p>
   * When the prefixes is non empty, completion for static final field name will begin with one of
   * the proposed prefixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd>
   * <code>"com.google.dart.tools.core.codeComplete.staticFinalFieldPrefixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;prefix&gt;[,&lt;prefix&gt;]*" }</code> where <code>&lt;prefix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_STATIC_FINAL_FIELD_PREFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.staticFinalFieldPrefixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Define the Suffixes for Static Final Field Name.
   * <p>
   * When the suffixes is non empty, completion for static final field name will end with one of the
   * proposed suffixes.
   * <dl>
   * <dt>Option id:</dt>
   * <dd>
   * <code>"com.google.dart.tools.core.codeComplete.staticFinalFieldSuffixes"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "&lt;suffix&gt;[,&lt;suffix&gt;]*" }</code>< where <code>&lt;suffix&gt;</code> is a
   * String without any wild-card</dd>
   * <dt>Default:</dt>
   * <dd><code>""</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_STATIC_FINAL_FIELD_SUFFIXES = DartCore.PLUGIN_ID
      + ".codeComplete.staticFinalFieldSuffixes"; //$NON-NLS-1$

  /**
   * Code assist option ID: Activate Suggestion of Static Import.
   * <p>
   * When enabled, completion proposals can contain static import pattern.
   * <dl>
   * <dt>Option id:</dt>
   * <dd>
   * <code>"com.google.dart.tools.core.codeComplete.suggestStaticImports"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "enabled", "disabled" }</code></dd>
   * <dt>Default:</dt>
   * <dd><code>"enabled"</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  @Deprecated
  public static final String CODEASSIST_SUGGEST_STATIC_IMPORTS = DartCore.PLUGIN_ID
      + ".codeComplete.suggestStaticImports"; //$NON-NLS-1$

  /**
   * Code assist option ID: Activate Visibility Sensitive Completion.
   * <p>
   * When active, completion doesn't show that you can not see (for example, you can not see private
   * methods of a super class).
   * <dl>
   * <dt>Option id:</dt>
   * <dd><code>"com.google.dart.tools.core.codeComplete.visibilityCheck"</code></dd>
   * <dt>Possible values:</dt>
   * <dd><code>{ "enabled", "disabled" }</code></dd>
   * <dt>Default:</dt>
   * <dd><code>"disabled"</code></dd>
   * </dl>
   * 
   * @category CodeAssistOptionID
   */
  public static final String CODEASSIST_VISIBILITY_CHECK = DartCore.PLUGIN_ID
      + ".codeComplete.visibilityCheck"; //$NON-NLS-1$

  /**
   * Configurable option value: {@value} .
   * 
   * @category OptionValue
   */
  public static final String DISABLED = "disabled"; //$NON-NLS-1$

  /**
   * Configurable option value: {@value} .
   * 
   * @category OptionValue
   */
  public static final String DO_NOT_INSERT = "do not insert"; //$NON-NLS-1$

  /**
   * Configurable option value: {@value} .
   * 
   * @category OptionValue
   */
  public static final String ENABLED = "enabled"; //$NON-NLS-1$

  /**
   * Configurable option value: {@value} .
   * 
   * @category OptionValue
   */
  public static final String INSERT = "insert"; //$NON-NLS-1$

  /**
   * Configurable option value: {@value} .
   * 
   * @category OptionValue
   */
  public static final String TAB = "tab"; //$NON-NLS-1$

  /**
   * Configurable option value: {@value} .
   * 
   * @category OptionValue
   */
  public static final String SPACE = "space"; //$NON-NLS-1$
}
