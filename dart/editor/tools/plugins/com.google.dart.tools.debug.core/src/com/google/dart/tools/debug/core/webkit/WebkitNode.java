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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * unused attrs:
 * 
 * contentDocument ( optional Node ) Content document for frame owner elements. name ( optional
 * string ) Attr's name. value ( optional string ) Attr's value.
 */

/**
 * A WIP DOM Node object.
 */
public class WebkitNode {

  // {"childNodeCount":3,"localName":"","nodeId":1,"documentURL":
  // "http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/samples/solar/solar.html",
  // "baseURL":"http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/samples/solar/solar.html",
  // "nodeValue":"","nodeName":"#document","xmlVersion":"","children":[{"localName":"","nodeId":2,
  // "internalSubset":"","publicId":"","nodeValue":"","nodeName":"html","systemId":"",
  // "nodeType":10},{"localName":"","nodeId":3,"nodeValue":" Copyright (c) 2012, ... CENSE file. ",
  // "nodeName":"","nodeType":8},{"childNodeCount":2,"localName":"html","nodeId":4,"nodeValue":"",
  // "nodeName":"HTML","children":[{"childNodeCount":3,"localName":"head","nodeId":5,"nodeValue":"",
  // "nodeName":"HEAD","attributes":[],"nodeType":1},{"childNodeCount":6,"localName":"body",
  // "nodeId":6,"nodeValue":"","nodeName":"BODY","attributes":[],"nodeType":1}],"attributes":[],
  // "nodeType":1}],"nodeType":9}

  public static class WebkitAttribute {
    static List<WebkitAttribute> createFrom(JSONArray arr) throws JSONException {
      List<WebkitAttribute> attributes = new ArrayList<WebkitAttribute>(arr.length());

      for (int i = 0; i < arr.length();) {
        WebkitAttribute attribute = new WebkitAttribute();

        attribute.name = arr.getString(i++);
        attribute.value = arr.getString(i++);

        attributes.add(attribute);
      }

      return attributes;
    }

    public String name;
    public String value;

    public WebkitAttribute() {

    }

    public WebkitAttribute(String name, String value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public String toString() {
      return name + "=" + value;
    }
  }

  static List<WebkitNode> createFrom(JSONArray arr) throws JSONException {
    List<WebkitNode> children = new ArrayList<WebkitNode>(arr.length());

    for (int i = 0; i < arr.length(); i++) {
      children.add(createFrom(arr.getJSONObject(i)));
    }

    return children;
  }

  static WebkitNode createFrom(JSONObject params) throws JSONException {
    WebkitNode node = new WebkitNode();

    node.baseURL = params.optString("baseURL");
    node.documentURL = params.optString("documentURL");
    node.nodeName = params.optString("nodeName");
    node.nodeId = params.optInt("nodeId", -1);
    node.nodeType = params.optInt("nodeType", -1);
    node.nodeValue = params.optString("nodeValue");
    node.localName = params.optString("localName");
    node.publicId = params.optString("publicId");
    node.systemId = params.optString("systemId");
    node.xmlVersion = params.optString("xmlVersion");
    node.childNodeCount = params.optInt("childNodeCount", 0);
    node.internalSubset = params.optString("internalSubset");

    if (params.has("children")) {
      node.children = createFrom(params.getJSONArray("children"));
    } else {
      node.children = Collections.emptyList();
    }

    if (params.has("attributes")) {
      node.attributes = WebkitAttribute.createFrom(params.getJSONArray("attributes"));
    } else {
      node.attributes = Collections.emptyList();
    }

    return node;
  }

  private int nodeType;

  private String nodeValue;

  private String localName;

  private String publicId;

  private String systemId;

  private String xmlVersion;

  private int childNodeCount;

  private String internalSubset;

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

  private List<WebkitNode> children;

  private List<WebkitAttribute> attributes;

  public List<WebkitAttribute> getAttributes() {
    return attributes;
  }

  public String getBaseURL() {
    return baseURL;
  }

  public int getChildNodeCount() {
    return childNodeCount;
  }

  public List<WebkitNode> getChildren() {
    return children;
  }

  public String getDocumentURL() {
    return documentURL;
  }

  public String getInternalSubset() {
    return internalSubset;
  }

  public String getLocalName() {
    return localName;
  }

  public int getNodeId() {
    return nodeId;
  }

  public String getNodeName() {
    return nodeName;
  }

  public int getNodeType() {
    return nodeType;
  }

  public String getNodeValue() {
    return nodeValue;
  }

  public String getPublicId() {
    return publicId;
  }

  public String getSystemId() {
    return systemId;
  }

  public String getXmlVersion() {
    return xmlVersion;
  }

  @Override
  public String toString() {
    return "[" + nodeName + "," + nodeId + "]";
  }

}
