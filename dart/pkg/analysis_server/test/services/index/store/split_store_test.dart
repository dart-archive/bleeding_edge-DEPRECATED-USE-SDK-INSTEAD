// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.services.src.index.store.split_store;

import 'dart:async';

import 'package:analysis_server/src/services/index/index.dart';
import 'package:analysis_server/src/services/index/store/codec.dart';
import 'package:analysis_server/src/services/index/store/memory_node_manager.dart';
import 'package:analysis_server/src/services/index/store/split_store.dart';
import 'package:analyzer/src/generated/element.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/source.dart';
import 'package:typed_mock/typed_mock.dart';
import 'package:unittest/unittest.dart';

import '../../../mocks.dart';
import '../../../reflective_tests.dart';
import 'mocks.dart';
import 'single_source_container.dart';


main() {
  groupSep = ' | ';
  runReflectiveTests(_FileNodeManagerTest);
  runReflectiveTests(_IndexNodeTest);
  runReflectiveTests(_LocationDataTest);
  runReflectiveTests(_RelationKeyDataTest);
  runReflectiveTests(_SplitIndexStoreTest);
}


void _assertHasLocation(List<Location> locations, Element element, int offset,
    int length, {bool isQualified: false, bool isResolved: true}) {
  for (Location location in locations) {
    if ((element == null || location.element == element) &&
        location.offset == offset &&
        location.length == length &&
        location.isQualified == isQualified &&
        location.isResolved == isResolved) {
      return;
    }
  }
  fail(
      'Expected to find Location'
          '(element=$element, offset=$offset, length=$length)');
}


void _assertHasLocationQ(List<Location> locations, Element element, int offset,
    int length) {
  _assertHasLocation(locations, element, offset, length, isQualified: true);
}


@reflectiveTest
class _FileNodeManagerTest {
  MockLogger logger = new MockLogger();
  StringCodec stringCodec = new StringCodec();
  RelationshipCodec relationshipCodec;

  AnalysisContext context = new MockAnalysisContext('context');
  ContextCodec contextCodec = new MockContextCodec();
  int contextId = 13;

  ElementCodec elementCodec = new MockElementCodec();
  int nextElementId = 0;

  FileNodeManager nodeManager;
  FileManager fileManager = new _MockFileManager();

  void setUp() {
    relationshipCodec = new RelationshipCodec(stringCodec);
    nodeManager = new FileNodeManager(
        fileManager,
        logger,
        stringCodec,
        contextCodec,
        elementCodec,
        relationshipCodec);
    when(contextCodec.encode(context)).thenReturn(contextId);
    when(contextCodec.decode(contextId)).thenReturn(context);
  }

  void test_clear() {
    nodeManager.clear();
    verify(fileManager.clear()).once();
  }

  void test_getLocationCount_empty() {
    expect(nodeManager.locationCount, 0);
  }

  void test_getNode_contextNull() {
    String name = '42.index';
    // record bytes
    List<int> bytes;
    when(fileManager.write(name, anyObject)).thenInvoke((name, bs) {
      bytes = bs;
    });
    // put Node
    Future putFuture;
    {
      IndexNode node = new IndexNode(context, elementCodec, relationshipCodec);
      putFuture = nodeManager.putNode(name, node);
    }
    // do in the "put" Future
    putFuture.then((_) {
      // force "null" context
      when(contextCodec.decode(contextId)).thenReturn(null);
      // prepare input bytes
      when(fileManager.read(name)).thenReturn(new Future.value(bytes));
      // get Node
      return nodeManager.getNode(name).then((IndexNode node) {
        expect(node, isNull);
        // no exceptions
        verifyZeroInteractions(logger);
      });
    });
  }

  test_getNode_invalidVersion() {
    String name = '42.index';
    // prepare a stream with an invalid version
    when(
        fileManager.read(name)).thenReturn(new Future.value([0x01, 0x02, 0x03, 0x04]));
    // do in the Future
    return nodeManager.getNode(name).then((IndexNode node) {
      // no IndexNode
      expect(node, isNull);
      // failed
      verify(logger.logError(anyObject, anyObject)).once();
    });
  }

  test_getNode_streamException() {
    String name = '42.index';
    when(fileManager.read(name)).thenReturn(new Future(() {
      return throw new Exception();
    }));
    // do in the Future
    return nodeManager.getNode(name).then((IndexNode node) {
      expect(node, isNull);
      // failed
      verify(logger.logError(anyString, anyObject)).once();
    });
  }

