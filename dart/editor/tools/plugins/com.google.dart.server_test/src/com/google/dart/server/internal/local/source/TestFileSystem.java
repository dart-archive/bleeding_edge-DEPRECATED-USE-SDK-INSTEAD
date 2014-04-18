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

package com.google.dart.server.internal.local.source;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.UriKind;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Map;

/**
 * In-memory {@link Resource}s provider.
 */
public class TestFileSystem {
  private class TestResource implements Resource {
    private final String path;

    public TestResource(String path) {
      this.path = path;
    }

    @Override
    public Source createSource(UriKind uriKind) {
      return new TestResourceSource(this, uriKind);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof TestResource)) {
        return false;
      }
      TestResource other = (TestResource) obj;
      return other.path.equals(path);
    }

    @Override
    public boolean exists() {
      return pathToResource.containsKey(path);
    }

    @Override
    public Resource getChild(String path) {
      if (path.contains("..")) {
        throw new IllegalArgumentException("'..' is not supported");
      }
      return new TestResource(this.path + "/" + path);
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public int hashCode() {
      return path.hashCode();
    }

    @Override
    public String toString() {
      return path;
    }
  }

  private class TestResourceSource implements Source {
    private final TestResource resource;
    private final UriKind uriKind;

    public TestResourceSource(TestResource resource, UriKind uriKind) {
      this.resource = resource;
      this.uriKind = uriKind;
    }

    @Override
    public boolean exists() {
      return resource.exists();
    }

    @Override
    public TimestampedData<CharSequence> getContents() throws Exception {
      byte[] bytes = resourceToContents.get(resource);
      return new TimestampedData<CharSequence>(getModificationStamp(), new String(
          bytes,
          Charsets.UTF_8));
    }

    @Override
    @Deprecated
    public void getContentsToReceiver(ContentReceiver receiver) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getEncoding() {
      return resource.path;
    }

    @Override
    public String getFullName() {
      return resource.path;
    }

    @Override
    public long getModificationStamp() {
      return resourceToModificationStamp.get(resource);
    }

    @Override
    public String getShortName() {
      return resource.path;
    }

    @Override
    public UriKind getUriKind() {
      return uriKind;
    }

    @Override
    public boolean isInSystemLibrary() {
      return false;
    }

    @Override
    public Source resolveRelative(URI relativeUri) {
      return null;
    }

    @Override
    public String toString() {
      return getFullName();
    }
  }

  private final Map<String, TestResource> pathToResource = Maps.newHashMap();
  private final Map<TestResource, byte[]> resourceToContents = Maps.newHashMap();
  private final Map<TestResource, Long> resourceToModificationStamp = Maps.newHashMap();

  /**
   * Creates a new directory.
   * 
   * @param path the {@code '/'} separated path
   */
  public Resource newDirectory(String path) {
    return newFile_(path);
  }

  /**
   * Creates a new file.
   * 
   * @param path the {@code '/'} separated path
   */
  public Resource newFile(Resource parent, String path, String contents) {
    path = ((TestResource) parent).path + "/" + path;
    return newFile(path, contents);
  }

  /**
   * Creates a new file.
   * 
   * @param path the {@code '/'} separated path
   */
  public Resource newFile(String path, byte[] contents) {
    TestResource resource = newFile_(path);
    setFileContents(resource, contents);
    return resource;
  }

  /**
   * Creates a new file.
   * 
   * @param path the {@code '/'} separated path
   */
  public Resource newFile(String path, String contents) {
    TestResource resource = newFile_(path);
    setFileContents(resource, contents);
    return resource;
  }

  /**
   * Sets the resource contents.
   */
  public void setFileContents(TestResource resource, byte[] contents) {
    resourceToContents.put(resource, contents);
  }

  /**
   * Sets the resource contents.
   */
  public void setFileContents(TestResource resource, String contents) {
    setFileContents(resource, contents.getBytes(Charsets.UTF_8));
  }

  /**
   * Increments {@link Resource} modification stamp.
   */
  public void touchFile(TestResource resource) {
    long stamp = resourceToModificationStamp.get(resource);
    resourceToModificationStamp.put(resource, stamp + 1);
  }

  private TestResource newFile_(String path) {
    if (StringUtils.isEmpty(path)) {
      throw new IllegalArgumentException("Empty path is not supported");
    }
    TestResource resource = null;
    String partialPath = "";
    for (String pathPart : StringUtils.split(path, '/')) {
      partialPath += "/" + pathPart;
      resource = pathToResource.get(partialPath);
      if (resource == null) {
        resource = new TestResource(partialPath);
        pathToResource.put(partialPath, resource);
      }
    }
    resourceToModificationStamp.put(resource, 0L);
    return resource;
  }
}
