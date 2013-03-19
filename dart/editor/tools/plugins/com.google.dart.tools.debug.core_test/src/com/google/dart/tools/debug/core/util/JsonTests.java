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

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * These tests exits to speed test the json parser we use.
 */
public class JsonTests extends TestCase {

  public void test_parse1() throws IOException, JSONException {
    InputStream in = JsonTests.class.getResourceAsStream("test1.json.gz");
    GZIPInputStream gzipIn = new GZIPInputStream(in);
    String string = CharStreams.toString(new InputStreamReader(gzipIn, Charsets.UTF_8));

    int iterations = 1;

    for (int i = 0; i < iterations; i++) {
      @SuppressWarnings("unused")
      JSONObject obj = new JSONObject(string);
    }
  }

  public void xxx_test_parse100() throws IOException, JSONException {
    InputStream in = JsonTests.class.getResourceAsStream("test1.json.gz");
    GZIPInputStream gzipIn = new GZIPInputStream(in);
    String string = CharStreams.toString(new InputStreamReader(gzipIn, Charsets.UTF_8));

    long start = System.nanoTime();

    int iterations = 100;

    for (int i = 0; i < iterations; i++) {
      @SuppressWarnings("unused")
      JSONObject obj = new JSONObject(string);
    }

    long elapsed = System.nanoTime() - start;

    double mb = (string.length() * iterations) / (1024.0 * 1024.0);
    double sec = elapsed / (1000 * 1000 * 1000.0);

    System.out.println(String.format("%.2f MB/sec", (mb / sec)));
  }

}
