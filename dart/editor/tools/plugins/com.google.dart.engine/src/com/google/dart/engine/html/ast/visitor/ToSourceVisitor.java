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
package com.google.dart.engine.html.ast.visitor;

import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.scanner.Token;

import java.io.PrintWriter;

/**
 * Instances of the class {@code ToSourceVisitor} write a source representation of a visited XML
 * node (and all of it's children) to a writer.
 * 
 * @coverage dart.engine.html
 */
public class ToSourceVisitor implements XmlVisitor<Void> {
  /**
   * The writer to which the source is to be written.
   */
  private PrintWriter writer;

  /**
   * Initialize a newly created visitor to write source code representing the visited nodes to the
   * given writer.
   * 
   * @param writer the writer to which the source is to be written
   */
  public ToSourceVisitor(PrintWriter writer) {
    this.writer = writer;
  }

  @Override
  public Void visitHtmlScriptTagNode(HtmlScriptTagNode node) {
    return visitXmlTagNode(node);
  }

  @Override
  public Void visitHtmlUnit(HtmlUnit node) {
    for (XmlTagNode child : node.getTagNodes()) {
      visit(child);
    }
    return null;
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    String name = node.getName();
    Token value = node.getValueToken();
    if (name.length() == 0) {
      writer.print("__");
    } else {
      writer.print(name);
    }
    writer.print("=");
    if (value == null) {
      writer.print("__");
    } else {
      writer.print(value.getLexeme());
    }
    return null;
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    writer.print("<");
    String tagName = node.getTag();
    writer.print(tagName);
    for (XmlAttributeNode attribute : node.getAttributes()) {
      writer.print(" ");
      visit(attribute);
    }
    writer.print(node.getAttributeEnd().getLexeme());
    if (node.getClosingTag() != null) {
      for (XmlTagNode child : node.getTagNodes()) {
        visit(child);
      }
      writer.print("</");
      writer.print(tagName);
      writer.print(">");
    }
    return null;
  }

  /**
   * Safely visit the given node.
   * 
   * @param node the node to be visited
   */
  private void visit(XmlNode node) {
    if (node != null) {
      node.accept(this);
    }
  }
}
