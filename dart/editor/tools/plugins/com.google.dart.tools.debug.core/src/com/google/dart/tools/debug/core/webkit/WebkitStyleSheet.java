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

package com.google.dart.tools.debug.core.webkit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A representation of a Webkit CSS style sheet. This is a more detailed representation of a style
 * sheet then the {@link WebkitStyleSheetRef} class.
 * 
 * @see WebkitStyleSheetRef
 */
public class WebkitStyleSheet {

  static WebkitStyleSheet createFrom(JSONObject obj) throws JSONException {
//  "styleSheet":{
//  "text":"h1 { font-size: 10pt }",
//  "styleSheetId":"1",
//  "rules":[
//     {
//        "sourceLine":3,
//        "style":{ ... },
//        "sourceURL":"http://0.0.0.0:3030/Users/dcarew/projects/dart/dart/samples/clock/Clock.html",
//        "selectorText":"h1",
//        "ruleId":{
//           "ordinal":0,
//           "styleSheetId":"1"
//        },
//        "origin":"regular",
//        "selectorRange":{
//           "start":0,
//           "end":2
//        }
//     }
//  ]
//}

    WebkitStyleSheet styleSheet = new WebkitStyleSheet();

    styleSheet.styleSheetId = JsonUtils.getString(obj, "styleSheetId");
    styleSheet.text = JsonUtils.getString(obj, "text");

    // TODO: parse rules[].sourceLine and rules[].sourceURL

    return styleSheet;
  }

  private String styleSheetId;

  private String text;

  public String getStyleSheetId() {
    return styleSheetId;
  }

  public String getText() {
    return text;
  }

  @Override
  public String toString() {
    return "[" + styleSheetId + "]";
  }

}
