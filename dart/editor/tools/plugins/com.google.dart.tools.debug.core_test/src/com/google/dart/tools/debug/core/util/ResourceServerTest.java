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

package com.google.dart.tools.debug.core.util;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.core.utilities.net.URIUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;

import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ResourceServerTest extends TestCase {
  private ResourceServer server;
  private TestProject project;

  public void test_404MissingResource() throws Exception {
    IFile file = project.setFileContent("foo.txt", "foo");
    String url = server.getUrlForResource(file) + "s";

    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

    assertEquals(404, connection.getResponseCode());

    connection.disconnect();
  }

  public void test_canServeResource() throws Exception {
    IFile file = project.setFileContent("foo.txt", "foo");
    String url = server.getUrlForResource(file);

    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

    assertEquals(200, connection.getResponseCode());
    assertEquals(3, connection.getContentLength());
    assertEquals("text/plain", connection.getContentType());
    assertEquals(
        "foo",
        CharStreams.toString(new InputStreamReader(connection.getInputStream(), "UTF-8")));

    connection.disconnect();
    connection.getInputStream().close();
  }

  public void test_onlyServeWorkspaceFiles() throws Exception {
    File file = File.createTempFile("foo", ".txt");
    Files.write("foo", file, Charsets.UTF_8);
    file.deleteOnExit();

    String filePath = file.getAbsolutePath();
    if (filePath.startsWith("/")) {
      filePath = filePath.substring(1);
    }
    String url = "http://localhost:" + server.getPort() + "/" + URIUtilities.uriEncode(filePath);

    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

    assertEquals(404, connection.getResponseCode());

    connection.disconnect();
    file.delete();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    server = new ResourceServer();
    project = new TestProject();
  }

  @Override
  protected void tearDown() throws Exception {
    project.dispose();
    server.shutdown();

    super.tearDown();
  }

}
