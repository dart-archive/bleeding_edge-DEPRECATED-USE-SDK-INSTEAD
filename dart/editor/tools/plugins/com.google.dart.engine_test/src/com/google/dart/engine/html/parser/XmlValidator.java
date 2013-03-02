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
package com.google.dart.engine.html.parser;

import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.html.scanner.TokenType;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import static com.google.dart.engine.html.scanner.TokenType.EOF;

import junit.framework.Assert;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;

/**
 * Instances of {@code XmlValidator} traverse an {@link XmlNode} structure and validate the node
 * hierarchy.
 */
public class XmlValidator extends RecursiveXmlVisitor<Void> {

  public static class Attributes {
    final String[] keyValuePairs;

    public Attributes(String... keyValuePairs) {
      this.keyValuePairs = keyValuePairs;
    }
  }

  public static class Tag {
    final String tag;
    final Attributes attributes;
    final String content;
    final Tag[] children;

    public Tag(String tag, Attributes attributes, String content, Tag... children) {
      this.tag = tag;
      this.attributes = attributes;
      this.content = content;
      this.children = children;
    }
  }

  /**
   * A list containing the errors found while traversing the AST structure.
   */
  private ArrayList<String> errors = new ArrayList<String>();

  /**
   * The tags to expect when visiting or {@code null} if tags should not be checked.
   */
  private Tag[] expectedTagsInOrderVisited;

  /**
   * The current index into the {@link #expectedTagsInOrderVisited} array.
   */
  private int expectedTagsIndex;

  /**
   * The key/value pairs to expect when visiting or {@code null} if attributes should not be
   * checked.
   */
  private String[] expectedAttributeKeyValuePairs;

  /**
   * The current index into the {@link #expectedAttributeKeyValuePairs}.
   */
  private int expectedAttributeIndex;

