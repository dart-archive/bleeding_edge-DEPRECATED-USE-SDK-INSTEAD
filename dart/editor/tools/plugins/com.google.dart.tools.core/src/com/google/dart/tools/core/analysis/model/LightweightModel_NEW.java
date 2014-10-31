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

package com.google.dart.tools.core.analysis.model;

import com.google.dart.server.generated.types.ExecutableKind;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.ResourceUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * A lightweight version of the core model. This version can be queried quickly and is guaranteed to
 * have reasonably up-to-date information. As the latest data is available from the analysis engine,
 * this model is updated.
 */
public class LightweightModel_NEW extends LightweightModel {
  protected LightweightModel_NEW() {
    DartCore.getAnalysisServerData().subscribeLaunchData(new AnalysisServerLaunchDataListener() {
      @Override
      public void computedLaunchData(String filePath, String kind, String[] referencedFiles) {
        try {
          IFile file = ResourceUtil.getFile(filePath);
          // client or server library
          if (file != null) {
            setFileProperty(file, CLIENT_LIBRARY, ExecutableKind.CLIENT.equals(kind));
            setFileProperty(file, SERVER_LIBRARY, ExecutableKind.SERVER.equals(kind));
          }
          // Dart files referenced by this HTML file
          if (referencedFiles != null) {
            for (String referencedDartFilePath : referencedFiles) {
              IFile referencedDartFile = ResourceUtil.getFile(referencedDartFilePath);
              if (referencedDartFile != null) {
                setFileProperty(referencedDartFile, HTML_FILE, file);
              }
            }
          }
        } catch (CoreException e) {
          e.printStackTrace();
        }
      }
    });
  }
}
