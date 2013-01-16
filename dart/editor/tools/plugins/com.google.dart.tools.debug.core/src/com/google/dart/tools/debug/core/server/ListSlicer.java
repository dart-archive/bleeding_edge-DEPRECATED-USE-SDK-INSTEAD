/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.debug.core.server;

import org.eclipse.debug.core.model.IVariable;

import java.util.ArrayList;
import java.util.List;

class ListSlicer {

  public static List<IVariable> createValues(ServerDebugTarget target, VmValue arrValue) {
    int chunkLength = 100;

    List<IVariable> fields = new ArrayList<IVariable>();

    int length = arrValue.getLength();

    if (length > chunkLength) {
      if (length > (chunkLength * chunkLength)) {
        // A very large array - increase the chunk size.
        chunkLength *= 10;
      }

      int offset = 0;

      while (offset < length) {
        fields.add(new ServerDebugVariableArraySlice(target, arrValue, offset, chunkLength));

        offset += chunkLength;
      }
    } else {
      for (int i = 0; i < length; i++) {
        fields.add(new ServerDebugVariable(target, VmVariable.createArrayEntry(
            target.getConnection(),
            arrValue,
            i)));
      }
    }

    return fields;
  }

}
