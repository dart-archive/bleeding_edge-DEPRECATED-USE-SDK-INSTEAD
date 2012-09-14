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

import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Defines the definition IDs for the Dart editor actions.
 * <p>
 * This interface is not intended to be implemented or extended.
 * </p>
 * . Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface DartEditorActionDefinitionIds extends ITextEditorActionDefinitionIds {

  // edit

  /**
   * Action definition ID of the edit -> smart typing action (value
   * <code>"com.google.dart.tools.smartTyping.toggle"</code>).
   */
  public static final String TOGGLE_SMART_TYPING = "com.google.dart.tools.smartTyping.toggle"; //$NON-NLS-1$

  /**
   * Action definition ID of the edit -> go to matching bracket action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.goto.matching.bracket"</code> ).
   */
  public static final String GOTO_MATCHING_BRACKET = "com.google.dart.tools.ui.edit.text.dart.goto.matching.bracket"; //$NON-NLS-1$

  /**
   * Action definition ID of the edit -> go to next member action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.goto.next.member"</code>).
   */
  public static final String GOTO_NEXT_MEMBER = "com.google.dart.tools.ui.edit.text.dart.goto.next.member"; //$NON-NLS-1$

  /**
   * Action definition ID of the edit -> go to previous member action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.goto.previous.member"</code> ).
   */
  public static final String GOTO_PREVIOUS_MEMBER = "com.google.dart.tools.ui.edit.text.dart.goto.previous.member"; //$NON-NLS-1$

  /**
   * Action definition ID of the edit -> select enclosing action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.select.enclosing"</code>).
   */
  public static final String SELECT_ENCLOSING = "com.google.dart.tools.ui.edit.text.dart.select.enclosing"; //$NON-NLS-1$

  /**
   * Action definition ID of the edit -> select next action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.select.next"</code>).
   */
  public static final String SELECT_NEXT = "com.google.dart.tools.ui.edit.text.dart.select.next"; //$NON-NLS-1$

  /**
   * Action definition ID of the edit -> select previous action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.select.previous"</code>).
   */
  public static final String SELECT_PREVIOUS = "com.google.dart.tools.ui.edit.text.dart.select.previous"; //$NON-NLS-1$

  /**
   * Action definition ID of the edit -> select restore last action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.select.last"</code>).
   */
  public static final String SELECT_LAST = "com.google.dart.tools.ui.edit.text.dart.select.last"; //$NON-NLS-1$

  /**
   * Action definition ID of the edit -> content assist complete prefix action (value:
   * <code>"com.google.dart.tools.ui.edit.text.dart.complete.prefix"</code>).
   */
  public static final String CONTENT_ASSIST_COMPLETE_PREFIX = "com.google.dart.tools.ui.edit.text.dart.complete.prefix"; //$NON-NLS-1$

  /**
   * Action definition ID of the navigate -> Show Outline action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.show.outline"</code>).
   */
  public static final String SHOW_OUTLINE = "com.google.dart.tools.ui.edit.text.dart.show.outline"; //$NON-NLS-1$

  /**
   * Action definition ID of the navigate -> Show Hierarchy action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.open.hierarchy"</code>).
   */
  public static final String OPEN_HIERARCHY = "com.google.dart.tools.ui.edit.text.dart.open.hierarchy"; //$NON-NLS-1$

  /**
   * Action definition ID of the Navigate -> Open Structure action (value
   * <code>"com.google.dart.tools.ui.navigate.dart.open.structure"</code>).
   */
  public static final String OPEN_STRUCTURE = "com.google.dart.tools.ui.navigate.dart.open.structure"; //$NON-NLS-1$

  // source

  /**
   * Action definition ID of the source -> comment action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.comment"</code>).
   */
  public static final String COMMENT = "com.google.dart.tools.ui.edit.text.dart.comment"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> uncomment action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.uncomment"</code>).
   */
  public static final String UNCOMMENT = "com.google.dart.tools.ui.edit.text.dart.uncomment"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> toggle comment action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.toggle.comment"</code>).
   */
  public static final String TOGGLE_COMMENT = "com.google.dart.tools.ui.edit.text.dart.toggle.comment"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> add block comment action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.add.block.comment"</code>).
   */
  public static final String ADD_BLOCK_COMMENT = "com.google.dart.tools.ui.edit.text.dart.add.block.comment"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> remove block comment action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.remove.block.comment"</code> ).
   */
  public static final String REMOVE_BLOCK_COMMENT = "com.google.dart.tools.ui.edit.text.dart.remove.block.comment"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> indent action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.indent"</code>).
   */
  public static final String INDENT = "com.google.dart.tools.ui.edit.text.dart.indent"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> format action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.format"</code>).
   */
  public static final String FORMAT = "com.google.dart.tools.ui.edit.text.dart.format"; //$NON-NLS-1$

  /**
   * Action definition id of the Dart quick format action (value:
   * <code>"com.google.dart.tools.ui.edit.text.dart.quick.format"</code>).
   */
  public static final String QUICK_FORMAT = "com.google.dart.tools.ui.edit.text.dart.quick.format"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> add import action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.add.import"</code>).
   */
//  public static final String ADD_IMPORT = "com.google.dart.tools.ui.edit.text.dart.add.import"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> organize imports action (value
   * <code>"com.google.dart.tools.ui.edit.text.organize.imports"</code>).
   */
  public static final String ORGANIZE_IMPORTS = "com.google.dart.tools.ui.edit.text.organize.imports"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> sort order action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.sort.members"</code>).
   */
  public static final String SORT_MEMBERS = "com.google.dart.tools.ui.edit.text.dart.sort.members"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> add dartdoc comment action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.add.dartdoc.comment"</code> ).
   */
  public static final String ADD_JAVADOC_COMMENT = "com.google.dart.tools.ui.edit.text.dart.add.dartdoc.comment"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> surround with try/catch action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.surround.with.try.catch"</code> ).
   */
  public static final String SURROUND_WITH_TRY_CATCH = "com.google.dart.tools.ui.edit.text.dart.surround.with.try.catch"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> override methods action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.override.methods"</code>).
   */
  public static final String OVERRIDE_METHODS = "com.google.dart.tools.ui.edit.text.dart.override.methods"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> add unimplemented constructors action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.add.unimplemented.constructors"</code> ).
   */
  public static final String ADD_UNIMPLEMENTED_CONTRUCTORS = "com.google.dart.tools.ui.edit.text.dart.add.unimplemented.constructors"; //$NON-NLS-1$

  /**
   * Action definition ID of the source ->generate constructor using fields action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.generate.constructor.using.fields"</code> ).
   */
  public static final String GENERATE_CONSTRUCTOR_USING_FIELDS = "com.google.dart.tools.ui.edit.text.dart.generate.constructor.using.fields"; //$NON-NLS-1$

  /**
   * Action definition ID of the source ->generate hashcode() and equals() action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.generate.hashcode.equals"</code> ).
   */
  public static final String GENERATE_HASHCODE_EQUALS = "com.google.dart.tools.ui.edit.text.dart.generate.hashcode.equals"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> generate setter/getter action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.create.getter.setter"</code> ).
   */
  public static final String CREATE_GETTER_SETTER = "com.google.dart.tools.ui.edit.text.dart.create.getter.setter"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> generate delegates action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.create.delegate.methods"</code> ).
   */
  public static final String CREATE_DELEGATE_METHODS = "com.google.dart.tools.ui.edit.text.dart.create.delegate.methods"; //$NON-NLS-1$

  /**
   * Action definition ID of the source -> externalize strings action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.externalize.strings"</code> ).
   */
  public static final String EXTERNALIZE_STRINGS = "com.google.dart.tools.ui.edit.text.dart.externalize.strings"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> pull up action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.pull.up"</code>).
   */
  public static final String PULL_UP = "com.google.dart.tools.ui.edit.text.dart.pull.up"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> push down action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.push.down"</code>).
   */
  public static final String PUSH_DOWN = "com.google.dart.tools.ui.edit.text.dart.push.down"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> rename element action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.rename.element"</code>).
   */
  public static final String RENAME_ELEMENT = "com.google.dart.tools.ui.edit.text.dart.rename.element"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> modify method parameters action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.modify.method.parameters"</code> ).
   */
  public static final String MODIFY_METHOD_PARAMETERS = "com.google.dart.tools.ui.edit.text.dart.modify.method.parameters"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> move element action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.move.element"</code>).
   */
  public static final String MOVE_ELEMENT = "com.google.dart.tools.ui.edit.text.dart.move.element"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> extract local variable action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.extract.local.variable"</code> ).
   */
  public static final String EXTRACT_LOCAL_VARIABLE = "com.google.dart.tools.ui.edit.text.dart.extract.local.variable"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> extract constant action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.extract.constant"</code>).
   */
  public static final String EXTRACT_CONSTANT = "com.google.dart.tools.ui.edit.text.dart.extract.constant"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> introduce parameter action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.introduce.parameter"</code> ).
   */
  public static final String INTRODUCE_PARAMETER = "com.google.dart.tools.ui.edit.text.dart.introduce.parameter"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> introduce factory action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.introduce.factory"</code>).
   */
  public static final String INTRODUCE_FACTORY = "com.google.dart.tools.ui.edit.text.dart.introduce.factory"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> self encapsulate field action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.self.encapsulate.field"</code> ).
   */
  public static final String SELF_ENCAPSULATE_FIELD = "com.google.dart.tools.ui.edit.text.dart.self.encapsulate.field"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> extract method action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.extract.method"</code>).
   */
  public static final String EXTRACT_METHOD = "com.google.dart.tools.ui.edit.text.dart.extract.method"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> inline action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.inline"</code>).
   */
  public static final String INLINE = "com.google.dart.tools.ui.edit.text.dart.inline"; //$NON-NLS-1$

  public static final String CONVERT_METHOD_TO_GETTER = "com.google.dart.tools.ui.edit.text.dart.convertMethodToGetter"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> replace invocations action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.replace.invocations"</code> ).
   */
  public static final String REPLACE_INVOCATIONS = "com.google.dart.tools.ui.edit.text.dart.replace.invocations"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> introduce indirection action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.create.indirection"</code>).
   */
  public static final String INTRODUCE_INDIRECTION = "com.google.dart.tools.ui.edit.text.dart.introduce.indirection"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> extract interface action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.extract.interface"</code>).
   */
  public static final String EXTRACT_INTERFACE = "com.google.dart.tools.ui.edit.text.dart.extract.interface"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> change type action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.change.type"</code>).
   */
  public static final String CHANGE_TYPE = "com.google.dart.tools.ui.edit.text.dart.change.type"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> move inner type to top level action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.move.inner.to.top.level"</code> ).
   */
  public static final String MOVE_INNER_TO_TOP = "com.google.dart.tools.ui.edit.text.dart.move.inner.to.top.level"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> use supertype action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.use.supertype"</code>).
   */
  public static final String USE_SUPERTYPE = "com.google.dart.tools.ui.edit.text.dart.use.supertype"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> infer generic type arguments action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.infer.type.arguments"</code> ).
   */
  public static final String INFER_TYPE_ARGUMENTS_ACTION = "com.google.dart.tools.ui.edit.text.dart.infer.type.arguments"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> promote local variable action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.promote.local.variable"</code> ).
   */
  public static final String PROMOTE_LOCAL_VARIABLE = "com.google.dart.tools.ui.edit.text.dart.promote.local.variable"; //$NON-NLS-1$

  /**
   * Action definition ID of the refactor -> convert anonymous to nested action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.convert.anonymous.to.nested"</code> ).
   */
  public static final String CONVERT_ANONYMOUS_TO_NESTED = "com.google.dart.tools.ui.edit.text.dart.convert.anonymous.to.nested"; //$NON-NLS-1$

  // navigate

  /**
   * Action definition ID of the navigate -> open action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.open.editor"</code>).
   */
  public static final String OPEN_EDITOR = "com.google.dart.tools.ui.edit.text.dart.open.editor"; //$NON-NLS-1$

  /**
   * Action definition ID of the navigate -> open super implementation action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.open.super.implementation"</code> ).
   */
  public static final String OPEN_SUPER_IMPLEMENTATION = "com.google.dart.tools.ui.edit.text.dart.open.super.implementation"; //$NON-NLS-1$

  /**
   * Action definition ID of the navigate -> open external dartdoc action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.open.external.dartdoc"</code> ).
   */
  public static final String OPEN_EXTERNAL_JAVADOC = "com.google.dart.tools.ui.edit.text.dart.open.external.dartdoc"; //$NON-NLS-1$

  /**
   * Action definition ID of the navigate -> open type hierarchy action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.open.type.hierarchy"</code> ).
   */
  public static final String OPEN_TYPE_HIERARCHY = "com.google.dart.tools.ui.edit.text.dart.open.type.hierarchy"; //$NON-NLS-1$

  /**
   * Action definition ID of the navigate -> open call hierarchy action (value
   * <code>"com.google.dart.tools.ui.edit.text.open.call.hierarchy"</code> ).
   */
  public static final String OPEN_CALL_HIERARCHY = "com.google.dart.tools.ui.edit.text.open.call.hierarchy"; //$NON-NLS-1$

  /**
   * Action definition ID of the navigate -> open call hierarchy action (value
   * <code>"com.google.dart.tools.ui.edit.text.analyze.call.hierarchy"</code> ).
   */
  public static final String ANALYZE_CALL_HIERARCHY = "com.google.dart.tools.ui.edit.text.analyze.call.hierarchy"; //$NON-NLS-1$

  /**
   * Action definition ID of the navigate -> show in package explorer action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.show.in.package.view"</code> ).
   */
  public static final String SHOW_IN_PACKAGE_VIEW = "com.google.dart.tools.ui.edit.text.dart.show.in.package.view"; //$NON-NLS-1$

  /**
   * Action definition ID of the navigate -> show in navigator action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.show.in.navigator.view"</code> ).
   */
  public static final String SHOW_IN_NAVIGATOR_VIEW = "com.google.dart.tools.ui.edit.text.dart.show.in.navigator.view"; //$NON-NLS-1$

  // search

  /**
   * Action definition ID of the search -> references in workspace action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.references.in.workspace"</code> ).
   */
  public static final String SEARCH_REFERENCES_IN_WORKSPACE = "com.google.dart.tools.ui.edit.text.dart.search.references.in.workspace"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> references in project action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.references.in.project"</code> ).
   */
  public static final String SEARCH_REFERENCES_IN_PROJECT = "com.google.dart.tools.ui.edit.text.dart.search.references.in.project"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> references in hierarchy action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.references.in.hierarchy"</code> ).
   */
  public static final String SEARCH_REFERENCES_IN_HIERARCHY = "com.google.dart.tools.ui.edit.text.dart.search.references.in.hierarchy"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> references in working set action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.references.in.working.set"</code> ).
   */
  public static final String SEARCH_REFERENCES_IN_WORKING_SET = "com.google.dart.tools.ui.edit.text.dart.search.references.in.working.set"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> read access in workspace action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.read.access.in.workspace"</code> ).
   */
  public static final String SEARCH_READ_ACCESS_IN_WORKSPACE = "com.google.dart.tools.ui.edit.text.dart.search.read.access.in.workspace"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> read access in project action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.read.access.in.project"</code> ).
   */
  public static final String SEARCH_READ_ACCESS_IN_PROJECT = "com.google.dart.tools.ui.edit.text.dart.search.read.access.in.project"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> read access in hierarchy action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.read.access.in.hierarchy"</code> ).
   */
  public static final String SEARCH_READ_ACCESS_IN_HIERARCHY = "com.google.dart.tools.ui.edit.text.dart.search.read.access.in.hierarchy"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> read access in working set action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.read.access.in.working.set"</code> ).
   */
  public static final String SEARCH_READ_ACCESS_IN_WORKING_SET = "com.google.dart.tools.ui.edit.text.dart.search.read.access.in.working.set"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> write access in workspace action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.write.access.in.workspace"</code> ).
   */
  public static final String SEARCH_WRITE_ACCESS_IN_WORKSPACE = "com.google.dart.tools.ui.edit.text.dart.search.write.access.in.workspace"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> write access in project action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.write.access.in.project"</code> ).
   */
  public static final String SEARCH_WRITE_ACCESS_IN_PROJECT = "com.google.dart.tools.ui.edit.text.dart.search.write.access.in.project"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> write access in hierarchy action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.write.access.in.hierarchy"</code> ).
   */
  public static final String SEARCH_WRITE_ACCESS_IN_HIERARCHY = "com.google.dart.tools.ui.edit.text.dart.search.write.access.in.hierarchy"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> write access in working set action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.write.access.in.working.set"</code> ).
   */
  public static final String SEARCH_WRITE_ACCESS_IN_WORKING_SET = "com.google.dart.tools.ui.edit.text.dart.search.write.access.in.working.set"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> declarations in workspace action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.declarations.in.workspace"</code> ).
   */
  public static final String SEARCH_DECLARATIONS_IN_WORKSPACE = "com.google.dart.tools.ui.edit.text.dart.search.declarations.in.workspace"; //$NON-NLS-1$
  /**
   * Action definition ID of the search -> declarations in project action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.declarations.in.project"</code> ).
   */
  public static final String SEARCH_DECLARATIONS_IN_PROJECTS = "com.google.dart.tools.ui.edit.text.dart.search.declarations.in.project"; //$NON-NLS-1$
  /**
   * Action definition ID of the search -> declarations in hierarchy action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.declarations.in.hierarchy"</code> ).
   */
  public static final String SEARCH_DECLARATIONS_IN_HIERARCHY = "com.google.dart.tools.ui.edit.text.dart.search.declarations.in.hierarchy"; //$NON-NLS-1$
  /**
   * Action definition ID of the search -> declarations in working set action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.declarations.in.working.set"</code> ).
   */
  public static final String SEARCH_DECLARATIONS_IN_WORKING_SET = "com.google.dart.tools.ui.edit.text.dart.search.declarations.in.working.set"; //$NON-NLS-1$
  /**
   * Action definition ID of the search -> implementors in workspace action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.implementors.in.workspace"</code> ).
   */
  public static final String SEARCH_IMPLEMENTORS_IN_WORKSPACE = "com.google.dart.tools.ui.edit.text.dart.search.implementors.in.workspace"; //$NON-NLS-1$
  /**
   * Action definition ID of the search -> implementors in working set action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.implementors.in.working.set"</code> ).
   */
  public static final String SEARCH_IMPLEMENTORS_IN_WORKING_SET = "com.google.dart.tools.ui.edit.text.dart.search.implementors.in.working.set"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> implementors in project action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.implementors.in.project"</code> ).
   */
  public static final String SEARCH_IMPLEMENTORS_IN_PROJECT = "com.google.dart.tools.ui.edit.text.dart.search.implementors.in.project"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> occurrences in file quick menu action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.occurrences.in.file.quickMenu"</code> ).
   */
  public static final String SEARCH_OCCURRENCES_IN_FILE_QUICK_MENU = "com.google.dart.tools.ui.edit.text.dart.search.occurrences.in.file.quickMenu"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> occurrences in file > elements action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.occurrences.in.file"</code> ).
   */
  public static final String SEARCH_OCCURRENCES_IN_FILE = "com.google.dart.tools.ui.edit.text.dart.search.occurrences.in.file"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> occurrences in file > exceptions action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.exception.occurrences"</code> ).
   */
  public static final String SEARCH_EXCEPTION_OCCURRENCES_IN_FILE = "com.google.dart.tools.ui.edit.text.dart.search.exception.occurrences"; //$NON-NLS-1$

  /**
   * Action definition ID of the search -> occurrences in file > implements action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.search.implement.occurrences"</code> ).
   */
  public static final String SEARCH_IMPLEMENT_OCCURRENCES_IN_FILE = "com.google.dart.tools.ui.edit.text.dart.search.implement.occurrences"; //$NON-NLS-1$

  // miscellaneous

  /**
   * Action definition ID of the toggle text hover tool bar button action (value
   * <code>"com.google.dart.tools.ui.edit.text.dart.toggle.text.hover"</code>).
   */
  public static final String TOGGLE_TEXT_HOVER = "com.google.dart.tools.ui.edit.text.dart.toggle.text.hover"; //$NON-NLS-1$

  /**
   * Action definition ID of the remove occurrence annotations action (value
   * <code>"com.google.dart.tools.ui.edit.text.remove.occurrence.annotations"</code> ).
   */
  public static final String REMOVE_OCCURRENCE_ANNOTATIONS = "com.google.dart.tools.ui.edit.text.remove.occurrence.annotations"; //$NON-NLS-1$

  /**
   * Action definition id of toggle mark occurrences action (value:
   * <code>"com.google.dart.tools.ui.edit.text.dart.toggleMarkOccurrences"</code> ).
   */
  public static final String TOGGLE_MARK_OCCURRENCES = "com.google.dart.tools.ui.edit.text.dart.toggleMarkOccurrences"; //$NON-NLS-1$

  /**
   * Action definition id of the collapse members action (value:
   * <code>"com.google.dart.tools.ui.text.folding.collapseMembers"</code> ).
   */
  public static final String FOLDING_COLLAPSE_MEMBERS = "com.google.dart.tools.ui.text.folding.collapseMembers"; //$NON-NLS-1$

  /**
   * Action definition id of the collapse comments action (value:
   * <code>"com.google.dart.tools.ui.text.folding.collapseComments"</code> ).
   */
  public static final String FOLDING_COLLAPSE_COMMENTS = "com.google.dart.tools.ui.text.folding.collapseComments"; //$NON-NLS-1$

  /**
   * Action definition id of the code clean up action (value:
   * <code>"com.google.dart.tools.ui.edit.text.dart.clean.up"</code>).
   */
  public static final String CLEAN_UP = "com.google.dart.tools.ui.edit.text.dart.clean.up"; //$NON-NLS-1$
}
