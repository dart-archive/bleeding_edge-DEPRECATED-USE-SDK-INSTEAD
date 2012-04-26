/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompilerListener;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.UrlDartSource;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartBooleanLiteral;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartClassMember;
import com.google.dart.compiler.ast.DartExprStmt;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartSyntheticErrorExpression;
import com.google.dart.compiler.ast.DartSyntheticErrorIdentifier;
import com.google.dart.compiler.ast.DartSyntheticErrorStatement;
import com.google.dart.compiler.ast.DartThisExpression;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartTypeParameter;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.parser.DartScannerParserContext;
import com.google.dart.compiler.parser.ParserContext;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.ClassNodeElement;
import com.google.dart.compiler.resolver.ConstructorElement;
import com.google.dart.compiler.resolver.CoreTypeProvider;
import com.google.dart.compiler.resolver.CoreTypeProviderImplementation;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.ElementKind;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.compiler.resolver.FieldNodeElement;
import com.google.dart.compiler.resolver.FunctionAliasElement;
import com.google.dart.compiler.resolver.LibraryElement;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.resolver.MethodNodeElement;
import com.google.dart.compiler.resolver.NodeElement;
import com.google.dart.compiler.resolver.ResolutionContext;
import com.google.dart.compiler.resolver.Resolver;
import com.google.dart.compiler.resolver.Scope;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.type.FunctionType;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.TypeAnalyzer;
import com.google.dart.compiler.type.TypeKind;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.completion.CompletionMetrics;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.completion.CompletionRequestor;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.completion.ScopedNameFinder.ScopedName;
import com.google.dart.tools.core.internal.completion.ast.BlockCompleter;
import com.google.dart.tools.core.internal.completion.ast.FunctionCompleter;
import com.google.dart.tools.core.internal.completion.ast.MethodInvocationCompleter;
import com.google.dart.tools.core.internal.completion.ast.ParameterCompleter;
import com.google.dart.tools.core.internal.completion.ast.PropertyAccessCompleter;
import com.google.dart.tools.core.internal.completion.ast.TypeCompleter;
import com.google.dart.tools.core.internal.completion.ast.TypeParameterCompleter;
import com.google.dart.tools.core.internal.model.DartFunctionTypeAliasImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.search.listener.GatheringSearchListener;
import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.internal.util.TypeUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibraryImport;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.search.MatchKind;
import com.google.dart.tools.core.search.MatchQuality;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchPatternFactory;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

/**
 * The analysis engine for code completion. It uses visitors to break the analysis up into single
 * AST node-sized chunks.
 * <p>
 * Within the visitors the analyses are annotated with a shorthand of the AST form required to hit
 * that analysis block. A '!' is used to indicate the completion location. There may be multiple '!'
 * in a single example to indicate several distinct completions points that drive the same analysis.
 * A single space indicates arbitrary whitespace; imagine each as a newline to get a better idea of
 * the code patterns involved.
 */
public class CompletionEngine {

  /**
   * Default metrics used if either DEBUG or DEBUG_TIMING options are true
   */
  private class DebugMetrics extends CompletionMetrics {
    @Override
    public void completionBegin(CompilationUnit sourceUnit, int completionPosition) {
      if (DEBUG) {
        System.out.print("COMPLETION IN "); //$NON-NLS-1$
        System.out.print(sourceUnit.getPath());
        System.out.print(" AT POSITION "); //$NON-NLS-1$
        System.out.println(completionPosition);
        System.out.println("COMPLETION - Source :"); //$NON-NLS-1$
        try {
          System.out.println(sourceUnit.getSource());
        } catch (DartModelException e) {
          System.out.println(e);
        }
      }
    }

    @Override
    public void completionException(Exception e) {
      DartCore.logError(e);
      if (DEBUG) {
        System.out.println("Exception caught by CompletionEngine:"); //$NON-NLS-1$
        e.printStackTrace(System.out);
      }
    }

    @Override
    public void resolveLibraryFailed(Collection<DartCompilationError> parseErrors) {
      reportResolveLibraryFailed(parseErrors);
    }

    @Override
    public void resolveLibraryTime(long ms) {
      if (DartCoreDebug.ENABLE_CONTENT_ASSIST_TIMING) {
        System.out.println("Code Assist (resolve library): " + ms);
      }
    }

    @Override
    public void visitorNotImplementedYet(DartNode node, DartNode sourceNode,
        Class<? extends ASTVisitor<Void>> astClass) {
      if (DEBUG) {
        System.out.print("Need visitor for node: " + node.getClass().getSimpleName());
        if (sourceNode != node) {
          System.out.print(" for " + sourceNode.getClass().getSimpleName());
        }
        System.out.println(" in " + astClass.getSimpleName());
      }
    }
  }

  /**
   * In cases where the analysis is driven by an identifier, a finer-grained analysis is required.
   * The primary analyzer defers to this analyzer to propose completions based on the structure of
   * the parent of the identifier.
   */
  private class IdentifierCompletionProposer extends ASTVisitor<Void> {
    private DartIdentifier identifier;

    private IdentifierCompletionProposer(DartIdentifier node) {
      this.identifier = node;
    }

    @Override
    public Void visitBlock(DartBlock node) {
      // between statements
      return null;
    }

    @Override
    public Void visitExpression(DartExpression node) {
      // { xc = yc = MA! } and many others
      proposeIdentifierPrefixCompletions(identifier);
      return null;
    }

    @Override
    public Void visitExprStmt(DartExprStmt completionNode) {
      // { v! } or { x; v! }
      proposeIdentifierPrefixCompletions(completionNode);
      return null;
    }

    @Override
    public Void visitField(DartField node) {
      if (node.getValue() == identifier) {
        // { int f = Ma! }
        proposeIdentifierPrefixCompletions(node);
      } else if (node.getName() == identifier) {
        // { static final num MA! }
        proposeIdentifierPrefixCompletions(node);
      }
      return null;
    }

    @Override
    public Void visitIfStatement(DartIfStatement completionNode) {
      // { if (v!) }
      proposeIdentifierPrefixCompletions(completionNode);
      return null;
    }

    @Override
    public Void visitMethodDefinition(DartMethodDefinition node) {
      if (node.getName() == identifier) {
        if (isCompletionAfterDot && identifier.getName().isEmpty()) {
          return null;
        }
        proposeIdentifierPrefixCompletions(identifier);
        return null;
      }
      if (!(isCompletionAfterDot && node.getName() instanceof DartIdentifier)) {
        proposeIdentifierPrefixCompletions(identifier);
      }
      return null;
    }

    @Override
    public Void visitMethodInvocation(DartMethodInvocation completionNode) {
      if (completionNode instanceof MethodInvocationCompleter) {
        DartIdentifier methodName = completionNode.getFunctionName();
        if (methodName == identifier) {
          // { x.y! }
          Type type = analyzeType(completionNode.getTarget());
          if (TypeKind.of(type) == TypeKind.VOID) {
            DartExpression exp = completionNode.getTarget();
            if (exp instanceof DartIdentifier) {
              Element element = ((DartIdentifier) exp).getElement();
              type = element.getType();
            }
          }
          createCompletionsForQualifiedMemberAccess(methodName, type, false);
        } else {
          // { x!.y } or { x.y(a!) } or { x.y(a, b, C!) }
          // TODO Consider using proposeIdentifierPrefixCompletions() here
          proposeVariables(completionNode, identifier, resolvedMember);
          DartClass classDef = (DartClass) resolvedMember.getParent();
          ClassElement elem = classDef.getElement();
          Type type = elem.getType();
          createCompletionsForPropertyAccess(identifier, type, false, false);
        }
      }
      return null;
    }

    @Override
    public Void visitNewExpression(DartNewExpression node) {
      // { new x! }
      List<SearchMatch> matches = findTypesWithPrefix(identifier);
      if (matches == null || matches.size() == 0) {
        return null;
      }
      for (SearchMatch match : matches) {
        String prefix = extractFilterPrefix(identifier);
        createTypeCompletionsForConstructor(identifier, match, prefix);
      }
      return null;
    }

    @Override
    public Void visitNode(DartNode node) {
      visitorNotImplementedYet(node, identifier, getClass());
      return null;
    }

    @Override
    public Void visitPropertyAccess(DartPropertyAccess completionNode) {
      if (completionNode instanceof PropertyAccessCompleter) {
        DartIdentifier propertyName = completionNode.getName();
        if (propertyName == identifier) {
          Type type = analyzeType(completionNode.getQualifier());
          if (type.getKind() == TypeKind.DYNAMIC || type.getKind() == TypeKind.VOID) {
            if (completionNode.getQualifier() instanceof DartIdentifier) {
              Element element = ((DartIdentifier) completionNode.getQualifier()).getElement();
              if (ElementKind.of(element) == ElementKind.CLASS) {
                type = element.getType();
                if (type instanceof InterfaceType) {
                  // { Array.! } or { Array.f! }
                  createCompletionsForFactoryInvocation(propertyName, (InterfaceType) type);
                  createCompletionsForQualifiedMemberAccess(propertyName, type, false);
                }
              } else if (ElementKind.of(element) == ElementKind.LIBRARY) {
                // #import('dart:html', prefix: 'html'); class X{ html.!DivElement! div; }
                createCompletionsForLibraryPrefix(propertyName, (LibraryElement) element);
              } else {
                createCompletionsForQualifiedMemberAccess(propertyName, type, false);
              }
            } else {
              createCompletionsForQualifiedMemberAccess(propertyName, type, false);
            }
          } else {
            if (type instanceof InterfaceType) {
              DartNode q = completionNode.getQualifier();
              if (q instanceof DartTypeNode) {
                createCompletionsForFactoryInvocation(propertyName, (InterfaceType) type);
              } else {
                createCompletionsForQualifiedMemberAccess(propertyName, type, false);
              }
            } else {
              // { a.! } or { a.x! }
              boolean isInstance = completionNode.getQualifier() instanceof DartThisExpression;
              createCompletionsForQualifiedMemberAccess(propertyName, type, isInstance);
            }
          }
        } else {
          DartNode q = completionNode.getQualifier();
          if (q instanceof DartIdentifier) {
            DartIdentifier qualifier = (DartIdentifier) q;
            proposeIdentifierPrefixCompletions(qualifier);
          } else if (q instanceof DartTypeNode) {
            DartTypeNode type = (DartTypeNode) q;
            DartNode name = type.getIdentifier();
            if (name instanceof DartIdentifier) {
              DartIdentifier id = (DartIdentifier) name;
              proposeTypesForPrefix(id);
            }
          }
        }
      }
      return null;
    }

