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
package com.google.dart.tools.ui.internal.viewsupport;

import org.eclipse.core.resources.IResource;

/**
 * Can be added to a ProblemMarkerManager to get notified about problem marker changes. Used to
 * update error ticks.
 */
public interface IProblemChangedListener {

  /**
   * Called when problems changed. This call is posted in an aynch exec, therefore passed resources
   * must not exist.
   * 
   * @param changedResources A set with elements of type <code>IResource</code> that describe the
   *          resources that had an problem change.
   * @param isMarkerChange If set to <code>true</code>, the change was a marker change, if
   *          <code>false</code>, the change came from an annotation model modification.
   */
  void problemsChanged(IResource[] changedResources, boolean isMarkerChange);

}
