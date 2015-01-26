// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.services.src.index.store.codec;

import 'package:analysis_server/src/services/index/index.dart';
import 'package:analysis_server/src/services/index/store/codec.dart';
import 'package:analyzer/src/generated/element.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/source.dart';
import 'package:unittest/unittest.dart';

import '../../../abstract_single_unit.dart';
import '../../../mocks.dart';
import '../../../reflective_tests.dart';


main() {
  groupSep = ' | ';
  runReflectiveTests(_ContextCodecTest);
  runReflectiveTests(_ElementCodecTest);
  runReflectiveTests(_RelationshipCodecTest);
  runReflectiveTests(_StringCodecTest);
}


@reflectiveTest
class _ContextCodecTest {
  ContextCodec codec = new ContextCodec();

  void test_encode_decode() {
    AnalysisContext contextA = new MockAnalysisContext('contextA');
    AnalysisContext contextB = new MockAnalysisContext('contextB');
    int idA = codec.encode(contextA);
    int idB = codec.encode(contextB);
    expect(idA, codec.encode(contextA));
    expect(idB, codec.encode(contextB));
    expect(codec.decode(idA), contextA);
    expect(codec.decode(idB), contextB);
  }

  void test_remove() {
    // encode
    {
      AnalysisContext context = new MockAnalysisContext('context');
      // encode
      int id = codec.encode(context);
      expect(id, 0);
      expect(codec.decode(id), context);
      // remove
      codec.remove(context);
      expect(codec.decode(id), isNull);
    }
    // encode again
    {
      AnalysisContext context = new MockAnalysisContext('context');
      // encode
      int id = codec.encode(context);
      expect(id, 1);
      expect(codec.decode(id), context);
    }
  }
}


@reflectiveTest
class _ElementCodecTest extends AbstractSingleUnitTest {
  ElementCodec codec;
  AnalysisContext context = new MockAnalysisContext('context');
  StringCodec stringCodec = new StringCodec();

  void setUp() {
    super.setUp();
    codec = new ElementCodec(stringCodec);
  }

  void test_encodeHash_notLocal() {
    resolveTestUnit('''
class A {
  void mainA() {
    int foo; // A
  }
  void mainB() {
    int foo; // B
    int bar;
  }
}
''');
    MethodElement mainA = findElement('mainA');
    MethodElement mainB = findElement('mainB');
    Element fooA = mainA.localVariables[0];
    Element fooB = mainB.localVariables[0];
    Element bar = mainB.localVariables[1];
    int id_fooA = codec.encodeHash(fooA);
    int id_fooB = codec.encodeHash(fooB);
    int id_bar = codec.encodeHash(bar);
    expect(id_fooA == id_fooB, isTrue);
    expect(id_fooA == id_bar, isFalse);
  }

  void test_field() {
    resolveTestUnit('''
class A {
  int field;
}
''');
    FieldElement field = findElement('field', ElementKind.FIELD);
    PropertyAccessorElement getter = field.getter;
    PropertyAccessorElement setter = field.setter;
    {
      int id = codec.encode(getter, false);
      expect(codec.decode(context, id), getter);
    }
    {
      int id = codec.encode(setter, false);
      expect(codec.decode(context, id), setter);
    }
    {
      int id = codec.encode(field, false);
      expect(codec.decode(context, id), field);
    }
  }

