/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.frog;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class FrogServerTest extends TestCase {
  private final Object lock = new Object();
  private FrogServer server;
  protected JSONObject response;

  public void test_FrogServer_compile() throws Exception {
    server.compile(new Path("path/to/dart/app/file.dart"), new ResponseHandler() {
      @Override
      public void response(JSONObject response) throws IOException, JSONException {
        FrogServerTest.this.response = response;
        synchronized (lock) {
          lock.notifyAll();
        }
      }
    });
    synchronized (lock) {
      lock.wait(3000);
    }
    assertNotNull(response);
    server.compile(new Path("path/to/dart/app/file2.dart"), new ResponseHandler() {
      @Override
      public void response(JSONObject response) throws IOException, JSONException {
        FrogServerTest.this.response = response;
        synchronized (lock) {
          lock.notifyAll();
        }
      }
    });
    synchronized (lock) {
      lock.wait(3000);
    }
    assertNotNull(response);
  }

  @Override
  protected void setUp() throws Exception {
    server = new FrogServer();
  }

  @Override
  protected void tearDown() throws Exception {
    if (server != null) {
      server.shutdown();
    }
  }
}
