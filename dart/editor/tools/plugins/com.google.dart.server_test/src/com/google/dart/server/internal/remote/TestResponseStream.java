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

package com.google.dart.server.internal.remote;

import com.google.common.base.Joiner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A test implementation of {@link ResponseStream}.
 */
public class TestResponseStream implements ResponseStream {
  private final BlockingQueue<JsonObject> responses = new LinkedBlockingQueue<JsonObject>();

  /**
   * Puts the given response into the queue.
   */
  public void put(JsonObject response) throws Exception {
    responses.put(response);
  }

  /**
   * Puts the given response into the queue.
   */
  public void put(String... lines) throws Exception {
    String json = Joiner.on('\n').join(lines);
    json = json.replace('\'', '"');
    JsonObject response = (JsonObject) new JsonParser().parse(json);
    put(response);
  }

  @Override
  public JsonObject take() throws Exception {
    return responses.take();
  }
}
