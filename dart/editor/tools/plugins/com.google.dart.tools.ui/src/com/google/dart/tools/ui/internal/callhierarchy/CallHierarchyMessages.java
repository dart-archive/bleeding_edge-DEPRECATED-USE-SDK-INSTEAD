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
package com.google.dart.tools.ui.internal.callhierarchy;

import org.eclipse.osgi.util.NLS;

public final class CallHierarchyMessages extends NLS {

  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.internal.callhierarchy.CallHierarchyMessages";//$NON-NLS-1$

  public static String CallerMethodWrapper_taskname;

  public static String CalleeMethodWrapper_taskname;
  static {
    NLS.initializeMessages(BUNDLE_NAME, CallHierarchyMessages.class);
  }

  private CallHierarchyMessages() {
    // Do not instantiate
  }
}
