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
import com.google.common.io.Files;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.util.JavaUtils;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Context information for Java to Dart translation.
 */
public class Context {
  /**
   * Information about constructor and its usages.
   */
  class ConstructorDescription {
    final String binding;
    final List<SuperConstructorInvocation> superInvocations = Lists.newArrayList();
    final List<InstanceCreationExpression> instanceCreations = Lists.newArrayList();
    final List<SimpleIdentifier> implInvocations = Lists.newArrayList();
    String declName;
    String implName;

    public ConstructorDescription(String binding) {
      this.binding = binding;
    }
  }

  private static final String[] JAVA_EXTENSION = {"java"};
  private final List<File> sourceFolders = Lists.newArrayList();
  private final List<File> sourceFiles = Lists.newArrayList();

  private final Map<String, String> renameMap = Maps.newHashMap();

  private final CompilationUnit dartUniverse = new CompilationUnit(null, null, null, null, null);
  private final Map<File, List<CompilationUnitMember>> fileToMembers = Maps.newHashMap();
  // information about names
  private final Set<String> usedNames = Sets.newHashSet();
  private final Map<SimpleIdentifier, String> identifierToBinding = Maps.newHashMap();
  private final Map<String, List<SimpleIdentifier>> bindingToIdentifiers = Maps.newHashMap();
  private final Map<ASTNode, Object> nodeToBinding = Maps.newHashMap();
  // information about constructors
  private int technicalConstructorIndex;
  private final Map<String, ConstructorDescription> bindingToConstructor = Maps.newHashMap();
  private final Map<SimpleIdentifier, String> constructorNameToBinding = Maps.newHashMap();
  // information about inner classes
  private int technicalInnerClassIndex;

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

  public Map<File, List<CompilationUnitMember>> getFileToMembers() {
    return fileToMembers;
  }

  /**
   * @return some Java binding for the given Dart {@link ASTNode}.
   */
  public Object getNodeBinding(ASTNode node) {
    return nodeToBinding.get(node);
  }