  test_getNode_streamNull() {
    String name = '42.index';
    when(fileManager.read(name)).thenReturn(new Future.value(null));
    // do in the Future
    return nodeManager.getNode(name).then((IndexNode node) {
      expect(node, isNull);
      // OK
      verifyZeroInteractions(logger);
    });
  }

  void test_newNode() {
    IndexNode node = nodeManager.newNode(context);
    expect(node.context, context);
    expect(node.locationCount, 0);
  }

  test_putNode_getNode() {
    String name = '42.index';
    // record bytes
    List<int> bytes;
    when(fileManager.write(name, anyObject)).thenInvoke((name, bs) {
      bytes = bs;
    });
    // prepare elements
    Element elementA = _mockElement();
    Element elementB = _mockElement();
    Element elementC = _mockElement();
    Relationship relationship = Relationship.getRelationship('my-relationship');
    // put Node
    Future putFuture;
    {
      // prepare relations
      int elementIdA = 0;
      int elementIdB = 1;
      int elementIdC = 2;
      int relationshipId = relationshipCodec.encode(relationship);
      RelationKeyData key =
          new RelationKeyData.forData(elementIdA, relationshipId);
      List<LocationData> locations = [
          new LocationData.forData(elementIdB, 1, 10, 2),
          new LocationData.forData(elementIdC, 2, 20, 3)];
      Map<RelationKeyData, List<LocationData>> relations = {
        key: locations
      };
      // prepare Node
      IndexNode node = new _MockIndexNode();
      when(node.context).thenReturn(context);
      when(node.relations).thenReturn(relations);
      when(node.locationCount).thenReturn(2);
      // put Node
      putFuture = nodeManager.putNode(name, node);
    }
    // do in the Future
    putFuture.then((_) {
      // has locations
      expect(nodeManager.locationCount, 2);
      // prepare input bytes
      when(fileManager.read(name)).thenReturn(new Future.value(bytes));
      // get Node
      return nodeManager.getNode(name).then((IndexNode node) {
        expect(2, node.locationCount);
        {
          List<Location> locations =
              node.getRelationships(elementA, relationship);
          expect(locations, hasLength(2));
          _assertHasLocation(locations, elementB, 1, 10);
          _assertHasLocationQ(locations, elementC, 2, 20);
        }
      });
    });
  }

  test_putNode_streamException() {
    String name = '42.index';
    Exception exception = new Exception();
    when(fileManager.write(name, anyObject)).thenReturn(new Future(() {
      return throw exception;
    }));
    // prepare IndexNode
    IndexNode node = new _MockIndexNode();
    when(node.context).thenReturn(context);
    when(node.locationCount).thenReturn(0);
    when(node.relations).thenReturn({});
    // try to put
    return nodeManager.putNode(name, node).then((_) {
      // failed
      verify(logger.logError(anyString, anyObject)).once();
    });
  }

  void test_removeNode() {
    String name = '42.index';
    nodeManager.removeNode(name);
    verify(fileManager.delete(name)).once();
  }

  Element _mockElement() {
    int elementId = nextElementId++;
    Element element = new MockElement();
    when(elementCodec.encode(element, anyBool)).thenReturn(elementId);
    when(elementCodec.decode(context, elementId)).thenReturn(element);
    return element;
  }
}


@reflectiveTest
class _IndexNodeTest {
  AnalysisContext context = new MockAnalysisContext('context');
  ElementCodec elementCodec = new MockElementCodec();
  int nextElementId = 0;
  IndexNode node;
  RelationshipCodec relationshipCodec;
  StringCodec stringCodec = new StringCodec();

  void setUp() {
    relationshipCodec = new RelationshipCodec(stringCodec);
    node = new IndexNode(context, elementCodec, relationshipCodec);
  }

  void test_getContext() {
    expect(node.context, context);
  }

