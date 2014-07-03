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

package com.google.dart.engine.source;

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.utilities.io.ProcessRunner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An explicit package: resolver. This UriResolver shells out to pub, calling it's list-package-dirs
 * command. It parses the resulting json map, which maps symbolic package references to their
 * concrete locations on disk.
 * 
 * <pre>
 *{
 *"packages": {
 *"foo": "path/to/foo",
 *"bar": "path/to/bar"
 *},
 *"input_files": [
 *...
 *]
 *},
 *</pre>
 */
public class ExplicitPackageUriResolver extends UriResolver {
  /**
   * The name of the {@code package} scheme.
   */
  public static final String PACKAGE_SCHEME = "package";

  protected static final String PUB_LIST_COMMAND = "list-package-dirs";

  /**
   * Return {@code true} if the given URI is a {@code package} URI.
   * 
   * @param uri the URI being tested
   * @return {@code true} if the given URI is a {@code package} URI
   */
  public static boolean isPackageUri(URI uri) {
    return PACKAGE_SCHEME.equals(uri.getScheme());
  }

  private File rootDir;
  private DirectoryBasedDartSdk sdk;
  @VisibleForTesting
  protected Map<String, List<File>> packageMap;

  // TODO: For now, this takes a DirectoryBasedDartSdk. We may want to abstract this out into
  // something that can return a package map.

  /**
   * Create a new ExplicitPackageUriResolver.
   * 
   * @param sdk the sdk; this is used to locate the pub command to run
   * @param rootDir the directory for which we'll be resolving package information
   */
  public ExplicitPackageUriResolver(DirectoryBasedDartSdk sdk, File rootDir) {
    if (rootDir == null) {
      throw new IllegalArgumentException("the root dir must not be null");
    }

    this.sdk = sdk;
    this.rootDir = rootDir;
  }

  @Override
  public Source fromEncoding(UriKind kind, URI uri) {
    if (kind == UriKind.PACKAGE_URI) {
      return new FileBasedSource(new File(uri), kind);
    } else {
      return null;
    }
  }

  public String[] getCommand() {
    return new String[] {sdk.getPubExecutable().getAbsolutePath(), PUB_LIST_COMMAND};
  }

  public File getRootDir() {
    return rootDir;
  }

  @Override
  public Source resolveAbsolute(URI uri) {
    if (!isPackageUri(uri)) {
      return null;
    }
    String path = uri.getPath();
    if (path == null) {
      path = uri.getSchemeSpecificPart();
      if (path == null) {
        return null;
      }
    }
    String pkgName;
    String relPath;
    int index = path.indexOf('/');
    if (index == -1) {
      // No slash
      pkgName = path;
      relPath = "";
    } else if (index == 0) {
      // Leading slash is invalid
      return null;
    } else {
      // <pkgName>/<relPath>
      pkgName = path.substring(0, index);
      relPath = path.substring(index + 1);
    }

    if (packageMap == null) {
      packageMap = calculatePackageMap();
    }

    List<File> dirs = packageMap.get(pkgName);

    if (dirs != null) {
      for (File packageDir : dirs) {
        if (packageDir.exists()) {
          File resolvedFile = new File(packageDir, relPath.replace('/', File.separatorChar));

          if (resolvedFile.exists()) {
            return new FileBasedSource(resolvedFile, UriKind.PACKAGE_URI);
          }
        }
      }
    }

    // Return a FileBasedSource that doesn't exist. This helps provide more meaningful error 
    // messages to users (a missing file error, as opposed to an invalid uri error).

    String fullPackagePath = pkgName + "/" + relPath;

    return new FileBasedSource(new File(getRootDir(), fullPackagePath.replace(
        '/',
        File.separatorChar)), UriKind.PACKAGE_URI);
  }

  public String resolvePathToPackage(String path) {
    if (packageMap == null) {
      return null;
    }

    for (String key : packageMap.keySet()) {
      List<File> files = packageMap.get(key);
      for (File file : files) {
        try {
          if (file.getCanonicalPath().endsWith(path)) {
            return key;
          }
        } catch (IOException e) {

        }
      }
    }
    return null;
  }

  @Override
  public URI restoreAbsolute(Source source) {
    if (packageMap == null) {
      return null;
    }

    if (source instanceof FileBasedSource) {
      String sourcePath = ((FileBasedSource) source).getFile().getPath();
      for (Entry<String, List<File>> entry : packageMap.entrySet()) {
        for (File pkgFolder : entry.getValue()) {
          String pkgCanonicalPath = pkgFolder.getAbsolutePath();
          if (sourcePath.startsWith(pkgCanonicalPath)) {
            String packageName = entry.getKey();
            String relPath = sourcePath.substring(pkgCanonicalPath.length());
            return URI.create(PACKAGE_SCHEME + ":" + packageName + relPath);
          }
        }
      }
    }

    return null;
  }

  protected Map<String, List<File>> calculatePackageMap() {
    ProcessBuilder builder = new ProcessBuilder(getCommand());
    builder.directory(getRootDir());
    ProcessRunner runner = new ProcessRunner(builder);

    try {
      if (runProcess(runner) == 0) {
        return parsePackageMap(runner.getStdOut());
      } else {
        AnalysisEngine.getInstance().getLogger().logInformation(
            "pub " + PUB_LIST_COMMAND + " failed: exit code " + runner.getExitCode());
      }
    } catch (IOException ioe) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "error running pub " + PUB_LIST_COMMAND,
          ioe);
    } catch (JSONException e) {
      AnalysisEngine.getInstance().getLogger().logError(
          "malformed json from pub " + PUB_LIST_COMMAND,
          e);
    }

    return new HashMap<String, List<File>>();
  }

  protected Map<String, List<File>> parsePackageMap(String jsonText) throws JSONException {
    Map<String, List<File>> map = new HashMap<String, List<File>>();

    // Json format:
//    {
//      "packages": {
//        "foo": "path/to/foo",
//        "bar": "path/to/bar",
//        "myapp": "path/to/myapp"   // <- self link
//      },
//      "input_files": [
//        "path/to/myapp/pubspec.lock"
//      ]
//    }

    JSONObject obj = new JSONObject(jsonText);

    JSONObject packages = obj.optJSONObject("packages");

    // TODO: also parse 'input_files'; use that information to check file timestamps

    if (packages != null) {
      Iterator<?> keys = packages.keys();

      while (keys.hasNext()) {
        Object key = keys.next();

        if (key instanceof String) {
          String strKey = (String) key;

          List<File> files = new ArrayList<File>();
          map.put(strKey, files);

          Object val = packages.get(strKey);

          if (val instanceof String) {
            String path = (String) val;

            files.add(new File(path));
          } else if (val instanceof JSONArray) {
            JSONArray arr = (JSONArray) val;

            for (int i = 0; i < arr.length(); i++) {
              files.add(new File(arr.getString(i)));
            }
          }
        }
      }
    }

    return map;
  }

  /**
   * Run the external process and return the exit value once the external process has completed.
   * 
   * @param runner the external process runner
   * @return the external process exit code
   */
  protected int runProcess(ProcessRunner runner) throws IOException {
    return runner.runSync(0);
  }
}