  public CompilationUnit translate() throws Exception {
    // sort source files
    Collections.sort(sourceFiles);
    // perform syntax translation
    translateSyntax();
    // perform configured renames
    for (Entry<String, String> renameEntry : renameMap.entrySet()) {
      String signature = renameEntry.getKey();
      List<SimpleIdentifier> identifiers = bindingToIdentifiers.remove(signature);
      if (identifiers != null) {
        String newName = renameEntry.getValue();
        String newSignature = JavaUtils.getRenamedJdtSignature(signature, newName);
        Assert.isLegal(!bindingToIdentifiers.containsKey(newSignature), "Signature '"
            + newSignature + "' is already used.");
        bindingToIdentifiers.put(newSignature, identifiers);
        for (SimpleIdentifier identifier : identifiers) {
          identifier.setToken(new StringToken(TokenType.IDENTIFIER, newName, 0));
          identifierToBinding.remove(identifier);
          identifierToBinding.put(identifier, newSignature);
        }
      }
    }
    // run processors
    ensureFieldInitializers(dartUniverse);
    ensureUniqueClassMemberNames(dartUniverse);
    ensureNoVariableNameReferenceFromInitializer(dartUniverse);
    renameConstructors(dartUniverse);
    // done
    return dartUniverse;
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
  ConstructorDescription getConstructorDescription(String binding) {
    ConstructorDescription description = bindingToConstructor.get(binding);
    if (description == null) {
      description = new ConstructorDescription(binding);
      bindingToConstructor.put(binding, description);
    }
    return description;
  }

  /**
   * Remembers that given {@link SimpleIdentifier} used as name of the named
   * {@link ConstructorDeclaration} is reference to the given Java signature.
   */
  void putConstructorNameSignature(SimpleIdentifier name, String signature) {
    constructorNameToBinding.put(name, signature);
  }

  /**
   * Remembers some Java binding for the given Dart {@link ASTNode}.
   */
  void putNodeBinding(ASTNode node, Object binding) {
    nodeToBinding.put(node, binding);
  }

  /**
   * Remembers that "identifier" is reference to the given Java signature.
   */
  void putReference(String signature, SimpleIdentifier identifier) {
    if (signature != null) {
      // remember binding for reference
      identifierToBinding.put(identifier, signature);
      // add reference to binding
      List<SimpleIdentifier> names = bindingToIdentifiers.get(signature);
      if (names == null) {
        names = Lists.newLinkedList();
        bindingToIdentifiers.put(signature, names);
      }
      names.add(identifier);
    }
    // remember global name
    usedNames.add(identifier.getName());
  }

  private void ensureFieldInitializers(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitFieldDeclaration(FieldDeclaration node) {
        VariableDeclarationList fields = node.getFields();
        String typeName = fields.getType().toString();
        for (VariableDeclaration variable : fields.getVariables()) {
          if (variable.getInitializer() == null) {
            Expression initializer = null;
            if ("bool".equals(typeName)) {
              initializer = new BooleanLiteral(new KeywordToken(Keyword.FALSE, 0), false);
            }
            if ("int".equals(typeName)) {
              initializer = new IntegerLiteral(new StringToken(TokenType.INT, "0", 0), 0);
            }
            if ("double".equals(typeName)) {
              initializer = new IntegerLiteral(new StringToken(TokenType.DOUBLE, "0.0", 0), 0);
            }
            if (initializer != null) {
              variable.setInitializer(initializer);
            }
          }
        }
        return super.visitFieldDeclaration(node);
      }
    });
  }

  private void ensureNoVariableNameReferenceFromInitializer(CompilationUnit unit) {
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
          if (hasNameReference) {
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

  private void ensureUniqueClassMemberNames(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      private final Set<String> usedClassMemberNames = Sets.newHashSet();

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        usedClassMemberNames.clear();
        // ensure unique method names (and prefer to keep method name over field name)
        for (ClassMember member : node.getMembers()) {
          if (member instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) member;
            ensureUniqueName(methodDeclaration.getName());
          }
        }
        // ensure unique field names (if name is already used be method)
        for (ClassMember member : node.getMembers()) {
          if (member instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) member;
            for (VariableDeclaration field : fieldDeclaration.getFields().getVariables()) {
              ensureUniqueName(field.getName());
            }
          }
        }
        // no recursion
        return null;
      }

      private void ensureUniqueName(Identifier declarationName) {
        if (declarationName instanceof SimpleIdentifier) {
          SimpleIdentifier declarationIdentifier = (SimpleIdentifier) declarationName;
          String name = declarationIdentifier.getName();
          if (usedClassMemberNames.contains(name)) {
            String newName = generateUniqueName(name);
            // rename binding
            if (!newName.equals(name)) {
              renameIdentifier(declarationIdentifier, newName);
              name = newName;
            }
          }
          // remember that name is used
          usedClassMemberNames.add(name);
        }
      }
    });
  }

  /**
   * @return the globally unique name, based on the given one.
   */
  private String generateUniqueName(String name) {
    if (usedNames.contains(name)) {
      int index = 2;
      while (true) {
        String newName = name + index;
        if (!usedNames.contains(newName)) {
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
  private org.eclipse.jdt.core.dom.CompilationUnit parseJavaFile(File javaFile) throws Exception {
    String javaPath = javaFile.getAbsolutePath();
    String javaName = StringUtils.substringAfterLast(javaPath, "/");
    String javaSource = Files.toString(javaFile, Charsets.UTF_8);
    // prepare Java parser
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    {
      String[] sourceEntries = new String[sourceFolders.size()];
      for (int i = 0; i < sourceFolders.size(); i++) {
        sourceEntries[i] = sourceFolders.get(i).getAbsolutePath();
      }
      parser.setEnvironment(null, sourceEntries, null, true);
    }
    parser.setResolveBindings(true);
    parser.setCompilerOptions(ImmutableMap.of(
        JavaCore.COMPILER_SOURCE,
        JavaCore.VERSION_1_5,
        JavaCore.COMPILER_DOC_COMMENT_SUPPORT,
        JavaCore.ENABLED));
    // do parse
    parser.setUnitName(javaName);
    parser.setSource(javaSource.toCharArray());
    return (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(null);
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
        SimpleIdentifier nameNode = node.getName();
        String binding = constructorNameToBinding.get(nameNode);
        // prepare name
        String name = renameMap.get(binding);
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
        if (name == null) {
          // remove constructor name
          node.setName(null);
        } else {
          // rename constructor
          node.getName().setToken(new StringToken(TokenType.IDENTIFIER, name, 0));
          // set name in InstanceCreationExpression
          ConstructorDescription constructorDescription = bindingToConstructor.get(binding);
          {
            List<InstanceCreationExpression> creations = constructorDescription.instanceCreations;
            for (InstanceCreationExpression creation : creations) {
              creation.getConstructorName().setName(
                  new SimpleIdentifier(new StringToken(TokenType.IDENTIFIER, name, 0)));
            }
          }
          // set name in SuperConstructorInvocation
          {
            List<SuperConstructorInvocation> invocations = constructorDescription.superInvocations;
            for (SuperConstructorInvocation invocation : invocations) {
              invocation.setConstructorName(new SimpleIdentifier(new StringToken(
                  TokenType.IDENTIFIER,
                  name,
                  0)));
            }
          }
          // set name in invocation of implementation
          {
            List<SimpleIdentifier> invocations = constructorDescription.implInvocations;
            for (SimpleIdentifier identifier : invocations) {
              identifier.setToken(new StringToken(
                  TokenType.IDENTIFIER,
                  constructorDescription.implName,
                  0));
            }
          }
        }
        // continue
        return super.visitConstructorDeclaration(node);
      }

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        return super.visitInstanceCreationExpression(node);
      }
    });
  }

  /**
   * Sets the {@link SimpleIdentifier} name and updates all references.
   */
  private void renameIdentifier(SimpleIdentifier declarationIdentifier, String newName) {
    // move identifiers to the new signature
    String signature = identifierToBinding.get(declarationIdentifier);
    String newSignature = JavaUtils.getRenamedJdtSignature(signature, newName);
    List<SimpleIdentifier> identifiers = bindingToIdentifiers.remove(signature);
    bindingToIdentifiers.put(newSignature, identifiers);
    // update identifiers to the new name
    for (SimpleIdentifier identifier : identifiers) {
      identifier.setToken(new StringToken(TokenType.IDENTIFIER, newName, 0));
      identifierToBinding.remove(identifier);
      identifierToBinding.put(identifier, newSignature);
    }
  }

  /**
   * Translate {@link #sourceFiles} into Dart AST in {@link #dartUnits}.
   */
  private void translateSyntax() throws Exception {
    for (File javaFile : sourceFiles) {
      org.eclipse.jdt.core.dom.CompilationUnit javaUnit = parseJavaFile(javaFile);
      CompilationUnit dartUnit = SyntaxTranslator.translate(this, javaUnit);
      List<CompilationUnitMember> dartDeclarations = dartUnit.getDeclarations();
      // add to the Dart universe
      dartUniverse.getDeclarations().addAll(dartDeclarations);
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
}
