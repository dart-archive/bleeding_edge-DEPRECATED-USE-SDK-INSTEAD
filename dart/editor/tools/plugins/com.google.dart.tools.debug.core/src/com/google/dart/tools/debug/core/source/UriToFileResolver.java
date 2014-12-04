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

package com.google.dart.tools.debug.core.source;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.CreateContextConsumer;
import com.google.dart.server.MapUriConsumer;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.IFileInfo;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A helper for converting URIs to files paths and vice versa.
 */
public class UriToFileResolver {
  private IContainer container;
  private final Map<String, String> urlToFileCache = Maps.newHashMap();
  private final Map<String, String> fileToUriCache = Maps.newHashMap();

  private String executionContextId;

  public UriToFileResolver(ILaunch launch) {
    ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(launchConfiguration);

    container = wrapper.getSourceDirectory();
    if (container == null) {
      IResource resource = wrapper.getApplicationResource();

      if (resource instanceof IContainer) {
        container = resource.getParent();
      } else if (resource != null) {
        container = resource.getParent();
      }
    }

    String resourcePath = container != null ? container.getLocation().toOSString() : null;

    // create an execution context
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER && resourcePath != null) {
      DartCore.getAnalysisServer().execution_createContext(
          resourcePath,
          new CreateContextConsumer() {
            @Override
            public void computedExecutionContext(String contextId) {
              executionContextId = contextId;
            }
          });
    }
  }

  public void dispose() {
    // delete the execution context
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER && executionContextId != null) {
      DartCore.getAnalysisServer().execution_deleteContext(executionContextId);
      executionContextId = null;
    }
  }

  public String getFileForUri(String url) {
    if (container == null) {
      return null;
    }
    String file = urlToFileCache.get(url);
    if (file == null) {
      file = getFileForUri0(url);
      urlToFileCache.put(url, file);
    }
    return file;
  }

  public String getUriForPath(String file) {
    if (container == null) {
      return null;
    }
    String uri = fileToUriCache.get(file);
    if (uri == null) {
      uri = getUriForPath0(file);
      fileToUriCache.put(file, uri);
    }
    return uri;
  }

  private String getFileForUri0(String url) {
    final String chromeExt = "chrome-extension://";

    try {
      String filePath;

      // /Users/foo/dart/serverapp/serverapp.dart
      // file:///Users/foo/dart/webapp2/webapp2.dart
      // http://0.0.0.0:3030/webapp/webapp.dart
      // package:abc/abc.dart
      // chrome-extension://kcjgcakhgelcejampmijgkjkadfcncjl/spark.dart

      // resolve package: urls to file: urls
      if (DartCore.isPackageSpec(url)) {
        url = resolvePackageUri(url);
      }

      if (url == null) {
        return null;
      }
      URI uri = new URI(url);
      String uriScheme = uri.getScheme();

      // Special case Chrome extension paths.
      if (url.startsWith(chromeExt)) {
        url = url.substring(chromeExt.length());
        if (url.indexOf('/') != -1) {
          url = url.substring(url.indexOf('/') + 1);
        }
      }

      // handle dart:lib/lib.dart in DartSdkSourceContainer,
      // exclude "_patch.dart" files, they don't exist as files in sdk/lib folder
      if (uri != null && "dart".equals(uriScheme)) {
        if (!url.endsWith("_patch.dart")) {
          return url;
        }
      }

      // Handle both fully absolute path names and http: urls.
      if (url.startsWith("/")) {
        return url;
      } else if (uriScheme == null) {
        // handle relative file path
        filePath = resolveRelativePath(url);
      } else {
        filePath = uri.getPath();
      }

      if (filePath == null) {
        return null;
      } else {
        return returnAbsoluteOrRelative(filePath);
      }
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * It seems that {@link ProjectManager#resolvePathToPackage} works only in limited cases. So, here
   * is a copy of another implementation.
   */
  private String getUriForPath_fromServerBreakpointManager(String filePath) {
    File javaFile = new File(filePath);
    IFile resourceFile = ResourceUtil.getFile(javaFile);
    if (resourceFile == null) {
      return null;
    }
    String locationUrl = resourceFile.getLocation().toFile().toURI().toString();

    int index = locationUrl.indexOf(DartCore.PACKAGES_DIRECTORY_URL);

    if (index != -1) {
      locationUrl = DartCore.PACKAGE_SCHEME_SPEC
          + locationUrl.substring(index + DartCore.PACKAGES_DIRECTORY_URL.length());

      return locationUrl;
    }

    index = locationUrl.lastIndexOf(DartCore.LIB_URL_PATH);

    if (index != -1) {
      String path = resourceFile.getLocation().toString();
      path = path.substring(0, path.lastIndexOf(DartCore.LIB_URL_PATH));
      File packagesDir = new File(path, DartCore.PACKAGES_DIRECTORY_NAME);

      if (packagesDir.exists()) {
        String packageName = DartCore.getSelfLinkedPackageName(resourceFile);

        if (packageName != null) {
          locationUrl = DartCore.PACKAGE_SCHEME_SPEC + packageName + "/"
              + locationUrl.substring(index + DartCore.LIB_URL_PATH.length());

          return locationUrl;
        }
      }
    }

    return null;
  }

  private String getUriForPath0(String file) {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (executionContextId != null) {
        final String[] uriPtr = {null};
        final CountDownLatch latch = new CountDownLatch(1);
        DartCore.getAnalysisServer().execution_mapUri(
            executionContextId,
            file,
            null,
            new MapUriConsumer() {
              @Override
              public void computedFileOrUri(String file, String uri) {
                uriPtr[0] = uri;
                latch.countDown();
              }
            });
        Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.SECONDS);
        return uriPtr[0];
      }
    } else {
      {
        String uri = getUriForPath_fromServerBreakpointManager(file);
        if (uri != null) {
          return uri;
        }
      }
      return DartCore.getProjectManager().resolvePathToPackage(container, file);
    }
    return null;
  }

  private String resolvePackageUri(String url) {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (executionContextId != null) {
        final String[] filePtr = {null};
        final CountDownLatch latch = new CountDownLatch(1);
        DartCore.getAnalysisServer().execution_mapUri(
            executionContextId,
            null,
            url,
            new MapUriConsumer() {
              @Override
              public void computedFileOrUri(String file, String uri) {
                filePtr[0] = file;
                latch.countDown();
              }
            });
        Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.SECONDS);
        return filePtr[0];
      }
    } else {
      ProjectManager projectManager = DartCore.getProjectManager();
      IFileInfo fileInfo = projectManager.resolveUriToFileInfo(container, url);
      if (fileInfo != null) {
        File file = fileInfo.getFile();
        File absoluteFile = file.getAbsoluteFile();
        return absoluteFile.toURI().toString();
      }
    }
    return null;
  }

  private String resolveRelativePath(String url) {
    if (container != null) {
      IResource file = container.findMember(url);
      if (file != null) {
        return file.getLocation().toOSString();
      }
    }
    return null;
  }

  private String returnAbsoluteOrRelative(String path) {
    // If path exists, return that.
    File file = new File(path);
    if (file.exists()) {
      return path;
    }

    // Try and return a path relative to the resource.
    if (container != null) {
      int index = path.indexOf('/');

      while (index != -1) {
        String subPath = path.substring(index + 1);
        IResource resource = container.findMember(subPath);
        if (resource != null) {
          return resource.getLocation().toFile().getAbsolutePath();
        }
        index = path.indexOf('/', index + 1);
      }
    }

    // Else, return the original path.
    return path;
  }
}
