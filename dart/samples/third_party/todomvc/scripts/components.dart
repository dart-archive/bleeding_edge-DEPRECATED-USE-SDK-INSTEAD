// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** Component representing one todo list element. */
class TodoListElement implements WebComponent {
  ShadowRoot _shadowRoot;
  Element element;
  Todo todo;
  Todos model;
  InputElement _input;
  InputElement _toggle;
  ButtonElement _destroy;
  LabelElement _label;
  DivElement _nonEditingView;

  factory TodoListElement(Todo todo) {
    var component =  manager.expandHtml('<li is="x-todo-list-element"></li>');
      // TODO(samhop) make editing work
    component.todo = todo;
    component._render();
    return component;
  }

  TodoListElement.internal(this._shadowRoot, this.element);

  // set appropriate classes to view mode
  void _render() {
    _label.innerHTML = todo.value;
    _input.attributes['value'] = todo.value;
    _nonEditingView.classes = ['view'];
    _input.classes.remove('todo-edit-editing');
    _input.classes.add('todo-edit-static');
  }

  void complete() {
    _label.classes.add('completed');
    _toggle.checked = true;
  }

  void uncomplete() {
    _label.classes.remove('completed');
    _toggle.checked = false;
  }

  void created() {
    _input = _shadowRoot.query('.todo-edit');
    _label = _shadowRoot.query('label');
    _toggle = _shadowRoot.query('.todo-toggle');
    _nonEditingView = _shadowRoot.query('.view');
    _destroy = _shadowRoot.query('.todo-destroy');
  }

  void inserted() {
    _nonEditingView.on.doubleClick.add((event) {
      _nonEditingView.classes = ['editing'];
      _input.classes.remove('todo-edit-static');
      _input.classes.add('todo-edit-editing');
      _input.select();
    });
    _destroy.on.click.add((event) {
      model.removeTodo(todo);
    });
    _toggle.on.change.add((event) {
      if (_toggle.checked) {
        model.complete(todo);
      } else {
        model.uncomplete(todo);
      }
    });
    // TODO(samhop): This event listener should only be attached when the todo
    // is in editing mode. 
    _input.on.keyPress.add((event) {
      if (event.keyCode == ENTER_KEY) { _saveTodo(); }
    });
    _input.on.blur.add((event) => _saveTodo());
    // TODO(samhop): These listeners should be detached when the component is
    // removed; otherwise presumably it won't get gc'd. 
    model.on.completed.add((todo) {
      if (todo == this.todo) {
        complete();
      }
    });
    model.on.uncompleted.add((todo) {
      if (todo == this.todo) {
        uncomplete();
      }
    });
    model.on.removed.add((todo) {
      if (todo == this.todo) {
        element.remove();
      }
    });
  }

  void _saveTodo() {
    var trimmedText = _input.value.trim();
    if (trimmedText != '') {
      todo.value = trimmedText;
      _render();
    } else {
      model.removeTodo(todo);
    }
  }

  void attributeChanged(String name, String oldValue, String newValue) { }

  // We don't bother removing event listeners, since any todo that gets
  // removed should get gc'd.
  void removed() { }
}

/** Component representing a todo list app. */
class TodoList implements WebComponent {
  ShadowRoot _shadowRoot;
  Element element;
  Todos _model;
  // dart:html has no FooterElement
  Element _footer;
  ButtonElement _clearCompleted;

  factory TodoList() {
    return manager.expandHtml('<div is="x-todo-list"></div>');
  }

  TodoList.internal(this._shadowRoot, this.element);

  void addTodo(Todo todo) {
    var todoComponent = new TodoListElement(todo);
    todoComponent.model = model;
    _shadowRoot.query('#todo-list').nodes.add(todoComponent.element);
  }

  void set model(Todos model) {
    this._model = model;
    model.on.added.addAll([addTodo, updateCount]);
    model.on.removed.add(updateCount);
    model.on.completed.add(updateCount);
    model.on.uncompleted.add(updateCount);
  }

  // We take a Todo argument so that updateCount can be a TodoCallback
  void updateCount(Todo todo) {
    // TODO(samhop): Not clear how to make this less hacky, since the
    // component doesn't have a lifecycle event for "model is hooked up"
    if (model == null || model._todos.length == 0) {
      _footer.classes.add('no-todos');
    } else {
      var uncomplete = model.remaining;
      _footer.classes.remove('no-todos');
      if (uncomplete == 1) {
        _footer.query('#todo-count').innerHTML =
            '<strong>1</strong> item left';
      } else {
        _footer.query('#todo-count').innerHTML =
            '<strong>$uncomplete</strong> items left';
      }
      _clearCompleted.innerHTML =
           'Clear completed '
           '(${model._todos.length - model.remaining})';
    }
  }

  Todos get model => _model;

  void created() {
    _footer = _shadowRoot.query('#footer');
    _clearCompleted = _shadowRoot.query('#clear-completed');
  }

  void inserted() {
    updateCount(null);
    var toggleAll = _shadowRoot.query('#toggle-all');
    toggleAll.on.change.add((event) {
      if (toggleAll.checked) {
        model.completeAll();
      } else {
        model.uncompleteAll();
      }
    });
    _shadowRoot.query('#clear-completed').on.click.add((event) {
      model.clearCompleted();
    });
  }

  // TODO(samhop): Expose current todo list as a data attribute of the TodoList
  // component.
  void attributeChanged(String name, String oldValue, String newValue) { }

  void removed() { }
}

/** Component representing a todo input bar */
class NewTodo implements WebComponent {
  ShadowRoot _shadowRoot;
  Element element;
  Todos model;

  factory NewTodo() {
    return manager.expandHtml('<div is="x-new-todo"></div>');
  }

  NewTodo.internal(this._shadowRoot, this.element);

  void created() { }
  void inserted() {
    element.on.keyPress.add((event) {
      if (event.keyCode == ENTER_KEY) {
        var input = _shadowRoot.query('#new-todo');
        var trimmedStr = input.value.trim();
        if (trimmedStr != '') {
          model.addTodo(new Todo(trimmedStr));
          input.value = '';
        }
      }
    });
  }
  void attributeChanged(String name, String oldValue, String newValue) { }
  void removed() { }
}
