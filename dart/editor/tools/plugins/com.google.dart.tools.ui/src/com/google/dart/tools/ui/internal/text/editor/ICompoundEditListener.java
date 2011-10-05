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
 * <code>ICompoundEditListener</code>s can be registered with a {@link CompoundEditExitStrategy} to
 * be notified if a compound edit session is ended.
 */
public interface ICompoundEditListener {
  /**
   * Notifies the receiver that the sending <code>CompoundEditExitStrategy</code> has detected the
   * end of a compound operation.
   */
  void endCompoundEdit();
}
