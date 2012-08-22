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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartDocumentable;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartSdkManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to manage dartdoc sidecar data. This is dartdoc content that is not stored with the
 * library, but in some other location.
 */
class ExternalDartDocManager {
  private static ExternalDartDocManager manager = new ExternalDartDocManager();

  public static ExternalDartDocManager getManager() {
    return manager;
  }

  private Map<File, ExternalDartDocFile> fileMap = new HashMap<File, ExternalDartDocFile>();

  private ExternalDartDocManager() {

  }

  public String getExtDartDoc(DartDocumentable documentable) {
    if (documentable == null) {
      return null;
    }

    if (!DartSdkManager.getManager().getSdk().hasDocumentation()) {
      return null;
    }

    DartLibrary library = documentable.getAncestor(DartLibrary.class);

    if (library == null) {
      return null;
    }

    String libraryName = library.getElementName();

    // TODO(devoncarew): system library names should either all contain dart: or none of them should
    if (libraryName.indexOf(':') != -1) {
      libraryName = libraryName.substring(libraryName.indexOf(':') + 1);
    }

    File file = DartSdkManager.getManager().getSdk().getDocFileFor(libraryName);

    if (file == null) {
      return null;
    }

    ExternalDartDocFile docFile = getCreateDartDocFile(file);

    if (docFile == null) {
      return null;
    }

    return docFile.getDocFor(documentable);
  }

  private ExternalDartDocFile getCreateDartDocFile(File file) {
    if (fileMap.containsKey(file)) {
      return fileMap.get(file);
    }

    try {
      ExternalDartDocFile docFile = new ExternalDartDocFile(file);

      fileMap.put(file, docFile);

      return docFile;
    } catch (IOException ioe) {
      DartCore.logError(ioe);

      return null;
    }
  }

}
