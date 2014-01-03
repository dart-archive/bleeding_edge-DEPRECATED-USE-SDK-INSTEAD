// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

var x = 42;

var _x = 123;

var myArray = ["value1"];

var foreignDoc = (function(){
  var doc = document.implementation.createDocument("", "root", null);
  var element = doc.createElement('element');
  element.setAttribute('id', 'abc');
  doc.documentElement.appendChild(element);
  return doc;
})();

function razzle() {
  return x;
}

function getTypeOf(o) {
  return typeof(o);
}

function varArgs() {
  var args = arguments;
  var sum = 0;
  for (var i = 0; i < args.length; ++i) {
    sum += args[i];
  }
  return sum;
}

function Foo(a) {
  this.a = a;
}

Foo.b = 38;

Foo.prototype.bar = function() {
  return this.a;
}
Foo.prototype.toString = function() {
  return "I'm a Foo a=" + this.a;
}

var container = new Object();
container.Foo = Foo;

function isArray(a) {
  return a instanceof Array;
}

function checkMap(m, key, value) {
  if (m.hasOwnProperty(key))
    return m[key] == value;
  else
    return false;
}

function invokeCallback() {
  return callback();
}

function invokeCallbackWith11params() {
  return callbackWith11params(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
}

function returnElement(element) {
  return element;
}

function getElementAttribute(element, attr) {
  return element.getAttribute(attr);
}

function addClassAttributes(list) {
  var result = "";
  for (var i=0; i<list.length; i++) {
    result += list[i].getAttribute("class");
  }
  return result;
}

function getNewDivElement() {
  return document.createElement("div");
}

function testJsMap(callback) {
  var result = callback();
  return result['value'];
}

function Bar() {
  return "ret_value";
}
Bar.foo = "property_value";
