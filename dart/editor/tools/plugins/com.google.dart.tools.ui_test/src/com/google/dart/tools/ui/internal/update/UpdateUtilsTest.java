/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.update;

import com.google.dart.tools.update.core.internal.UpdateUtils;

import junit.framework.TestCase;

public class UpdateUtilsTest extends TestCase {

  public void testParseRevisionJSON() throws Exception {

    // {
    //   "revision" : "9826",
    //   "version"  : "0.0.1_v2012070961811",
    //   "date"     : "2012-07-09"
    // }  
    String json = new StringBuilder().append("{ \"revision\": \"9826\", ").append(
        "\"version\": \"0.0.1_v2012070961811\", ").append("\"date\": \"2012-07-09\"").append(" }").toString();

    String revision = UpdateUtils.parseRevisionNumberFromJSON(json);
    assertEquals(revision, "9826");

  }
}
