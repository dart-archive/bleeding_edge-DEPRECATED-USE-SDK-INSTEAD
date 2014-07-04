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
package com.google.dart.server.internal.remote.processor;

import com.google.dart.server.HoverConsumer;
import com.google.dart.server.HoverInformation;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Instances of {@code HoverResultProcessor} translate JSON result objects for a given
 * {@link HoverConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class HoverResultProcessor extends ResultProcessor {

  private final class Info implements HoverInformation {
    private final int offset;
    private final int length;
    private final String containingLibraryName;
    private final String containingLibraryPath;
    private final String dartdoc;
    private final String elementDescription;
    private final String elementKind;
    private final String parameter;
    private final String propagatedType;
    private final String staticType;

    Info(int offset, int length, String containingLibraryName, String containingLibraryPath,
        String dartdoc, String elementDescription, String elementKind, String parameter,
        String propagatedType, String staticType) {
      this.offset = offset;
      this.length = length;
      this.containingLibraryName = containingLibraryName;
      this.containingLibraryPath = containingLibraryPath;
      this.dartdoc = dartdoc;
      this.elementDescription = elementDescription;
      this.elementKind = elementKind;
      this.parameter = parameter;
      this.propagatedType = propagatedType;
      this.staticType = staticType;
    }

    @Override
    public String getContainingLibraryName() {
      return containingLibraryName;
    }

    @Override
    public String getContainingLibraryPath() {
      return containingLibraryPath;
    }

    @Override
    public String getDartdoc() {
      return dartdoc;
    }

    @Override
    public String getElementDescription() {
      return elementDescription;
    }

    @Override
    public String getElementKind() {
      return elementKind;
    }

    @Override
    public int getLength() {
      return length;
    }

    @Override
    public int getOffset() {
      return offset;
    }

    @Override
    public String getParameter() {
      return parameter;
    }

    @Override
    public String getPropagatedType() {
      return propagatedType;
    }

    @Override
    public String getStaticType() {
      return staticType;
    }
  }

  private final HoverConsumer consumer;

  public HoverResultProcessor(HoverConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject) {
    JsonArray hoversArray = safelyGetAsJsonArray(resultObject, "hovers");
    if (hoversArray != null) {
      ArrayList<HoverInformation> hovers = new ArrayList<HoverInformation>();
      Iterator<JsonElement> iter = hoversArray.iterator();
      while (iter.hasNext()) {
        JsonElement hoverElem = iter.next();
        if (hoverElem instanceof JsonObject) {
          JsonObject hoverObj = (JsonObject) hoverElem;
          hovers.add(new Info( //
              safelyGetAsInt(hoverObj, "offset", 0),
              safelyGetAsInt(hoverObj, "length", 0),
              safelyGetAsString(hoverObj, "containingLibraryName"),
              safelyGetAsString(hoverObj, "containingLibraryPath"),
              safelyGetAsString(hoverObj, "dartdoc"),
              safelyGetAsString(hoverObj, "elementDescription"),
              safelyGetAsString(hoverObj, "elementKind"),
              safelyGetAsString(hoverObj, "parameter"),
              safelyGetAsString(hoverObj, "propagatedType"),
              safelyGetAsString(hoverObj, "staticType")));
        }
      }
      consumer.computedHovers(hovers.toArray(new HoverInformation[hovers.size()]));
    }
  }
}
