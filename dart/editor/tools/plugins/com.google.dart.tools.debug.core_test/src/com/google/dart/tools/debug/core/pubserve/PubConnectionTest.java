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

package com.google.dart.tools.debug.core.pubserve;

import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.tools.core.generator.WebAppSample;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.test.util.PlainTestProject;
import com.google.dart.tools.core.utilities.net.NetUtils;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PubConnectionTest extends TestCase {

//  pub ==> {"id":1,"method":"pathToUrls","params":{"path":"web/foo.html"},"jsonrpc":"2.0"}
//  pub <== {"id":1,"result":{"urls":["http://127.0.0.1:8080/foo.html"]},"jsonrpc":"2.0"}
//  pub ==> {"id":2,"method":"urlToAssetId","params":{"url":"http://127.0.0.1:8080/foo.html"},"jsonrpc":"2.0"}
//  pub <== {"id":2,"result":{"path":"web/foo.html","package":"foo"},"jsonrpc":"2.0"}
//  pub ==> {"id":3,"method":"serveDirectory","params":{"path":"test"},"jsonrpc":"2.0"}
//  pub <== {"id":3,"result":{"url":"http://127.0.0.1:8081"},"jsonrpc":"2.0"}

  private PubCallback<String> serveDirectoryCallback = new PubCallback<String>() {

    @Override
    public void handleResult(PubResult<String> result) {
      assertTrue(!result.isError());
      assertTrue(result.getResult().matches("http://.*"));
      latch.countDown();
    }
  };

  private PubCallback<String> assetToUrlCallback = new PubCallback<String>() {

    @Override
    public void handleResult(PubResult<String> result) {
      assertTrue(!result.isError());
      assertTrue(result.getResult().matches("http://.*"));
      assertTrue(result.getResult().contains("foo.html"));
      latch.countDown();
    }
  };

  private PubCallback<PubAsset> urlToAssestCallback = new PubCallback<PubAsset>() {

    @Override
    public void handleResult(PubResult<PubAsset> result) {
      assertTrue(!result.isError());
      assertEquals("foo", result.getResult().getPackageStr());
      assertEquals("web/foo.html", result.getResult().getPath());
      latch.countDown();
    }
  };

  protected PubConnection connection;
  private PlainTestProject testProject;
  private Process process;
  private String port;
  private PubConnection pubConnection;
  private StringBuilder stdOut = new StringBuilder();

  private static CountDownLatch latch;

  /**
   * An integration test of pub protocol support.
   */
  public void testPubProtocolIntegration() throws Exception {

    PubCommands command = pubConnection.getCommands();

    latch = new CountDownLatch(1);
    command.pathToUrl("web/foo.html", assetToUrlCallback);
    if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
      throw new Exception("No response from pub command assetIdToUrl");
    }

    latch = new CountDownLatch(1);
    command.urlToAssetId("http://localhost:8080/foo.html", urlToAssestCallback);
    if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
      throw new Exception("No response from pub command urlToAssestId");
    }

    // TODO(keertip): get test passing and enable
    latch = new CountDownLatch(1);
    command.serveDirectory("test", serveDirectoryCallback);
    if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
      throw new Exception("No response from pub command serveDirectory");
    }

  }

  @Override
  protected void setUp() throws Exception {
    testProject = new PlainTestProject("webby");
    IProject project = testProject.getProject();

    WebAppSample generator = new WebAppSample();
    generator.generateInto(project, "foo");

    testProject.setFileContent("pubspec.lock", "packages:\n"
        + "  browser:\n    description: browser\n    source: hosted\n    version: \"0.10.0\"");

    testProject.createFolder("test");
    testProject.setFileContent("test/test.dart", "main() {}");

    List<String> args = buildPubServeCommand();

    ProcessBuilder builder = new ProcessBuilder();
    builder.command(args);

    builder.directory(project.getLocation().toFile());
    process = builder.start();

    Thread stdoutThread = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          copyStream(process.getInputStream(), stdOut);
        } catch (IOException e) {
          processDestroy();
          fail("Exception while reading pub serve stdout");
        }
      }
    });
    stdoutThread.start();

    long endTime = System.currentTimeMillis() + 3 * 1000;

    while (!isTerminated() && !stdOut.toString().contains("127.0.0.1")
        && System.currentTimeMillis() < endTime) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException exception) {
        // do nothing, continue
      }
    }
    if (isTerminated()) {
      fail("Pub serve process terminated");
    }
    if (!stdOut.toString().contains("127.0.0.1")) {
      fail("Timeout: pub serve did not start in 3 secs");
      processDestroy();
    }
    pubConnection = new PubConnection(new URI("ws://127.0.0.1:" + port + "/"));
    pubConnection.connect();

  }

  @Override
  protected void tearDown() throws Exception {

    try {
      testProject.dispose();
    } catch (Exception e) {
      processDestroy();
      fail("Exception while deleting test project");
    }
    processDestroy();

  }

  private List<String> buildPubServeCommand() {
    DirectoryBasedDartSdk sdk = DartSdkManager.getManager().getSdk();
    File pubFile = sdk.getPubExecutable();
    List<String> args = new ArrayList<String>();
    args.add(pubFile.getAbsolutePath());
    args.add("serve");
    args.add("--admin-port");
    port = Integer.toString(NetUtils.findUnusedPort(8080));
    args.add(port);
    args.add("--hostname");
    args.add("127.0.0.1");
    //  args.add("--verbose");
    return args;
  }

  private void copyStream(InputStream in, StringBuilder stringBuilder) throws IOException {
    byte[] buffer = new byte[2048];
    try {
      int count = in.read(buffer);
      while (count != -1) {
        if (count > 0) {
          String str = new String(buffer, 0, count);
          stringBuilder.append(str);
        }
        count = in.read(buffer);
      }
      in.close();
    } catch (IOException ioe) {
      in.close();
      process.destroy();
      fail("IOException while reading pub serve stream");
    }
  }

  private boolean isTerminated() {
    try {
      if (process != null) {
        process.exitValue();
      }
    } catch (IllegalThreadStateException exception) {
      return false;
    }
    return true;
  }

  private void processDestroy() {
    if (process != null) {
      process.destroy();
    }
  }

}