  void test_recordRelationship() {
    Element elementA = _mockElement();
    Element elementB = _mockElement();
    Element elementC = _mockElement();
    Relationship relationship = Relationship.getRelationship('my-relationship');
    Location locationA = new Location(elementB, 1, 2);
    Location locationB = new Location(elementC, 10, 20);
    // empty initially
    expect(node.locationCount, 0);
    // record
    node.recordRelationship(elementA, relationship, locationA);
    expect(node.locationCount, 1);
    node.recordRelationship(elementA, relationship, locationB);
    expect(node.locationCount, 2);
    // get relations
    expect(node.getRelationships(elementB, relationship), isEmpty);
    {
      List<Location> locations = node.getRelationships(elementA, relationship);
      expect(locations, hasLength(2));
      _assertHasLocation(locations, null, 1, 2);
      _assertHasLocation(locations, null, 10, 20);
    }
    // verify relations map
    {
      Map<RelationKeyData, List<LocationData>> relations = node.relations;
      expect(relations, hasLength(1));
      List<LocationData> locations = relations.values.first;
      expect(locations, hasLength(2));
    }
  }

  void test_setRelations() {
    Element elementA = _mockElement();
    Element elementB = _mockElement();
    Element elementC = _mockElement();
    Relationship relationship = Relationship.getRelationship('my-relationship');
    // record
    {
      int elementIdA = 0;
      int elementIdB = 1;
      int elementIdC = 2;
      int relationshipId = relationshipCodec.encode(relationship);
      RelationKeyData key =
          new RelationKeyData.forData(elementIdA, relationshipId);
      List<LocationData> locations = [
          new LocationData.forData(elementIdB, 1, 10, 2),
          new LocationData.forData(elementIdC, 2, 20, 3)];
      node.relations = {
        key: locations
      };
    }
    // request
    List<Location> locations = node.getRelationships(elementA, relationship);
    expect(locations, hasLength(2));
    _assertHasLocation(locations, elementB, 1, 10);
    _assertHasLocationQ(locations, elementC, 2, 20);
  }

  Element _mockElement() {
    int elementId = nextElementId++;
    Element element = new MockElement();
    when(elementCodec.encode(element, anyBool)).thenReturn(elementId);
    when(elementCodec.decode(context, elementId)).thenReturn(element);
    return element;
  }
}


@reflectiveTest
class _LocationDataTest {
  AnalysisContext context = new MockAnalysisContext('context');
  ElementCodec elementCodec = new MockElementCodec();
  StringCodec stringCodec = new StringCodec();

  void test_newForData() {
    Element element = new MockElement();
    when(elementCodec.decode(context, 0)).thenReturn(element);
    LocationData locationData = new LocationData.forData(0, 1, 2, 0);
    Location location = locationData.getLocation(context, elementCodec);
    expect(location.element, element);
    expect(location.offset, 1);
    expect(location.length, 2);
    expect(location.isQualified, isFalse);
    expect(location.isResolved, isFalse);
  }

  void test_newForObject() {
    // prepare Element
    Element element = new MockElement();
    when(elementCodec.encode(element, anyBool)).thenReturn(42);
    when(elementCodec.decode(context, 42)).thenReturn(element);
    // create
    Location location = new Location(element, 1, 2);
    LocationData locationData =
        new LocationData.forObject(elementCodec, location);
    // touch 'hashCode'
    locationData.hashCode;
    // ==
    expect(locationData == new LocationData.forData(42, 1, 2, 2), isTrue);
    // getLocation()
    {
      Location newLocation = locationData.getLocation(context, elementCodec);
      expect(newLocation.element, element);
      expect(newLocation.offset, 1);
      expect(newLocation.length, 2);
    }
    // no Element - no Location
    {
      when(elementCodec.decode(context, 42)).thenReturn(null);
      Location newLocation = locationData.getLocation(context, elementCodec);
      expect(newLocation, isNull);
    }
  }
}


/**
 * [Location] has no [==] and [hashCode], so to compare locations by value we
 * need to wrap them into such object.
 */
class _LocationEqualsWrapper {
  final Location location;

  _LocationEqualsWrapper(this.location);

  @override
  int get hashCode {
    return 31 * (31 * location.element.hashCode + location.offset) +
        location.length;
  }

  @override
  bool operator ==(Object other) {
    if (other is _LocationEqualsWrapper) {
      return other.location.offset == location.offset &&
          other.location.length == location.length &&
          other.location.element == location.element;
    }
    return false;
  }
}


class _MockFileManager extends TypedMock implements FileManager {
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}


class _MockIndexNode extends TypedMock implements IndexNode {
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}