  void test_filePackage_uriMix() {
    MethodElement buildMethodElement(Source source) {
      CompilationUnitElementImpl unitElement = new CompilationUnitElementImpl('file.dart');
      LibraryElementImpl libraryElement = new LibraryElementImpl(null, 'lib', 0);
      ClassElementImpl classElement = new ClassElementImpl('A', 0);
      MethodElementImpl methodElement = new MethodElementImpl('m', 0);
      unitElement.source = source;
      libraryElement.definingCompilationUnit = unitElement;
      unitElement.types = [classElement];
      classElement.methods = [methodElement];
      return methodElement;
    }
    // file:
    int fileId;
    {
      Source source = new _TestSource('/my/file.dart', 'file:///my/file.dart');
      var methodElement = buildMethodElement(source);
      fileId = codec.encode(methodElement, true);
    }
    // package:
    int packageId;
    {
      Source source = new _TestSource('/my/file.dart', 'package:my_pkg/file.dart');
      var methodElement = buildMethodElement(source);
      packageId = codec.encode(methodElement, true);
    }
    // should be the same
    expect(packageId, fileId);
  }

  void test_localLocalVariable() {
    resolveTestUnit('''
main() {
  {
    foo() {
      int bar; // A
    }
  }
  {
    foo() {
      int bar; // B
    }
  }
}
''');
    {
      LocalVariableElement element = findNodeElementAtString('bar; // A', null);
      int id = codec.encode(element, false);
      expect(codec.decode(context, id), element);
    }
    {
      LocalVariableElement element = findNodeElementAtString('bar; // B', null);
      int id = codec.encode(element, false);
      expect(codec.decode(context, id), element);
    }
    // check strings, "foo" as a single string, no "foo@17" or "bar@35"
    expect(stringCodec.nameToIndex, hasLength(4));
    expect(stringCodec.nameToIndex, containsPair('file:///test.dart', 0));
    expect(stringCodec.nameToIndex, containsPair('main', 1));
    expect(stringCodec.nameToIndex, containsPair('foo', 2));
    expect(stringCodec.nameToIndex, containsPair('bar', 3));
  }

  void test_localVariable() {
    resolveTestUnit('''
main() {
  {
    int foo; // A
  }
  {
    int foo; // B
  }
}
''');
    {
      LocalVariableElement element = findNodeElementAtString('foo; // A', null);
      int id = codec.encode(element, false);
      expect(codec.decode(context, id), element);
    }
    {
      LocalVariableElement element = findNodeElementAtString('foo; // B', null);
      int id = codec.encode(element, false);
      expect(codec.decode(context, id), element);
    }
    // check strings, "foo" as a single string, no "foo@21" or "foo@47"
    expect(stringCodec.nameToIndex, hasLength(3));
    expect(stringCodec.nameToIndex, containsPair('file:///test.dart', 0));
    expect(stringCodec.nameToIndex, containsPair('main', 1));
    expect(stringCodec.nameToIndex, containsPair('foo', 2));
  }

  void test_notLocal() {
    resolveTestUnit('''
main() {
  int foo;
}
''');
    LocalVariableElement element = findElement('foo');
    int id = codec.encode(element, false);
    expect(codec.encode(element, false), id);
    expect(codec.decode(context, id), element);
    // check strings
    expect(stringCodec.nameToIndex, hasLength(3));
    expect(stringCodec.nameToIndex, containsPair('file:///test.dart', 0));
    expect(stringCodec.nameToIndex, containsPair('main', 1));
    expect(stringCodec.nameToIndex, containsPair('foo', 2));
  }
}


@reflectiveTest
class _RelationshipCodecTest {
  StringCodec stringCodec = new StringCodec();
  RelationshipCodec codec;

  void setUp() {
    codec = new RelationshipCodec(stringCodec);
  }

  void test_all() {
    Relationship relationship = Relationship.getRelationship('my-relationship');
    int id = codec.encode(relationship);
    expect(codec.decode(id), relationship);
  }
}


@reflectiveTest
class _StringCodecTest {
  StringCodec codec = new StringCodec();

  void test_all() {
    int idA = codec.encode('aaa');
    int idB = codec.encode('bbb');
    expect(codec.decode(idA), 'aaa');
    expect(codec.decode(idB), 'bbb');
  }
}


class _TestSource implements Source {
  final String fullName;
  final String encoding;

  _TestSource(this.fullName, this.encoding);

  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
