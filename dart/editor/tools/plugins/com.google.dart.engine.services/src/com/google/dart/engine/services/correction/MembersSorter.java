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
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.error.BooleanErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.CharacterReader;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Sorter for unit members.
 */
public class MembersSorter {
  private static class DirectiveInfo implements Comparable<DirectiveInfo> {
    private final Directive directive;
    private final DirectivePriority priority;
    private final String text;

    public DirectiveInfo(Directive directive, DirectivePriority priority, String text) {
      this.directive = directive;
      this.priority = priority;
      this.text = text;
    }

    @Override
    public int compareTo(DirectiveInfo other) {
      if (priority == other.priority) {
        return text.compareTo(other.text);
      }
      return priority.ordinal() - other.priority.ordinal();
    }

    @Override
    public String toString() {
      return "(priority=" + priority + "; text=" + text + ")";
    }
  }

  private static enum DirectivePriority {
    DIRECTIVE_IMPORT_SDK,
    DIRECTIVE_IMPORT_PKG,
    DIRECTIVE_IMPORT_FILE,
    DIRECTIVE_EXPORT_SDK,
    DIRECTIVE_EXPORT_PKG,
    DIRECTIVE_EXPORT_FILE,
    DIRECTIVE_PART,
  }

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
      if (kind == MemberKind.CLASS_FIELD) {
        return other.kind == kind && other.isStatic == isStatic;
      }
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
      new PriorityItem(true, MemberKind.CLASS_ACCESSOR, false),
      new PriorityItem(true, MemberKind.CLASS_ACCESSOR, true),
      new PriorityItem(false, MemberKind.CLASS_FIELD, false),
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
          // don't reorder class fields
          if (o1.item.kind == MemberKind.CLASS_FIELD) {
            return 0;
          }
          // sort all other members by name
          return o1.name.compareTo(o2.name);
        }
        return priority1 - priority2;
      }
    });
    return membersSorted;
  }

  private final String initialCode;
  private final CompilationUnit unit;

  private String code;

  /**
   * Initialize a newly created {@link MembersSorter}. Creates
   * 
   * @param code the Dart code
   * @param unit an optional parsed {@link CompilationUnit} for the given "code", may be
   *          {@code null}
   */
  public MembersSorter(String code, CompilationUnit unit) {
    this.initialCode = code;
    if (unit != null) {
      this.unit = unit;
    } else {
      this.unit = parseUnit(code);
    }
    this.code = code;
  }

  /**
   * Returns the sorted source or {@code null} if no changes.
   */
  public String createSortedCode() {
    if (unit == null) {
      return null;
    }
    sortClassesMembers();
    sortUnitDirectives();
    sortUnitMembers();
    // is the any change?
    if (code.equals(initialCode)) {
      return null;
    }
    return code;
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

  /**
   * @return the EOL to use for {@link #code}.
   */
  private String getEndOfLine() {
    if (code.contains("\r\n")) {
      return "\r\n";
    } else {
      return "\n";
    }
  }

  private CompilationUnit parseUnit(String code) {
    BooleanErrorListener listener = new BooleanErrorListener();
    CharacterReader reader = new CharSequenceReader(code);
    Scanner scanner = new Scanner(null, reader, listener);
    Token token = scanner.tokenize();
    if (listener.getErrorReported()) {
      return null;
    }
    Parser parser = new Parser(null, listener);
    CompilationUnit unit = parser.parseCompilationUnit(token);
    if (listener.getErrorReported()) {
      return null;
    }
    return unit;
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
   * Sorts all {@link Directive}s.
   */
  private void sortUnitDirectives() {
    List<DirectiveInfo> directives = Lists.newArrayList();
    for (Directive directive : unit.getDirectives()) {
      if (!(directive instanceof UriBasedDirective)) {
        continue;
      }
      UriBasedDirective uriDirective = (UriBasedDirective) directive;
      String uriContent = uriDirective.getUri().getStringValue();
      DirectivePriority kind = null;
      if (directive instanceof ImportDirective) {
        if (uriContent.startsWith("dart:")) {
          kind = DirectivePriority.DIRECTIVE_IMPORT_SDK;
        } else if (uriContent.startsWith("package:")) {
          kind = DirectivePriority.DIRECTIVE_IMPORT_PKG;
        } else {
          kind = DirectivePriority.DIRECTIVE_IMPORT_FILE;
        }
      }
      if (directive instanceof ExportDirective) {
        if (uriContent.startsWith("dart:")) {
          kind = DirectivePriority.DIRECTIVE_EXPORT_SDK;
        } else if (uriContent.startsWith("package:")) {
          kind = DirectivePriority.DIRECTIVE_EXPORT_PKG;
        } else {
          kind = DirectivePriority.DIRECTIVE_EXPORT_FILE;
        }
      }
      if (directive instanceof PartDirective) {
        kind = DirectivePriority.DIRECTIVE_PART;
      }
      if (kind != null) {
        int offset = directive.getOffset();
        int length = directive.getLength();
        String text = code.substring(offset, offset + length);
        directives.add(new DirectiveInfo(directive, kind, text));
      }
    }
    // nothing to do
    if (directives.isEmpty()) {
      return;
    }
    int firstDirectiveOffset = directives.get(0).directive.getOffset();
    int lastDirectiveEnd = directives.get(directives.size() - 1).directive.getEnd();
    // do sort
    Collections.sort(directives);
    // append directives with grouping
    String directivesCode;
    {
      StringBuilder sb = new StringBuilder();
      String endOfLine = getEndOfLine();
      DirectivePriority currentPriority = null;
      for (DirectiveInfo directive : directives) {
        if (currentPriority != directive.priority) {
          if (sb.length() != 0) {
            sb.append(endOfLine);
          }
          currentPriority = directive.priority;
        }
        sb.append(directive.text);
        sb.append(endOfLine);
      }
      directivesCode = sb.toString();
      directivesCode = StringUtils.chomp(directivesCode);
    }
    code = code.substring(0, firstDirectiveOffset) + directivesCode
        + code.substring(lastDirectiveEnd);
  }

  /**
   * Sorts all {@link CompilationUnitMember}s.
   */
  private void sortUnitMembers() {
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