@reflectiveTest
class _RelationKeyDataTest {
  AnalysisContext context = new MockAnalysisContext('context');
  ElementCodec elementCodec = new MockElementCodec();
  RelationshipCodec relationshipCodec = new MockRelationshipCodec();
  StringCodec stringCodec = new StringCodec();

  void test_newFromData() {
    RelationKeyData keyData = new RelationKeyData.forData(1, 2);
    // equals
    expect(keyData == this, isFalse);
    expect(keyData == new RelationKeyData.forData(10, 20), isFalse);
    expect(keyData == keyData, isTrue);
    expect(keyData == new RelationKeyData.forData(1, 2), isTrue);
  }

  void test_newFromObjects() {
    // prepare Element
    Element element;
    int elementId = 2;
    {
      element = new MockElement();
      ElementLocation location = new ElementLocationImpl.con3(['foo', 'bar']);
      when(element.location).thenReturn(location);
      when(context.getElement(location)).thenReturn(element);
      when(elementCodec.encode(element, anyBool)).thenReturn(elementId);
    }
    // prepare relationship
    Relationship relationship = Relationship.getRelationship('my-relationship');
    int relationshipId = 1;
    when(relationshipCodec.encode(relationship)).thenReturn(relationshipId);
    // create RelationKeyData
    RelationKeyData keyData = new RelationKeyData.forObject(
        elementCodec,
        relationshipCodec,
        element,
        relationship);
    // touch
    keyData.hashCode;
    // equals
    expect(keyData == this, isFalse);
    expect(keyData == new RelationKeyData.forData(10, 20), isFalse);
    expect(keyData == keyData, isTrue);
    expect(
        keyData == new RelationKeyData.forData(elementId, relationshipId),
        isTrue);
  }
}




@reflectiveTest
class _SplitIndexStoreTest {
  AnalysisContext contextA = new MockAnalysisContext('contextA');

  AnalysisContext contextB = new MockAnalysisContext('contextB');

  AnalysisContext contextC = new MockAnalysisContext('contextC');

  Element elementA = new MockElement('elementA');
  Element elementB = new MockElement('elementB');

