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
package com.google.dart.tools.debug.core.pubserve;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.util.IResourceResolver;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A resolver that calls out to pub serve for the information.
 */
public class PubServeResourceResolver implements IResourceResolver {

  private class FilePathForUrlCallback implements PubCallback<PubAsset> {

    private CountDownLatch latch;
    private String[] packageName;
    private String[] path;

    public FilePathForUrlCallback(CountDownLatch latch, String[] name, String[] path) {
      this.latch = latch;
      this.packageName = name;
      this.path = path;
    }

    @Override
    public void handleResult(PubResult<PubAsset> result) {
      if (result.isError()) {
        path[0] = null;
      } else {
        packageName[0] = result.getResult().getPackageStr();
        path[0] = result.getResult().getPath();
      }
      latch.countDown();
    }
  }
  private class UrlForFileCallback implements PubCallback<String> {

    private CountDownLatch latch;
    private String[] done;

    public UrlForFileCallback(CountDownLatch latch, String[] done) {
      this.latch = latch;
      this.done = done;
    }

    @Override
    public void handleResult(PubResult<String> result) {
      if (result.isError()) {
        done[0] = null;
      } else {
        done[0] = result.getResult();
      }
      latch.countDown();
    }
  }

  private Map<String, String> urlToAsset = new HashMap<String, String>();
  private Map<String, String> resourceToUrl = new HashMap<String, String>();

  public PubServeResourceResolver() {

  }

  @Override
  public String getUrlForFile(File file) {
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());

    if (files.length > 0) {
      return getUrlForResource(files[0]);
    } else {
      return null;
    }

  }

  @Override
  public String getUrlForResource(IResource resource) {

    String url = resourceToUrl.get(resource.getFullPath().toString());
    if (url != null) {
      return url;
    }

    CountDownLatch latch = new CountDownLatch(1);
    final String[] done = new String[1];

    try {
      PubServeManager.getManager().sendGetUrlCommand(resource, new UrlForFileCallback(latch, done));
    } catch (IOException e) {
      DartCore.logError(e);
      return done[0];
    }
    try {
      latch.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // do nothing
    }
    if (done[0] != null) {
      resourceToUrl.put(resource.getFullPath().toString(), done[0]);
    }
    return done[0];
  }

  @Override
  public String getUrlRegexForResource(IResource resource) {
    IContainer appDir = DartCore.getApplicationDirectory(resource);
    // remove till application directory
    // L/sample/web/sample.dart => web/sample.dart

    if (appDir != null) {
      String regex = resource.getProjectRelativePath().removeFirstSegments(
          appDir.getProjectRelativePath().segmentCount()).toPortableString();

      // remove the pub serve root dir for the resource
      // web/sample.dart => sample.dart
      // http://127.0.0.1:8080/sample.html
      String rootdir = PubServeManager.getManager().getPubServeRootDir(appDir, resource);
      if (rootdir != null) {
        regex = regex.substring(rootdir.length());
      }
      // for BreakpointManager to process the regex
      if (!regex.startsWith("/")) {
        regex = "/" + regex;
      }
      return regex;
    }
    return null;
  }

  @Override
  public IResource resolveUrl(String url) {

    String assetId = urlToAsset.get(url);
    if (assetId != null) {
      return getResourceForPath(assetId);
    }

    CountDownLatch latch = new CountDownLatch(1);
    final String[] name = new String[1];
    final String[] path = new String[1];

    try {
      PubServeManager.getManager().sendGetAssetIdCommand(
          url,
          new FilePathForUrlCallback(latch, name, path));
    } catch (IOException e) {
      DartCore.logError(e);
      return null;
    }
    try {
      latch.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // do nothing
    }
    if (path[0] != null) {
      urlToAsset.put(url, path[0]);
      return getResourceForPath(path[0]);
    }
    return null;
  }

  private IResource getResourceForPath(final String path) {
    IContainer appDir = PubServeManager.getManager().getCurrentServeWorkingDir();
    // TODO(keertip): check if appdir has pubfolder with same pubspec name
    IResource resource = appDir.findMember(path);
    return resource;
  }

}
