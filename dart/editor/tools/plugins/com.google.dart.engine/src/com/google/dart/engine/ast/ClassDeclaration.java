/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code ClassDeclaration} represent the declaration of a class.
 * 
 * <pre>
 * classDeclaration ::=
 *     'abstract'? 'class' {@link SimpleIdentifier name} {@link TypeParameterList typeParameterList}?
 *     ({@link ExtendsClause extendsClause} {@link WithClause withClause}?)?
 *     {@link ImplementsClause implementsClause}?
 *     '{' {@link ClassMember classMember}* '}'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ClassDeclaration extends CompilationUnitMember {
  /**
   * The 'abstract' keyword, or {@code null} if the keyword was absent.
   */
  private Token abstractKeyword;

  /**
   * The token representing the 'class' keyword.
   */
  private Token classKeyword;

  /**
   * The name of the class being declared.
   */
  private SimpleIdentifier name;

  /**
   * The type parameters for the class, or {@code null} if the class does not have any type
   * parameters.
   */
  private TypeParameterList typeParameters;

  /**
   * The extends clause for the class, or {@code null} if the class does not extend any other class.
   */
  private ExtendsClause extendsClause;

  /**
   * The with clause for the class, or {@code null} if the class does not have a with clause.
   */
  private WithClause withClause;

  /**
   * The implements clause for the class, or {@code null} if the class does not implement any
   * interfaces.
   */
  private ImplementsClause implementsClause;

  /**
   * The native clause for the class, or {@code null} if the class does not have a native clause.
   */
  private NativeClause nativeClause;

  /**
   * The left curly bracket.
   */
  private Token leftBracket;

  /**
   * The members defined by the class.
   */
  private NodeList<ClassMember> members = new NodeList<ClassMember>(this);

  /**
   * The right curly bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created class declaration.
   * 
   * @param comment the documentation comment associated with this class
   * @param metadata the annotations associated with this class
   * @param abstractKeyword the 'abstract' keyword, or {@code null} if the keyword was absent
   * @param classKeyword the token representing the 'class' keyword
   * @param name the name of the class being declared
   * @param typeParameters the type parameters for the class
   * @param extendsClause the extends clause for the class
   * @param withClause the with clause for the class
   * @param implementsClause the implements clause for the class
   * @param leftBracket the left curly bracket
   * @param members the members defined by the class
   * @param rightBracket the right curly bracket
   */
  public ClassDeclaration(Comment comment, List<Annotation> metadata, Token abstractKeyword,
      Token classKeyword, SimpleIdentifier name, TypeParameterList typeParameters,
      ExtendsClause extendsClause, WithClause withClause, ImplementsClause implementsClause,
      Token leftBracket, List<ClassMember> members, Token rightBracket) {
    super(comment, metadata);
    this.abstractKeyword = abstractKeyword;
    this.classKeyword = classKeyword;
    this.name = becomeParentOf(name);
    this.typeParameters = becomeParentOf(typeParameters);
    this.extendsClause = becomeParentOf(extendsClause);
    this.withClause = becomeParentOf(withClause);
    this.implementsClause = becomeParentOf(implementsClause);
    this.leftBracket = leftBracket;
    this.members.addAll(members);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitClassDeclaration(this);
  }

  /**
   * Return the 'abstract' keyword, or {@code null} if the keyword was absent.
   * 
   * @return the 'abstract' keyword
   */
  public Token getAbstractKeyword() {
    return abstractKeyword;
  }

  /**
   * Return the token representing the 'class' keyword.
   * 
   * @return the token representing the 'class' keyword
   */
  public Token getClassKeyword() {
    return classKeyword;
  }

  /**
   * Return the constructor declared in the class with the given name.
   * 
   * @param name the name of the constructor to find, {@code null} for default
   * @return the found constructor or {@code null} if not found
   */
  public ConstructorDeclaration getConstructor(String name) {
    for (ClassMember classMember : members) {
      if (classMember instanceof ConstructorDeclaration) {
        ConstructorDeclaration constructor = (ConstructorDeclaration) classMember;
        SimpleIdentifier constructorName = constructor.getName();
        if (name == null && constructorName == null) {
          return constructor;
        }
        if (constructorName != null && constructorName.getName().equals(name)) {
          return constructor;
        }
      }
    }
    return null;
  }

  @Override
  public ClassElement getElement() {
    return name != null ? (ClassElement) name.getStaticElement() : null;
  }

  @Override
  public Token getEndToken() {
    return rightBracket;
  }

  /**
   * Return the extends clause for this class, or {@code null} if the class does not extend any
   * other class.
   * 
   * @return the extends clause for this class
   */
  public ExtendsClause getExtendsClause() {
    return extendsClause;
  }

  /**
   * Return the field declared in the class with the given name.
   * 
   * @param name the name of the field to find
   * @return the found field or {@code null} if not found
   */
  public VariableDeclaration getField(String name) {
    for (ClassMember classMember : members) {
      if (classMember instanceof FieldDeclaration) {
        FieldDeclaration fieldDeclaration = (FieldDeclaration) classMember;
        NodeList<VariableDeclaration> fields = fieldDeclaration.getFields().getVariables();
        for (VariableDeclaration field : fields) {
          SimpleIdentifier fieldName = field.getName();
          if (fieldName != null && name.equals(fieldName.getName())) {
            return field;
          }
        }
      }
    }
    return null;
  }

  /**
   * Return the implements clause for the class, or {@code null} if the class does not implement any
   * interfaces.
   * 
   * @return the implements clause for the class
   */
  public ImplementsClause getImplementsClause() {
    return implementsClause;
  }

  /**
   * Return the left curly bracket.
   * 
   * @return the left curly bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the members defined by the class.
   * 
   * @return the members defined by the class
   */
  public NodeList<ClassMember> getMembers() {
    return members;
  }

  /**
   * Return the method declared in the class with the given name.
   * 
   * @param name the name of the method to find
   * @return the found method or {@code null} if not found
   */
  public MethodDeclaration getMethod(String name) {
    for (ClassMember classMember : members) {
      if (classMember instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) classMember;
        SimpleIdentifier methodName = method.getName();
        if (methodName != null && name.equals(methodName.getName())) {
          return method;
        }
      }
    }
    return null;
  }

  /**
   * Return the name of the class being declared.
   * 
   * @return the name of the class being declared
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Return the native clause for this class, or {@code null} if the class does not have a native
   * cluse.
   * 
   * @return the native clause for this class
   */
  public NativeClause getNativeClause() {
    return nativeClause;
  }

  /**
   * Return the right curly bracket.
   * 
   * @return the right curly bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Return the type parameters for the class, or {@code null} if the class does not have any type
   * parameters.
   * 
   * @return the type parameters for the class
   */
  public TypeParameterList getTypeParameters() {
    return typeParameters;
  }

  /**
   * Return the with clause for the class, or {@code null} if the class does not have a with clause.
   * 
   * @return the with clause for the class
   */
  public WithClause getWithClause() {
    return withClause;
  }

  /**
   * Set the 'abstract' keyword to the given keyword.
   * 
   * @param abstractKeyword the 'abstract' keyword
   */
  public void setAbstractKeyword(Token abstractKeyword) {
    this.abstractKeyword = abstractKeyword;
  }

  /**
   * Set the token representing the 'class' keyword to the given token.
   * 
   * @param classKeyword the token representing the 'class' keyword
   */
  public void setClassKeyword(Token classKeyword) {
    this.classKeyword = classKeyword;
  }

  /**
   * Set the extends clause for this class to the given clause.
   * 
   * @param extendsClause the extends clause for this class
   */
  public void setExtendsClause(ExtendsClause extendsClause) {
    this.extendsClause = becomeParentOf(extendsClause);
  }

  /**
   * Set the implements clause for the class to the given clause.
   * 
   * @param implementsClause the implements clause for the class
   */
  public void setImplementsClause(ImplementsClause implementsClause) {
    this.implementsClause = becomeParentOf(implementsClause);
  }

  /**
   * Set the left curly bracket to the given token.
   * 
   * @param leftBracket the left curly bracket
   */
  public void setLeftBracket(Token leftBracket) {
    this.leftBracket = leftBracket;
  }

  /**
   * Set the name of the class being declared to the given identifier.
   * 
   * @param identifier the name of the class being declared
   */
  public void setName(SimpleIdentifier identifier) {
    name = becomeParentOf(identifier);
  }

  /**
   * Set the native clause for this class to the given clause.
   * 
   * @param nativeClause the native clause for this class
   */
  public void setNativeClause(NativeClause nativeClause) {
    this.nativeClause = nativeClause;
  }

  /**
   * Set the right curly bracket to the given token.
   * 
   * @param rightBracket the right curly bracket
   */
  public void setRightBracket(Token rightBracket) {
    this.rightBracket = rightBracket;
  }

  /**
   * Set the type parameters for the class to the given list of type parameters.
   * 
   * @param typeParameters the type parameters for the class
   */
  public void setTypeParameters(TypeParameterList typeParameters) {
    this.typeParameters = typeParameters;
  }

  /**
   * Set the with clause for the class to the given clause.
   * 
   * @param withClause the with clause for the class
   */
  public void setWithClause(WithClause withClause) {
    this.withClause = becomeParentOf(withClause);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(name, visitor);
    safelyVisitChild(typeParameters, visitor);
    safelyVisitChild(extendsClause, visitor);
    safelyVisitChild(withClause, visitor);
    safelyVisitChild(implementsClause, visitor);
    safelyVisitChild(nativeClause, visitor);
    getMembers().accept(visitor);
  }

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    if (abstractKeyword != null) {
      return abstractKeyword;
    }
    return classKeyword;
  }
}
