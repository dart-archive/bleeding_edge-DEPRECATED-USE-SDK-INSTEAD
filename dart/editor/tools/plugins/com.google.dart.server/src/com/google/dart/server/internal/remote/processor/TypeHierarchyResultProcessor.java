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

import com.google.common.collect.Lists;
import com.google.dart.server.Element;
import com.google.dart.server.TypeHierarchyConsumer;
import com.google.dart.server.TypeHierarchyItem;
import com.google.dart.server.internal.TypeHierarchyItemImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;

/**
 * Instances of {@code TypeHierarchyResultProcessor} translate JSON result objects for a given
 * {@link TypeHierarchyConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class TypeHierarchyResultProcessor extends ResultProcessor {

  private final TypeHierarchyConsumer consumer;

  public TypeHierarchyResultProcessor(TypeHierarchyConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject) {
    JsonObject hierarchyObject = resultObject.get("hierarchy").getAsJsonObject();
    // compute type hierarchy and notify listener
    consumer.computedHierarchy(constructTypeHierarchyItem(hierarchyObject));
  }

  private TypeHierarchyItem constructTypeHierarchyItem(JsonObject hierarchyObject) {
    // classElement
    Element classElement = constructElement(hierarchyObject.get("classElement").getAsJsonObject());

    // memberElement
    JsonObject memberElementObject = safelyGetAsJsonObject(hierarchyObject, "memberElement");
    Element memberElement = memberElementObject != null ? constructElement(memberElementObject)
        : null;

    // extendedType
    JsonObject extendedTypeObject = safelyGetAsJsonObject(hierarchyObject, "extendedType");
    TypeHierarchyItem extendedType = extendedTypeObject != null
        ? constructTypeHierarchyItem(extendedTypeObject) : null;

    // implementedTypes    
    JsonArray implementedTypesArray = hierarchyObject.get("implementedTypes").getAsJsonArray();
    List<TypeHierarchyItem> implementedTypesList = Lists.newArrayList();
    Iterator<JsonElement> implementedTypeElementIterator = implementedTypesArray.iterator();
    while (implementedTypeElementIterator.hasNext()) {
      JsonObject implementedTypeObject = implementedTypeElementIterator.next().getAsJsonObject();
      implementedTypesList.add(constructTypeHierarchyItem(implementedTypeObject));
    }

    // withTypes
    JsonArray withTypesArray = hierarchyObject.get("withTypes").getAsJsonArray();
    List<TypeHierarchyItem> withTypesList = Lists.newArrayList();
    Iterator<JsonElement> withTypeElementIterator = withTypesArray.iterator();
    while (withTypeElementIterator.hasNext()) {
      JsonObject withTypeObject = withTypeElementIterator.next().getAsJsonObject();
      withTypesList.add(constructTypeHierarchyItem(withTypeObject));
    }

    // subtypes
    JsonArray subtypesArray = hierarchyObject.get("subtypes").getAsJsonArray();
    List<TypeHierarchyItem> subtypesList = Lists.newArrayList();
    Iterator<JsonElement> subtypeElementIterator = subtypesArray.iterator();
    while (subtypeElementIterator.hasNext()) {
      JsonObject subtypeTypeObject = subtypeElementIterator.next().getAsJsonObject();
      subtypesList.add(constructTypeHierarchyItem(subtypeTypeObject));
    }

    return new TypeHierarchyItemImpl(
        classElement,
        memberElement,
        extendedType,
        implementedTypesList.toArray(new TypeHierarchyItem[implementedTypesList.size()]),
        withTypesList.toArray(new TypeHierarchyItem[withTypesList.size()]),
        subtypesList.toArray(new TypeHierarchyItem[subtypesList.size()]));
  }
}
