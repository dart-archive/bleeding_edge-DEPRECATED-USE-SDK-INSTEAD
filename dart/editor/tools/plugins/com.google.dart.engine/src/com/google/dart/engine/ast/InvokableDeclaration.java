package com.google.dart.engine.ast;


/**
 * An invokable body of code with a return type which may be invoked, have parameters, and/or be an
 * accessor.
 */
public interface InvokableDeclaration {

  public SimpleIdentifier getName();

  public TypeName getReturnType();

  public boolean isGetter();

  public boolean isSetter();

}