  Element elementC = new MockElement('elementC');
  Element elementD = new MockElement('elementD');
  ElementLocation elementLocationA =
      new ElementLocationImpl.con3(['/home/user/sourceA.dart', 'ClassA']);
  ElementLocation elementLocationB =
      new ElementLocationImpl.con3(['/home/user/sourceB.dart', 'ClassB']);
  ElementLocation elementLocationC =
      new ElementLocationImpl.con3(['/home/user/sourceC.dart', 'ClassC']);
  ElementLocation elementLocationD =
      new ElementLocationImpl.con3(['/home/user/sourceD.dart', 'ClassD']);
  HtmlElement htmlElementA = new MockHtmlElement();
  HtmlElement htmlElementB = new MockHtmlElement();
  LibraryElement libraryElement = new MockLibraryElement();
  Source librarySource = new MockSource('librarySource');
  CompilationUnitElement libraryUnitElement = new MockCompilationUnitElement();
  MemoryNodeManager nodeManager = new MemoryNodeManager();
  Relationship relationship = Relationship.getRelationship('test-relationship');
  Source sourceA = new MockSource('sourceA');
  Source sourceB = new MockSource('sourceB');
  Source sourceC = new MockSource('sourceC');
  Source sourceD = new MockSource('sourceD');
  SplitIndexStore store;
  CompilationUnitElement unitElementA = new MockCompilationUnitElement();
  CompilationUnitElement unitElementB = new MockCompilationUnitElement();
  CompilationUnitElement unitElementC = new MockCompilationUnitElement();
  CompilationUnitElement unitElementD = new MockCompilationUnitElement();
  void setUp() {
    store = new SplitIndexStore(nodeManager);
    when(contextA.isDisposed).thenReturn(false);
    when(contextB.isDisposed).thenReturn(false);
    when(contextC.isDisposed).thenReturn(false);
    when(contextA.getElement(elementLocationA)).thenReturn(elementA);
    when(contextA.getElement(elementLocationB)).thenReturn(elementB);
    when(contextA.getElement(elementLocationC)).thenReturn(elementC);
    when(contextA.getElement(elementLocationD)).thenReturn(elementD);
    when(librarySource.fullName).thenReturn('/home/user/librarySource.dart');
    when(sourceA.fullName).thenReturn('/home/user/sourceA.dart');
    when(sourceB.fullName).thenReturn('/home/user/sourceB.dart');
    when(sourceC.fullName).thenReturn('/home/user/sourceC.dart');
    when(sourceD.fullName).thenReturn('/home/user/sourceD.dart');
    when(elementA.context).thenReturn(contextA);
    when(elementB.context).thenReturn(contextA);
    when(elementC.context).thenReturn(contextA);
    when(elementD.context).thenReturn(contextA);
    when(elementA.location).thenReturn(elementLocationA);
    when(elementB.location).thenReturn(elementLocationB);
    when(elementC.location).thenReturn(elementLocationC);
    when(elementD.location).thenReturn(elementLocationD);
    when(elementA.enclosingElement).thenReturn(unitElementA);
    when(elementB.enclosingElement).thenReturn(unitElementB);
    when(elementC.enclosingElement).thenReturn(unitElementC);
    when(elementD.enclosingElement).thenReturn(unitElementD);
    when(elementA.source).thenReturn(sourceA);
    when(elementB.source).thenReturn(sourceB);
    when(elementC.source).thenReturn(sourceC);
    when(elementD.source).thenReturn(sourceD);
    when(elementA.library).thenReturn(libraryElement);
    when(elementB.library).thenReturn(libraryElement);
    when(elementC.library).thenReturn(libraryElement);
    when(elementD.library).thenReturn(libraryElement);
    when(unitElementA.source).thenReturn(sourceA);
    when(unitElementB.source).thenReturn(sourceB);
    when(unitElementC.source).thenReturn(sourceC);
    when(unitElementD.source).thenReturn(sourceD);
    when(unitElementA.library).thenReturn(libraryElement);
    when(unitElementB.library).thenReturn(libraryElement);
    when(unitElementC.library).thenReturn(libraryElement);
    when(unitElementD.library).thenReturn(libraryElement);
    when(htmlElementA.source).thenReturn(sourceA);
    when(htmlElementB.source).thenReturn(sourceB);
    // library
    when(libraryUnitElement.library).thenReturn(libraryElement);
    when(libraryUnitElement.source).thenReturn(librarySource);
    when(libraryElement.source).thenReturn(librarySource);
    when(libraryElement.definingCompilationUnit).thenReturn(libraryUnitElement);
  }

  void test_aboutToIndexDart_disposedContext() {
    when(contextA.isDisposed).thenReturn(true);
    expect(store.aboutToIndexDart(contextA, unitElementA), isFalse);
  }

