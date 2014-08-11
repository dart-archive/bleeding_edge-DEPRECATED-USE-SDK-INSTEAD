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
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.ast.visitor.UnifyingAstVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.processor.ConstructorSemanticProcessor;
import com.google.dart.java2dart.processor.LocalVariablesSemanticProcessor;
import com.google.dart.java2dart.util.Bindings;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.AstFactory.assignmentExpression;
import static com.google.dart.java2dart.util.AstFactory.block;
import static com.google.dart.java2dart.util.AstFactory.blockFunctionBody;
import static com.google.dart.java2dart.util.AstFactory.compilationUnit;
import static com.google.dart.java2dart.util.AstFactory.constructorDeclaration;
import static com.google.dart.java2dart.util.AstFactory.expressionStatement;
import static com.google.dart.java2dart.util.AstFactory.formalParameterList;
import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.propertyAccess;
import static com.google.dart.java2dart.util.AstFactory.thisExpression;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

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
  public static class ConstructorDescription {
    final IMethodBinding binding;
    public final List<RedirectingConstructorInvocation> redirectingInvocations = Lists.newArrayList();
    public final List<SuperConstructorInvocation> superInvocations = Lists.newArrayList();
    public final List<InstanceCreationExpression> instanceCreations = Lists.newArrayList();
    public boolean isEnum;
    public boolean insertEnclosingTypeRef;
    String declName;

    public ConstructorDescription(IMethodBinding binding) {
      this.binding = binding;
    }
  }

  private static final String[] JAVA_EXTENSION = {"java"};
  private final List<File> classpathFiles = Lists.newArrayList();
  private final List<File> sourceFolders = Lists.newArrayList();
  private final List<File> sourceFiles = Lists.newArrayList();

  private final Map<String, String> renameMap = Maps.newHashMap();
  private final Set<String> notPropertySet = Sets.newHashSet();

  private final CompilationUnit dartUniverse = compilationUnit();
  private final Map<File, List<CompilationUnitMember>> fileToMembers = Maps.newHashMap();
  private final Map<CompilationUnitMember, File> memberToFile = Maps.newHashMap();
  // information about names
  public static final Set<String> FORBIDDEN_NAMES = Sets.newHashSet();
  private final Set<String> usedNames = Sets.newHashSet();
  private final Set<ClassMember> privateClassMembers = Sets.newHashSet();
  private final Map<SimpleIdentifier, String> identifierToName = Maps.newHashMap();
  private final Map<String, Object> signatureToBinding = Maps.newHashMap();
  private final Map<Object, List<SimpleIdentifier>> bindingToIdentifiers = Maps.newHashMap();
  private final Map<AstNode, IBinding> nodeToBinding = Maps.newHashMap();
  private final Map<AstNode, ITypeBinding> nodeToTypeBinding = Maps.newHashMap();
  private final Map<InstanceCreationExpression, ClassDeclaration> anonymousDeclarations = Maps.newHashMap();
  private final Set<SimpleIdentifier> innerClassNames = Sets.newHashSet();
  private final Map<AstNode, List<ParsedAnnotation>> nodeAnnotations = Maps.newHashMap();
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
        FORBIDDEN_NAMES.add(keyword.getSyntax());
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
   * Specifies that the method with given signature should not be converted to getter/setter.
   */
  public void addNotProperty(String signature) {
    notPropertySet.add(signature);
  }

  /**
   * Specifies that method with given signature should be renamed before normalizing member names.
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

  public void applyLocalVariableSemanticChanges(CompilationUnit unit) {
    new LocalVariablesSemanticProcessor(this).process(unit);
  }

  /**
   * @return {@code true} if the method with the given signature (which could be made getter or
   *         setter) is allowed to be converted into getter/setter.
   */
  public boolean canMakeProperty(SimpleIdentifier identifier) {
    IBinding binding = getNodeBinding(identifier);
    String signature = JavaUtils.getJdtSignature(binding);
    return !notPropertySet.contains(signature);
  }

  /**
   * Clears information about the given {@link AstNode} and its children.
   */
  public void clearNodes(AstNode node) {
    if (node != null) {
      node.accept(new UnifyingAstVisitor<Void>() {
        @Override
        public Void visitNode(AstNode node) {
          clearNode(node);
          return super.visitNode(node);
        }
      });
    }
  }

  public void ensureUniqueClassMemberNames() {
    dartUniverse.accept(new RecursiveAstVisitor<Void>() {
      private final Set<ClassMember> untouchableMethods = Sets.newHashSet();
      private final Map<String, ClassMember> usedClassMembers = Maps.newHashMap();
      private final Set<String> superNames = Sets.newHashSet();
      private final Map<String, List<IMethodBinding>> superMembers = Maps.newHashMap();

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        untouchableMethods.clear();
        usedClassMembers.clear();
        superNames.clear();
        superMembers.clear();
        // fill "static" methods from super classes
        {
          org.eclipse.jdt.core.dom.ITypeBinding binding = getNodeTypeBinding(node);
          if (binding != null) {
            binding = binding.getSuperclass();
            while (binding != null) {
              for (org.eclipse.jdt.core.dom.IMethodBinding method : binding.getDeclaredMethods()) {
                if (org.eclipse.jdt.core.dom.Modifier.isStatic(method.getModifiers())) {
                  usedClassMembers.put(method.getName(), null);
                } else {
                  addSuperMember(method);
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
            // may be overloaded method
            {
              Object binding = nodeToBinding.get(method);
              if (binding instanceof IMethodBinding) {
                IMethodBinding methodBinding = (IMethodBinding) binding;
                String name = methodBinding.getName();
                if (superNames.contains(name)) {
                  IMethodBinding over = Bindings.findOverriddenMethod(methodBinding, false);
                  if (over == null) {
                    usedClassMembers.put(name, null);
                  }
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

      private void addSuperMember(IMethodBinding binding) {
        String name = binding.getName();
        superNames.add(name);
        List<IMethodBinding> members = superMembers.get(name);
        if (members == null) {
          members = Lists.newArrayList();
          superMembers.put(name, members);
        }
        members.add(binding);
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
        return !FORBIDDEN_NAMES.contains(name) && !usedClassMembers.containsKey(name);
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

  /**
   * @return some Java binding for the given Dart {@link ConstructorDeclaration}.
   */
  public IMethodBinding getConstructorBinding(ConstructorDeclaration node) {
    return constructorToBinding.get(node);
  }

  /**
   * @return the not <code>null</code> {@link ConstructorDescription}, may be just added.
   */
  public ConstructorDescription getConstructorDescription(ConstructorDeclaration node) {
    IMethodBinding binding = getConstructorBinding(node);
    return getConstructorDescription(binding);
  }

  /**
   * @return the not <code>null</code> {@link ConstructorDescription}, may be just added.
   */
  public ConstructorDescription getConstructorDescription(IMethodBinding binding) {
    binding = (IMethodBinding) Bindings.getDeclaration(binding);
    ConstructorDescription description = bindingToConstructor.get(binding);
    if (description == null) {
      description = new ConstructorDescription(binding);
      bindingToConstructor.put(binding, description);
    }
    return description;
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

  public List<MethodInvocation> getInvocations(MethodDeclaration method) {
    List<MethodInvocation> invocations = Lists.newArrayList();
    SimpleIdentifier methodName = method.getName();
    List<SimpleIdentifier> references = getReferences(methodName);
    for (SimpleIdentifier reference : references) {
      if (reference.getParent() instanceof MethodInvocation) {
        MethodInvocation invocation = (MethodInvocation) reference.getParent();
        if (invocation.getMethodName() == reference) {
          invocations.add(invocation);
        }
      }
    }
    return invocations;
  }

  public Map<CompilationUnitMember, File> getMemberToFile() {
    return memberToFile;
  }

  /**
   * Returns all node annotations. Used for testing.
   */
  public Map<AstNode, List<ParsedAnnotation>> getNodeAnnotations() {
    return nodeAnnotations;
  }

  /**
   * Returns {@link ParsedAnnotation}s object for the Java annotation specified on the Java node of
   * the given {@link AstNode}, maybe {@code null}.
   */
  public List<ParsedAnnotation> getNodeAnnotations(AstNode node) {
    return nodeAnnotations.get(node);
  }

  /**
   * @return some Java binding for the given Dart {@link AstNode}.
   */
  public IBinding getNodeBinding(AstNode node) {
    return nodeToBinding.get(node);
  }

  /**
   * @return some Java {@link ITypeBinding} for the given Dart {@link AstNode}.
   */
  public ITypeBinding getNodeTypeBinding(AstNode node) {
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

  /**
   * @return the name of member declared in enclosing {@link ClassDeclaration} and its super
   *         classes.
   */
  public Set<String> getSuperMembersNames(AstNode node) {
    Set<String> hierarchyNames = Sets.newHashSet();
    ClassDeclaration classDeclaration = node.getAncestor(ClassDeclaration.class);
    org.eclipse.jdt.core.dom.ITypeBinding binding = getNodeTypeBinding(classDeclaration);
    if (binding != null) {
      binding = binding.getSuperclass();
      while (binding != null) {
        for (org.eclipse.jdt.core.dom.IVariableBinding field : binding.getDeclaredFields()) {
          hierarchyNames.add(field.getName());
        }
        for (org.eclipse.jdt.core.dom.IMethodBinding method : binding.getDeclaredMethods()) {
          hierarchyNames.add(method.getName());
        }
        binding = binding.getSuperclass();
      }
    }
    return hierarchyNames;
  }

  public boolean isFieldBinding(AstNode node) {
    IBinding binding = getNodeBinding(node);
    if (binding instanceof IVariableBinding) {
      return ((IVariableBinding) binding).isField();
    }
    return false;
  }

  public boolean isMethodBinding(AstNode node) {
    IBinding binding = getNodeBinding(node);
    return binding instanceof IMethodBinding;
  }

  /**
   * Remembers that "identifier" is reference to the given Java binding.
   */
  public void putReference(SimpleIdentifier identifier, IBinding binding, String bindingSignature) {
    String name = identifier.getName();
    if (binding != null) {
      signatureToBinding.put(bindingSignature, binding);
      identifierToName.put(identifier, name);
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
    usedNames.add(name);
  }

  /**
   * Removes recorded identifier reference.
   */
  public void removeReference(SimpleIdentifier identifier) {
    IBinding binding = getNodeBinding(identifier);
    List<SimpleIdentifier> identifiers = bindingToIdentifiers.get(binding);
    if (identifiers != null) {
      identifiers.remove(identifier);
    }
  }

  /**
   * Specifies that the given {@link File} should not be translated.
   */
  public void removeSourceFile(File file) {
    Assert.isLegal(file.exists(), "File '" + file + "' does not exist.");
    file = file.getAbsoluteFile();
    sourceFiles.remove(file);
  }

  /**
   * Specifies that all files in given folder should not be translated.
   */
  public void removeSourceFiles(File folder) {
    Assert.isLegal(folder.exists(), "Folder '" + folder + "' does not exist.");
    Assert.isLegal(folder.isDirectory(), "Folder '" + folder + "' is not a folder.");
    folder = folder.getAbsoluteFile();
    Collection<File> folderFiles = FileUtils.listFiles(folder, JAVA_EXTENSION, true);
    sourceFiles.removeAll(folderFiles);
  }

  public void renameConstructor(ConstructorDeclaration node, String name) {
    IMethodBinding binding = getConstructorBinding(node);
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
      // set name in RedirectingConstructorInvocation
      {
        List<RedirectingConstructorInvocation> invocations = constructorDescription.redirectingInvocations;
        for (RedirectingConstructorInvocation invocation : invocations) {
          invocation.setConstructorName(newIdentifier);
        }
      }
      // set name in SuperConstructorInvocation
      {
        List<SuperConstructorInvocation> invocations = constructorDescription.superInvocations;
        for (SuperConstructorInvocation invocation : invocations) {
          invocation.setConstructorName(newIdentifier);
        }
      }
      // set name in InstanceCreationExpression
      {
        List<InstanceCreationExpression> creations = constructorDescription.instanceCreations;
        for (InstanceCreationExpression creation : creations) {
          creation.getConstructorName().setName(newIdentifier);
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
    if (identifiers == null) {
      return;
    }
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
      renameAnonymousClassDeclarations();
      renamePrivateClassMembers();
      new ConstructorSemanticProcessor(this).process(dartUniverse);
      insertEnclosingTypeForInstanceCreationArguments(dartUniverse);
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
    binding = (IMethodBinding) Bindings.getDeclaration(binding);
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
   * Remembers the parsed annotation for the given node.
   */
  void putNodeAnnotation(AstNode node, ParsedAnnotation parsedAnnotation) {
    List<ParsedAnnotation> annotations = nodeAnnotations.get(node);
    if (annotations == null) {
      annotations = Lists.newArrayList();
      nodeAnnotations.put(node, annotations);
    }
    annotations.add(parsedAnnotation);
  }

  /**
   * Remembers some Java binding for the given Dart {@link AstNode}.
   */
  void putNodeBinding(AstNode node, IBinding binding) {
    nodeToBinding.put(node, binding);
  }

  /**
   * Remembers Java {@link ITypeBinding} for the given Dart {@link AstNode}.
   */
  void putNodeTypeBinding(AstNode node, ITypeBinding binding) {
    nodeToTypeBinding.put(node, binding);
  }

  /**
   * Remembers that given {@link ClassMember} was created for private Java element.
   */
  void putPrivateClassMember(ClassMember member) {
    privateClassMembers.add(member);
  }

  /**
   * Clears information about the given {@link AstNode}.
   */
  private void clearNode(AstNode node) {
    if (node instanceof SimpleIdentifier) {
      removeReference((SimpleIdentifier) node);
    }
    nodeToBinding.remove(node);
    nodeToTypeBinding.remove(node);
  }

  private void dontUseThisInFieldInitializers(CompilationUnit unit) {
    unit.accept(new RecursiveAstVisitor<Void>() {
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
        classDeclaration.accept(new RecursiveAstVisitor<Void>() {
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

          private boolean hasThisExpression(AstNode node) {
            final AtomicBoolean result = new AtomicBoolean();
            node.accept(new GeneralizingAstVisitor<Void>() {
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
        boolean hasConstructor = false;
        for (ClassMember classMember : classDeclaration.getMembers()) {
          if (classMember instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructor = (ConstructorDeclaration) classMember;
            hasConstructor = true;
            Block block = ((BlockFunctionBody) constructor.getBody()).getBlock();
            addAssignmentsToBlock(block, thisInitializers);
          }
        }
        // no constructors, generate default constructor
        if (!hasConstructor) {
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
    unit.accept(new RecursiveAstVisitor<Void>() {
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

  private void insertEnclosingTypeForInstanceCreationArguments(CompilationUnit unit) {
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        IMethodBinding binding = (IMethodBinding) getNodeBinding(node);
        ConstructorDescription constructorDescription = getConstructorDescription(binding);
        if (constructorDescription.insertEnclosingTypeRef) {
          node.getArgumentList().getArguments().add(0, thisExpression());
        }
        return super.visitInstanceCreationExpression(node);
      }
    });
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
//        for (IProblem problem : javaUnit.getProblems()) {
//          System.out.println(problem);
//        }
        try {
          File astFile = pathToFile.get(sourceFilePath);
          String javaSource = Files.toString(astFile, Charsets.UTF_8);
          CompilationUnit dartUnit = SyntaxTranslator.translate(Context.this, javaUnit, javaSource);
          units.put(astFile, dartUnit);
        } catch (Throwable e) {
          throw new Error(e);
        }
      }
    },
        null);
    return units;
  }

  /**
   * Improves names for anonymous {@link ClassDeclaration}s.
   */
  private void renameAnonymousClassDeclarations() {
    // prepare unused top-level names
    Set<String> usedTopNames = Sets.newHashSet();
    for (CompilationUnitMember unitMember : dartUniverse.getDeclarations()) {
      if (unitMember instanceof ClassDeclaration) {
        ClassDeclaration classDeclaration = (ClassDeclaration) unitMember;
        String name = classDeclaration.getName().getName();
        usedTopNames.add(name);
      }
    }
    // rename anonymous types
    for (Entry<InstanceCreationExpression, ClassDeclaration> entry : anonymousDeclarations.entrySet()) {
      // prepare enclosing information
      InstanceCreationExpression creation = entry.getKey();
      ClassDeclaration enclosingClass = creation.getAncestor(ClassDeclaration.class);
      //
      SimpleIdentifier enclosingClassMemberName = null;
      if (enclosingClassMemberName == null) {
        MethodDeclaration enclosingMethod = creation.getAncestor(MethodDeclaration.class);
        if (enclosingMethod != null) {
          enclosingClassMemberName = enclosingMethod.getName();
        }
      }
      if (enclosingClassMemberName == null) {
        VariableDeclaration enclosingField = creation.getAncestor(VariableDeclaration.class);
        if (enclosingField != null) {
          enclosingClassMemberName = enclosingField.getName();
        }
      }
      // prepare new name for anonymous class
      ClassDeclaration classDeclaration = entry.getValue();
      SimpleIdentifier nameNode = classDeclaration.getName();
      String name = nameNode.getName();
      name = StringUtils.substringBeforeLast(name, "_");
      {
        String enclosingClassName = enclosingClass.getName().getName();
        if (!enclosingClassName.equals(name)) {
          name = name + "_" + enclosingClassName;
        }
      }
      if (enclosingClassMemberName != null) {
        name += "_" + enclosingClassMemberName.getName();
      }
      // ensure unique name
      if (!usedTopNames.add(name)) {
        int index = 2;
        while (true) {
          String newName = name + "_" + index++;
          if (usedTopNames.add(newName)) {
            name = newName;
            break;
          }
        }
      }
      // rename
      renameIdentifier(nameNode, name);
    }
  }

  private void renamePrivateClassMembers() {
    for (ClassMember member : privateClassMembers) {
      if (member instanceof FieldDeclaration) {
        FieldDeclaration fieldDeclaration = (FieldDeclaration) member;
        NodeList<VariableDeclaration> variables = fieldDeclaration.getFields().getVariables();
        for (VariableDeclaration field : variables) {
          SimpleIdentifier nameNode = field.getName();
          String name = nameNode.getName();
          renameIdentifier(nameNode, "_" + name);
        }
      }
      if (member instanceof MethodDeclaration) {
        MethodDeclaration methodDeclaration = (MethodDeclaration) member;
        SimpleIdentifier nameNode = methodDeclaration.getName();
        String name = nameNode.getName();
        renameIdentifier(nameNode, "_" + name);
      }
    }
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

  private void unwrapVarArgIfAlreadyArray(CompilationUnit unit) {
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        process(node, node.getArgumentList());
        return super.visitInstanceCreationExpression(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        process(node, node.getArgumentList());
        return super.visitMethodInvocation(node);
      }

      @Override
      public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
        process(node, node.getArgumentList());
        return super.visitSuperConstructorInvocation(node);
      }

      private void process(AstNode node, ArgumentList argumentList) {
        Object binding = nodeToBinding.get(node);
        if (binding instanceof IMethodBinding) {
          IMethodBinding methodBinding = (IMethodBinding) binding;
          if (methodBinding.isVarargs()) {
            List<Expression> args = argumentList.getArguments();
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
      }
    });
  }
}
