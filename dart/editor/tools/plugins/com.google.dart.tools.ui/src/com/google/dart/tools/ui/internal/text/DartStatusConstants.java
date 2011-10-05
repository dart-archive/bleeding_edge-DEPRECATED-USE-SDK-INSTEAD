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
package com.google.dart.tools.ui.internal.text;

/**
 * Defines status codes relevant to the Java UI plug-in. When a Core exception is thrown, it contain
 * a status object describing the cause of the exception. The status objects originating from the
 * Java UI plug-in use the codes defined in this interface.
 */
public interface DartStatusConstants {

  // Java UI status constants start at 10000 to make sure that we don't
  // collide with resource and java model constants.

  public static final int INTERNAL_ERROR = 10001;

  /**
   * Status constant indicating that an exception occurred on storing or loading templates.
   */
  public static final int TEMPLATE_IO_EXCEPTION = 10002;

  /**
   * Status constant indicating that an validateEdit call has changed the content of a file on disk.
   */
  public static final int VALIDATE_EDIT_CHANGED_CONTENT = 10003;

  /**
   * Status constant indicating that a <tt>ChangeAbortException</tt> has been caught.
   */
  public static final int CHANGE_ABORTED = 10004;

  /**
   * Status constant indicating that an exception occurred while parsing template file.
   */
  public static final int TEMPLATE_PARSE_EXCEPTION = 10005;

  /**
   * Status constant indicating that a problem occurred while notifying a post save listener.
   * 
   * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.saveparticipant.IPostSaveListener
   */
  public static final int EDITOR_POST_SAVE_NOTIFICATION = 10006;

}
