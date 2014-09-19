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
package com.google.dart.engine.internal.index.file;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.utilities.logging.Logger;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class FileNodeManagerTest extends TestCase {
  private FileManager fileManager = mock(FileManager.class);
  private Logger logger = mock(Logger.class);
  private AnalysisContext context = mock(AnalysisContext.class);
  private int contextId = 13;
  private ContextCodec contextCodec = mock(ContextCodec.class);
  private StringCodec stringCodec = new StringCodec();
  private ElementCodec elementCodec = mock(ElementCodec.class);
  private int nextElementId = 0;
  private RelationshipCodec relationshipCodec = new RelationshipCodec(stringCodec);
  private FileNodeManager nodeManager = new FileNodeManager(
      fileManager,
      logger,
      stringCodec,
      contextCodec,
      elementCodec,
      relationshipCodec);

  public void test_clear() throws Exception {
    nodeManager.clear();
    verify(fileManager, times(1)).clear();
  }

  public void test_getContextCodec() throws Exception {
    assertSame(contextCodec, nodeManager.getContextCodec());
  }

  public void test_getElementCodec() throws Exception {
    assertSame(elementCodec, nodeManager.getElementCodec());
  }

  public void test_getLocationCount_empty() throws Exception {
    assertEquals(0, nodeManager.getLocationCount());
  }

  public void test_getNode_contextNull() throws Exception {
    String name = "42.index";
    // prepare output stream
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    when(fileManager.openOutputStream(name)).thenReturn(outputStream);
    // put Node
    {
      IndexNode node = new IndexNode(context, elementCodec, relationshipCodec);
      nodeManager.putNode(name, node);
    }
    // force "null" context
    when(contextCodec.decode(contextId)).thenReturn(null);
    // prepare input stream
    InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    when(fileManager.openInputStream(name)).thenReturn(inputStream);
    // no Node
    IndexNode node = nodeManager.getNode(name);
    assertNull(node);
    // no exceptions
    verifyZeroInteractions(logger);
  }

  public void test_getNode_invalidVersion() throws Exception {
    String name = "42.index";
    // prepare a stream with an invalid version
    when(fileManager.openInputStream(name)).thenReturn(
        new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03, 0x04}));
    // no Node
    IndexNode node = nodeManager.getNode(name);
    assertNull(node);
    // failed
    verify(logger).logError(anyString(), any(IllegalStateException.class));
  }

  public void test_getNode_streamException() throws Exception {
    String name = "42.index";
    when(fileManager.openInputStream(name)).thenThrow(new Exception());
    // no Node
    IndexNode node = nodeManager.getNode(name);
    assertNull(node);
    // failed
    verify(logger).logError(anyString(), any(Throwable.class));
  }

  public void test_getNode_streamNull() throws Exception {
    String name = "42.index";
    when(fileManager.openInputStream(name)).thenReturn(null);
    // no Node
    IndexNode node = nodeManager.getNode(name);
    assertNull(node);
  }

  public void test_getStringCodec() throws Exception {
    assertSame(stringCodec, nodeManager.getStringCodec());
  }

  public void test_newNode() throws Exception {
    IndexNode node = nodeManager.newNode(context);
    assertSame(context, node.getContext());
    assertEquals(0, node.getLocationCount());
  }

  public void test_putNode_getNode() throws Exception {
    String name = "42.index";
    // prepare output stream
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    when(fileManager.openOutputStream(name)).thenReturn(outputStream);
    // prepare elements
    Element elementA = mockElement();
    Element elementB = mockElement();
    Element elementC = mockElement();
    Relationship relationship = Relationship.getRelationship("my-relationship");
    // put Node
    {
      // prepare relations
      int elementIdA = 0;
      int elementIdB = 1;
      int elementIdC = 2;
      int relationshipId = relationshipCodec.encode(relationship);
      RelationKeyData key = new RelationKeyData(elementIdA, relationshipId);
      List<LocationData> locations = Lists.newArrayList(
          new LocationData(elementIdB, 1, 10),
          new LocationData(elementIdC, 2, 20));
      Map<RelationKeyData, List<LocationData>> relations = ImmutableMap.of(key, locations);
      // prepare Node
      IndexNode node = new IndexNode(context, elementCodec, relationshipCodec);
      node.setRelations(relations);
      // put Node
      nodeManager.putNode(name, node);
    }
    // has locations
    assertEquals(2, nodeManager.getLocationCount());
    // prepare input stream
    InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    when(fileManager.openInputStream(name)).thenReturn(inputStream);
    // get Node
    IndexNode node = nodeManager.getNode(name);
    assertEquals(2, node.getLocationCount());
    {
      Location[] locations = node.getRelationships(elementA, relationship);
      assertThat(locations).hasSize(2);
      assertHasLocation(locations, elementB, 1, 10);
      assertHasLocation(locations, elementC, 2, 20);
    }
  }

  public void test_putNode_streamException() throws Exception {
    String name = "42.index";
    when(fileManager.openOutputStream(name)).thenThrow(new Exception());
    // try to put
    IndexNode node = mock(IndexNode.class);
    nodeManager.putNode(name, node);
    // failed
    verify(logger).logError(anyString(), any(Throwable.class));
  }

  public void test_removeNode() throws Exception {
    String name = "42.index";
    nodeManager.removeNode(name);
    verify(fileManager).delete(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(contextCodec.encode(context)).thenReturn(contextId);
    when(contextCodec.decode(contextId)).thenReturn(context);
  }

  @Override
  protected void tearDown() throws Exception {
    fileManager = null;
    stringCodec = null;
    elementCodec = null;
    relationshipCodec = null;
    nodeManager = null;
    super.tearDown();
  }

  private void assertHasLocation(Location[] locations, Element element, int offset, int length) {
    for (Location location : locations) {
      if (Objects.equal(location.getElement(), element) && location.getOffset() == offset
          && location.getLength() == length) {
        return;
      }
    }
    fail("Expected to find Location(element=" + element + ", offset=" + offset + ", length="
        + length + ")");
  }

  private Element mockElement() {
    int elementId = nextElementId++;
    Element element = mock(Element.class);
    when(elementCodec.encode(element)).thenReturn(elementId);
    when(elementCodec.decode(context, elementId)).thenReturn(element);
    return element;
  }
}
