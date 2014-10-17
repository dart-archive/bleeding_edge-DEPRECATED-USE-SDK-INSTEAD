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
package com.google.dart.tools.debug.core.source;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.util.IDartStackFrame;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;

/**
 * Converts from a Dart debug object to the path of the source Dart file.
 * <p>
 * The returned source path will either be workspace relative or be relative to a bundled library
 * identifier.
 */
public class DartSourceLookupParticipant extends AbstractSourceLookupParticipant {
  @Override
  public String getSourceName(Object object) throws CoreException {
    if (object instanceof String) {
      return (String) object;
    } else if (object instanceof IDartStackFrame) {
      IDartStackFrame sourceLookup = (IDartStackFrame) object;
      return sourceLookup.getSourceLocationPath();
    } else {
      DartDebugCorePlugin.logWarning("Unhandled type " + object.getClass()
          + " in DartSourceLookupParticipant.getSourceName()");

      return null;
    }
  }
}
