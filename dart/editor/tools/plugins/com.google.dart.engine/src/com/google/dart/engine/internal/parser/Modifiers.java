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
package com.google.dart.engine.internal.parser;

import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code Modifiers} implement a simple data-holder for a method that needs
 * to return multiple values.
 * 
 * @coverage dart.engine.parser
 */
public class Modifiers {
  /**
   * The token representing the keyword 'abstract', or {@code null} if the keyword was not found.
   */
  private Token abstractKeyword;

  /**
   * The token representing the keyword 'const', or {@code null} if the keyword was not found.
   */
  private Token constKeyword;

  /**
   * The token representing the keyword 'external', or {@code null} if the keyword was not found.
   */
  private Token externalKeyword;

  /**
   * The token representing the keyword 'factory', or {@code null} if the keyword was not found.
   */
  private Token factoryKeyword;

  /**
   * The token representing the keyword 'final', or {@code null} if the keyword was not found.
   */
  private Token finalKeyword;

  /**
   * The token representing the keyword 'static', or {@code null} if the keyword was not found.
   */
  private Token staticKeyword;

  /**
   * The token representing the keyword 'var', or {@code null} if the keyword was not found.
   */
  private Token varKeyword;

  /**
   * Initialize a newly created and empty set of modifiers.
   */
  public Modifiers() {
    super();
  }

  /**
   * Return the token representing the keyword 'abstract', or {@code null} if the keyword was not
   * found.
   * 
   * @return the token representing the keyword 'abstract'
   */
  public Token getAbstractKeyword() {
    return abstractKeyword;
  }

  /**
   * Return the token representing the keyword 'const', or {@code null} if the keyword was not
   * found.
   * 
   * @return the token representing the keyword 'const'
   */
  public Token getConstKeyword() {
    return constKeyword;
  }

  /**
   * Return the token representing the keyword 'external', or {@code null} if the keyword was not
   * found.
   * 
   * @return the token representing the keyword 'external'
   */
  public Token getExternalKeyword() {
    return externalKeyword;
  }

  /**
   * Return the token representing the keyword 'factory', or {@code null} if the keyword was not
   * found.
   * 
   * @return the token representing the keyword 'factory'
   */
  public Token getFactoryKeyword() {
    return factoryKeyword;
  }

  /**
   * Return the token representing the keyword 'final', or {@code null} if the keyword was not
   * found.
   * 
   * @return the token representing the keyword 'final'
   */
  public Token getFinalKeyword() {
    return finalKeyword;
  }

  /**
   * Return the token representing the keyword 'static', or {@code null} if the keyword was not
   * found.
   * 
   * @return the token representing the keyword 'static'
   */
  public Token getStaticKeyword() {
    return staticKeyword;
  }

  /**
   * Return the token representing the keyword 'var', or {@code null} if the keyword was not found.
   * 
   * @return the token representing the keyword 'var'
   */
  public Token getVarKeyword() {
    return varKeyword;
  }

  /**
   * Set the token representing the keyword 'abstract' to the given token.
   * 
   * @param abstractKeyword the token representing the keyword 'abstract'
   */
  public void setAbstractKeyword(Token abstractKeyword) {
    this.abstractKeyword = abstractKeyword;
  }

  /**
   * Set the token representing the keyword 'const' to the given token.
   * 
   * @param constKeyword the token representing the keyword 'const'
   */
  public void setConstKeyword(Token constKeyword) {
    this.constKeyword = constKeyword;
  }

  /**
   * Set the token representing the keyword 'external' to the given token.
   * 
   * @param externalKeyword the token representing the keyword 'external'
   */
  public void setExternalKeyword(Token externalKeyword) {
    this.externalKeyword = externalKeyword;
  }

  /**
   * Set the token representing the keyword 'factory' to the given token.
   * 
   * @param factoryKeyword the token representing the keyword 'factory'
   */
  public void setFactoryKeyword(Token factoryKeyword) {
    this.factoryKeyword = factoryKeyword;
  }

  /**
   * Set the token representing the keyword 'final' to the given token.
   * 
   * @param finalKeyword the token representing the keyword 'final'
   */
  public void setFinalKeyword(Token finalKeyword) {
    this.finalKeyword = finalKeyword;
  }

  /**
   * Set the token representing the keyword 'static' to the given token.
   * 
   * @param staticKeyword the token representing the keyword 'static'
   */
  public void setStaticKeyword(Token staticKeyword) {
    this.staticKeyword = staticKeyword;
  }

  /**
   * Set the token representing the keyword 'var' to the given token.
   * 
   * @param varKeyword the token representing the keyword 'var'
   */
  public void setVarKeyword(Token varKeyword) {
    this.varKeyword = varKeyword;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean needsSpace = appendKeyword(builder, false, abstractKeyword);
    needsSpace = appendKeyword(builder, needsSpace, constKeyword);
    needsSpace = appendKeyword(builder, needsSpace, externalKeyword);
    needsSpace = appendKeyword(builder, needsSpace, factoryKeyword);
    needsSpace = appendKeyword(builder, needsSpace, finalKeyword);
    needsSpace = appendKeyword(builder, needsSpace, staticKeyword);
    appendKeyword(builder, needsSpace, varKeyword);
    return builder.toString();
  }

  /**
   * If the given keyword is not {@code null}, append it to the given builder, prefixing it with a
   * space if needed.
   * 
   * @param builder the builder to which the keyword will be appended
   * @param needsSpace {@code true} if the keyword needs to be prefixed with a space
   * @param keyword the keyword to be appended
   * @return {@code true} if subsequent keywords need to be prefixed with a space
   */
  private boolean appendKeyword(StringBuilder builder, boolean needsSpace, Token keyword) {
    if (keyword != null) {
      if (needsSpace) {
        builder.append(' ');
      }
      builder.append(keyword.getLexeme());
      return true;
    }
    return needsSpace;
  }
}
