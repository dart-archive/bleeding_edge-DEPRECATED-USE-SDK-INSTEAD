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

package com.google.dart.java2dart;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.ASTFactory.assignmentExpression;
import static com.google.dart.java2dart.util.ASTFactory.block;
import static com.google.dart.java2dart.util.ASTFactory.blockFunctionBody;
import static com.google.dart.java2dart.util.ASTFactory.compilationUnit;
import static com.google.dart.java2dart.util.ASTFactory.constructorDeclaration;
import static com.google.dart.java2dart.util.ASTFactory.expressionStatement;
import static com.google.dart.java2dart.util.ASTFactory.formalParameterList;
import static com.google.dart.java2dart.util.ASTFactory.identifier;
import static com.google.dart.java2dart.util.ASTFactory.propertyAccess;
import static com.google.dart.java2dart.util.ASTFactory.thisExpression;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Context information for Java to Dart translation.
 */
public class Context {
  /**
   * Information about constructor and its usages.
   */
  class ConstructorDescription {
    final IMethodBinding binding;
    final List<SuperConstructorInvocation> superInvocations = Lists.newArrayList();
    final List<InstanceCreationExpression> instanceCreations = Lists.newArrayList();
    final List<SimpleIdentifier> implInvocations = Lists.newArrayList();
    String declName;
    String implName;

    public ConstructorDescription(IMethodBinding binding) {
      this.binding = binding;
    }
  }

  private static final String[] JAVA_EXTENSION = {"java"};
  private final List<File> classpathFiles = Lists.newArrayList();
  private final List<File> sourceFolders = Lists.newArrayList();
  private final List<File> sourceFiles = Lists.newArrayList();

  private final Map<String, String> renameMap = Maps.newHashMap();

  private final CompilationUnit dartUniverse = compilationUnit();
  private final Map<File, List<CompilationUnitMember>> fileToMembers = Maps.newHashMap();
  private final Map<CompilationUnitMember, File> memberToFile = Maps.newHashMap();
  // information about names
  private static final Set<String> forbiddenNames = Sets.newHashSet();
  private final Set<String> usedNames = Sets.newHashSet();
  private final Set<ClassMember> privateClassMembers = Sets.newHashSet();
  private final Map<SimpleIdentifier, String> identifierToName = Maps.newHashMap();
  private final Map<String, Object> signatureToBinding = Maps.newHashMap();
  private final Map<Object, List<SimpleIdentifier>> bindingToIdentifiers = Maps.newHashMap();
  private final Map<ASTNode, Object> nodeToBinding = Maps.newHashMap();
  private final Map<ASTNode, ITypeBinding> nodeToTypeBinding = Maps.newHashMap();
  private final Map<InstanceCreationExpression, ClassDeclaration> anonymousDeclarations = Maps.newHashMap();
  private final Set<SimpleIdentifier> innerClassNames = Sets.newHashSet();
  // information about constructors
  private int technicalConstructorIndex;
  private final Map<IMethodBinding, ConstructorDescription> bindingToConstructor = Maps.newHashMap();
  private final Map<ConstructorDeclaration, IMethodBinding> constructorToBinding = Maps.newHashMap();
  // information about inner classes
  private int technicalInnerClassIndex;
  private int technicalAnonymousClassIndex;

  static {
    for (Keyword keyword : Keyword.values()) {
      if (!keyword.isPseudoKeyword()) {
        forbiddenNames.add(keyword.getSyntax());
      }
    }
  }

  /**
   * Specifies that given {@link File} should be added to Java classpath.
   */
  public void addClasspathFile(File file) {
    Assert.isLegal(file.exists(), "File '" + file + "' does not exist.");
    Assert.isLegal(file.isFile(), "File '" + file + "' is not a regular file.");
    file = file.getAbsoluteFile();
    classpathFiles.add(file);
  }

  /**
   * Specifies that field with given signature should be renamed before normalizing member names.
   */
  public void addRename(String signature, String newName) {
    renameMap.put(signature, newName);
  }

