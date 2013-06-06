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
package com.google.dart.engine.resolver;

import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.PubSuggestionCode;
import com.google.dart.engine.source.Source;

public class PubSuggestionCodeTest extends ResolverTestCase {

  public void test_import_packageWithDotDot() throws Exception {
    Source source = addSource(createSource(//
    "import 'package:somepackage/../other.dart';"));
    resolve(source);
    assertErrors(
        CompileTimeErrorCode.URI_DOES_NOT_EXIST,
        PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT);
  }

  public void test_import_packageWithLeadingDotDot() throws Exception {
    Source source = addSource(createSource(//
    "import 'package:../other.dart';"));
    resolve(source);
    assertErrors(
        CompileTimeErrorCode.URI_DOES_NOT_EXIST,
        PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT);
  }

  public void test_import_referenceIntoLibDirectory() throws Exception {
    Source source = addSource(createSource(//
    "import '../lib/other.dart';"));
    resolve(source);
    assertErrors(
        CompileTimeErrorCode.URI_DOES_NOT_EXIST,
        PubSuggestionCode.FILE_IMPORT_OUTSIDE_LIB_REFERENCES_FILE_INSIDE);
  }

  public void test_import_referenceOutOfLibDirectory() throws Exception {
    Source source = addSource("lib/test.dart", createSource(//
        "import '../web/other.dart';"));
    resolve(source);
    assertErrors(
        CompileTimeErrorCode.URI_DOES_NOT_EXIST,
        PubSuggestionCode.FILE_IMPORT_INSIDE_LIB_REFERENCES_FILE_OUTSIDE);
  }

  public void test_import_valid() throws Exception {
    Source source = addSource("lib2/test.dart", createSource(//
        "import '../web/other.dart';"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.URI_DOES_NOT_EXIST);
  }
}
