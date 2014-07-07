/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.debug.core.webkit.WebkitRemoteObject;

/**
 * An 'empty' value. Used as a place-holder when Dartium returns null values in some places.
 */
public class DartiumEmptyValue extends DartiumDebugValue {

  public DartiumEmptyValue(DartiumDebugTarget target, DartiumDebugVariable variable) {
    super(target, variable, WebkitRemoteObject.createNull());
  }

}
