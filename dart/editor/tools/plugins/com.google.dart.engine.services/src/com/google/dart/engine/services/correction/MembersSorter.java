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

package com.google.dart.engine.services.correction;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.source.Source;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Sorter for unit members.
 */
public class MembersSorter {
  private static class MemberInfo {
    PriorityItem item;
    String name;
    int offset;
    int length;
    int end;
    String text;

    public MemberInfo(PriorityItem item, String name, int offset, int length, String text) {
      this.item = item;
      this.offset = offset;
      this.length = length;
      this.end = offset + length;
      this.name = name;
      this.text = text;
    }

    @Override
    public String toString() {
      return "(priority=" + item + "; name=" + name + "; offset=" + offset + "; length=" + length
          + ")";
    }
  }

  private static enum MemberKind {
    UNIT_ACCESSOR,
    UNIT_FUNCTION,
    UNIT_FUNCTION_TYPE,
    UNIT_CLASS,
    UNIT_VARIABLE,
    CLASS_ACCESSOR,
    CLASS_CONSTRUCTOR,
    CLASS_FIELD,
    CLASS_METHOD,
  }

  private static class PriorityItem {
    private final MemberKind kind;
    private final boolean isPrivate;
    private final boolean isStatic;

    public PriorityItem(boolean isStatic, MemberKind kind, boolean isPrivate) {
      this.kind = kind;
      this.isPrivate = isPrivate;
      this.isStatic = isStatic;
    }

    public PriorityItem(boolean isStatic, String name, MemberKind kind) {
      this.kind = kind;
      this.isPrivate = Identifier.isPrivateName(name);
      this.isStatic = isStatic;
    }

    @Override
    public boolean equals(Object obj) {
      PriorityItem other = (PriorityItem) obj;
      return other.kind == kind && other.isPrivate == isPrivate && other.isStatic == isStatic;
    }
  }

  private static final PriorityItem[] PRIORITY_ITEMS = {
      // unit
      new PriorityItem(false, MemberKind.UNIT_VARIABLE, false),
      new PriorityItem(false, MemberKind.UNIT_VARIABLE, true),
      new PriorityItem(false, MemberKind.UNIT_ACCESSOR, false),
      new PriorityItem(false, MemberKind.UNIT_ACCESSOR, true),
      new PriorityItem(false, MemberKind.UNIT_FUNCTION, false),
      new PriorityItem(false, MemberKind.UNIT_FUNCTION, true),
      new PriorityItem(false, MemberKind.UNIT_FUNCTION_TYPE, false),
      new PriorityItem(false, MemberKind.UNIT_FUNCTION_TYPE, true),
      new PriorityItem(false, MemberKind.UNIT_CLASS, false),
      new PriorityItem(false, MemberKind.UNIT_CLASS, true),
      // class
      new PriorityItem(true, MemberKind.CLASS_FIELD, false),
      new PriorityItem(true, MemberKind.CLASS_FIELD, true),
      new PriorityItem(true, MemberKind.CLASS_ACCESSOR, false),
      new PriorityItem(true, MemberKind.CLASS_ACCESSOR, true),
      new PriorityItem(false, MemberKind.CLASS_FIELD, false),
      new PriorityItem(false, MemberKind.CLASS_FIELD, true),
      new PriorityItem(false, MemberKind.CLASS_CONSTRUCTOR, false),
      new PriorityItem(false, MemberKind.CLASS_CONSTRUCTOR, true),
      new PriorityItem(false, MemberKind.CLASS_ACCESSOR, false),
      new PriorityItem(false, MemberKind.CLASS_ACCESSOR, true),
      new PriorityItem(false, MemberKind.CLASS_METHOD, false),
      new PriorityItem(false, MemberKind.CLASS_METHOD, true),
      new PriorityItem(true, MemberKind.CLASS_METHOD, false),
      new PriorityItem(true, MemberKind.CLASS_METHOD, true),};

  private static int getPriority(PriorityItem item) {
    for (int i = 0; i < PRIORITY_ITEMS.length; i++) {
      if (PRIORITY_ITEMS[i].equals(item)) {
        return i;
      }
    }
    throw new IllegalArgumentException("Unknown priority item: " + item);
  }

  private static List<MemberInfo> getSortedMembers(List<MemberInfo> members) {
    List<MemberInfo> membersSorted = Lists.newArrayList(members);
    Collections.sort(membersSorted, new Comparator<MemberInfo>() {
      @Override
      public int compare(MemberInfo o1, MemberInfo o2) {
        int priority1 = getPriority(o1.item);
        int priority2 = getPriority(o2.item);
        if (priority1 == priority2) {
          return o1.name.compareTo(o2.name);
        }
        return priority1 - priority2;
      }
    });
    return membersSorted;
  }

  private final SourceChange change;
  private final CompilationUnit unit;
  private final CorrectionUtils utils;

  private String code;

  public MembersSorter(Source source, CompilationUnit unit) throws Exception {
    this.change = new SourceChange("Sort unit/class members", source);
    this.unit = unit;
    this.utils = new CorrectionUtils(unit);
    this.code = utils.getText();
  }

