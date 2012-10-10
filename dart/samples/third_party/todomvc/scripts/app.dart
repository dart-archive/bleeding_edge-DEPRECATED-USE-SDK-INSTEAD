// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('todomvc');

#import('../../../webcomponents/shadow_polyfill.dart');
#import('dart:html');

#source('components.dart');

const int ENTER_KEY = 13;

// hack to get unique todo id's.
int id = 0;

typedef Dynamic TodoCallback(Todo todo);

void main() {
  RegistryLookupFunction lookup(tagName) {
    switch(tagName) {
      case 'x-todo-list-element' : 
          return ((shadowRoot, element) =>
          new TodoListElement.internal(shadowRoot, element));
      case 'x-todo-list' : 
          return ((shadowRoot, element) => 
          new TodoList.internal(shadowRoot, element));
      case 'x-new-todo' : 
          return ((shadowRoot, element) => 
          new NewTodo.internal(shadowRoot, element));
    };
  };
  initializeComponents(lookup);

  // wire everything up to the model
  var model = new Todos();
  TodoList list = manager[query('div[is=x-todo-list]')];
  list.model = model;
  
  NewTodo newTodo = manager[query('div[is=x-new-todo]')];
  newTodo.model = model;
}

/** Global model for TodoMVC app */
class Todos {
  List<Todo> _todos;
  int remaining;
  TodoCallbacks on;
 
  Todos()
    : remaining = 0,
      _todos = <Todo>[],
      on = new TodoCallbacks();

  void addTodo(Todo todo) {
    _todos.add(todo);
    remaining++;
    on.added.forEach((c) => c(todo));
  }

  void removeTodo(Todo todo) {
    _todos.removeRange(_todos.indexOf(todo),1);
    if (!(todo.completed)) {
      remaining--;
    }
    on.removed.forEach((c) => c(todo));
  }

  void complete(Todo todo) {
    todo.completed = true;
    remaining--;
    on.completed.forEach((c) => c(todo));
  }

  void uncomplete(Todo todo) {
    todo.completed = false;
    remaining++;
    on.uncompleted.forEach((c) => c(todo));
  }

  void completeAll() => _todos.forEach((todo) => complete(todo));
  void uncompleteAll() => _todos.forEach((todo) => uncomplete(todo));

  void clearCompleted() {
    _todos.filter((t) => t.completed).forEach((t) => removeTodo(t));
  }
}

/** 
 * Wrapper for callbacks from the Todo model to the components.
 * This is separated into its own class so we can support the 
 * model.on.event.add(callback) style.
 */
class TodoCallbacks {
  List<TodoCallback> added;
  List<TodoCallback> removed;
  List<TodoCallback> completed;
  List<TodoCallback> uncompleted;

  TodoCallbacks()
    : added = <TodoCallback>[],
      removed = <TodoCallback>[],
      completed = <TodoCallback>[],
      uncompleted = <TodoCallback>[];
}

/** Wraps data for an individual todo */
class Todo {
  String value;
  bool completed;
  int _id;

  Todo._internal(this.value);

  factory Todo(String value) {
    var todo = new Todo._internal(value);
    todo._id = id++;
    return todo;
  }

  operator ==(other) {
    return _id == other._id;
  }

  int hashCode() => _id;
}
