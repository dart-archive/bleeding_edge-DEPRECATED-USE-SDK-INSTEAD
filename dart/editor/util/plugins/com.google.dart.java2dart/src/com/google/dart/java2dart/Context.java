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
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
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
  private static final String[] JAVA_EXTENSION = {"java"};

  private final List<File> sourceFolders = Lists.newArrayList();
  private final List<File> sourceFiles = Lists.newArrayList();
  private final Map<String, String> renameMap = Maps.newHashMap();

  private final Map<File, CompilationUnit> dartUnits = Maps.newLinkedHashMap();
  private final Map<SimpleIdentifier, String> identifierToBinding = Maps.newHashMap();
  private final Map<String, List<SimpleIdentifier>> bindingToIdentifiers = Maps.newHashMap();

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

  public CompilationUnit translate() throws Exception {
    // sort source files
    Collections.sort(sourceFiles);
    // perform syntax translation
    translateSyntax();
//    System.out.println(dartUnits);
//    System.out.println(identifierToBinding);
//    System.out.println(bindingToIdentifiers);
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
    // ensure field initializer
    for (CompilationUnit unit : dartUnits.values()) {
      ensureFieldInitializers(unit);
    }
    // ensure unique names
    for (CompilationUnit unit : dartUnits.values()) {
      ensureUniqueClassMemberNames(unit);
    }
    for (CompilationUnit unit : dartUnits.values()) {
      ensureNoVariableNameReferenceFromInitializer(unit);
    }
    // build single Dart unit
    List<CompilationUnitMember> declarations = Lists.newArrayList();
    for (CompilationUnit unit : dartUnits.values()) {
      declarations.addAll(unit.getDeclarations());
    }
    return new CompilationUnit(null, null, null, declarations, null);
  }

  /**
   * Remembers that "identifier" is reference to the given Java binding.
   */
  void putReference(org.eclipse.jdt.core.dom.IBinding binding, SimpleIdentifier identifier) {
    if (binding != null) {
      String signature = binding.getKey();
      signature = JavaUtils.getShortJdtSignature(signature);
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
      private String currentVaraibleName = null;
      private boolean hasNameReference = false;

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        hasNameReference |= node.getName().equals(currentVaraibleName);
        return super.visitSimpleIdentifier(node);
      }

      @Override
      public Void visitVariableDeclaration(VariableDeclaration node) {
        currentVaraibleName = node.getName().getName();
        hasNameReference = false;
        Expression initializer = node.getInitializer();
        if (initializer != null) {
          initializer.accept(this);
        }
//        System.out.println(node + " " + hasNameReference);
        currentVaraibleName = null;
        return null;
      }
    });
  }

  private void ensureUniqueClassMemberNames(CompilationUnit unit) {
    unit.accept(new RecursiveASTVisitor<Void>() {
      private final Set<String> usedNames = Sets.newHashSet();

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        usedNames.clear();
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
          if (usedNames.contains(name)) {
            // generate unique name
            String newName;
            {
              int index = 2;
              while (true) {
                newName = name + index;
                if (!usedNames.contains(newName)) {
                  break;
                }
                index++;
              }
            }
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
            // remember new name
            name = newName;
          }
          // remember that name is used
          usedNames.add(name);
        }
      }
    });
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

  /**
   * Translate {@link #sourceFiles} into Dart AST in {@link #dartUnits}.
   */
  private void translateSyntax() throws Exception {
    for (File javaFile : sourceFiles) {
      org.eclipse.jdt.core.dom.CompilationUnit javaUnit = parseJavaFile(javaFile);
      CompilationUnit dartUnit = SyntaxTranslator.translate(this, javaUnit);
      dartUnits.put(javaFile, dartUnit);
    }
  }
}