  /**
   * Returns the {@link SourceChange} or {@code null} if no changes.
   */
  public SourceChange createChange() {
    String initialCode = code;
    sortClassesMembers();
    sortUnitMembers();
    // is the any change?
    if (code.equals(initialCode)) {
      return null;
    }
    // replace content
    change.addEdit(new Edit(0, code.length(), code));
    return change;
  }

  /**
   * Sorts all members of all {@link ClassDeclaration}s.
   */
  public void sortClassesMembers() {
    for (CompilationUnitMember unitMember : unit.getDeclarations()) {
      if (unitMember instanceof ClassDeclaration) {
        ClassDeclaration classDeclaration = (ClassDeclaration) unitMember;
        sortClassMembers(classDeclaration);
      }
    }
  }

  private void sortAndReorderMembers(List<MemberInfo> members) {
    List<MemberInfo> membersSorted = getSortedMembers(members);
    int size = membersSorted.size();
    for (int i = 0; i < size; i++) {
      MemberInfo newInfo = membersSorted.get(size - 1 - i);
      MemberInfo oldInfo = members.get(size - 1 - i);
      code = code.substring(0, oldInfo.offset) + newInfo.text + code.substring(oldInfo.end);
    }
  }

  /**
   * Sorts all members of the given {@link ClassDeclaration}.
   */
  private void sortClassMembers(ClassDeclaration classDeclaration) {
    List<MemberInfo> members = Lists.newArrayList();
    for (ClassMember member : classDeclaration.getMembers()) {
      MemberKind kind = null;
      boolean isStatic = false;
      String name = null;
      if (member instanceof ConstructorDeclaration) {
        kind = MemberKind.CLASS_CONSTRUCTOR;
        SimpleIdentifier nameNode = ((ConstructorDeclaration) member).getName();
        if (nameNode == null) {
          name = "";
        } else {
          name = nameNode.getName();
        }
      }
      if (member instanceof FieldDeclaration) {
        FieldDeclaration fieldDeclaration = (FieldDeclaration) member;
        List<VariableDeclaration> fields = fieldDeclaration.getFields().getVariables();
        if (!fields.isEmpty()) {
          kind = MemberKind.CLASS_FIELD;
          isStatic = fieldDeclaration.isStatic();
          name = fields.get(0).getName().getName();
        }
      }
      if (member instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) member;
        isStatic = method.isStatic();
        name = method.getName().getName();
        if (method.isGetter()) {
          kind = MemberKind.CLASS_ACCESSOR;
          name += " getter";
        } else if (method.isSetter()) {
          kind = MemberKind.CLASS_ACCESSOR;
          name += " setter";
        } else {
          kind = MemberKind.CLASS_METHOD;
        }
      }
      if (name != null) {
        PriorityItem item = new PriorityItem(isStatic, name, kind);
        int offset = member.getOffset();
        int length = member.getLength();
        String text = code.substring(offset, offset + length);
        members.add(new MemberInfo(item, name, offset, length, text));
      }
    }
    // do sort
    sortAndReorderMembers(members);
  }

  /**
   * Sorts all {@link CompilationUnitMember}s.
   */
  private void sortUnitMembers() {
    List<Directive> directives = unit.getDirectives();
    // prepare information about unit members
    List<MemberInfo> members = Lists.newArrayList();
    for (CompilationUnitMember member : unit.getDeclarations()) {
      MemberKind kind = null;
      String name = null;
      if (member instanceof ClassDeclaration) {
        kind = MemberKind.UNIT_CLASS;
        name = ((ClassDeclaration) member).getName().getName();
      }
      if (member instanceof ClassTypeAlias) {
        kind = MemberKind.UNIT_CLASS;
        name = ((ClassTypeAlias) member).getName().getName();
      }
      if (member instanceof FunctionDeclaration) {
        FunctionDeclaration function = (FunctionDeclaration) member;
        name = function.getName().getName();
        if (function.isGetter()) {
          kind = MemberKind.UNIT_ACCESSOR;
          name += " getter";
        } else if (function.isSetter()) {
          kind = MemberKind.UNIT_ACCESSOR;
          name += " setter";
        } else {
          kind = MemberKind.UNIT_FUNCTION;
        }
      }
      if (member instanceof FunctionTypeAlias) {
        kind = MemberKind.UNIT_FUNCTION_TYPE;
        name = ((FunctionTypeAlias) member).getName().getName();
      }
      if (member instanceof TopLevelVariableDeclaration) {
        TopLevelVariableDeclaration variableDeclaration = (TopLevelVariableDeclaration) member;
        List<VariableDeclaration> variables = variableDeclaration.getVariables().getVariables();
        if (!variables.isEmpty()) {
          kind = MemberKind.UNIT_VARIABLE;
          name = variables.get(0).getName().getName();
        }
      }
      if (name != null) {
        PriorityItem item = new PriorityItem(false, name, kind);
        int offset = member.getOffset();
        int length = member.getLength();
        String text = code.substring(offset, offset + length);
        members.add(new MemberInfo(item, name, offset, length, text));
      }
    }
    // do sort
    sortAndReorderMembers(members);
  }
}
