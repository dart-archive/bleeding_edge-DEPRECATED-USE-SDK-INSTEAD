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
package com.google.dart.tools.core.internal.completion.ast;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.common.SourceInfo;

class CompletionUtil {

  static <T extends DartNode> T init(T newNode, DartNode oldNode) {
    SourceInfo oldSourceInfo = oldNode.getSourceInfo();
    newNode.setSourceInfo(new SourceInfo(
        oldSourceInfo.getSource(),
        oldSourceInfo.getOffset(),
        oldSourceInfo.getLength()));
    return newNode;
  }
}
