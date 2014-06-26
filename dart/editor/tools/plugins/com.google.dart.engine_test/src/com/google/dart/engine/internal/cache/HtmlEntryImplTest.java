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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.error.HtmlWarningCode;
import com.google.dart.engine.error.PolymerCode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.element.HtmlElementImpl;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.source.LineInfo;

public class HtmlEntryImplTest extends EngineTestCase {
  public void test_creation() {
    HtmlEntryImpl entry = new HtmlEntryImpl();
    assertNotNull(entry);
  }

  public void test_getAllErrors() {
    Source source = new TestSource();
    HtmlEntryImpl entry = new HtmlEntryImpl();
    assertLength(0, entry.getAllErrors());
    entry.setValue(HtmlEntry.PARSE_ERRORS, new AnalysisError[] {new AnalysisError(
        source,
        ParserErrorCode.EXPECTED_TOKEN,
        ";")});
    entry.setValue(HtmlEntry.RESOLUTION_ERRORS, new AnalysisError[] {new AnalysisError(
        source,
        HtmlWarningCode.INVALID_URI,
        "-")});
    entry.setValue(HtmlEntry.ANGULAR_ERRORS, new AnalysisError[] {new AnalysisError(
        source,
        AngularCode.INVALID_REPEAT_SYNTAX,
        "-")});
    entry.setValue(HtmlEntry.POLYMER_BUILD_ERRORS, new AnalysisError[] {new AnalysisError(
        source,
        PolymerCode.INVALID_ATTRIBUTE_NAME,
        "-")});
    entry.setValue(HtmlEntry.POLYMER_RESOLUTION_ERRORS, new AnalysisError[] {new AnalysisError(
        source,
        PolymerCode.INVALID_ATTRIBUTE_NAME,
        "-")});
    entry.setValue(HtmlEntry.HINTS, new AnalysisError[] {new AnalysisError(
        source,
        HintCode.DEAD_CODE)});
    assertLength(6, entry.getAllErrors());
  }

  public void test_getWritableCopy() {
    HtmlEntryImpl entry = new HtmlEntryImpl();
    HtmlEntryImpl copy = entry.getWritableCopy();
    assertNotNull(copy);
    assertNotSame(entry, copy);
  }

