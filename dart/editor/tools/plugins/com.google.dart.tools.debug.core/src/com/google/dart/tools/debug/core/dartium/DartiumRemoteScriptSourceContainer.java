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

package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.webkit.WebkitScript;

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
import java.util.Collection;

/**
 * An ISourceContainer that searches in active debug connections and returns remote script objects.
 */
public class DartiumRemoteScriptSourceContainer extends AbstractSourceContainer {
  public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier()
      + ".containerType.workspace"; //$NON-NLS-1$

  public DartiumRemoteScriptSourceContainer() {

  }

  @Override
  public Object[] findSourceElements(String name) throws CoreException {
    if (name == null) {
      return EMPTY;
    }

    DartiumDebugTarget target = DartiumDebugTarget.getActiveTarget();

    if (target != null) {
      WebkitScript script = target.getConnection().getDebugger().getScriptByUrl(name);

      if (script == null) {
        // In DartiumDebugStackFrame.getActualLocationPath(), we strip off the leading part of the
        // url. This traverses all the scripts that Dartium knows about to locate a script with
        // a url that ends with the given path fragment.
        script = findMatchingScript(target.getConnection().getDebugger().getAllScripts(), name);
      }

      if (script != null) {
        if (!script.hasScriptSource()) {
          try {
            target.getConnection().getDebugger().populateScriptSource(script);
          } catch (IOException e) {
            throw new CoreException(new Status(
                IStatus.ERROR,
                DartDebugCorePlugin.PLUGIN_ID,
                e.toString(),
                e));
          }
        }

        try {
          return new Object[] {getCreateStorageFor(script)};
        } catch (IOException e) {
          throw new CoreException(new Status(
              IStatus.ERROR,
              DartDebugCorePlugin.PLUGIN_ID,
              e.toString(),
              e));
        }
      }
    }

    return EMPTY;
  }

  @Override
  public String getName() {
    return "Remote Scripts";
  }

  @Override
  public ISourceContainerType getType() {
    return getSourceContainerType(TYPE_ID);
  }

  private String convertDataUrl(String url) {
    // convert data:application/dart;base64,CiAgICAgICAgaW1wb...3J0ICd== to
    // script:application/dart

    url = url.substring("data".length());
    url = "script" + url;

    if (url.indexOf(';') != -1) {
      url = url.substring(0, url.indexOf(';'));
    }

    if (url.length() > 40) {
      url = url.substring(0, 40);
    }

    return url;
  }

  private WebkitScript findMatchingScript(Collection<WebkitScript> scripts, String path) {
    for (WebkitScript script : scripts) {
      if (script.getUrl().endsWith(path)) {
        return script;
      }
    }

    return null;
  }

  private LocalFileStorage getCreateStorageFor(WebkitScript script) throws IOException {
    if (script.getPrivateData() == null) {
      String url = script.getUrl();

      if (url.startsWith("data:")) {
        // For things like data:application/dart;base64,CiAgICAgICAgaW1wb...3J0ICd==
        url = convertDataUrl(url);
      } else if (url.indexOf('/') != -1) {
        url = url.substring(url.lastIndexOf('/') + 1);
      } else if (url.indexOf('\\') != -1) {
        url = url.substring(url.lastIndexOf('\\') + 1);
      }

      File file = File.createTempFile(sanitizeFileName(url) + "$$", ".dart");
      file.deleteOnExit();

      Writer out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF8"));
      out.write(script.getScriptSource());
      out.close();

      file.setReadOnly();

      LocalFileStorage fileStorage = new LocalFileStorage(file);

      script.setPrivateData(fileStorage);
    }

    return (LocalFileStorage) script.getPrivateData();
  }

  private String sanitizeFileName(String str) {
    return str.replace(':', '~').replace('/', '_').replace('\\', '_');
  }
}
