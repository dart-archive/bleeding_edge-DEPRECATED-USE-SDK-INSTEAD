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

package com.google.dart.tools.debug.core.server;

import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * An ISourceContainer that searches in active debug connections and returns remote script objects.
 */
public class ServerRemoteScriptSourceContainer extends AbstractSourceContainer {
  public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier()
      + ".containerType.workspace"; //$NON-NLS-1$

  private static final Object[] EMPTY_COLLECTION = new Object[0];

  private Map<String, LocalFileStorage> cachedSourceMap = new HashMap<String, LocalFileStorage>();

  public ServerRemoteScriptSourceContainer() {

  }

  @Override
  public Object[] findSourceElements(String name) throws CoreException {
    if (name != null) {
      if (name.startsWith("builtin:")) {
        ServerDebugTarget target = ServerDebugTarget.getActiveTarget();

        if (target != null) {
          String[] strs = name.split(":");

          int libraryId = Integer.parseInt(strs[1]);
          String url = strs[2];

          String source = target.getConnection().getScriptSource(
              target.getMainIsolate(),
              libraryId,
              url);

          if (source != null) {
            try {
              return new Object[] {getCreateStorageFor(url, source)};
            } catch (IOException e) {
              throw new CoreException(new Status(
                  IStatus.ERROR,
                  DartDebugCorePlugin.PLUGIN_ID,
                  e.toString(),
                  e));
            }
          }
        }
      }

      if (PackageLibraryManager.isDartSpec(name)) {
        ServerDebugTarget target = ServerDebugTarget.getActiveTarget();

        if (target != null) {
          String source = target.getConnection().getScriptSource(target.getMainIsolate(), name);

          if (source != null) {
            try {
              return new Object[] {getCreateStorageFor(name, source)};
            } catch (IOException e) {
              throw new CoreException(new Status(
                  IStatus.ERROR,
                  DartDebugCorePlugin.PLUGIN_ID,
                  e.toString(),
                  e));
            }
          }
        }
      }
    }

    return EMPTY_COLLECTION;
  }

  @Override
  public String getName() {
    return "Remote Scripts";
  }

  @Override
  public ISourceContainerType getType() {
    return getSourceContainerType(TYPE_ID);
  }

  private LocalFileStorage getCreateStorageFor(String url, String source) throws IOException {
    if (!cachedSourceMap.containsKey(url)) {
      String fileName = url;

      if (fileName.equals("bootstrap_impl")) {
        fileName = "dart:" + fileName;
      }

      File file = File.createTempFile(fileName.replace(':', '$') + "$$", ".dart");
      file.deleteOnExit();

      Writer out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF8"));
      out.write(source);
      out.close();

      file.setReadOnly();

      LocalFileStorage fileStorage = new LocalFileStorage(file);

      cachedSourceMap.put(url, fileStorage);
    }

    return cachedSourceMap.get(url);
  }

}
