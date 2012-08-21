/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.dart2js;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JsonServerTest extends TestCase {
  private CountDownLatch doneLatch;
  private List<String> messages;

  public void test_JsonServer_compile() throws Exception {
    doneLatch = new CountDownLatch(1);
    messages = new ArrayList<String>();
    String path = "path/to/non/existant/dart/app/file.dart";
    JsonServerManager.getServer().compile(new Path(path), null, new ResponseHandler() {

      @Override
      public void processDone(ResponseDone done) {
        messages.add("done: " + done.isSuccess());
        doneLatch.countDown();
      }

      @Override
      public void processMessage(ResponseMessage message) {
        messages.add(message.getMessage());
      }
    });
    doneLatch.await(3000, TimeUnit.MILLISECONDS);
    assertEquals(0, doneLatch.getCount());
    assertEquals("File not found: " + path, messages.remove(0));
    assertEquals("no main method specified", messages.remove(0));
    assertEquals("done: false", messages.remove(0));
  }

  @Override
  protected void tearDown() throws Exception {
    JsonServerManager.shutdown();
  }
}
