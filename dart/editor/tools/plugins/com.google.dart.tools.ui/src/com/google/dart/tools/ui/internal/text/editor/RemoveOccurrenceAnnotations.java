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
package com.google.dart.tools.ui.internal.text.editor;

import org.eclipse.jface.action.Action;

/**
 * Remove occurrence annotations action.
 */
class RemoveOccurrenceAnnotations extends Action {

  /** The Java editor to which this actions belongs. */
  private final DartEditor fEditor;

  /**
   * Creates this action.
   * 
   * @param editor the Java editor for which to remove the occurrence annotations
   */
  RemoveOccurrenceAnnotations(DartEditor editor) {
    fEditor = editor;
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    // TODO(scheglov) is this action really used?
    // I see it's command, but it is not bound to any action.
//    fEditor.removeOccurrenceAnnotations();
  }
}
