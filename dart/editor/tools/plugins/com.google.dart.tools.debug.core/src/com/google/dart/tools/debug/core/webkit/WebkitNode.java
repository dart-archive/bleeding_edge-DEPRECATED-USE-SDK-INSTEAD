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
 * A WIP DOM Node object.
 */
public class WebkitNode {

  // {"childNodeCount":3,"localName":"","nodeId":1,"documentURL":"http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/samples/solar/solar.html","baseURL":"http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/samples/solar/solar.html","nodeValue":"","nodeName":"#document","xmlVersion":"","children":[{"localName":"","nodeId":2,"internalSubset":"","publicId":"","nodeValue":"","nodeName":"html","systemId":"","nodeType":10},{"localName":"","nodeId":3,"nodeValue":" Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file\n     for details. All rights reserved. Use of this source code is governed by a\n     BSD-style license that can be found in the LICENSE file. ","nodeName":"","nodeType":8},{"childNodeCount":2,"localName":"html","nodeId":4,"nodeValue":"","nodeName":"HTML","children":[{"childNodeCount":3,"localName":"head","nodeId":5,"nodeValue":"","nodeName":"HEAD","attributes":[],"nodeType":1},{"childNodeCount":6,"localName":"body","nodeId":6,"nodeValue":"","nodeName":"BODY","attributes":[],"nodeType":1}],"attributes":[],"nodeType":1}],"nodeType":9}

  public static WebkitNode createFrom(JSONObject params) throws JSONException {
    WebkitNode node = new WebkitNode();

    node.baseURL = params.optString("baseURL");
    node.documentURL = params.optString("documentURL");
    node.nodeName = params.optString("nodeName");
    node.nodeId = params.optInt("nodeId", -1);

    return node;
  }

  private String baseURL;

  /**
   * Document URL that Document or FrameOwner node points to.
   */
  private String documentURL;

  /**
   * Node identifier that is passed into the rest of the DOM messages as the nodeId. Backend will
   * only push node with given id once. It is aware of all requested nodes and will only fire DOM
   * events for nodes known to the client.
   */
  private int nodeId;

  /**
   * Node's nodeName.
   */
  private String nodeName;

  public String getBaseURL() {
    return baseURL;
  }

  public String getDocumentURL() {
    return documentURL;
  }

  public int getNodeId() {
    return nodeId;
  }

  public String getNodeName() {
    return nodeName;
  }

  @Override
  public String toString() {
    return "[" + nodeName + "," + nodeId + "," + documentURL + "]";
  }

}