  public void test_invalidateAllResolutionInformation() {
    HtmlEntryImpl entry = entryWithValidState();
    entry.invalidateAllResolutionInformation(false);
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.ANGULAR_APPLICATION));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.ANGULAR_COMPONENT));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.ANGULAR_ENTRY));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.ANGULAR_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.POLYMER_BUILD_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.POLYMER_RESOLUTION_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.ELEMENT));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.HINTS));
    assertSame(CacheState.VALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.PARSE_ERRORS));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.PARSED_UNIT));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.REFERENCED_LIBRARIES));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.RESOLUTION_ERRORS));
  }

  public void test_invalidateAllResolutionInformation_includingUris() {
    HtmlEntryImpl entry = entryWithValidState();
    entry.invalidateAllResolutionInformation(true);
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.ANGULAR_APPLICATION));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.ANGULAR_COMPONENT));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.ANGULAR_ENTRY));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.ANGULAR_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.POLYMER_BUILD_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.POLYMER_RESOLUTION_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.ELEMENT));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.HINTS));
    assertSame(CacheState.VALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.PARSE_ERRORS));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.PARSED_UNIT));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.REFERENCED_LIBRARIES));
    assertSame(CacheState.INVALID, entry.getState(HtmlEntry.RESOLUTION_ERRORS));
  }

  public void test_setState_angularErrors() {
    setState(HtmlEntry.ANGULAR_ERRORS);
  }

  public void test_setState_element() {
    setState(HtmlEntry.ELEMENT);
  }

  public void test_setState_hints() {
    setState(HtmlEntry.HINTS);
  }

  public void test_setState_lineInfo() {
    setState(SourceEntry.LINE_INFO);
  }

  public void test_setState_parsedUnit() {
    setState(HtmlEntry.PARSED_UNIT);
  }

  public void test_setState_parseErrors() {
    setState(HtmlEntry.PARSE_ERRORS);
  }

  public void test_setState_polymerBuildErrors() {
    setState(HtmlEntry.POLYMER_BUILD_ERRORS);
  }

  public void test_setState_polymerResolutionErrors() {
    setState(HtmlEntry.POLYMER_RESOLUTION_ERRORS);
  }

  public void test_setState_referencedLibraries() {
    setState(HtmlEntry.REFERENCED_LIBRARIES);
  }

  public void test_setState_resolutionErrors() {
    setState(HtmlEntry.RESOLUTION_ERRORS);
  }

  public void test_setValue_angularErrors() {
    setValue(HtmlEntry.ANGULAR_ERRORS, new AnalysisError[] {new AnalysisError(
        null,
        AngularCode.INVALID_REPEAT_SYNTAX,
        "-")});
  }

  public void test_setValue_element() {
    setValue(HtmlEntry.ELEMENT, new HtmlElementImpl(null, "test.html"));
  }

  public void test_setValue_hints() {
    setValue(HtmlEntry.HINTS, new AnalysisError[] {new AnalysisError(null, HintCode.DEAD_CODE)});
  }

  public void test_setValue_illegal() {
    HtmlEntryImpl entry = new HtmlEntryImpl();
    try {
      entry.setValue(DartEntry.ELEMENT, null);
      fail("Expected IllegalArgumentException for DartEntry.ELEMENT");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_setValue_lineInfo() {
    setValue(SourceEntry.LINE_INFO, new LineInfo(new int[] {0}));
  }

  public void test_setValue_parsedUnit() {
    setValue(HtmlEntry.PARSED_UNIT, new HtmlUnit(null, null, null));
  }

  public void test_setValue_parseErrors() {
    setValue(HtmlEntry.PARSE_ERRORS, new AnalysisError[] {new AnalysisError(
        null,
        HtmlWarningCode.INVALID_URI,
        "-")});
  }

  public void test_setValue_polymerBuildErrors() {
    setValue(HtmlEntry.POLYMER_BUILD_ERRORS, new AnalysisError[] {new AnalysisError(
        null,
        PolymerCode.INVALID_ATTRIBUTE_NAME,
        "-")});
  }

  public void test_setValue_polymerResolutionErrors() {
    setValue(HtmlEntry.POLYMER_RESOLUTION_ERRORS, new AnalysisError[] {new AnalysisError(
        null,
        PolymerCode.INVALID_ATTRIBUTE_NAME,
        "-")});
  }

  public void test_setValue_referencedLibraries() {
    setValue(HtmlEntry.REFERENCED_LIBRARIES, new Source[] {new TestSource()});
  }

  public void test_setValue_resolutionErrors() {
    setValue(HtmlEntry.RESOLUTION_ERRORS, new AnalysisError[] {new AnalysisError(
        null,
        HtmlWarningCode.INVALID_URI,
        "-")});
  }

  private HtmlEntryImpl entryWithValidState() {
    HtmlEntryImpl entry = new HtmlEntryImpl();
    entry.setValue(HtmlEntry.ANGULAR_ERRORS, null);
    entry.setValue(HtmlEntry.ELEMENT, null);
    entry.setValue(HtmlEntry.HINTS, null);
    entry.setValue(SourceEntry.LINE_INFO, null);
    entry.setValue(HtmlEntry.PARSE_ERRORS, null);
    entry.setValue(HtmlEntry.PARSED_UNIT, null);
    entry.setValue(HtmlEntry.POLYMER_BUILD_ERRORS, null);
    entry.setValue(HtmlEntry.POLYMER_RESOLUTION_ERRORS, null);
    entry.setValue(HtmlEntry.REFERENCED_LIBRARIES, null);
    entry.setValue(HtmlEntry.RESOLUTION_ERRORS, null);

    assertSame(CacheState.VALID, entry.getState(HtmlEntry.ANGULAR_ERRORS));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.ELEMENT));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.HINTS));
    assertSame(CacheState.VALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.PARSE_ERRORS));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.PARSED_UNIT));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.POLYMER_BUILD_ERRORS));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.POLYMER_RESOLUTION_ERRORS));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.REFERENCED_LIBRARIES));
    assertSame(CacheState.VALID, entry.getState(HtmlEntry.RESOLUTION_ERRORS));
    return entry;
  }

  private void setState(DataDescriptor<?> descriptor) {
    HtmlEntryImpl entry = new HtmlEntryImpl();
    assertNotSame(CacheState.FLUSHED, entry.getState(descriptor));
    entry.setState(descriptor, CacheState.FLUSHED);
    assertSame(CacheState.FLUSHED, entry.getState(descriptor));
  }

  private <E> void setValue(DataDescriptor<E> descriptor, E newValue) {
    HtmlEntryImpl entry = new HtmlEntryImpl();
    E value = entry.getValue(descriptor);
    assertNotSame(value, newValue);
    entry.setValue(descriptor, newValue);
    assertSame(CacheState.VALID, entry.getState(descriptor));
    assertSame(newValue, entry.getValue(descriptor));
  }
}
