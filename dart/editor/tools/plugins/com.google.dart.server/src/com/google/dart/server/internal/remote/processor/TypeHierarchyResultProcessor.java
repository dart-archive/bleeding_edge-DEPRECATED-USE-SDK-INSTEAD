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
    // construct type hierarchy and notify listener
    consumer.computedHierarchy(constructTypeHierarchyItem(hierarchyObject));
  }

  private TypeHierarchyItem constructTypeHierarchyItem(JsonObject hierarchyObject) {
    // classElement
    Element classElement = constructElement(hierarchyObject.get("classElement").getAsJsonObject());

    // displayName
    String displayName = safelyGetAsString(hierarchyObject, "displayName");

    // memberElement
    JsonObject memberElementObject = safelyGetAsJsonObject(hierarchyObject, "memberElement");
    Element memberElement = memberElementObject != null ? constructElement(memberElementObject)
        : null;

    // superclass
    JsonObject superclassObject = safelyGetAsJsonObject(hierarchyObject, "superclass");
    TypeHierarchyItem superclassItem = superclassObject != null
        ? constructTypeHierarchyItem(superclassObject) : null;

    // interfaces
    JsonArray interfacesArray = hierarchyObject.get("interfaces").getAsJsonArray();
    List<TypeHierarchyItem> interfacesList = Lists.newArrayList();
    Iterator<JsonElement> interfaceElementIterator = interfacesArray.iterator();
    while (interfaceElementIterator.hasNext()) {
      JsonObject interfaceObject = interfaceElementIterator.next().getAsJsonObject();
      interfacesList.add(constructTypeHierarchyItem(interfaceObject));
    }

    // mixins
    JsonArray mixinsArray = hierarchyObject.get("mixins").getAsJsonArray();
    List<TypeHierarchyItem> mixinsList = Lists.newArrayList();
    Iterator<JsonElement> mixinElementIterator = mixinsArray.iterator();
    while (mixinElementIterator.hasNext()) {
      JsonObject mixinObject = mixinElementIterator.next().getAsJsonObject();
      mixinsList.add(constructTypeHierarchyItem(mixinObject));
    }

    // subclasses
    JsonArray subclassesArray = hierarchyObject.get("subclasses").getAsJsonArray();
    List<TypeHierarchyItem> subclassesList = Lists.newArrayList();
    Iterator<JsonElement> subclassElementIterator = subclassesArray.iterator();
    while (subclassElementIterator.hasNext()) {
      JsonObject subclassObject = subclassElementIterator.next().getAsJsonObject();
      subclassesList.add(constructTypeHierarchyItem(subclassObject));
    }

    return new TypeHierarchyItemImpl(
        classElement,
        displayName,
        memberElement,
        superclassItem,
        interfacesList.toArray(new TypeHierarchyItem[interfacesList.size()]),
        mixinsList.toArray(new TypeHierarchyItem[mixinsList.size()]),
        subclassesList.toArray(new TypeHierarchyItem[subclassesList.size()]));
  }
}
