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

package com.google.dart.tools.ui.internal.filesview.nodes.old.sdk;

import com.google.dart.tools.ui.internal.filesview.nodes.old.IDartNode_OLD;

/**
 * A class used to represent the Dart SDK in the Files view.
 */
public abstract class DartSdkNode_OLD implements IDartNode_OLD {

  public static DartSdkNode_OLD createInstance() {
    return new NewDartSdkNode_OLD();
  }

  public abstract DartLibraryNode_OLD[] getLibraries();

}