    @Override
    public Void visitReturnStatement(DartReturnStatement completionNode) {
      // { return v! }
      proposeIdentifierPrefixCompletions(completionNode);
      return null;
    }

    @Override
    public Void visitStringInterpolation(DartStringInterpolation node) {
      // "$h!"
      proposeIdentifierPrefixCompletions(identifier);
      return null;
    }

    @Override
    public Void visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
      // { super.x!(); }
      ConstructorElement cn = node.getElement();
      if (cn != null) {
        ClassElement ce = cn.getConstructorType();
        if (ce != null) {
          // TODO Restrict proposals to constructors
          createCompletionsForMethodInvocation(identifier, ce.getType(), true, false);
        }
      }
      return null;
    }

    @Override
    public Void visitTypeNode(DartTypeNode completionNode) {
      if (completionNode instanceof TypeCompleter) {
        TypeCompleter typeCompleter = (TypeCompleter) completionNode;
        Stack<Mark> s = typeCompleter.getCompletionParsingContext();
        if (s.size() < 2) {
          throw new IllegalArgumentException(); // TODO Clean up: this should never happen
        }
        if (s.peek() == Mark.ConstructorName) {
          // { new x! }
          typeCompleter.getParent().accept(this);
        } else {
          Mark m = s.elementAt(s.size() - 2);
          switch (m) {
            case FormalParameter:
              // bar(x,!) {} or bar(!, int x) {} or bar(B! x) {}
              if (identifier.getName().length() == 0) {
                proposeTypesForNewParam();
                break;
              } else {
                proposeGenericTypeCompletions(typeCompleter);
                proposeTypesForPrefix(identifier);
              }
              break;
            case ForInitialization:
              // {for (in!t x = 0; i < 5; i++); }
            case TypeFunctionOrVariable:
              // { x; v! x; } or { v! x; }
              proposeGenericTypeCompletions(typeCompleter);
              proposeIdentifierPrefixCompletions(typeCompleter);
              break;
            case FunctionLiteral:
              // at top level
              // final num PI2 = Mat!
            case TypeParameter:
              // class X<K extends Ha!shable> {}
              proposeTypesForPrefix(identifier);
              break;
            case TypeExpression:
              // { if (x is !)}
              proposeGenericTypeCompletions(typeCompleter);
              proposeTypesForPrefix(identifier);
              break;
            case ClassBody:
              // class x extends A! (A may be empty string)
              // class x implements I! (I may be empty string)
              // interface x extends I!
              DartClass classDef = (DartClass) typeCompleter.getParent();
              boolean isClassDef = classDef.getSuperclass() == typeCompleter;
              proposeClassOrInterfaceNamesForPrefix(identifier, isClassDef);
              break;
            case ClassMember:
              // class x { ! }
              // TODO check for supertype methods whose name starts with identifier
              // if found propose a new method matching its signature
              proposeGenericTypeCompletions(typeCompleter);
              proposeTypesForPrefix(identifier, true);
              break;
            case TopLevelElement:
              if (completionNode.getParent() instanceof DartFunctionTypeAlias) {
                // typedef T!
                proposeTypesForPrefix(identifier, true);
              }
              break;
            case FunctionTypeInterface:
              proposeTypesForPrefix(identifier, true);
          }
        }
      }
      return null;
    }

