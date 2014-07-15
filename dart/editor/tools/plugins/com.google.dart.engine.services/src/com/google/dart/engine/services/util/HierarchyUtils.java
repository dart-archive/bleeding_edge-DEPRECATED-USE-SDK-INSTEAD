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

package com.google.dart.engine.services.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ClassMemberElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper for {@link ClassElement} hierarchy.
 */
@DartOmit
public class HierarchyUtils {
  public static List<SearchMatch> getAccessibleMatches(Element element, List<SearchMatch> matches) {
    Map<LibraryElement, Set<LibraryElement>> cachedVisibleLibraries = Maps.newHashMap();
    // just search for name
    if (element == null) {
      return matches;
    }
    LibraryElement elementLibrary = element.getLibrary();
    // prepare filtered matches
    List<SearchMatch> filteredMatches = Lists.newArrayList();
    for (SearchMatch match : matches) {
      Element matchElement = match.getElement();
      // HtmlElement has no enclosing LibraryElement to check, so always keep these matches
      if (matchElement instanceof HtmlElement) {
        HtmlElement htmlElement = (HtmlElement) matchElement;
        CompilationUnitElement angularUnit = htmlElement.getAngularCompilationUnit();
        if (angularUnit == null) {
          continue;
        }
        matchElement = angularUnit;
      }
      // check enclosing LibraryElement
      LibraryElement matchLibrary = matchElement.getLibrary();
      if (isImported(cachedVisibleLibraries, elementLibrary, matchLibrary)) {
        filteredMatches.add(match);
      }
    }
    // done
    return filteredMatches;
  }

  /**
   * @return direct non-synthetic members of the given {@link ClassElement}. This includes fields,
   *         accessors (if not synthetic). Does not include constructors.
   */
  public static List<Element> getDirectMembers(final ClassElement clazz,
      final boolean includeSynthetic) {
    final List<Element> members = Lists.newArrayList();
    clazz.accept(new GeneralizingElementVisitor<Void>() {
      @Override
      public Void visitElement(Element element) {
        if (element == clazz) {
          return super.visitElement(element);
        }
        if (!includeSynthetic && element.isSynthetic()) {
          return null;
        }
        if (element instanceof ConstructorElement) {
          return null;
        }
        if (element instanceof ExecutableElement) {
          members.add(element);
        }
        if (element instanceof FieldElement) {
          members.add(element);
        }
        return null;
      }
    });
    return members;
  }

  /**
   * @return the {@link List} with direct sub {@link ClassElement}s of the given.
   */
  public static List<ClassElement> getDirectSubClasses(SearchEngine searchEngine, ClassElement seed) {
    List<ClassElement> subClasses = Lists.newArrayList();
    // ask SearchEngine
    List<SearchMatch> subMatches = searchEngine.searchSubtypes(seed, null, null);
    for (SearchMatch subMatch : subMatches) {
      ClassElement subClass = (ClassElement) subMatch.getElement();
      subClasses.add(subClass);
    }
    // done
    return subClasses;
  }

  /**
   * @return all implementations of the given {@link ClassMemberElement} is its superclasses and
   *         their subclasses.
   */
  public static Set<ClassMemberElement> getHierarchyMembers(SearchEngine searchEngine,
      ClassMemberElement member) {
    if (member instanceof ConstructorElement) {
      return Sets.newHashSet(member);
    }
    String name = member.getName();
    ClassElement memberClass = member.getEnclosingElement();
    Set<ClassElement> superClasses = getSuperClasses(memberClass);
    superClasses.add(memberClass);
    Set<ClassMemberElement> result = Sets.newHashSet();
    for (ClassElement superClass : superClasses) {
      // ignore if super- class does not declare member
      if (CorrectionUtils.getChildren(superClass, name).isEmpty()) {
        continue;
      }
      // check all sub- classes
      Set<ClassElement> subClasses = getSubClasses(searchEngine, superClass);
      subClasses.add(superClass);
      for (ClassElement subClass : subClasses) {
        List<Element> subClassMembers = CorrectionUtils.getChildren(subClass, name);
        // add "name" children/member(s)
        for (Element subClassMember : subClassMembers) {
          if (subClassMember instanceof ClassMemberElement) {
            result.add((ClassMemberElement) subClassMember);
          }
        }
      }
    }
    return result;
  }

  /**
   * @return non-synthetic members of the given {@link ClassElement} and its super classes. This
   *         includes fields, accessors (if not synthetic), method. Does not include constructors.
   */
  public static List<Element> getMembers(ClassElement clazz, boolean includeSynthetic) {
    List<Element> members = Lists.newArrayList();
    members.addAll(getDirectMembers(clazz, includeSynthetic));
    Set<ClassElement> superClasses = getSuperClasses(clazz);
    for (ClassElement superClass : superClasses) {
      members.addAll(getDirectMembers(superClass, includeSynthetic));
    }
    return members;
  }

  /**
   * @return the {@link Set} with all direct and indirect sub {@link ClassElement}s of the given.
   */
  public static Set<ClassElement> getSubClasses(SearchEngine searchEngine, ClassElement seed) {
    Set<ClassElement> subClasses = Sets.newHashSet();
    // prepare queue
    LinkedList<ClassElement> subClassQueue = Lists.newLinkedList();
    subClassQueue.add(seed);
    // process queue
    while (!subClassQueue.isEmpty()) {
      ClassElement subClass = subClassQueue.removeFirst();
      if (subClasses.add(subClass)) {
        List<ClassElement> directSubClasses = getDirectSubClasses(searchEngine, subClass);
        subClassQueue.addAll(directSubClasses);
      }
    }
    // we don't need "seed" itself
    subClasses.remove(seed);
    return subClasses;
  }

  /**
   * @return the {@link Set} with all direct and indirect super {@link ClassElement}s of the given.
   */
  public static Set<ClassElement> getSuperClasses(ClassElement seed) {
    Set<ClassElement> result = Sets.newHashSet();
    // prepare queue
    LinkedList<ClassElement> queue = Lists.newLinkedList();
    queue.add(seed);
    // process queue
    while (!queue.isEmpty()) {
      ClassElement current = queue.removeFirst();
      // add if not checked already
      if (!result.add(current)) {
        continue;
      }
      // append supertype
      {
        InterfaceType superType = current.getSupertype();
        if (superType != null) {
          queue.add(superType.getElement());
        }
      }
      // append interfaces
      for (InterfaceType intf : current.getInterfaces()) {
        queue.add(intf.getElement());
      }
    }
    // we don't need "seed" itself
    result.remove(seed);
    return result;
  }

  public static Element getSyntheticAccessorVariable(Element element) {
    if (element instanceof PropertyAccessorElement) {
      PropertyAccessorElement accessor = (PropertyAccessorElement) element;
      if (accessor.isSynthetic()) {
        element = accessor.getVariable();
      }
    }
    return element;
  }

  /**
   * Checks if "what" is imported into "where" directly or indirectly, so there is a chance that it
   * has access to an object from "what". Otherwise we find too many "second-order" positive
   * matches.
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=12268
   */
  private static boolean isImported(
      Map<LibraryElement, Set<LibraryElement>> cachedVisibleLibraries, LibraryElement what,
      LibraryElement where) {
    Set<LibraryElement> visibleLibraries = cachedVisibleLibraries.get(where);
    if (visibleLibraries == null) {
      LibraryElement[] visibleLibrariesArray = where.getVisibleLibraries();
      visibleLibraries = ImmutableSet.copyOf(visibleLibrariesArray);
      cachedVisibleLibraries.put(where, visibleLibraries);
    }
    return visibleLibraries.contains(what);
  }
}
