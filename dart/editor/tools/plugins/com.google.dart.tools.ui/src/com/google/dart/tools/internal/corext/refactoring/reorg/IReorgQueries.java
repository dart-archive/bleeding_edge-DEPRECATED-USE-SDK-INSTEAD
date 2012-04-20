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
package com.google.dart.tools.internal.corext.refactoring.reorg;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public interface IReorgQueries {

  public static final int CONFIRM_DELETE_EMPTY_CUS = 2;

  public static final int CONFIRM_DELETE_FOLDERS_CONTAINING_SOURCE_FOLDERS = 4;

  public static final int CONFIRM_DELETE_GETTER_SETTER = 1;

  public static final int CONFIRM_DELETE_LINKED_PARENT = 8;

  public static final int CONFIRM_DELETE_REFERENCED_ARCHIVES = 3;

  public static final int CONFIRM_OVERWRITING = 6;

  public static final int CONFIRM_READ_ONLY_ELEMENTS = 5;

  public static final int CONFIRM_SKIPPING = 7;

  IConfirmQuery createSkipQuery(String queryTitle, int queryID);

  IConfirmQuery createYesNoQuery(String queryTitle, boolean allowCancel, int queryID);

  IConfirmQuery createYesYesToAllNoNoToAllQuery(String queryTitle, boolean allowCancel, int queryID);
}
