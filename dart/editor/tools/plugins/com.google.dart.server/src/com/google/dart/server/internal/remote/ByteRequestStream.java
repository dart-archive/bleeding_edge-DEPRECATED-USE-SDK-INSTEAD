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

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * An {@link OutputStream} based implementation of {@link RequestSink}.
 */
public class ByteRequestStream implements RequestSink {
  /**
   * The {@link PrintWriter} to print JSON strings to.
   */
  private final PrintWriter writer;

  /**
   * Initializes a newly created request sink.
   * 
   * @param stream the byte stream to write JSON strings to
   */
  public ByteRequestStream(OutputStream stream) {
    writer = new PrintWriter(new OutputStreamWriter(stream, Charsets.UTF_8));
  }

  @Override
  public void add(JsonObject request) {
    String text = request.toString();
    writer.println(text);
  }
}