  /**
   * Assert that no errors were found while traversing any of the AST structures that have been
   * visited.
   */
  public void assertValid() {
    while (expectedTagsIndex < expectedTagsInOrderVisited.length) {
      String expectedTag = expectedTagsInOrderVisited[expectedTagsIndex++].tag;
      errors.add("Expected to visit node with tag: " + expectedTag);
    }
    if (!errors.isEmpty()) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Invalid XML structure:");
      for (String message : errors) {
        writer.println();
        writer.print("   ");
        writer.print(message);
      }
      Assert.fail(writer.toString());
    }
  }

  /**
   * Set the tags to be expected when visiting
   * 
   * @param expectedTags the expected tags
   */
  public void expectTags(Tag... expectedTags) {
    // Flatten the hierarchy into expected order in which the tags are visited
    ArrayList<Tag> expected = new ArrayList<Tag>();
    expectTags(expected, expectedTags);
    this.expectedTagsInOrderVisited = expected.toArray(new Tag[expected.size()]);
  }

  @Override
  public Void visitHtmlUnit(HtmlUnit node) {
    if (node.getParent() != null) {
      errors.add("HtmlUnit should not have a parent");
    }
    if (node.getEndToken().getType() != EOF) {
      errors.add("HtmlUnit end token should be of type EOF");
    }
    validateNode(node);
    return super.visitHtmlUnit(node);
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode actual) {
    if (!(actual.getParent() instanceof XmlTagNode)) {
      errors.add("Expected " + actual.getClass().getSimpleName() + " to have parent of type "
          + XmlTagNode.class.getSimpleName());
    }

    String actualName = actual.getName().getLexeme();
    String actualValue = actual.getValue().getLexeme();
    if (expectedAttributeIndex < expectedAttributeKeyValuePairs.length) {
      String expectedName = expectedAttributeKeyValuePairs[expectedAttributeIndex];
      if (!expectedName.equals(actualName)) {
        errors.add("Expected " + (expectedTagsIndex - 1) + " tag: "
            + expectedTagsInOrderVisited[expectedTagsIndex - 1].tag + " attribute "
            + (expectedAttributeIndex / 2) + " to have name: " + expectedName + " but found: "
            + actualName);
      }
      String expectedValue = expectedAttributeKeyValuePairs[expectedAttributeIndex + 1];
      if (!expectedValue.equals(actualValue)) {
        errors.add("Expected " + (expectedTagsIndex - 1) + " tag: "
            + expectedTagsInOrderVisited[expectedTagsIndex - 1].tag + " attribute "
            + (expectedAttributeIndex / 2) + " to have value: " + expectedValue + " but found: "
            + actualValue);
      }
    } else {
      errors.add("Unexpected " + (expectedTagsIndex - 1) + " tag: "
          + expectedTagsInOrderVisited[expectedTagsIndex - 1].tag + " attribute "
          + (expectedAttributeIndex / 2) + " name: " + actualName + " value: " + actualValue);
    }
    expectedAttributeIndex += 2;

    validateNode(actual);
    return super.visitXmlAttributeNode(actual);
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode actual) {
    if (!(actual.getParent() instanceof HtmlUnit || actual.getParent() instanceof XmlTagNode)) {
      errors.add("Expected " + actual.getClass().getSimpleName() + " to have parent of type "
          + HtmlUnit.class.getSimpleName() + " or " + XmlTagNode.class.getSimpleName());
    }
    if (expectedTagsInOrderVisited != null) {
      String actualTag = actual.getTag().getLexeme();
      if (expectedTagsIndex < expectedTagsInOrderVisited.length) {

        Tag expected = expectedTagsInOrderVisited[expectedTagsIndex];
        if (!expected.tag.equals(actualTag)) {
          errors.add("Expected " + expectedTagsIndex + " tag: " + expected.tag + " but found: "
              + actualTag);
        }

        expectedAttributeKeyValuePairs = expected.attributes.keyValuePairs;
        int expectedAttributeCount = expectedAttributeKeyValuePairs.length / 2;
        int actualAttributeCount = actual.getAttributes().size();
        if (expectedAttributeCount != actualAttributeCount) {
          errors.add("Expected " + expectedTagsIndex + " tag: " + expected.tag + " to have "
              + expectedAttributeCount + " attributes but found " + actualAttributeCount);
        }
        expectedAttributeIndex = 0;
        expectedTagsIndex++;

        assertNotNull(actual.getAttributeEnd());
        assertNotNull(actual.getContentEnd());
        int count = 0;
        Token token = actual.getAttributeEnd().getNext();
        Token lastToken = actual.getContentEnd();
        while (token != lastToken) {
          token = token.getNext();
          if (++count > 1000) {
            Assert.fail("Expected " + expectedTagsIndex + " tag: " + expected.tag
                + " to have a sequence of tokens from getAttributeEnd() to getContentEnd()");
            break;
          }
        }
        if (actual.getAttributeEnd().getType() == TokenType.GT) {
          if (HtmlParser.SELF_CLOSING.contains(actual.getTag().getLexeme())) {
            assertNull(actual.getClosingTag());
          } else {
            assertNotNull(actual.getClosingTag());
          }
        } else if (actual.getAttributeEnd().getType() == TokenType.SLASH_GT) {
          assertNull(actual.getClosingTag());
        } else {
          Assert.fail("Unexpected attribute end token: " + actual.getAttributeEnd().getLexeme());
        }

        if (expected.content != null && !expected.content.equals(actual.getContent())) {
          errors.add("Expected " + expectedTagsIndex + " tag: " + expected.tag
              + " to have content '" + expected.content + "' but found '" + actual.getContent()
              + "'");
        }

        if (expected.children.length != actual.getTagNodes().size()) {
          errors.add("Expected " + expectedTagsIndex + " tag: " + expected.tag + " to have "
              + expected.children.length + " children but found " + actual.getTagNodes().size());
        } else {
          for (int index = 0; index < expected.children.length; index++) {
            String expectedChildTag = expected.children[index].tag;
            String actualChildTag = actual.getTagNodes().get(index).getTag().getLexeme();
            if (!expectedChildTag.equals(actualChildTag)) {
              errors.add("Expected " + expectedTagsIndex + " tag: " + expected.tag + " child "
                  + index + " to have tag: " + expectedChildTag + " but found: " + actualChildTag);
            }
          }
        }
      } else {
        errors.add("Visited unexpected tag: " + actualTag);
      }
    }
    validateNode(actual);
    return super.visitXmlTagNode(actual);
  }

  /**
   * Append the specified tags to the array in depth first order
   * 
   * @param expected the array to which the tags are added (not {@code null})
   * @param expectedTags the expected tags to be added (not {@code null}, contains no {@code null}s)
   */
  private void expectTags(ArrayList<Tag> expected, Tag... expectedTags) {
    for (Tag tag : expectedTags) {
      expected.add(tag);
      expectTags(expected, tag.children);
    }
  }

  private void validateNode(XmlNode node) {

    if (node.getBeginToken() == null) {
      errors.add("No begin token for " + node.getClass().getName());
    }
    if (node.getEndToken() == null) {
      errors.add("No end token for " + node.getClass().getName());
    }

    int nodeStart = node.getOffset();
    int nodeLength = node.getLength();
    if (nodeStart < 0 || nodeLength < 0) {
      errors.add("No source info for " + node.getClass().getName());
    }

    XmlNode parent = node.getParent();
    if (parent != null) {
      int nodeEnd = nodeStart + nodeLength;
      int parentStart = parent.getOffset();
      int parentEnd = parentStart + parent.getLength();
      if (nodeStart < parentStart) {
        errors.add("Invalid source start (" + nodeStart + ") for " + node.getClass().getName()
            + " inside " + parent.getClass().getName() + " (" + parentStart + ")");
      }
      if (nodeEnd > parentEnd) {
        errors.add("Invalid source end (" + nodeEnd + ") for " + node.getClass().getName()
            + " inside " + parent.getClass().getName() + " (" + parentStart + ")");
      }
    }
  }
}
