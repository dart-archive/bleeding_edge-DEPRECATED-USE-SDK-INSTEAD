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
package com.google.dart.tools.ui.internal.text.editor;

/**
 * Defines action IDs for private DartEditor actions.
 */
public interface IDartEditorActionConstants {

  /**
   * ID of the action to toggle smart typing. Value: <code>"smartTyping"</code>
   */
  public static final String TOGGLE_SMART_TYPING = "smartTyping"; //$NON-NLS-1$

  /**
   * ID of the smart typing status item Value: <code>"SmartTyping"</code>
   */
  public static final String STATUS_CATEGORY_SMART_TYPING = "SmartTyping"; //$NON-NLS-1$

  /**
   * ID of the action to toggle the style of the presentation.
   */
  public static final String TOGGLE_PRESENTATION = "togglePresentation"; //$NON-NLS-1$

  /**
   * ID of the action to copy the qualified name.
   */
  public static final String COPY_QUALIFIED_NAME = "copyQualifiedName"; //$NON-NLS-1$

  /**
   * ID of the action to show debugging information
   */
  public static final String STATUS_CATEGORY_OFFSET = "showOffset"; //$NON-NLS-1$
}