  /**
   * Specifies that given {@link File} should be translated.
   */
  public void addSourceFile(File file) {
    Assert.isLegal(file.exists(), "File '" + file + "' does not exist.");
    Assert.isLegal(file.isFile(), "File '" + file + "' is not a regular file.");
    file = file.getAbsoluteFile();
    sourceFiles.add(file);
  }

  /**
   * Specifies that all files in given folder should be translated.
   */
  public void addSourceFiles(File folder) {
    Assert.isLegal(folder.exists(), "Folder '" + folder + "' does not exist.");
    Assert.isLegal(folder.isDirectory(), "Folder '" + folder + "' is not a folder.");
    folder = folder.getAbsoluteFile();
    Collection<File> folderFiles = FileUtils.listFiles(folder, JAVA_EXTENSION, true);
    sourceFiles.addAll(folderFiles);
  }

  /**
   * Specifies that given folder is a source folder (root of Java packages hierarchy).
   */
  public void addSourceFolder(File folder) {
    Assert.isLegal(folder.exists(), "Folder '" + folder + "' does not exist.");
    Assert.isLegal(folder.isDirectory(), "Folder '" + folder + "' is not a folder.");
    folder = folder.getAbsoluteFile();
    sourceFolders.add(folder);
  }

  /**
   * In Java we can have method parameter "foo" and invoke method named "foo", and parameter will
   * not shadow invoked method. But in Dart it will.
   */
  public void ensureMethodParameterDoesNotHide(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        FormalParameterList parameterList = node.getParameters();
        if (parameterList != null) {
          for (FormalParameter parameter : parameterList.getParameters()) {
            final String parameterName = parameter.getIdentifier().getName();
            final Object parameterBinding = getNodeBinding(parameter.getIdentifier());
            final AtomicBoolean hasHiding = new AtomicBoolean();
            node.accept(new RecursiveASTVisitor<Void>() {
              @Override
              public Void visitSimpleIdentifier(SimpleIdentifier node) {
                if (node.getName().equals(parameterName)
                    && getNodeBinding(node) != parameterBinding) {
                  hasHiding.set(true);
                }
                return super.visitSimpleIdentifier(node);
              }
            });
            if (hasHiding.get()) {
              String newName = generateUniqueName(parameterName);
              renameIdentifier(parameter.getIdentifier(), newName);
            }
          }
        }
        return super.visitMethodDeclaration(node);
      }
    });
  }

  public void ensureNoVariableNameReferenceFromInitializer(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      private String currentVariableName = null;
      private boolean hasNameReference = false;

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        hasNameReference |= node.getName().equals(currentVariableName);
        return super.visitSimpleIdentifier(node);
      }

      @Override
      public Void visitVariableDeclaration(VariableDeclaration node) {
        String oldVariableName = currentVariableName;
        try {
          currentVariableName = node.getName().getName();
          hasNameReference = false;
          Expression initializer = node.getInitializer();
          if (initializer != null) {
            initializer.accept(this);
          }
          if (hasNameReference || forbiddenNames.contains(currentVariableName)) {
            String newName = generateUniqueName(currentVariableName);
            renameIdentifier(node.getName(), newName);
          }
        } finally {
          currentVariableName = oldVariableName;
        }
        return null;
      }
    });
  }

  public void ensureUniqueClassMemberNames(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      private final Set<ClassMember> untouchableMethods = Sets.newHashSet();
      private final Map<String, ClassMember> usedClassMembers = Maps.newHashMap();

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        usedClassMembers.clear();
        // fill "static" methods from super classes
        {
          org.eclipse.jdt.core.dom.ITypeBinding binding = getNodeTypeBinding(node);
          if (binding != null) {
            binding = binding.getSuperclass();
            while (binding != null) {
              for (org.eclipse.jdt.core.dom.IMethodBinding method : binding.getDeclaredMethods()) {
                if (org.eclipse.jdt.core.dom.Modifier.isStatic(method.getModifiers())) {
                  usedClassMembers.put(method.getName(), null);
                }
              }
              binding = binding.getSuperclass();
            }
          }
        }
        // fill "untouchable" methods
        for (ClassMember member : node.getMembers()) {
          if (member instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) member;
            Object binding = nodeToBinding.get(member);
            if (JavaUtils.isMethodDeclaredInClass(binding, "java.lang.Object")) {
              untouchableMethods.add(member);
              usedClassMembers.put(methodDeclaration.getName().getName(), null);
            }
          }
        }
        // ensure unique method names (and prefer to keep method name over field name)
        for (ClassMember member : node.getMembers()) {
          if (member instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) member;
            // untouchable
            if (untouchableMethods.contains(method)) {
              continue;
            }
            // getter/setter can share name
            {
              ClassMember otherMember = usedClassMembers.get(method.getName().getName());
              if (otherMember instanceof MethodDeclaration) {
                MethodDeclaration otherMethod = (MethodDeclaration) otherMember;
                if (method.isGetter() && otherMethod.isSetter() || method.isSetter()
                    && otherMethod.isGetter()) {
                  continue;
                }
              }
            }
            // ensure unique name
            ensureUniqueName(method.getName(), method);
          }
        }
        // ensure unique field names (if name is already used be method)
        for (ClassMember member : node.getMembers()) {
          if (member instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) member;
            for (VariableDeclaration field : fieldDeclaration.getFields().getVariables()) {
              ensureUniqueName(field.getName(), fieldDeclaration);
            }
          }
        }
        // no recursion
        return null;
      }

      private void ensureUniqueName(Identifier declarationName, ClassMember member) {
        if (declarationName instanceof SimpleIdentifier) {
          SimpleIdentifier declarationIdentifier = (SimpleIdentifier) declarationName;
          String name = declarationIdentifier.getName();
          if (!isUniqueClassMemberName(name)) {
            String newName = generateUniqueName(name);
            // rename binding
            if (!newName.equals(name)) {
              renameIdentifier(declarationIdentifier, newName);
              name = newName;
            }
          }
          // remember that name is used
          usedClassMembers.put(name, member);
        }
      }

      private String generateUniqueName(String name) {
        if (!isGloballyUniqueClassMemberName(name)) {
          int index = 2;
          while (true) {
            String newName = name + index;
            if (isGloballyUniqueClassMemberName(newName)) {
              usedNames.add(newName);
              return newName;
            }
            index++;
          }
        }
        return name;
      }

      private boolean isGloballyUniqueClassMemberName(String name) {
        return isUniqueClassMemberName(name) && !usedNames.contains(name);
      }

      private boolean isUniqueClassMemberName(String name) {
        return !forbiddenNames.contains(name) && !usedClassMembers.containsKey(name);
      }
    });
  }

  /**
   * @return the artificial {@link ClassDeclaration}created for Java creation of anonymous class
   *         declaration.
   */
  public ClassDeclaration getAnonymousDeclaration(InstanceCreationExpression creation) {
    return anonymousDeclarations.get(creation);
  }

  public Map<File, List<CompilationUnitMember>> getFileToMembers() {
    return fileToMembers;
  }

  /**
   * We rename {@link SimpleIdentifier}s, but sometimes we need to know original name.
   */
  public String getIdentifierOriginalName(SimpleIdentifier identifier) {
    String name = identifierToName.get(identifier);
    if (name == null) {
      name = identifier.getName();
    }
    return name;
  }

  public Map<CompilationUnitMember, File> getMemberToFile() {
    return memberToFile;
  }

  /**
   * @return some Java binding for the given Dart {@link ASTNode}.
   */
  public Object getNodeBinding(ASTNode node) {
    return nodeToBinding.get(node);
  }

  /**
   * @return some Java {@link ITypeBinding} for the given Dart {@link ASTNode}.
   */
  public ITypeBinding getNodeTypeBinding(ASTNode node) {
    return nodeToTypeBinding.get(node);
  }

  /**
   * @return the Dart {@link ClassMember} which are generated from private Java elements.
   */
  public Set<ClassMember> getPrivateClassMembers() {
    return privateClassMembers;
  }

  /**
   * @return all references (actual references and declarations)
   */
  public List<SimpleIdentifier> getReferences(SimpleIdentifier target) {
    Object binding = nodeToBinding.get(target);
    List<SimpleIdentifier> references = bindingToIdentifiers.get(binding);
    return references != null ? references : Lists.<SimpleIdentifier> newArrayList();
  }

  public void renameConstructor(ConstructorDeclaration node, String name) {
    IMethodBinding binding = constructorToBinding.get(node);
    //
    SimpleIdentifier newIdentifier;
    if (name == null) {
      newIdentifier = null;
    } else {
      newIdentifier = identifier(name);
    }
    // rename constructor
    node.setName(newIdentifier);
    // update references
    ConstructorDescription constructorDescription = bindingToConstructor.get(binding);
    if (constructorDescription != null) {
      // set name in InstanceCreationExpression
      {
        List<InstanceCreationExpression> creations = constructorDescription.instanceCreations;
        for (InstanceCreationExpression creation : creations) {
          creation.getConstructorName().setName(newIdentifier);
        }
      }
      // set name in SuperConstructorInvocation
      {
        List<SuperConstructorInvocation> invocations = constructorDescription.superInvocations;
        for (SuperConstructorInvocation invocation : invocations) {
          invocation.setConstructorName(newIdentifier);
        }
      }
      // set name in invocation of implementation
      {
        List<SimpleIdentifier> invocations = constructorDescription.implInvocations;
        for (SimpleIdentifier identifier : invocations) {
          identifier.setToken(token(TokenType.IDENTIFIER, constructorDescription.implName));
        }
      }
    }
  }

  /**
   * Sets the {@link SimpleIdentifier} name and updates all references.
   */
  public void renameIdentifier(SimpleIdentifier declarationIdentifier, String newName) {
    // move identifiers to the new signature
    Object binding = nodeToBinding.get(declarationIdentifier);
    List<SimpleIdentifier> identifiers = bindingToIdentifiers.get(binding);
    // update identifiers to the new name
    for (SimpleIdentifier identifier : identifiers) {
      identifier.setToken(token(TokenType.IDENTIFIER, newName));
    }
  }

  public CompilationUnit translate() throws Exception {
    // sort source files
    Collections.sort(sourceFiles);
    // perform syntax translation
    translateSyntax();
    // perform configured renames
    for (Entry<String, String> renameEntry : renameMap.entrySet()) {
      String signature = renameEntry.getKey();
      Object binding = signatureToBinding.get(signature);
      List<SimpleIdentifier> identifiers = bindingToIdentifiers.get(binding);
      if (identifiers != null) {
        String newName = renameEntry.getValue();
        for (SimpleIdentifier identifier : identifiers) {
          identifier.setToken(token(TokenType.IDENTIFIER, newName));
        }
      }
    }
    // run processors
    {
      replaceInnerClassReferences(dartUniverse);
      unwrapVarArgIfAlreadyArray(dartUniverse);
      ensureFieldInitializers(dartUniverse);
      dontUseThisInFieldInitializers(dartUniverse);
      ensureUniqueClassMemberNames(dartUniverse);
      ensureNoVariableNameReferenceFromInitializer(dartUniverse);
      ensureMethodParameterDoesNotHide(dartUniverse);
      renameConstructors(dartUniverse);
    }
    // done
    return dartUniverse;
  }

  /**
   * @return the "technical" name for the top-level Dart class for Java anonymous class.
   */
  int generateTechnicalAnonymousClassIndex() {
    return technicalAnonymousClassIndex++;
  }

  /**
   * @return the "technical" name for the Dart constructor.
   */
  String generateTechnicalConstructorName() {
    return "jtd_constructor_" + technicalConstructorIndex++;
  }

  /**
   * @return the "technical" name for the top-level Dart class for Java inner class.
   */
  String generateTechnicalInnerClassName() {
    return "JtdClass_" + technicalInnerClassIndex++;
  }

  /**
   * @return the not <code>null</code> {@link ConstructorDescription}, may be just added.
   */
  ConstructorDescription getConstructorDescription(IMethodBinding binding) {
    ConstructorDescription description = bindingToConstructor.get(binding);
    if (description == null) {
      description = new ConstructorDescription(binding);
      bindingToConstructor.put(binding, description);
    }
    return description;
  }

  /**
   * Remembers artificial {@link ClassDeclaration} created for Java creation of anonymous class
   * declaration.
   */
  void putAnonymousDeclaration(InstanceCreationExpression creation, ClassDeclaration declaration) {
    if (declaration != null) {
      anonymousDeclarations.put(creation, declaration);
    }
  }

  /**
   * Remembers that given {@link SimpleIdentifier} used as name of the named
   * {@link ConstructorDeclaration} is reference to the given Java signature.
   */
  void putConstructorBinding(ConstructorDeclaration node, IMethodBinding binding) {
    constructorToBinding.put(node, binding);
  }

  /**
   * Remembers that given {@link SimpleIdentifier} is the name of inner class and all references to
   * it should be renamed.
   */
  void putInnerClassName(SimpleIdentifier identifier) {
    innerClassNames.add(identifier);
  }

  /**
   * Remembers some Java binding for the given Dart {@link ASTNode}.
   */
  void putNodeBinding(ASTNode node, Object binding) {
    nodeToBinding.put(node, binding);
  }

  /**
   * Remembers Java {@link ITypeBinding} for the given Dart {@link ASTNode}.
   */
  void putNodeTypeBinding(ASTNode node, ITypeBinding binding) {
    nodeToTypeBinding.put(node, binding);
  }

  /**
   * Remembers that given {@link ClassMember} was created for private Java element.
   */
  void putPrivateClassMember(ClassMember member) {
    privateClassMembers.add(member);
  }

  /**
   * Remembers that "identifier" is reference to the given Java binding.
   */
  void putReference(SimpleIdentifier identifier, Object binding, String bindingSignature) {
    if (binding != null) {
      signatureToBinding.put(bindingSignature, binding);
      identifierToName.put(identifier, identifier.getName());
      // remember binding for reference
      nodeToBinding.put(identifier, binding);
      // add reference to binding
      List<SimpleIdentifier> identifiers = bindingToIdentifiers.get(binding);
      if (identifiers == null) {
        identifiers = Lists.newLinkedList();
        bindingToIdentifiers.put(binding, identifiers);
      }
      identifiers.add(identifier);
    }
    // remember global name
    usedNames.add(identifier.getName());
  }

  private void dontUseThisInFieldInitializers(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        processClass(node);
        return super.visitClassDeclaration(node);
      }

      private void addAssignmentsToBlock(Block block, Map<SimpleIdentifier, Expression> initializers) {
        int index = 0;
        for (Entry<SimpleIdentifier, Expression> entry : initializers.entrySet()) {
          block.getStatements().add(
              index++,
              expressionStatement(assignmentExpression(
                  propertyAccess(thisExpression(), entry.getKey()),
                  TokenType.EQ,
                  entry.getValue())));
        }
      }

      private void processClass(final ClassDeclaration classDeclaration) {
        final Map<SimpleIdentifier, Expression> thisInitializers = Maps.newLinkedHashMap();
        // find field initializers which use "this"
        classDeclaration.accept(new RecursiveASTVisitor<Void>() {
          @Override
          public Void visitVariableDeclaration(VariableDeclaration node) {
            if (node.getParent().getParent() instanceof FieldDeclaration) {
              if (hasThisExpression(node)) {
                thisInitializers.put(node.getName(), node.getInitializer());
                node.setInitializer(null);
              }
            }
            return super.visitVariableDeclaration(node);
          }

          private boolean hasThisExpression(ASTNode node) {
            final AtomicBoolean result = new AtomicBoolean();
            node.accept(new GeneralizingASTVisitor<Void>() {
              @Override
              public Void visitThisExpression(ThisExpression node) {
                result.set(true);
                return super.visitThisExpression(node);
              }
            });
            return result.get();
          }
        });
        // add field assignment for each "this" field initializer
        if (thisInitializers.isEmpty()) {
          return;
        }
        ConstructorDeclaration singleConstructor = null;
        boolean hasImpl = false;
        for (ClassMember classMember : classDeclaration.getMembers()) {
          if (classMember instanceof ConstructorDeclaration) {
            singleConstructor = (ConstructorDeclaration) classMember;
          }
          if (classMember instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) classMember;
            String methodName = method.getName().getName();
            if (methodName.startsWith("_jtd_constructor_") && methodName.endsWith("_impl")) {
              hasImpl = true;
              Block block = ((BlockFunctionBody) method.getBody()).getBlock();
              addAssignmentsToBlock(block, thisInitializers);
            }
          }
        }
        // no "_impl", add assignments to the single constructor
        if (!hasImpl && singleConstructor != null) {
          Block block = ((BlockFunctionBody) singleConstructor.getBody()).getBlock();
          addAssignmentsToBlock(block, thisInitializers);
        }
        // no "_impl", generate default constructor
        if (singleConstructor == null) {
          Block block = block();
          addAssignmentsToBlock(block, thisInitializers);
          ConstructorDeclaration constructor = constructorDeclaration(
              classDeclaration.getName(),
              null,
              formalParameterList(),
              null,
              blockFunctionBody(block));
          classDeclaration.getMembers().add(constructor);
        }
      }
    });
  }

  private void ensureFieldInitializers(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitFieldDeclaration(FieldDeclaration node) {
        VariableDeclarationList fields = node.getFields();
        // final fields should be initialized as part of the constructor 
        // or already have an initializer
        if (fields.getKeyword() instanceof KeywordToken) {
          KeywordToken token = (KeywordToken) fields.getKeyword();
          if (token.getKeyword() == Keyword.FINAL) {
            return super.visitFieldDeclaration(node);
          }
        }
        String typeName = fields.getType().toString();
        for (VariableDeclaration variable : fields.getVariables()) {
          if (variable.getInitializer() == null) {
            Expression initializer = SyntaxTranslator.getPrimitiveTypeDefaultValue(typeName);
            if (initializer != null) {
              variable.setInitializer(initializer);
            }
          }
        }
        return super.visitFieldDeclaration(node);
      }
    });
  }

  /**
   * @return the globally unique name, based on the given one.
   */
  private String generateUniqueName(String name) {
    if (usedNames.contains(name) || forbiddenNames.contains(name)) {
      int index = 2;
      while (true) {
        String newName = name + index;
        if (!usedNames.contains(newName) && !forbiddenNames.contains(newName)) {
          usedNames.add(newName);
          return newName;
        }
        index++;
      }
    }
    return name;
  }

  /**
   * @return the Java AST of the given Java {@link File} in context of {@link #sourceFolders}.
   */
  private Map<File, CompilationUnit> parseJavaFiles(final List<File> javaFiles) throws Exception {
    String paths[] = new String[javaFiles.size()];
    final Map<String, File> pathToFile = Maps.newHashMap();
    for (int i = 0; i < javaFiles.size(); i++) {
      File javaFile = javaFiles.get(i);
      String javaPath = javaFile.getAbsolutePath();
      paths[i] = javaPath;
      pathToFile.put(javaPath, javaFile);
    }
    // prepare Java parser
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    {
      String[] classpathEntries = new String[classpathFiles.size()];
      for (int i = 0; i < classpathFiles.size(); i++) {
        classpathEntries[i] = classpathFiles.get(i).getAbsolutePath();
      }
      String[] sourceEntries = new String[sourceFolders.size()];
      for (int i = 0; i < sourceFolders.size(); i++) {
        sourceEntries[i] = sourceFolders.get(i).getAbsolutePath();
      }
      parser.setEnvironment(classpathEntries, sourceEntries, null, true);
    }
    parser.setResolveBindings(true);
    parser.setCompilerOptions(ImmutableMap.of(
        JavaCore.COMPILER_SOURCE,
        JavaCore.VERSION_1_5,
        JavaCore.COMPILER_DOC_COMMENT_SUPPORT,
        JavaCore.ENABLED));
    // do parse
    final Map<File, CompilationUnit> units = Maps.newLinkedHashMap();
    parser.createASTs(paths, null, ArrayUtils.EMPTY_STRING_ARRAY, new FileASTRequestor() {
      @Override
      public void acceptAST(String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit javaUnit) {
        File astFile = pathToFile.get(sourceFilePath);
        CompilationUnit dartUnit = SyntaxTranslator.translate(Context.this, javaUnit);
        units.put(astFile, dartUnit);
      }
    },
        null);
    return units;
  }

  private void renameConstructors(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      private final Set<String> memberNamesInClass = Sets.newHashSet();
      private int numConstructors;

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        memberNamesInClass.clear();
        numConstructors = 0;
        NodeList<ClassMember> members = node.getMembers();
        for (ClassMember member : members) {
          if (member instanceof ConstructorDeclaration) {
            numConstructors++;
          }
          if (member instanceof MethodDeclaration) {
            String name = ((MethodDeclaration) member).getName().getName();
            memberNamesInClass.add(name);
          }
          if (member instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) member;
            NodeList<VariableDeclaration> variables = fieldDeclaration.getFields().getVariables();
            for (VariableDeclaration variable : variables) {
              String name = variable.getName().getName();
              memberNamesInClass.add(name);
            }
          }
        }
        return super.visitClassDeclaration(node);
      }

      @Override
      public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        IMethodBinding binding = constructorToBinding.get(node);
        String bindingSignature = JavaUtils.getJdtSignature(binding);
        // prepare name
        String name = renameMap.get(bindingSignature);
        if (name == null) {
          if (numConstructors == 1 || node.getParameters().getParameters().isEmpty()) {
            // don't set name, use unnamed constructor
          } else {
            int index = 1;
            while (true) {
              name = "con" + index++;
              if (!memberNamesInClass.contains(name)) {
                break;
              }
            }
          }
        }
        memberNamesInClass.add(name);
        // apply name
        renameConstructor(node, name);
        // continue
        return super.visitConstructorDeclaration(node);
      }
    });
  }

  private void replaceInnerClassReferences(CompilationUnit unit) {
    for (SimpleIdentifier identifier : innerClassNames) {
      renameIdentifier(identifier, identifier.getName());
    }
  }

  /**
   * Translate {@link #sourceFiles} into Dart AST in {@link #dartUnits}.
   */
  private void translateSyntax() throws Exception {
    Map<File, CompilationUnit> unitMap = parseJavaFiles(sourceFiles);
    for (Entry<File, CompilationUnit> entry : unitMap.entrySet()) {
      File javaFile = entry.getKey();
      CompilationUnit dartUnit = entry.getValue();
      List<CompilationUnitMember> dartDeclarations = dartUnit.getDeclarations();
      // add to the Dart universe
      dartUniverse.getDeclarations().addAll(dartDeclarations);
      // add to the CompilationUnitMember map
      for (CompilationUnitMember member : dartDeclarations) {
        memberToFile.put(member, javaFile);
      }
      // add to the File map
      {
        List<CompilationUnitMember> fileMembers = fileToMembers.get(javaFile);
        if (fileMembers == null) {
          fileMembers = Lists.newArrayList();
          fileToMembers.put(javaFile, fileMembers);
        }
        fileMembers.addAll(dartDeclarations);
      }
    }
  }

  // XXX
  private void unwrapVarArgIfAlreadyArray(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        Object binding = nodeToBinding.get(node);
        if (binding instanceof IMethodBinding) {
          IMethodBinding methodBinding = (IMethodBinding) binding;
          if (methodBinding.isVarargs()) {
            List<Expression> args = node.getArgumentList().getArguments();
            if (!args.isEmpty() && args.get(args.size() - 1) instanceof ListLiteral) {
              ListLiteral listLiteral = (ListLiteral) args.get(args.size() - 1);
              List<Expression> elements = listLiteral.getElements();
              if (elements.size() == 1) {
                Expression element = elements.get(0);
                if (nodeToTypeBinding.get(element) instanceof ITypeBinding) {
                  ITypeBinding elementTypeBinding = nodeToTypeBinding.get(element);
                  if (elementTypeBinding.isArray()) {
                    args.set(args.size() - 1, element);
                  }
                }
              }
            }
          }
        }
        return super.visitMethodInvocation(node);
      }
    });
  }
}
