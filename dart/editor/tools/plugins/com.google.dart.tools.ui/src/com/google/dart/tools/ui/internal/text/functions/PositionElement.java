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

package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.server.generated.AnalysisServer;

/**
 * An "element" to return from {@link DartElementProvider} when {@link AnalysisServer} is used.
 */
public class PositionElement {
  public final String file;
  public final int offset;

  public PositionElement(String file, int offset) {
    this.file = file;
    this.offset = offset;
  }
}