    @Override
    public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
      // { bar!(); } or { bar(z!); }
      proposeIdentifierPrefixCompletions(node);
      return null;
    }

    @Override
    public Void visitVariable(DartVariable node) {
      // { X a = b! }
      proposeIdentifierPrefixCompletions(identifier);
      return null;
    }

    @Override
    public Void visitVariableStatement(DartVariableStatement completionNode) {
      if (completionNode instanceof DartVariableStatement) {
//      DartIdentifier propertyName = completionNode.getName();
//      if (propertyName.getSourceInfo().getSourceStart() > actualCompletionPosition) {
//        propertyName = null;
//      }
//      Type type = analyzeType(completionNode.getQualifier());
//      createCompletionsForQualifiedMemberAccess(propertyName, type);
      }
      return null;
    }

    @Override
    public Void visitWhileStatement(DartWhileStatement node) {
      if (node.getCondition() == this.identifier) {
        proposeIdentifierPrefixCompletions(identifier);
      }
      return null;
    }

    private void proposeIdentifierPrefixCompletions(DartNode node) {
      // Complete an unqualified identifier prefix.
      // We know there is a prefix, otherwise the parser would not produce a DartIdentifier.
      // TODO Determine if statics are being handled properly
      boolean isStatic = resolvedMember.getModifiers().isStatic();
      createCompletionsForLocalVariables(identifier, identifier, resolvedMember);
      Element parentElement = resolvedMember.getElement().getEnclosingElement();
      if (parentElement instanceof ClassElement) {
        Type type = ((ClassElement) parentElement).getType();
        createCompletionsForPropertyAccess(identifier, type, false, isStatic);
        createCompletionsForMethodInvocation(identifier, type, false, isStatic);
        proposeTypesForPrefix(identifier, false);
      } else {
        // TODO top-level element
        proposeTypesForPrefix(identifier, false);
      }
    }
  }

  /**
   * Collects all method and field definitions from every class in all known libraries.
   */
  private static class MemberElementVisitor extends ElementVisitor {

    String prefix = null;
    boolean mustIncludeStatics = true;
    private Set<Element> elements = new HashSet<Element>();

    public MemberElementVisitor(String prefix, boolean mustIncludeStatics) {
      this.prefix = prefix; // may be null
      this.mustIncludeStatics = mustIncludeStatics;
    }

    @Override
    public void element(FieldNodeElement element) {
      if (mustIncludeStatics || !element.isStatic()) {
        add(element);
      }
    }

    @Override
    public void element(MethodNodeElement element) {
      if (mustIncludeStatics || !element.isStatic()) {
        add(element);
      }
    }

    public Set<Element> getElements() {
      return elements;
    }

    private void add(NodeElement element) {
      if (filter(element.getName())) {
        elements.add(element);
      }
    }

    private boolean filter(String name) {
      if (prefix == null) {
        return true;
      } else {
        return name.startsWith(prefix);
      }
    }
  }

  /**
   * In most cases completion processing begins at an identifier. The identifier itself is not very
   * informative so most identifiers defer to their parent node for analysis.
   */
  private class OuterCompletionProposer extends ASTVisitor<Void> {
    private DartNode completionNode;

    private OuterCompletionProposer(DartNode node) {
      this.completionNode = node;
    }

    @Override
    public Void visitBlock(DartBlock node) {
      if (node instanceof BlockCompleter) {
        BlockCompleter block = (BlockCompleter) node;
        Stack<Mark> stack = block.getCompletionParsingContext();
        if (stack.isEmpty() || stack.peek() == Mark.Block || stack.peek() == Mark.ClassMember) {
          Element parentElement = resolvedMember.getElement().getEnclosingElement();
          if (parentElement instanceof ClassElement) {
            // between statements: { ! } or { ! x; ! y; ! }
            boolean isStatic = resolvedMember.getModifiers().isStatic();
            createCompletionsForLocalVariables(block, null, resolvedMember);
            Type type = ((ClassElement) parentElement).getType();
            createCompletionsForPropertyAccess(null, type, false, isStatic);
            createCompletionsForMethodInvocation(null, type, false, isStatic);
            // Types are legal here but we are not proposing them since they are optional
          } else {
            // another case of error recovery producing odd AST shapes
            // an incomplete new expr at the end of a block has a source range that terminates prior
            // to the completion position so it is not found when searching for the completion node
            // class XXX {XXX.fisk();}main() {main(); new !}}
            List<DartStatement> stmts = node.getStatements();
            if (stmts.size() > 0) {
              DartStatement stmt = stmts.get(stmts.size() - 1);
              if (stmt instanceof DartExprStmt) {
                DartExpression expr = ((DartExprStmt) stmt).getExpression();
                if (expr instanceof DartNewExpression) {
                  if (actualCompletionPosition >= expr.getSourceInfo().getEnd()) {
                    // { ... new ! }
                    return ((DartNewExpression) expr).accept(this);
                  }
                }
              }
            }
          }
        } else if (stack.peek() == Mark.FunctionStatementBody) {
          if (block.getStatements().isEmpty() && block.getParent() instanceof FunctionCompleter) {
            if (block.getParent().getParent() instanceof DartMethodDefinition) {
              // this odd AST is the price of better error recovery
              DartMethodDefinition method = (DartMethodDefinition) block.getParent().getParent();
              if (actualCompletionPosition + 1 == method.getName().getSourceInfo().getEnd()) {
                DartExpression expr = method.getName();
                if (expr instanceof DartIdentifier) {
                  SourceInfo src = method.getSourceInfo();
                  String nameString = source.substring(src.getOffset(), src.getEnd());
                  if (nameString.indexOf('.') >= 0) {
                    return null;
                  }
                  SyntheticIdentifier synth = new SyntheticIdentifier(nameString,
                      actualCompletionPosition - nameString.length() + 1, nameString.length());
                  method.accept(new IdentifierCompletionProposer(synth));
                } else if (expr instanceof DartPropertyAccess) {
                  expr.accept(this);
                }
              }
            }
          }
        }
      }
      return null;
    }

    @Override
    public Void visitBooleanLiteral(DartBooleanLiteral node) {
      createProposalsForLiterals(node, "false", "true");
      // TODO Should we add identifiers here?
      return null;
    }

    @Override
    public Void visitClass(DartClass node) {
      String classSrc = source.substring(node.getSourceInfo().getOffset(),
          actualCompletionPosition + 1);
      int completionPos = actualCompletionPosition + 1 - node.getSourceInfo().getOffset();
      boolean beforeBrace = classSrc.indexOf('{') < 0;
      if (beforeBrace) {
        int extendsLoc = classSrc.indexOf(C_EXTENDS);
        int implementsLoc = classSrc.indexOf(C_IMPLEMENTS);
        if (extendsLoc < 0 && implementsLoc < 0) {
          return null;
        }
        boolean isClassDef = false;
        if (extendsLoc > 0) {
          int extendsEnd = extendsLoc + C_EXTENDS.length();
          if (completionPos <= extendsEnd) {
            return null;
          }
          DartNode sc = node.getSuperclass();
          if (sc == null) {
            isClassDef = false; // parsing an interface
          } else {
            if (actualCompletionPosition >= sc.getSourceInfo().getOffset() && implementsLoc < 0) {
              return null;
            }
            if (implementsLoc < completionPos || implementsLoc < 0) {
              isClassDef = true;
            }
          }
        }
        if (implementsLoc > 0) {
          isClassDef = false;
          int implementsEnd = implementsLoc + C_IMPLEMENTS.length();
          if (implementsLoc < completionPos && completionPos <= implementsEnd) {
            return null;
          }
        }
        proposeClassOrInterfaceNamesForPrefix(null, isClassDef);
      } else {
        // for top-level elements, try type names
        proposeTypesForNewParam();
        createProposalsForLiterals(node, C_VOID);
      }
      return null;
    }

    @Override
    public Void visitExprStmt(DartExprStmt node) {
      DartExpression expr = node.getExpression();
      if (expr instanceof DartNewExpression) {
        expr.accept(this);
      }
      return null;
    }

    @Override
    public Void visitField(DartField node) {
      // { int f = Ma! }
      DartIdentifier name = node.getName();
      int begin = name.getSourceInfo().getOffset();
      int len = name.getSourceInfo().getLength();
      if (begin <= actualCompletionPosition + 1 && actualCompletionPosition < begin + len) {
        // bug in visitor does not visit name
        return node.accept(new IdentifierCompletionProposer(name));
      }
      return null;
    }

    @Override
    public Void visitFunction(DartFunction node) {
      if (node instanceof FunctionCompleter) {
        if (node.getParent() instanceof DartMethodDefinition) {
          DartMethodDefinition methodDef = (DartMethodDefinition) node.getParent();
          DartExpression methodName = methodDef.getName();
          if (methodName instanceof DartIdentifier && isCompletionNode(methodName)) {
            // { const B!ara(); }
            methodDef.accept(new IdentifierCompletionProposer((DartIdentifier) methodName));
            return null;
          }
        }
        // new parameter: bar(!) {} or bar(! int x) {} or bar(x, B !) {}
        List<DartParameter> params = node.getParameters();
        if (params.isEmpty()) {
          proposeTypesForNewParam();
        } else {
          DartParameter param = params.get(0);
          boolean beforeFirstParam = actualCompletionPosition < param.getSourceInfo().getOffset();
          if (beforeFirstParam) {
            if (node.getParent() instanceof DartMethodDefinition) {
              DartMethodDefinition methodDef = (DartMethodDefinition) node.getParent();
              DartExpression methodName = methodDef.getName();
              if (methodName instanceof DartIdentifier) {
                // TODO check for supertype methods whose name starts with identifier and
                // matches the return type, if found propose a new method matching its signature
                proposeTypesForNewParam();
              } else {
                // TODO qualified names
              }
            } else {
              proposeTypesForNewParam();
              createProposalsForLiterals(node, C_VOID);
            }
          } else {
            param = params.get(params.size() - 1);
            int end = param.getSourceInfo().getOffset() + param.getSourceInfo().getLength();
            boolean afterLastParam = actualCompletionPosition >= end;
            if (afterLastParam) {
              proposeTypesForNewParam();
            }
          }
        }
      }
      return null;
    }

    @Override
    public Void visitFunctionTypeAlias(DartFunctionTypeAlias node) {
      // TODO: This should not need to recurse, but the inner node is not being found
      node.visitChildren(this);
      return null;
    }

    @Override
    public Void visitIdentifier(DartIdentifier node) {
      DartNode parent = node.getParent();
      return parent.accept(new IdentifierCompletionProposer(node));
    }

    @Override
    public Void visitIfStatement(DartIfStatement completionNode) {
      // { if (v!) }
      createCompletionsForLocalVariables(completionNode, null, resolvedMember);
      boolean isStatic = resolvedMember.getModifiers().isStatic();
      Element parentElement = resolvedMember.getElement().getEnclosingElement();
      if (parentElement instanceof ClassElement) {
        Type type = ((ClassElement) parentElement).getType();
        createCompletionsForPropertyAccess(null, type, false, isStatic);
        createCompletionsForMethodInvocation(null, type, false, isStatic);
      }
      return null;
    }

    @Override
    public Void visitMethodInvocation(DartMethodInvocation completionNode) {
      if (completionNode instanceof MethodInvocationCompleter) {
        DartIdentifier functionName = completionNode.getFunctionName();
        int nameStart = functionName.getSourceInfo().getOffset();
        if (!(actualCompletionPosition >= nameStart + functionName.getSourceInfo().getLength())) {
          if (nameStart > actualCompletionPosition) {
            functionName = null;
          }
          // { foo.! doFoo(); }
          Type type = analyzeType(completionNode.getTarget());
          if (type != null) {
            createCompletionsForQualifiedMemberAccess(functionName, type, false);
          }
        }
      }
      return null;
    }

    @Override
    public Void visitNewExpression(DartNewExpression node) {
      char lastCh = source.charAt(actualCompletionPosition);
      if (lastCh == 'w' || lastCh == 't') {
        // no space after 'new' { new! } { const! }
        if (resolvedMember != null) {
          // TODO generalize and reuse single definition of this block
          boolean isStatic = resolvedMember.getModifiers().isStatic();
          String name = lastCh == 'w' ? "new" : "const";
          SyntheticIdentifier synth = new SyntheticIdentifier(name, actualCompletionPosition
              - name.length() + 1, name.length());
          createCompletionsForLocalVariables(node, synth, resolvedMember);
          Element parentElement = resolvedMember.getElement().getEnclosingElement();
          if (parentElement.getKind() == ElementKind.CLASS) {
            Type type = ((ClassElement) parentElement).getType();
            createCompletionsForPropertyAccess(synth, type, false, isStatic);
            createCompletionsForMethodInvocation(synth, type, false, isStatic);
          }
          return null;
        }
      } else {
        // { new ! }
        List<SearchMatch> matches = findTypesWithPrefix(null);
        if (matches == null || matches.size() == 0) {
          return null;
        }
        for (SearchMatch match : matches) {
          createTypeCompletionsForConstructor(null, match, "");
        }
      }
      return null;
    }

    @Override
    public Void visitNode(DartNode node) {
      visitorNotImplementedYet(node, this.completionNode, getClass());
      return null;
    }

    @Override
    public Void visitParameter(DartParameter node) {
      // parameter type prefix: bar(B!) {} or bar(1, B!) {}
      if (node instanceof ParameterCompleter) {
        ParameterCompleter param = (ParameterCompleter) node;
        // when completion is requested on the first word of a param decl we assume it is a type
        DartExpression typeName = param.getName();
        if (typeName.getSourceInfo().getOffset() <= actualCompletionPosition
            && typeName.getSourceInfo().getOffset() + typeName.getSourceInfo().getLength() >= actualCompletionPosition) {
          if (typeName instanceof DartIdentifier) {
            DartIdentifier typeId = (DartIdentifier) typeName;
            List<SearchMatch> matches = findTypesWithPrefix(typeId);
            if (matches == null || matches.size() == 0) {
              return null;
            }
            for (SearchMatch match : matches) {
              String prefix = extractFilterPrefix(typeId);
              createTypeCompletionsForParameterDecl(typeId, match, prefix);
            }
          } else if (typeName instanceof DartPropertyAccess) {
            DartPropertyAccess prop = (DartPropertyAccess) typeName;
            if (isCompletionAfterDot
                || actualCompletionPosition + 1 >= prop.getName().getSourceInfo().getOffset()) {
              // { class X { X(this.!c) : super() {}}
              typeName.accept(this);
            }
          }
        }
      }
      return null;
    }

    @Override
    public Void visitPropertyAccess(DartPropertyAccess completionNode) {
      if (completionNode instanceof PropertyAccessCompleter) {
        DartIdentifier propertyName = completionNode.getName();
        if (propertyName.getSourceInfo().getOffset() > actualCompletionPosition) {
          propertyName = null;
        }
        // { foo.! } or { class X { X(this.!c) : super() {}}
        Type type = analyzeType(completionNode.getQualifier());
        if (TypeKind.of(type) == TypeKind.DYNAMIC) {
          // if dynamic use ScopedNameFinder to look for a declaration
          // { List list; list.! Map map; }
          DartNode qualifier = completionNode.getQualifier();
          DartIdentifier name;
          if (qualifier instanceof DartIdentifier) {
            name = (DartIdentifier) qualifier;
          } else {
            name = ((DartPropertyAccess) qualifier).getName();
          }
          Element element = name.getElement();
          ScopedNameFinder vars = new ScopedNameFinder(actualCompletionPosition);
          completionNode.accept(vars);
          ScopedName varName = vars.getLocals().get(name.getName());
          if (varName != null) {
            element = varName.getSymbol();
            type = element.getType();
          }
        }
        createCompletionsForQualifiedMemberAccess(propertyName, type, false);
      }
      return null;
    }

    @Override
    public Void visitStringLiteral(DartStringLiteral node) {
      DartNode parent = node.getParent();
      if (parent instanceof DartStringInterpolation) {
        DartStringInterpolation interp = (DartStringInterpolation) parent;
        List<DartStringLiteral> iStrings = interp.getStrings();
        List<DartExpression> iExprs = interp.getExpressions();
        // the source positions of string interpolation nodes are not recorded
        // so we will check that we could have a variable interpolation but will not
        // try to be too strict, since we cannot
        if (iStrings.size() >= 1) {
          for (int idxStrings = 1, idxExprs = 0; idxStrings < iStrings.size(); idxStrings++, idxExprs++) {
            DartStringLiteral lit = iStrings.get(idxStrings);
            DartExpression exp = iExprs.get(idxExprs);
            if (lit.getValue().isEmpty() && exp instanceof DartSyntheticErrorExpression) {
              // "$!"
              createCompletionsForLocalVariables(node, null, resolvedMember);
              Element parentElement = resolvedMember.getElement().getEnclosingElement();
              if (parentElement instanceof ClassElement) {
                Type type = ((ClassElement) parentElement).getType();
                boolean isStatic = resolvedMember.getModifiers().isStatic();
                createCompletionsForPropertyAccess(null, type, false, isStatic);
                createCompletionsForMethodInvocation(null, type, false, isStatic);
              }
              break;
            }
          }
        }
      }
      return null;
    }

    @Override
    public Void visitSyntheticErrorExpression(DartSyntheticErrorExpression node) {
      return node.getParent().accept(this);
    }

    @Override
    public Void visitSyntheticErrorIdentifier(DartSyntheticErrorIdentifier node) {
      return node.getParent().accept(this);
    }

    @Override
    public Void visitSyntheticErrorStatement(DartSyntheticErrorStatement node) {
      return node.getParent().accept(this);
    }

    @Override
    public Void visitThisExpression(DartThisExpression node) {
      // { this! } the only legal continuation is punctuation, which we do not propose
      // you can't get here directly, this occurs when backspacing from {this.!}
      return null;
    }

    @Override
    public Void visitTypeNode(DartTypeNode completionNode) {
      if (completionNode instanceof TypeCompleter) {
        if (completionNode.getParent() instanceof DartPropertyAccess) {
          if (completionNode.getParent().getParent() instanceof DartNewExpression) {
            // { new X.! }
            DartPropertyAccess prop = (DartPropertyAccess) completionNode.getParent();
            prop.accept(new IdentifierCompletionProposer(prop.getName()));
          }
        } else if (completionNode.getParent() instanceof DartTypeParameter) {
          // < T extends !>
          if (completionNode.getIdentifier() instanceof DartIdentifier) {
            proposeTypeNamesForPrefix((DartIdentifier) completionNode.getIdentifier());
          }
        }
      }
      return null;
    }

    @Override
    public Void visitTypeParameter(DartTypeParameter node) {
      // class test <K extends ! {}
      // typedef T Foo<T!>(Object input);
      if (node instanceof TypeParameterCompleter) {
        if (node.getBound() != null) {
          DartNode name = node.getBound().getIdentifier();
          int start = node.getSourceInfo().getOffset();
          String src = source.substring(start, start + node.getSourceInfo().getLength());
          int n = src.indexOf(C_EXTENDS);
          if (actualCompletionPosition - start >= n + C_EXTENDS.length()) {
            if (name instanceof DartIdentifier) {
              proposeTypeNamesForPrefix((DartIdentifier) name);
            }
          }
        }
      }
      // TODO(zundel): This should not recurse
      node.visitChildren(this);
      return null;
    }

    @Override
    public Void visitUnit(DartUnit node) {
      return null;
    }

    @Override
    public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
      // { bar( ! ); } or { bar(! x); } or { bar(x !); } or { bar(x,! y); }
      Element baseElement = node.getTarget().getElement();
      if (ElementKind.of(baseElement) == ElementKind.METHOD) {
        MethodElement methodElement = (MethodElement) baseElement;
        List<VariableElement> paramDefs = methodElement.getParameters();
        if (paramDefs.size() == 0) {
          // assume a new param will be added to method def
          createCompletionsForLocalVariables(node, null, resolvedMember);
        } else {
          List<DartExpression> args = node.getArguments();
          if (args.size() == 0) {
            createCompletionsForLocalVariables(node, null, resolvedMember);
          } else {
            // could do positional type matching to order proposals
            createCompletionsForLocalVariables(node, null, resolvedMember);
          }
        }
      }
      return null;
    }

    @Override
    public Void visitVariable(DartVariable node) {
      if (source.charAt(actualCompletionPosition) == '=') {
        // { num y =! } Note: no space after equals
        proposeVariables(completionNode, null, resolvedMember);
      }
      return null;
    }

    @Override
    public Void visitVariableStatement(DartVariableStatement completionNode) {
      if (completionNode instanceof DartVariableStatement) {
        List<DartVariable> vars = completionNode.getVariables();
        if (vars.size() > 0) {
          DartVariable var = vars.get(vars.size() - 1);
          if (var.getSourceInfo().getOffset() + var.getSourceInfo().getLength() <= actualCompletionPosition) {
            // { num theta = i * ! }
            proposeVariables(completionNode, null, resolvedMember);
            proposeTypesForNewParam();
          }
        }
        return null;
      }
      return null;
    }
  }

  @SuppressWarnings("unused")
  private static class SyntheticIdentifier extends DartIdentifier {
    private static final long serialVersionUID = 1L;
    private int srcStart, srcLen;

    SyntheticIdentifier(String name, int srcStart, int srcLen) {
      super(name);
      setSourceInfo(new SourceInfo(null, srcStart, srcLen));
      this.srcStart = srcStart;
      this.srcLen = srcLen;
    }
  }

  private static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("com.google.dart.tools.ui/debug/CompletionEngine"));

  public static char[][] createDefaultParameterNames(int length) {
    char[][] names = new char[length][];
    for (int i = 0; i < length; i++) {
      names[i] = ("p" + (i + 1)).toCharArray();
    }
    return names;
  }

  static private int countPositionalParameters(List<VariableElement> params) {
    int posParamCount = 0;
    for (VariableElement elem : params) {
      if (elem.getModifiers().isNamed()) {
        break;
      }
      posParamCount++;
    }
    return posParamCount;
  }

  static private List<Element> findAllElements(LibraryUnit library, String prefix) {
    Set<Element> elemSet = findAllElements(library, prefix, new HashSet<LibraryUnit>());
    List<Element> elements = new ArrayList<Element>(elemSet.size());
    elements.addAll(elemSet);
    return elements;
  }

  static private Set<Element> findAllElements(LibraryUnit library, String prefix,
      Set<LibraryUnit> libs) {
    if (libs.contains(library)) {
      return new HashSet<Element>();
    }
    libs.add(library);
    MemberElementVisitor visitor = new MemberElementVisitor(prefix, false);
    for (DartUnit unit : library.getUnits()) {
      unit.accept(visitor);
    }
    Set<Element> elements = visitor.getElements();
    for (LibraryUnit lib : library.getImports()) {
      elements.addAll(findAllElements(lib, prefix, libs));
    }
    return elements;
  }

  static private List<Element> getAllElements(Type type) {
    Map<String, Element> map = new HashMap<String, Element>();
    List<Element> list = new ArrayList<Element>();
    List<InterfaceType> types = TypeUtil.allSupertypes((InterfaceType) type);
    for (InterfaceType itype : types) {
      ClassElement cls = itype.getElement();
      Iterable<? extends Element> members = cls.getMembers();
      for (Element elem : members) {
        String name = elem.getName();
        if (!map.containsKey(name)) {
          map.put(name, elem);
          list.add(elem);
        }
      }
    }
    return list;
  }

  static private List<Element> getConstructors(InterfaceType type) {
    List<Element> list = new ArrayList<Element>();
    for (Element elem : type.getElement().getConstructors()) {
      list.add(elem);
    }
    return list;
  }

  static private char[][] getParameterNames(FunctionAliasElement alias) {
    FunctionType type = alias.getFunctionType();
    List<Type> paramTypes = type.getParameterTypes();
    Set<String> dups = new HashSet<String>();
    char[][] names = new char[paramTypes.size()][];
    for (int i = 0; i < names.length; i++) {
      String name = paramTypes.get(i).getElement().getName();
      if (Character.isLowerCase(name.charAt(0))) {
        name = "x" + name;
      }
      if (dups.contains(name)) {
        String newName = name;
        int k = 1;
        while (dups.contains(newName)) {
          newName = name + k++;
        }
        name = newName;
      }
      dups.add(name);
      names[i] = name.toCharArray();
      names[i][0] = Character.toLowerCase(names[i][0]);
    }
    return names;
  }

  static private char[][] getParameterNames(Method method) {
    try {
      String[] paramNames = method.getParameterNames();
      int count = paramNames.length;
      char[][] names = new char[count][];
      for (int i = 0; i < count; i++) {
        names[i] = paramNames[i].toCharArray();
      }
      return names;
    } catch (DartModelException exception) {
      return CharOperation.NO_CHAR_CHAR;
    }
  }

  static private char[][] getParameterNames(MethodElement method) {
    List<VariableElement> params = method.getParameters();
    int posParamCount = countPositionalParameters(params);
    char[][] names = new char[posParamCount][];
    for (int i = 0; i < posParamCount; i++) {
      names[i] = params.get(i).getName().toCharArray();
    }
    return names;
  }

  static private char[][] getParameterTypeNames(FunctionAliasElement alias) {
    FunctionType type = alias.getFunctionType();
    List<Type> paramTypes = type.getParameterTypes();
    char[][] names = new char[paramTypes.size()][];
    for (int i = 0; i < names.length; i++) {
      names[i] = paramTypes.get(i).getElement().getName().toCharArray();
    }
    return names;
  }

  static private char[][] getParameterTypeNames(Method method) {
    try {
      String[] paramNames = method.getParameterTypeNames();
      int count = paramNames.length;
      char[][] names = new char[count][];
      for (int i = 0; i < count; i++) {
        names[i] = paramNames[i].toCharArray();
      }
      return names;
    } catch (DartModelException exception) {
      return CharOperation.NO_CHAR_CHAR;
    }
  }

  static private char[][] getParameterTypeNames(MethodElement method) {
    List<VariableElement> params = method.getParameters();
    int posParamCount = countPositionalParameters(params);
    char[][] names = new char[posParamCount][];
    for (int i = 0; i < posParamCount; i++) {
      names[i] = params.get(i).getType().getElement().getName().toCharArray();
    }
    return names;
  }

  // keys are either String or char[]; values are Object unless Type works
  public HashMap<Object, Object> typeCache;

  private CompletionEnvironment environment;
  private CompletionRequestor requestor;
  private DartProject project;
  private WorkingCopyOwner owner;
  private IProgressMonitor monitor;
  private IPath fileName;
  private int actualCompletionPosition;
  private int offset;
  private String source;
  private ErrorRecordingContext context = new ErrorRecordingContext();
  private DartClassMember<? extends DartExpression> resolvedMember;
  private CoreTypeProvider typeProvider;
  private ClassNodeElement classElement;
  private boolean isCompletionAfterDot;
  private DartUnit parsedUnit;
  private CompilationUnit currentCompilationUnit;
  private CompletionMetrics metrics;

  private static final String C_EXTENDS = "extends";
  private static final String C_IMPLEMENTS = "implements";
  private static final String C_VOID = "void";

  /**
   * @param options
   */
  public CompletionEngine(CompletionEnvironment environment, CompletionRequestor requestor,
      Hashtable<String, String> options, DartProject project, WorkingCopyOwner owner,
      IProgressMonitor monitor) {
    this.environment = environment;
    this.requestor = requestor;
    this.project = project;
    this.owner = owner;
    this.monitor = monitor;
    typeCache = new HashMap<Object, Object>();
    metrics = requestor.getMetrics();
    if (metrics == null && (DEBUG || DartCoreDebug.ENABLE_CONTENT_ASSIST_TIMING)) {
      metrics = new DebugMetrics();
    }
  }

  public void complete(CompilationUnit sourceUnit, int completionPosition, int pos)
      throws DartModelException {
    if (metrics != null) {
      metrics.completionBegin(sourceUnit, completionPosition);
    }
    if (monitor != null) {
      monitor.beginTask(Messages.engine_completing, IProgressMonitor.UNKNOWN);
    }
    try {
      fileName = sourceUnit.getPath();
      // look for the node that ends before the cursor position,
      // which may mean we start looking at a single-char node that is just
      // before the cursor position

      checkCancel();

      currentCompilationUnit = sourceUnit;
      String sourceParam = sourceUnit.getSource();
      DartSource sourceFile;
      LibrarySource library = ((DartLibraryImpl) sourceUnit.getLibrary()).getLibrarySourceFile();
      if (sourceUnit.getResource() == null) {
        sourceFile = sourceUnit.getSourceRef();
      } else {
        // TODO Find a better way to get the File?
        File file = sourceUnit.getBuffer().getUnderlyingResource().getRawLocation().toFile();
        sourceFile = new UrlDartSource(file, library);
      }

      complete(library, sourceFile, sourceParam, completionPosition, pos);

    } catch (ClassCastException e) {
      if (metrics != null) {
        metrics.completionException(e);
      } else {
        DartCore.logError(e);
      }
    } catch (DartModelException e) {
      if (metrics != null) {
        metrics.completionException(e);
      } else {
        DartCore.logError(e);
      }
    } catch (IndexOutOfBoundsException e) {
      if (metrics != null) {
        metrics.completionException(e);
      } else {
        DartCore.logError(e);
      }
    } catch (NullPointerException ex) {
      if (metrics != null) {
        metrics.completionException(ex);
      } else {
        DartCore.logError(ex);
      }
    }
    if (metrics != null) {
      metrics.completionEnd();
    }
  }

  /*
   * Visible for testing
   */
  public void complete(LibrarySource library, DartSource sourceFile, String sourceContent,
      int completionPosition, int pos) throws DartModelException {
    source = sourceContent;
    actualCompletionPosition = completionPosition - 1;
    offset = pos;
    isCompletionAfterDot = actualCompletionPosition >= 0
        && source.charAt(actualCompletionPosition) == '.';
    CompletionMetrics metrics = requestor.getMetrics();

    DartCompilerListener listener = DartCompilerListener.EMPTY;
    ParserContext ctx = new DartScannerParserContext(sourceFile, source, listener);
    Set<String> prefixes = new HashSet<String>();
    if (currentCompilationUnit != null) {
      DartLibraryImport[] imports = currentCompilationUnit.getLibrary().getImports();
      for (DartLibraryImport imp : imports) {
        String prefix = imp.getPrefix();
        if (prefix != null) {
          prefixes.add(prefix);
        }
      }
    }
    CompletionParser parser = new CompletionParser(ctx, prefixes);
    parser.setCompletionPosition(completionPosition);
    parsedUnit = parser.parseUnit(sourceFile);

    if (parsedUnit == null) {
      return;
    }
    if (parsedUnit.getTopLevelNodes().isEmpty()) {
      return;
    }
    Collection<DartCompilationError> parseErrors = new ArrayList<DartCompilationError>();

    NodeFinder finder = NodeFinder.find(parsedUnit, completionPosition, 0);
    DartNode resolvedNode = finder.selectNode();
    // NodeFinder returns the inner most node, in case of DartParameter - name of its type, or name.
    if (resolvedNode instanceof DartIdentifier && resolvedNode.getParent() instanceof DartParameter) {
      resolvedNode = resolvedNode.getParent();
    }
    resolvedMember = finder.getEnclosingMethod();
    if (resolvedMember == null) {
      resolvedMember = finder.getEnclosingField();
    }
    DartNode analyzedNode = null;
    if (resolvedNode != null) {
      long resolutionStartTime = DartCoreDebug.ENABLE_CONTENT_ASSIST_TIMING
          ? System.currentTimeMillis() : 0L;
      analyzedNode = DartCompilerUtilities.analyzeDelta(library, source, parsedUnit, resolvedNode,
          completionPosition, parseErrors);
      if (metrics != null) {
        metrics.resolveLibraryTime(System.currentTimeMillis() - resolutionStartTime);
      }
    }
    if (analyzedNode == null) {
      if (metrics != null) {
        metrics.resolveLibraryFailed(parseErrors);
      } else {
        reportResolveLibraryFailed(parseErrors);
      }
      return;
    }
    Scope unitScope = parsedUnit.getLibrary().getElement().getScope();
    typeProvider = new CoreTypeProviderImplementation(unitScope, DartCompilerListener.EMPTY);

    classElement = null;
    if (resolvedMember != null) {
      Element encElement = resolvedMember.getElement().getEnclosingElement();
      if (encElement instanceof ClassNodeElement) {
        classElement = (ClassNodeElement) encElement;
      }
    } else {
      DartClass resolvedClass = finder.getEnclosingClass();
      if (resolvedClass != null) {
        classElement = resolvedClass.getElement();
      }
    }
    context.reset();
    Resolver resolver = new Resolver(context, unitScope, typeProvider);

    ResolutionContext resolutionContext = new ResolutionContext(unitScope, context, typeProvider);
    if (classElement != null) {
      resolutionContext = resolutionContext.extend(classElement);
    }
    NodeElement member = resolvedMember == null ? classElement : resolvedMember.getElement();
    try {
      resolver.resolveMember(classElement, member, resolutionContext);
    } catch (AssertionError ex) {
      // Expected if completing an extends or implements clause of type declaration
    } catch (NullPointerException ex) {
      // Expected if completing an extends or implements clause of type declaration
    } catch (Throwable ex) {
      DartCore.logError(ex);
      ex.printStackTrace();
    }
    requestor.beginReporting();
    requestor.acceptContext(new InternalCompletionContext());
    resolvedNode.accept(new OuterCompletionProposer(resolvedNode));
    requestor.endReporting();
  }

  public CompletionEnvironment getEnvironment() {
    return environment;
  }

  public IProgressMonitor getMonitor() {
    return monitor;
  }

  public WorkingCopyOwner getOwner() {
    return owner;
  }

  public DartProject getProject() {
    return project;
  }

  public CompletionRequestor getRequestor() {
    return requestor;
  }

  public HashMap<Object, Object> getTypeCache() {
    return typeCache;
  }

  private Type analyzeType(DartNode target) {
    InterfaceType currentType;
    if (classElement != null) {
      currentType = classElement.getType();
    } else {
      currentType = null;
    }
    Type type = TypeAnalyzer.analyze(target, typeProvider, context, currentType);
    if (TypeKind.of(type) == TypeKind.VOID || TypeKind.of(type) == TypeKind.DYNAMIC) {
      if (target instanceof DartIdentifier) {
        Element element = ((DartIdentifier) target).getElement();
        if (element != null) {
          // TODO Remove after verifying correct AST
          type = element.getType();
        }
      }
    }
    if (DartCoreDebug.ENABLE_TYPE_REFINEMENT && target instanceof DartIdentifier) {
      Type newType = TypeRefiner.refineType((DartIdentifier) target, type, typeProvider);
      // TODO newType should not be null but can be currently
      if (newType != null && TypeKind.of(newType) != TypeKind.DYNAMIC) {
        type = newType;
      }
    }
    return type;
  }

  private void checkCancel() {
    if (monitor != null && monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  private void createCompletionsForFactoryInvocation(DartIdentifier memberName, InterfaceType itype) {
    String prefix = extractFilterPrefix(memberName);
    List<Element> members = getConstructors(itype);
    if (!isCompletionAfterDot && memberName == null) {
      return;
    }
    for (Element elem : members) {
      MethodElement method = (MethodElement) elem;
      String name = method.getName();
      if (prefix != null && !name.startsWith(prefix)) {
        continue;
      }
      if (prefix == null && name.length() == 0) {
        continue;
      }
      int kind = CompletionProposal.METHOD_REF;
      InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
          kind, actualCompletionPosition - offset);
      proposal.setDeclarationSignature(method.getEnclosingElement().getName().toCharArray());
      proposal.setSignature(name.toCharArray());
      proposal.setCompletion(name.toCharArray());
      proposal.setName(name.toCharArray());
      proposal.setIsContructor(true);
      proposal.setIsGetter(false);
      proposal.setIsSetter(false);
      proposal.setParameterNames(getParameterNames(method));
      proposal.setParameterTypeNames(getParameterTypeNames(method));
      String returnTypeName = itype.getElement().getName();
      proposal.setTypeName(returnTypeName.toCharArray());
      proposal.setDeclarationTypeName(returnTypeName.toCharArray());
      setSourceLoc(proposal, memberName, prefix);
      proposal.setRelevance(1);
      requestor.accept(proposal);
    }
  }

  private void createCompletionsForLibraryPrefix(DartIdentifier identifier,
      LibraryElement libraryElement) {
    String prefix = extractFilterPrefix(identifier);
    Scope scope = libraryElement.getScope();
    Map<String, Element> elements = scope.getElements();
    for (Entry<String, Element> entry : elements.entrySet()) {
      String name = entry.getKey();
      Element element = entry.getValue();
      boolean disallowPrivate = true;
      if (prefix != null) {
        disallowPrivate = !prefix.startsWith("_");
        if (prefix.length() == 0) {
          prefix = null;
        }
      }
      if (prefix != null && !name.startsWith(prefix)) {
        continue;
      }
      if (disallowPrivate && name.startsWith("_")) {
        continue;
      }
      String typeName = name;
      char[][] parameterNames = null;
      char[][] parameterTypeNames = null;
      char[] returnTypeName = null;
      boolean isInterface = false;
      int kind;
      switch (ElementKind.of(element)) {
        case CLASS:
          kind = CompletionProposal.TYPE_REF;
          isInterface = ((ClassElement) element).isInterface();
          break;
        case FUNCTION_TYPE_ALIAS:
          kind = CompletionProposal.METHOD_NAME_REFERENCE;
          FunctionAliasElement function = (FunctionAliasElement) element;
          parameterNames = getParameterNames(function);
          parameterTypeNames = getParameterTypeNames(function);
          returnTypeName = function.getFunctionType().getReturnType().getElement().getName().toCharArray();
          break;
        case METHOD:
          kind = CompletionProposal.METHOD_NAME_REFERENCE;
          MethodElement method = (MethodElement) element;
          parameterNames = getParameterNames(method);
          parameterTypeNames = getParameterTypeNames(method);
          returnTypeName = method.getReturnType().getElement().getName().toCharArray();
          break;
        case FIELD:
          kind = CompletionProposal.FIELD_REF;
          FieldElement field = (FieldElement) element;
          typeName = field.getType().getElement().getName();
          break;
        default:
          continue;
      }
      InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
          kind, actualCompletionPosition - offset);
      proposal.setDeclarationSignature(libraryElement.getLibraryUnit().getName().toCharArray());
      proposal.setSignature(typeName.toCharArray());
      proposal.setCompletion(name.toCharArray());
      proposal.setName(name.toCharArray());
      proposal.setIsInterface(isInterface);
      proposal.setParameterNames(parameterNames);
      proposal.setParameterTypeNames(parameterTypeNames);
      proposal.setTypeName(returnTypeName);
      setSourceLoc(proposal, identifier, prefix);
      proposal.setRelevance(1);
      requestor.accept(proposal);
    }
  }

  private void createCompletionsForLocalVariables(DartNode terminalNode, DartIdentifier node,
      DartClassMember<? extends DartExpression> method) {
    String prefix = extractFilterPrefix(node);
    ScopedNameFinder vars = new ScopedNameFinder(actualCompletionPosition);
    terminalNode.accept(vars);
    Map<String, ScopedName> localNames = vars.getLocals();
    for (ScopedName para : localNames.values()) {
      String name = para.getName();
      if (prefix != null && !name.startsWith(prefix)) {
        continue;
      }
      Element element = para.getSymbol();
      boolean isSetter = element.getModifiers().isSetter();
      boolean isGetter = element.getModifiers().isGetter();
      boolean isMethod = element.getKind() == ElementKind.METHOD;
      String typeName = isMethod ? ((MethodElement) element).getReturnType().getElement().getName()
          : element.getType().toString();
      int kind = isMethod ? CompletionProposal.METHOD_REF : isGetter || isSetter
          ? CompletionProposal.FIELD_REF : CompletionProposal.LOCAL_VARIABLE_REF;
      InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
          kind, actualCompletionPosition - offset);
      proposal.setSignature(typeName.toCharArray());
      proposal.setIsGetter(isGetter);
      proposal.setIsSetter(isSetter);
      proposal.setCompletion(name.toCharArray());
      proposal.setDeclarationSignature(name.toCharArray());
      proposal.setTypeName(typeName.toCharArray());
      proposal.setName(name.toCharArray());
      if (isMethod) {
        proposal.setParameterNames(getParameterNames((MethodElement) element));
        proposal.setParameterTypeNames(getParameterTypeNames((MethodElement) element));
      }
      setSourceLoc(proposal, node, prefix);
      proposal.setRelevance(1);
      requestor.accept(proposal);
    }
  }

  private void createCompletionsForMethodInvocation(DartIdentifier node, Type type,
      boolean isQualifiedByThis, boolean isMethodStatic) {
    String prefix = extractFilterPrefix(node);
    if (TypeKind.of(type) == TypeKind.VOID) {
      return;
    }
    InterfaceType itype = (InterfaceType) type;
    boolean includeDeclaration = true;
    List<Element> members;
    if (TypeKind.of(itype) == TypeKind.DYNAMIC) {
      members = findAllElements(parsedUnit.getLibrary(), prefix);
      includeDeclaration = false;
    } else {
      members = getAllElements(itype);
    }
    Set<String> previousNames = new HashSet<String>(members.size());
    for (Element elem : members) {
      if (!(elem instanceof MethodElement)) {
        continue;
      }
      MethodElement method = (MethodElement) elem;
      boolean candidateMethodIsStatic = elem.getModifiers().isStatic();
      if (isMethodStatic && !candidateMethodIsStatic || isQualifiedByThis
          && candidateMethodIsStatic) {
        continue;
      }
      String name = method.getName();
      char[][] paramTypeNames = getParameterTypeNames(method);
      String sig = makeSig(name, paramTypeNames);
      if (name.isEmpty() || previousNames.contains(sig)) {
        continue;
      }
      previousNames.add(sig);
      if (prefix != null && !name.startsWith(prefix)) {
        continue;
      }
      // TODO Filtering: No operators appear following '.'; only operators if no '.'
      boolean isOperator = elem.getModifiers().isOperator();
      if (isCompletionAfterDot && isOperator) {
        continue;
      }
      if (isOperator && node == null) {
        continue;
      }
      boolean isSetter = method.getModifiers().isSetter();
      boolean isGetter = method.getModifiers().isGetter();
      int kind = isGetter || isSetter ? CompletionProposal.FIELD_REF
          : CompletionProposal.METHOD_REF;
      InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
          kind, actualCompletionPosition - offset);
      if (includeDeclaration) {
        proposal.setDeclarationSignature(method.getEnclosingElement().getName().toCharArray());
      } else {
        proposal.setDeclarationSignature("".toCharArray());
      }
      proposal.setSignature(name.toCharArray());
      proposal.setCompletion(name.toCharArray());
      proposal.setName(name.toCharArray());
      proposal.setIsContructor(method.isConstructor());
      proposal.setIsGetter(isGetter);
      proposal.setIsSetter(isSetter);
      proposal.setParameterNames(getParameterNames(method));
      proposal.setParameterTypeNames(paramTypeNames);
      String returnTypeName = method.getReturnType().getElement().getName();
      proposal.setTypeName(returnTypeName.toCharArray());
      if (includeDeclaration) {
        proposal.setDeclarationTypeName(method.getEnclosingElement().getName().toCharArray());
      }
      setSourceLoc(proposal, node, prefix);
      proposal.setRelevance(1);
      requestor.accept(proposal);
    }
  }

  private void createCompletionsForPropertyAccess(DartIdentifier node, Type type,
      boolean isQualifiedByThis, boolean isMethodStatic) {
    if (!(type instanceof InterfaceType)) {
      return;
    }
    String prefix = extractFilterPrefix(node);
    InterfaceType itype = (InterfaceType) type;
    boolean includeDeclaration = true;
    List<Element> members;
    if (TypeKind.of(itype) == TypeKind.DYNAMIC) {
      members = findAllElements(parsedUnit.getLibrary(), prefix);
      includeDeclaration = false;
    } else {
      members = getAllElements(itype);
    }
    Set<String> previousNames = new HashSet<String>(members.size());
    for (Element elem : members) {
      if (!(elem instanceof FieldElement)) {
        continue;
      }
      FieldElement field = (FieldElement) elem;
      boolean fieldIsStatic = field.getModifiers().isStatic();
      if (isMethodStatic && !fieldIsStatic || isQualifiedByThis && fieldIsStatic) {
        continue;
      }
      if (fieldIsStatic && node != null && node.getParent() instanceof DartPropertyAccess) {
        DartPropertyAccess parent = (DartPropertyAccess) node.getParent();
        if (field.getEnclosingElement() != parent.getQualifier().getElement()) {
          continue;
        }
      }
      String name = field.getName();
      String sig = name + field.getType().getElement().getName();
      if (prefix != null && !name.startsWith(prefix)) {
        continue;
      }
      if (previousNames.contains(sig)) {
        continue;
      }
      previousNames.add(sig);
      InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
          CompletionProposal.FIELD_REF, actualCompletionPosition - offset);
      if (includeDeclaration) {
        proposal.setDeclarationSignature(field.getEnclosingElement().getName().toCharArray());
      } else {
        proposal.setDeclarationSignature("".toCharArray());
      }
      proposal.setSignature(field.getType().getElement().getName().toCharArray());
      proposal.setCompletion(name.toCharArray());
      proposal.setName(name.toCharArray());
      proposal.setIsContructor(false);
      proposal.setIsGetter(true);
      proposal.setIsSetter(true);
      proposal.setTypeName(field.getType().getElement().getName().toCharArray());
      if (includeDeclaration) {
        proposal.setDeclarationTypeName(field.getEnclosingElement().getName().toCharArray());
      }
      setSourceLoc(proposal, node, prefix);
      proposal.setRelevance(1);
      requestor.accept(proposal);
    }
  }

  private void createCompletionsForQualifiedMemberAccess(DartIdentifier memberName, Type type,
      boolean isInstance) {
    // At the completion point, the language allows both field and method access.
    // The parser needs more look-ahead to disambiguate. Those tokens may not have
    // been typed yet.
    createCompletionsForPropertyAccess(memberName, type, isInstance, false);
    createCompletionsForMethodInvocation(memberName, type, isInstance, false);
  }

  private void createCompletionsForStaticVariables(DartIdentifier identifier, DartClass classDef) {
    ClassElement elem = classDef.getElement();
    Type type = elem.getType();
    createCompletionsForPropertyAccess(identifier, type, false, true);
  }

  private void createProposalsForLiterals(DartNode node, String... names) {
    String prefix = extractFilterPrefix(node);
    for (String name : names) {
      InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
          CompletionProposal.LOCAL_VARIABLE_REF, actualCompletionPosition - offset);
      proposal.setSignature(name.toCharArray());
      proposal.setCompletion(name.toCharArray());
      proposal.setName(name.toCharArray());
      setSourceLoc(proposal, node, prefix);
      proposal.setRelevance(1);
      requestor.accept(proposal);
    }
  }

  private void createTypeCompletionsForConstructor(DartNode node, SearchMatch match, String prefix) {
    DartElement element = match.getElement();
    if (!(element instanceof com.google.dart.tools.core.model.Type)) {
      return;
    }
    boolean disallowPrivate = true;
    if (prefix != null) {
      disallowPrivate = !prefix.startsWith("_");
      if (prefix.length() == 0) {
        prefix = null;
      }
    }
    com.google.dart.tools.core.model.Type type = (com.google.dart.tools.core.model.Type) element;
    String name = type.getElementName();
    if (disallowPrivate && name.startsWith("_")) {
      return;
    }
    if (!isCompletionAfterDot) {
      InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
          CompletionProposal.TYPE_REF, actualCompletionPosition - offset);
      char[] nameChars = name.toCharArray();
      proposal.setCompletion(nameChars);
      proposal.setSignature(nameChars);
      setSourceLoc(proposal, node, prefix);
      proposal.setRelevance(1);
      requestor.accept(proposal);
    } else {
      try {
        for (com.google.dart.tools.core.model.Method method : type.getMethods()) {
          if (method.isConstructor()) {
            if (!method.getElementName().equals(name)) {
              // not sure why method.isFactory() doesn't work
              continue;
            }
            InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
                CompletionProposal.METHOD_REF, actualCompletionPosition - offset);
            char[] declaringTypeName = method.getDeclaringType().getElementName().toCharArray();
            char[] methodName = name.toCharArray();
            proposal.setDeclarationSignature(declaringTypeName);
            proposal.setSignature(methodName);
            proposal.setCompletion(methodName);
            proposal.setName(methodName);
            proposal.setIsContructor(method.isConstructor());
            proposal.setIsGetter(false);
            proposal.setIsSetter(false);
            proposal.setParameterNames(getParameterNames(method));
            proposal.setParameterTypeNames(getParameterTypeNames(method));
            proposal.setTypeName(CharOperation.toCharArray(method.getReturnTypeName()));
            proposal.setDeclarationTypeName(declaringTypeName);
            setSourceLoc(proposal, node, prefix);
            proposal.setRelevance(1);
            requestor.setAllowsRequiredProposals(CompletionProposal.CONSTRUCTOR_INVOCATION,
                CompletionProposal.TYPE_REF, true);
            requestor.accept(proposal);
          }
        }
      } catch (DartModelException exception) {
        InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
            CompletionProposal.TYPE_REF, actualCompletionPosition - offset);
        char[] nameChars = name.toCharArray();
        proposal.setCompletion(nameChars);
        proposal.setSignature(nameChars);
        setSourceLoc(proposal, node, prefix);
        proposal.setRelevance(1);
        requestor.accept(proposal);
      }
    }
  }

  private void createTypeCompletionsForGenericType(DartNode node, Type type, String prefix) {
    String name = type.getElement().getName();
    boolean disallowPrivate = true;
    if (prefix != null) {
      disallowPrivate = !prefix.startsWith("_");
      if (prefix.length() == 0) {
        prefix = null;
      }
    }
    if (disallowPrivate && name.startsWith("_")) {
      return;
    }
    InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
        CompletionProposal.TYPE_REF, actualCompletionPosition - offset);
    char[] nameChars = name.toCharArray();
    proposal.setCompletion(nameChars);
    proposal.setSignature(nameChars);
    proposal.setIsInterface(true); // TODO For now, use the Interface icon for generic types
    setSourceLoc(proposal, node, prefix);
    proposal.setRelevance(1);
    requestor.accept(proposal);
  }

  private void createTypeCompletionsForParameterDecl(DartNode node, SearchMatch match, String prefix) {
    DartElement element = match.getElement();
    String name;
    boolean isInterface;
    if (element instanceof DartFunctionTypeAliasImpl) {
      DartFunctionTypeAliasImpl alias = (DartFunctionTypeAliasImpl) element;
      name = alias.getElementName();
      isInterface = false;
    } else if (element instanceof com.google.dart.tools.core.model.Type) {
      com.google.dart.tools.core.model.Type type = (com.google.dart.tools.core.model.Type) element;
      name = type.getElementName();
      try {
        isInterface = type.isInterface();
      } catch (DartModelException ex) {
        isInterface = false;
      }
    } else {
      return;
    }
    boolean disallowPrivate = true;
    if (prefix != null) {
      disallowPrivate = !prefix.startsWith("_");
      if (prefix.length() == 0) {
        prefix = null;
      }
    }
    if (disallowPrivate && name.startsWith("_")) {
      return;
    }
    InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
        CompletionProposal.TYPE_REF, actualCompletionPosition - offset);
    char[] nameChars = name.toCharArray();
    proposal.setCompletion(nameChars);
    proposal.setSignature(nameChars);
    proposal.setIsInterface(isInterface);
    setSourceLoc(proposal, node, prefix);
    proposal.setRelevance(1);
    requestor.accept(proposal);
  }

  private void createTypeCompletionsForTypeDecl(DartNode node, SearchMatch match, String prefix,
      boolean isClassOnly, boolean isInterfaceOnly) {
    DartElement element = match.getElement();
    if (!(element instanceof com.google.dart.tools.core.model.Type)) {
      return;
    }
    boolean disallowPrivate = true;
    if (prefix != null) {
      disallowPrivate = !prefix.startsWith("_");
      if (prefix.length() == 0) {
        prefix = null;
      }
    }
    com.google.dart.tools.core.model.Type type = (com.google.dart.tools.core.model.Type) element;
    boolean isInterface = false;
    try {
      isInterface = type.isInterface();
      if (isClassOnly && isInterface) {
        return;
      }
      if (isInterfaceOnly && !isInterface) {
        return;
      }
    } catch (DartModelException ex) {
      // no one cares
    }
    String name = type.getElementName();
    if (disallowPrivate && name.startsWith("_")) {
      return;
    }
    InternalCompletionProposal proposal = (InternalCompletionProposal) CompletionProposal.create(
        CompletionProposal.TYPE_REF, actualCompletionPosition - offset);
    char[] nameChars = name.toCharArray();
    proposal.setIsInterface(isInterface);
    proposal.setCompletion(nameChars);
    proposal.setSignature(nameChars);
    setSourceLoc(proposal, node, prefix);
    proposal.setRelevance(1);
    requestor.accept(proposal);
  }

  private String extractFilterPrefix(DartNode node) {
    if (node == null || isCompletionAfterDot) {
      return null;
    }
    int begin = node.getSourceInfo().getOffset();
    int dot = actualCompletionPosition + 1;
    if (dot < begin) {
      return null;
    }
    String name = source.substring(begin, begin + node.getSourceInfo().getLength());
    String prefix = name.substring(0, Math.min(name.length(), dot - begin));
    return prefix.length() == 0 ? null : prefix;
  }

  private List<SearchMatch> findTypesWithPrefix(DartIdentifier id) {
    SearchEngine engine = SearchEngineFactory.createSearchEngine();
    SearchScope scope = SearchScopeFactory.createWorkspaceScope();
    GatheringSearchListener listener = new GatheringSearchListener();
    String prefix = extractFilterPrefix(id);
    if (prefix == null) {
      prefix = "";
    }
    List<SearchMatch> matches;
    try {
      if (DartCoreDebug.NEW_INDEXER) {
        matches = engine.searchTypeDeclarations(scope,
            SearchPatternFactory.createPrefixPattern(prefix, true), (SearchFilter) null,
            new NullProgressMonitor());
      } else {
        engine.searchTypeDeclarations(scope,
            SearchPatternFactory.createPrefixPattern(prefix, true), null, listener,
            new NullProgressMonitor());
        matches = listener.getMatches();
      }
    } catch (SearchException ex) {
      return null;
    }
    try {
      int idx = 0;
      for (com.google.dart.tools.core.model.Type localType : getCurrentCompilationUnit().getTypes()) {
        String typeName = localType.getElementName();
        if (typeName.startsWith(prefix)) { // this test is case sensitive
          SearchMatch match = new SearchMatch(MatchQuality.EXACT, MatchKind.NOT_A_REFERENCE,
              localType, localType.getSourceRange());
          boolean found = false;
          for (SearchMatch foundMatch : matches) {
            if (foundMatch.getElement().getElementName().equals(typeName)) {
              found = true;
              break;
            }
          }
          if (!found) {
            matches.add(idx++, match);
          }
        }
      }
      for (com.google.dart.tools.core.model.DartFunctionTypeAlias localType : getCurrentCompilationUnit().getFunctionTypeAliases()) {
        String typeName = localType.getElementName();
        if (typeName.startsWith(prefix)) { // this test is case sensitive
          SearchMatch match = new SearchMatch(MatchQuality.EXACT, MatchKind.NOT_A_REFERENCE,
              localType, localType.getSourceRange());
          boolean found = false;
          for (SearchMatch foundMatch : matches) {
            if (foundMatch.getElement().getElementName().equals(typeName)) {
              found = true;
              break;
            }
          }
          if (!found) {
            matches.add(idx++, match);
          }
        }
      }
    } catch (DartModelException ex) {
      // no one cares
    } catch (NullPointerException ex) {
      // happens during tests because currentCompilationUnit is null
    }
    return matches;
  }

  private CompilationUnit getCurrentCompilationUnit() {
    // This is actually useful -- type search does not find local defs
    return currentCompilationUnit;
  }

  private boolean isCompletionNode(DartNode node) {
    int completionPos = actualCompletionPosition + 1;
    int nodeStart = node.getSourceInfo().getOffset();
    int nodeEnd = node.getSourceInfo().getLength() + nodeStart;
    return nodeStart <= completionPos && completionPos <= nodeEnd;
  }

  private String makeSig(String name, char[][] paramTypeNames) {
    StringBuffer buf = new StringBuffer();
    buf.append(name);
    for (char[] ca : paramTypeNames) {
      for (char c : ca) {
        buf.append(c);
      }
    }
    return buf.toString();
  }

  private void proposeClassOrInterfaceNamesForPrefix(DartIdentifier identifier, boolean isClass) {
    List<SearchMatch> matches = findTypesWithPrefix(identifier);
    if (matches == null || matches.size() == 0) {
      return;
    }
    String prefix = extractFilterPrefix(identifier);
    for (SearchMatch match : matches) {
      createTypeCompletionsForTypeDecl(identifier, match, prefix, isClass, !isClass);
    }
  }

  private void proposeGenericTypeCompletions(DartNode node) {
    if (classElement == null) {
      // TODO Handle top-level functions
      return;
    }
    String prefix = extractFilterPrefix(node);
    List<? extends Type> typeParams = classElement.getTypeParameters();
    for (Type type : typeParams) {
      createTypeCompletionsForGenericType(node, type, prefix);
    }
  }

  private void proposeTypeNamesForPrefix(DartIdentifier identifier) {
    List<SearchMatch> matches = findTypesWithPrefix(identifier);
    if (matches == null || matches.size() == 0) {
      return;
    }
    String prefix = extractFilterPrefix(identifier);
    for (SearchMatch match : matches) {
      createTypeCompletionsForTypeDecl(identifier, match, prefix, false, false);
    }
  }

  private void proposeTypesForNewParam() {
    proposeGenericTypeCompletions(null);
    // TODO Combine with proposeTypesForPrefix()
    List<SearchMatch> matches = findTypesWithPrefix(null);
    if (matches == null || matches.size() == 0) {
      return;
    }
    for (SearchMatch match : matches) {
      createTypeCompletionsForParameterDecl(null, match, "");
    }
  }

  private void proposeTypesForPrefix(DartIdentifier identifier) {
    proposeTypesForPrefix(identifier, false); // disallow void in most places
  }

  private void proposeTypesForPrefix(DartIdentifier identifier, boolean allowVoid) {
    List<SearchMatch> matches = findTypesWithPrefix(identifier);
    if (matches == null || matches.size() == 0) {
      return;
    }
    String prefix = extractFilterPrefix(identifier);
    Set<String> uniques = new HashSet<String>(matches.size()); // indexer returns duplicates
    for (SearchMatch match : matches) {
      String matchName = match.getElement().getElementName();
      if (uniques.contains(matchName)) {
        continue;
      }
      uniques.add(matchName);
      createTypeCompletionsForParameterDecl(identifier, match, prefix);
    }
    if (allowVoid) {
      if (prefix == null || prefix.length() == 0) {
        createProposalsForLiterals(identifier, C_VOID);
      } else {
        String id = identifier.getName();
        if (id.length() <= C_VOID.length() && C_VOID.startsWith(id)) {
          createProposalsForLiterals(identifier, C_VOID);
        }
      }
    }
  }

  private void proposeVariables(DartNode completionNode, DartIdentifier identifier,
      DartClassMember<? extends DartExpression> method) {
    createCompletionsForLocalVariables(completionNode, identifier, method);
    DartClass classDef = (DartClass) method.getParent();
    createCompletionsForStaticVariables(identifier, classDef);
  }

  private void reportResolveLibraryFailed(Collection<DartCompilationError> parseErrors) {
    DartCore.logError("Could not resolve AST: " + fileName, null);
    for (DartCompilationError err : parseErrors) {
      DartCore.logError(err.getMessage(), null);
      if (DEBUG) {
        System.out.println(err.getMessage());
        System.out.println(err.getSource().getUri());
      }
    }
  }

  private void setSourceLoc(InternalCompletionProposal proposal, DartNode name, String prefix) {
    // Bug in source positions causes name node to have its parent's source locations.
    // That causes sourceLoc to be incorrect, which also causes completion list to close
    // when the next char is typed rather than filtering the list based on that char.
    // It also causes editing to fail when a completion is selected.
    if (prefix == null) {
      name = null;
    }
    int sourceLoc = name == null ? actualCompletionPosition + 1 : name.getSourceInfo().getOffset();
    int length = name == null ? 0 : name.getSourceInfo().getLength();
    proposal.setReplaceRange(sourceLoc - offset, length + sourceLoc - offset);
    proposal.setTokenRange(sourceLoc - offset, length + sourceLoc - offset);
  }

  private void visitorNotImplementedYet(DartNode node, DartNode sourceNode,
      Class<? extends ASTVisitor<Void>> astClass) {
    if (metrics != null) {
      metrics.visitorNotImplementedYet(node, sourceNode, astClass);
    }
  }
}
