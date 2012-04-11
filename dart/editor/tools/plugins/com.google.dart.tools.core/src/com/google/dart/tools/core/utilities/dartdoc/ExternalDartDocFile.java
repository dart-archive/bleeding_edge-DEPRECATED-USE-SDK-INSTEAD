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

package com.google.dart.tools.core.utilities.dartdoc;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartDocumentable;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A representation of an id ==> dartdoc map file.
 */
class ExternalDartDocFile {

  /**
   * Return a qualified, unique symbol name for the library namespace (Ex. "main" or "foo.bar").
   * 
   * @param element
   * @return
   */
  private static String getQualifiedName(DartElement element) {
    if (element == null) {
      return null;
    }

    if (element instanceof DartLibrary || element instanceof CompilationUnit) {
      return null;
    }

    String parentId = getQualifiedName(element.getParent());

    if (parentId == null) {
      return element.getElementName();
    } else {
      return parentId + "." + element.getElementName();
    }
  }

  private Map<String, String> idDocMap = new HashMap<String, String>();

  ExternalDartDocFile(File file) throws IOException {
    parseDocFile(file);
  }

  public String getDocFor(DartDocumentable documentable) {
    String id = getQualifiedName(documentable);

    return (id == null ? null : idDocMap.get(id));
  }

  private void parseDocFile(File file) throws IOException {
    // TODO(devoncarew): implement this when we have a file format agreed on...

  }

}