  Future test_aboutToIndexDart_library_first() {
    when(
        libraryElement.parts).thenReturn(
            <CompilationUnitElement>[unitElementA, unitElementB]);
    {
      store.aboutToIndexDart(contextA, libraryUnitElement);
      store.doneIndex();
    }
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, []);
    });
  }

  test_aboutToIndexDart_library_secondWithoutOneUnit() {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    // "A" and "B" locations
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
      // apply "libraryUnitElement", only with "B"
      when(libraryElement.parts).thenReturn([unitElementB]);
      {
        store.aboutToIndexDart(contextA, libraryUnitElement);
        store.doneIndex();
      }
    }).then((_) {
      return store.getRelationships(
          elementA,
          relationship).then((List<Location> locations) {
        assertLocations(locations, [locationB]);
      });
    });
  }

  void test_aboutToIndexDart_nullLibraryElement() {
    when(unitElementA.library).thenReturn(null);
    expect(store.aboutToIndexDart(contextA, unitElementA), isFalse);
  }

  void test_aboutToIndexDart_nullLibraryUnitElement() {
    when(libraryElement.definingCompilationUnit).thenReturn(null);
    expect(store.aboutToIndexDart(contextA, unitElementA), isFalse);
  }

  void test_aboutToIndexDart_nullUnitElement() {
    expect(store.aboutToIndexDart(contextA, null), isFalse);
  }

  test_aboutToIndexHtml_() {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexHtml(contextA, htmlElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexHtml(contextA, htmlElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    // "A" and "B" locations
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
    });
  }

  void test_aboutToIndexHtml_disposedContext() {
    when(contextA.isDisposed).thenReturn(true);
    expect(store.aboutToIndexHtml(contextA, htmlElementA), isFalse);
  }

  void test_clear() {
    Location locationA = mockLocation(elementA);
    store.aboutToIndexDart(contextA, unitElementA);
    store.recordRelationship(elementA, relationship, locationA);
    store.doneIndex();
    expect(nodeManager.isEmpty(), isFalse);
    // clear
    store.clear();
    expect(nodeManager.isEmpty(), isTrue);
  }

  test_getRelationships_empty() {
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      expect(locations, isEmpty);
    });
  }

  void test_getStatistics() {
    // empty initially
    {
      String statistics = store.statistics;
      expect(statistics, contains('0 locations'));
      expect(statistics, contains('0 sources'));
    }
    // add 2 locations
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    {
      String statistics = store.statistics;
      expect(statistics, contains('2 locations'));
      expect(statistics, contains('3 sources'));
    }
  }

  void test_recordRelationship_multiplyDefinedElement() {
    Element multiplyElement =
        new MultiplyDefinedElementImpl(contextA, <Element>[elementA, elementB]);
    Location location = mockLocation(elementA);
    store.recordRelationship(multiplyElement, relationship, location);
    store.doneIndex();
    expect(nodeManager.isEmpty(), isTrue);
  }

  void test_recordRelationship_nullElement() {
    Location locationA = mockLocation(elementA);
    store.recordRelationship(null, relationship, locationA);
    store.doneIndex();
    expect(nodeManager.isEmpty(), isTrue);
  }

  void test_recordRelationship_nullLocation() {
    store.recordRelationship(elementA, relationship, null);
    store.doneIndex();
    expect(nodeManager.isEmpty(), isTrue);
  }

  test_recordRelationship_oneElement_twoNodes() {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
    });
  }

  test_recordRelationship_oneLocation() {
    Location locationA = mockLocation(elementA);
    store.aboutToIndexDart(contextA, unitElementA);
    store.recordRelationship(elementA, relationship, locationA);
    store.doneIndex();
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA]);
    });
  }

  test_recordRelationship_twoLocations() {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementA);
    store.aboutToIndexDart(contextA, unitElementA);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementA, relationship, locationB);
    store.doneIndex();
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
    });
  }

  test_removeContext() {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    // "A" and "B" locations
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
      // remove "A" context
      store.removeContext(contextA);
    }).then((_) {
      return store.getRelationships(
          elementA,
          relationship).then((List<Location> locations) {
        assertLocations(locations, []);
      });
    });
  }

  void test_removeContext_nullContext() {
    store.removeContext(null);
  }

  test_removeSource_library() {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementC);
      store.recordRelationship(elementA, relationship, locationC);
      store.doneIndex();
    }
    // "A", "B" and "C" locations
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB, locationC]);
    }).then((_) {
      // remove "librarySource"
      store.removeSource(contextA, librarySource);
      return store.getRelationships(
          elementA,
          relationship).then((List<Location> locations) {
        assertLocations(locations, []);
      });
    });
  }

  void test_removeSource_nullContext() {
    store.removeSource(null, sourceA);
  }

  test_removeSource_unit() {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementC);
      store.recordRelationship(elementA, relationship, locationC);
      store.doneIndex();
    }
    // "A", "B" and "C" locations
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB, locationC]);
    }).then((_) {
      // remove "A" source
      store.removeSource(contextA, sourceA);
      return store.getRelationships(
          elementA,
          relationship).then((List<Location> locations) {
        assertLocations(locations, [locationB, locationC]);
      });
    });
  }

  test_removeSources_library() {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    // "A" and "B" locations
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
    }).then((_) {
      // remove "librarySource"
      store.removeSources(contextA, new SingleSourceContainer(librarySource));
      return store.getRelationships(
          elementA,
          relationship).then((List<Location> locations) {
        assertLocations(locations, []);
      });
    });
  }

  void test_removeSources_nullContext() {
    store.removeSources(null, null);
  }

  test_removeSources_unit() {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementC);
      store.recordRelationship(elementA, relationship, locationC);
      store.doneIndex();
    }
    // "A", "B" and "C" locations
    return store.getRelationships(
        elementA,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB, locationC]);
    }).then((_) {
      // remove "A" source
      store.removeSources(contextA, new SingleSourceContainer(sourceA));
      store.removeSource(contextA, sourceA);
      return store.getRelationships(
          elementA,
          relationship).then((List<Location> locations) {
        assertLocations(locations, [locationB, locationC]);
      });
    });
  }

  test_universe_aboutToIndex() {
    when(contextA.getElement(elementLocationA)).thenReturn(elementA);
    when(contextB.getElement(elementLocationB)).thenReturn(elementB);
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(
          UniverseElement.INSTANCE,
          relationship,
          locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextB, unitElementB);
      store.recordRelationship(
          UniverseElement.INSTANCE,
          relationship,
          locationB);
      store.doneIndex();
    }
    // get relationships
    return store.getRelationships(
        UniverseElement.INSTANCE,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
    }).then((_) {
      // re-index "unitElementA"
      store.aboutToIndexDart(contextA, unitElementA);
      store.doneIndex();
      return store.getRelationships(
          UniverseElement.INSTANCE,
          relationship).then((List<Location> locations) {
        assertLocations(locations, [locationB]);
      });
    });
  }

  test_universe_clear() {
    when(contextA.getElement(elementLocationA)).thenReturn(elementA);
    when(contextB.getElement(elementLocationB)).thenReturn(elementB);
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(
          UniverseElement.INSTANCE,
          relationship,
          locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(
          UniverseElement.INSTANCE,
          relationship,
          locationB);
      store.doneIndex();
    }
    return store.getRelationships(
        UniverseElement.INSTANCE,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
    }).then((_) {
      // clear
      store.clear();
      return store.getRelationships(
          UniverseElement.INSTANCE,
          relationship).then((List<Location> locations) {
        expect(locations, isEmpty);
      });
    });
  }

  test_universe_removeContext() {
    when(contextA.getElement(elementLocationA)).thenReturn(elementA);
    when(contextB.getElement(elementLocationB)).thenReturn(elementB);
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(
          UniverseElement.INSTANCE,
          relationship,
          locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextB, unitElementB);
      store.recordRelationship(
          UniverseElement.INSTANCE,
          relationship,
          locationB);
      store.doneIndex();
    }
    return store.getRelationships(
        UniverseElement.INSTANCE,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
    }).then((_) {
      // remove "contextA"
      store.removeContext(contextA);
      return store.getRelationships(
          UniverseElement.INSTANCE,
          relationship).then((List<Location> locations) {
        assertLocations(locations, [locationB]);
      });
    });
  }

  test_universe_removeSource() {
    when(contextA.getElement(elementLocationA)).thenReturn(elementA);
    when(contextB.getElement(elementLocationB)).thenReturn(elementB);
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(
          UniverseElement.INSTANCE,
          relationship,
          locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(
          UniverseElement.INSTANCE,
          relationship,
          locationB);
      store.doneIndex();
    }
    return store.getRelationships(
        UniverseElement.INSTANCE,
        relationship).then((List<Location> locations) {
      assertLocations(locations, [locationA, locationB]);
    }).then((_) {
      // remove "sourceA"
      store.removeSource(contextA, sourceA);
      return store.getRelationships(
          UniverseElement.INSTANCE,
          relationship).then((List<Location> locations) {
        assertLocations(locations, [locationB]);
      });
    });
  }

  /**
   * Asserts that the [actual] locations have all the [expected] locations and
   * only them.
   */
  static void assertLocations(List<Location> actual, List<Location> expected) {
    List<_LocationEqualsWrapper> actualWrappers = wrapLocations(actual);
    List<_LocationEqualsWrapper> expectedWrappers = wrapLocations(expected);
    expect(actualWrappers, unorderedEquals(expectedWrappers));
  }

  /**
   * @return the new [Location] mock.
   */
  static Location mockLocation(Element element) {
    Location location = new MockLocation();
    when(location.element).thenReturn(element);
    when(location.offset).thenReturn(0);
    when(location.length).thenReturn(0);
    when(location.isQualified).thenReturn(true);
    when(location.isResolved).thenReturn(true);
    return location;
  }

  /**
   * Wraps the given locations into [LocationEqualsWrapper].
   */
  static List<_LocationEqualsWrapper> wrapLocations(List<Location> locations) {
    List<_LocationEqualsWrapper> wrappers = <_LocationEqualsWrapper>[];
    for (Location location in locations) {
      wrappers.add(new _LocationEqualsWrapper(location));
    }
    return wrappers;
  }
}
