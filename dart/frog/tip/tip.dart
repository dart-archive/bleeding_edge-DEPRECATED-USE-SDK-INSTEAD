// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * This giant file has pieces for compiling dart in the browser, injecting
 * scripts into pages and a bunch of classes to build a simple dart editor.
 *
 * TODO(jimhug): Separate these pieces cleanly.
 */

#import('dart:dom');
#import('../lang.dart');
#import('../file_system_dom.dart');


void main() {
  final systemPath = getRootPath(window) + '/..';
  final userPath = getRootPath(window.parent);

  if (window !== window.parent) {
    // I'm in an iframe - frogify my surrounding code unless dart is native.
    if (!document.implementation.hasFeature('dart', '')) {
      // Suppress warnings to avoid diff-based tests from failing on them.
      initialize(systemPath, userPath, ['--suppress_warnings']);
      window.addEventListener('DOMContentLoaded',
                              (e) => frogify(window.parent),
                              false);
    }
  } else {
    // I'm at the top level - run the tip shell.
    shell = new Shell();
    document.body.appendChild(shell._node);
    initialize(systemPath, userPath);
  }
}


/** The filename to use for Dart code compiled directly from the browser. */
final String DART_FILENAME = 'input.dart';

/** Helper to get my base url. */
String getRootPath(Window window) {
  String url = window.location.href;
  final tail = url.lastIndexOf('/', url.length);
  final dir = url.substring(0, tail);
  return dir;
}


/**
 * Invoke this script in the given window's context.  Use
 * this frame's window by default.
 */
void inject(String code, Window win, [String name = 'generated.js']) {
  final doc = win.document;
  var script = doc.createElement('script');
  // TODO(vsm): Enable debugging of injected code.  This sourceURL
  // trick only appears to work for eval'ed code, not script injected
  // code.
  // Append sourceURL to enable debugging.
  script.innerHTML = code + '\n//@ sourceURL=$name';
  script.type = 'application/javascript';
  doc.body.appendChild(script);
}


/**
 * Compile all dart scripts in a window and inject/invoke the corresponding JS.
 */
void frogify(Window win) {
  final doc = win.document;
  int n = doc.scripts.length;
  // TODO(vsm): Implement foreach iteration on native DOM types.  This
  // should be for (var script in doc.scripts) { ... }.
  for (int i = 0; i < n; ++i) {
    final script = doc.scripts[i];
    if (script.type == 'application/dart') {
      final src = script.src;
      var input;
      var name;
      if (src == '') {
        input = script.innerHTML;
        name = null;
      } else {
        input = world.files.readAll(src);
        name = '$src.js';
      }
      world.files.writeString(DART_FILENAME, input);
      world.reset();
      var success = world.compile();

      if (success) {
        inject(world.getGeneratedCode(), win, name);
      } else {
        inject('window.alert("compilation failed");', win, name);
      }
    }
  }
}

void initialize(String systemPath, String userPath,
    [List<String> flags = const []]) {
  DomFileSystem fs = new DomFileSystem(userPath);
  // TODO(jimhug): Workaround lib path hack in frog_options.dart
  final options = [null, null, '--libdir=$systemPath/lib'];
  options.addAll(flags);
  options.add(DART_FILENAME);
  parseOptions(systemPath, options, fs);
  initializeWorld(fs);
}

final int LINE_HEIGHT = 22; // TODO(jimhug): This constant sucks.
final int CHAR_WIDTH = 8; // TODO(jimhug): See above.

final String CODE = '''#import("dart:dom");

// This is an interesting field;
final int y = 22;
String name;

/** This is my main method. */
void main() {
  var element = document.createElement('div');
  element.innerHTML = "Hello dom from Dart!";
  document.body.appendChild(element);

  HTMLCanvasElement canvas = document.createElement('canvas');
  document.body.appendChild(canvas);

  var context = canvas.getContext('2d');
  context.setFillColor('purple');
  context.fillRect(10, 10, 30, 30);
}

/**
 * The usual method of computing factorial in the slowest possible way.
 */
num fact(n) {
  if (n == 0) return 1;
  return n * fact(n - 1);
}

final x = 22;
 ''';

final String HCODE = '''#import("dart:html");

void main() {
  var element = document.createElement('div');
  element.innerHTML = "Hello html from Dart!";
  document.body.nodes.add(element);

  CanvasElement canvas = document.createElement('canvas');
  canvas.width = 100;
  canvas.height = 100;
  document.body.nodes.add(canvas);

  var context = canvas.getContext('2d');
  context.setFillColor('blue');
  context.fillRect(10, 10, 30, 30);
}
''';

var shell;

class Shell {
  var _textInputArea;
  KeyBindings _bindings;

  Cursor cursor;
  Editor _editor;

  var _node;
  var _output;

  var _repl;
  var _errors;

  Shell() {
    _node = document.createElement('div');
    _node.className = 'shell';
    _editor = new Editor(this);

    _editor._node.style.setProperty('height', '70%');
    _node.appendChild(_editor._node);

    _textInputArea = document.createElement('textarea');
    _textInputArea.className = 'hiddenTextArea';

    _node.appendChild(_textInputArea);

    var outDiv = document.createElement('div');
    outDiv.className = 'output';
    outDiv.style.setProperty('height', '49%');


    _output = document.createElement('iframe');
    outDiv.appendChild(_output);
    _node.appendChild(outDiv);

    _repl = document.createElement('div');
    _repl.className = 'repl';
    _repl.style.setProperty('height', '28%');
    _repl.innerHTML = '<h3>REPL Under Construction...</h3>';
    _node.appendChild(_repl);

    _errors = document.createElement('div');
    _errors.className = 'errors';
    _errors.innerHTML = '<h3>Errors/Warnings Under Construction...</h3>';
    _errors.style.setProperty('height', '49%');

    _node.appendChild(_errors);

    // TODO(jimhug): Ick!
    window.setTimeout( () {
      _editor.focus();
      _output.contentDocument.head.innerHTML = '''
        <style>body {
          font-family: arial , sans-serif;
        }
        h3 {
          text-align: center;
        }
        </style>''';
      _output.contentDocument.body.innerHTML = '<h3>Output will go here</h3>';
    }, .5);

    // TODO(jimhug): These are hugely incomplete and Mac-centric.
    var bindings = {
      'Left': () {
        cursor.clearSelection();
        cursor._pos = cursor._pos.moveColumn(-1);
      },
      'Shift-Left': () {
        if (cursor._toPos == null) {
          cursor._toPos = cursor._pos;
        }
        cursor._pos = cursor._pos.moveColumn(-1);
      },

      'Right': () {
        cursor.clearSelection();
        cursor._pos = cursor._pos.moveColumn(+1);
      },
      'Shift-Right': () {
        if (cursor._toPos == null) {
          cursor._toPos = cursor._pos;
        }
        cursor._pos = cursor._pos.moveColumn(+1);
      },
      'Up': () {
        cursor.clearSelection();
        // TODO(jimhug): up and down lose column info on shorter lines.
        cursor._pos = cursor._pos.moveLine(-1);
      },
      'Shift-Up': () {
        if (cursor._toPos == null) {
          cursor._toPos = cursor._pos;
        }
        cursor._pos = cursor._pos.moveLine(-1);
      },
      'Down': () {
        cursor.clearSelection();
        cursor._pos = cursor._pos.moveLine(+1);
      },
      'Shift-Down': () {
        if (cursor._toPos == null) {
          cursor._toPos = cursor._pos;
        }
        cursor._pos = cursor._pos.moveLine(+1);
      },

      'Meta-Up': () {
        cursor.clearSelection();
        cursor._pos = _editor._code.start;
      },
      'Meta-Shift-Up': () {
        if (cursor._toPos == null) {
          cursor._toPos = cursor._pos;
        }
        cursor._pos = _editor._code.start;
      },

      'Meta-Down': () {
        cursor.clearSelection();
        cursor._pos = _editor._code.end;
      },
      'Meta-Shift-Down': () {
        if (cursor._toPos == null) {
          cursor._toPos = cursor._pos;
        }
        cursor._pos = _editor._code.end;
      },

      'Delete': () {
        //TODO(jimhug): go back to beginning of line when appropriate.
        if (cursor._toPos == null) {
          cursor._toPos = cursor._pos.moveColumn(-1);
        }
        cursor.deleteSelection();
      },
      'Control-D': () {
        if (cursor._toPos == null) {
          cursor._toPos = cursor._pos.moveColumn(+1);
        }
        cursor.deleteSelection();
      },

      'Meta-A': () {
        cursor._pos = _editor._code.start;
        cursor._toPos = _editor._code.end;
      },

      '.' : () {
        // TODO(jimhug): complete hints here
        cursor.write('.');
      },

      '}' : () {
        // TODO(jimhug): Get indentation right.
        cursor.write('}');
      },


      'Enter': () {
        cursor.write('\n');
        // TODO(jimhug): Indent on the new line appropriately...
      },

      'Tab': () {
        // TODO(jimhug): force tab to always "properly" indent the line
        cursor.write('  ');
      },

      'Space': () => cursor.write(' '),
      // This seems to be a common typo, so just allow it.
      'Shift-Space': () => cursor.write(' '),

      'Meta-G': () => cursor.write(CODE),
      'Meta-H': () => cursor.write(HCODE),

      'Meta-P': () => cursor._pos.block.parse(),

      'Shift-Enter': run,
      'Meta-Enter': run,
    };

    _bindings = new KeyBindings(_textInputArea, bindings,
        (String text) { cursor.write(text); },
        (String key) {
          // Use native bindings for cut and paste
          if (key == 'Meta-V') return false;
          if (key == 'Control-V') return false;

          // TODO(jimhug): No good, very bad hack.
          if (key == 'Meta-X' || key == 'Control-X') {
            window.setTimeout(() {
              cursor.deleteSelection();
              _editor._redraw();
            }, 0);
            return false;
          }

          if (key == 'Meta-C') return false;
          if (key == 'Control-C') return false;

          // Use native bindings for dev tools
          if (key == 'Alt-Meta-I') return false;
          if (key == 'Control-Shift-I') return false;
          window.console.log('Unbound key "$key"');
          return true;
        });
  }

  void run() {
    _output.contentDocument.body.innerHTML =
      '<h3 style="color:green">Compiling...</h3>';

    window.setTimeout( () {
      final sw = new Stopwatch();
      sw.start();
      var code = _editor.getCode();
      world.files.writeString(DART_FILENAME, code);
      world.reset();
      var success = world.compile();
      sw.stop();

      if (success) {
        _output.contentDocument.body.innerHTML = '';
        inject(world.getGeneratedCode(), _output.contentWindow);
      } else {
        _output.contentDocument.body.innerHTML =
          '<h3 style="color:red">Compilation failed</h3>';
      }

      print('compiled in ${sw.elapsedInMs()}msec');
    }, 0);
  }

  void focusKeys(Editor editor) {
    _editor = editor;
    cursor = editor._cursor;
    _textInputArea.focus();
  }
}


class Editor {
  Shell _shell;
  Cursor _cursor;
  CodeBlock _code;
  var _node;

  // Some temp state for mouse down and selections
  bool _isSelecting;
  int _lastClickTime;
  bool _didDoubleClick;

  Editor(this._shell) {
    _node = document.createElement('div');
    _node.className = 'editor';

    _code = new CodeBlock(null, CODE, 0, null);
    _code.top = 0;
    _node.appendChild(_code._node);

    _cursor = new Cursor(_code.start);
    _node.appendChild(_cursor._node);

    _node.addEventListener('mousedown', mousedown, false);
    _node.addEventListener('mousemove', mousemove, false);
    _node.addEventListener('mouseup', mouseup, false);

    // TODO(jimhug): Final bit to make region selection clean.
  	//this.node.addEventListener('mouseout', this, false);

    // TODO(jimhug): Lazy rendering should be triggered by this.
    //_node.addEventListener('scroll', this, false);
  }

  String getCode() {
    return _code.text;
  }

  void goto(int line, int column) {
    _cursor._pos = _code.getPosition(line, column);
  }

  void mousedown(MouseEvent e) {
    // for shift click, create selection region
    if (e.shiftKey) {
      _cursor._toPos = _cursor._pos;
    } else {
      _cursor.clearSelection();
    }
    _cursor._pos = _code.positionFromMouse(e);
    focus();
    e.preventDefault();

    _isSelecting = true;
  }

  void mousemove(MouseEvent e) {
    // TODO(jimhug): Would REALLY like to check that button is down here!
    if (_isSelecting) {
      if (_cursor._toPos == null) {
        _cursor._toPos = _cursor._pos;
      }
      _cursor._pos = _code.positionFromMouse(e);
      e.preventDefault();
      _redraw();
    }
  }

  void mouseup(MouseEvent e) {
    _isSelecting = false;
    if (_cursor.emptySelection) {
      _cursor.clearSelection();
    }
  }

  void _redraw() {
    _code._redraw();
    _cursor._redraw();
  }

  void focus() {
    _shell.focusKeys(this);
    _cursor._visible = true;
    _redraw();
  }

  void blur() {
    _shell.blurKeys(this);
    _cursor._visible = false;
    _redraw();
  }
}

class Point {
  final int x, y;
  const Point(this.x, this.y);
}

class LineColumn {
  final int line, column;
  LineColumn(this.line, this.column);
}

class Cursor {
  CodePosition _pos;

  CodePosition _toPos;
  bool _visible = true;
  var _node;

  var _cursorNode;
  var _selectionNode;

  Cursor(this._pos) {
    _node = document.createElement('div');
    _node.className = 'cursorDiv';
  }

  bool get emptySelection() {
    return _toPos == null ||
      (_toPos.block == _pos.block && _toPos.offset == _pos.offset);
  }

  _redraw() {
    // Approach is to kill and recreate everything on a redraw.
    // There are lots of potential improvements if this proves costly.
    // However: If we don't do this we need a different dance to make cursor
    // blinking disabled when it is moving.

    _node.innerHTML = '';
    if (!_visible) return;

    _cursorNode = document.createElement('div');
    _cursorNode.className = 'cursor blink';
    _cursorNode.style.setProperty('height', '${LINE_HEIGHT}px');

    var p = _pos.getPoint();
    _cursorNode.style.setProperty('left', '${p.x}px');
    _cursorNode.style.setProperty('top', '${p.y}px');
    _node.appendChild(_cursorNode);

    if (_toPos == null) return;
    //_toPos = _pos.moveColumn(-20);

    // First version assumes same line...
    void addDiv(top, left, height, width) {
      var child = document.createElement('div');
      child.className = 'selection';
      child.style.setProperty('left', '${left}px');
      child.style.setProperty('top', '${top}px');

      child.style.setProperty('height', '${height}px');
      if (width == null) {
        child.style.setProperty('right', '0px');
      } else {
        child.style.setProperty('width', '${width}px');
      }
      _node.appendChild(child);
    }

    var toP = _toPos.getPoint();
    // Same line - only one line to highlight
    if (toP.y == p.y) {
      if (toP.x < p.x) {
        addDiv(p.y, toP.x, LINE_HEIGHT, p.x - toP.x);
      } else {
        addDiv(p.y, p.x, LINE_HEIGHT, toP.x - p.x);
      }
    } else {
      if (toP.y < p.y) {
        var tmp = toP; toP = p; p = tmp;
      }
      addDiv(p.y, p.x, LINE_HEIGHT, null);
      if (toP.y > p.y + LINE_HEIGHT) {
        addDiv(p.y + LINE_HEIGHT, 0, toP.y - p.y - LINE_HEIGHT, null);
      }
      addDiv(toP.y, 0, LINE_HEIGHT, toP.x);
    }

    // TODO(jimhug): separate out - this makes default copy/cut work
    var i0 = _pos.offset;
    var i1 = _toPos.offset;
    if (i1 < i0) {
      var tmp = i1; i1 = i0; i0 = tmp;
    }
    var text = _pos.block.text.substring(i0, i1);
    shell._textInputArea.value = text;
    shell._textInputArea.select();
  }

  void clearSelection() {
    _toPos = null;
  }

  void moveColumn(int delta) {
    _pos = _pos.moveColumn(delta);
  }

  void moveLine(int delta) {
    _pos = _pos.moveLine(delta);
  }

  void deleteSelection() {
    if (_toPos == null) return;

    assert(_toPos.block == _pos.block);
    if (_toPos.offset < _pos.offset) {
      _pos.block.delete(_toPos.offset, _pos.offset);
      _pos = _toPos;
    } else {
      _pos.block.delete(_pos.offset, _toPos.offset);
    }
    _toPos = null;
  }

  void write(String text) {
    // TODO(jimhug): combine insert and delete to optimize
    deleteSelection();
    _pos.block.insertText(_pos.offset, text);

    _pos = new CodePosition(_pos.block, _pos.offset + text.length);
  }
}

class CodePosition {
  final CodeBlock block;
  final int offset;

  CodePosition(this.block, this.offset);

  CodePosition moveLine(int delta) {
    if (delta == 0) return this;

    var lineCol = block.getLineColumn(offset);
    return block.getPosition(lineCol.line + delta, lineCol.column);
  }

  CodePosition moveColumn(int delta) {
    if (delta == 0) return this;

    var newBlock = block;
    var newOffset = offset + delta;

    while (newOffset < 0 && block.previous !== null) {
      newBlock = block.previous;
      newOffset += newBlock.text.length;
    }
    while (newOffset > newBlock.text.length && block.next !== null) {
      newOffset -= newBlock.text.length;
      newBlock = block.next;
    }

    if (newOffset < 0) newOffset = 0;
    if (newOffset > newBlock.text.length) newOffset = newBlock.text.length;

    return new CodePosition(newBlock, newOffset);
  }

  Point getPoint() {
    return block.offsetToPoint(offset);
  }
}

class CodeBlock {
  CodeBlock _parent;

  CodeBlock _previous;
  CodeBlock _next;

  // TODO(jimhug): Container vs. text blocks is still fuzzy.
  CodeBlock _firstChild;
  String _text;
  int _depth = 0;
  List<int> _lineStarts;

  bool _dirty = true;
  var _node;
  int _top, _height;

  CodeBlock(this._parent, this._text, this._depth, [String htmlText = null]) {
    _node = document.createElement('div');
    _node.className = 'code';
    if (htmlText != null) {
      _node.innerHTML = htmlText;
      _dirty = false;
    }
  }

  CodeBlock get firstChild() => _firstChild !== null ? _firstChild : this;

  CodeBlock get lastChild() {
    var ret = firstChild;
    if (ret == null) return this;
    // O(N) in blocks - could cache if this is perf bottleneck.
    while (ret._next != null) ret = ret._next;
    return ret.lastChild;
  }

  CodeBlock get previous() {
    if (_previous !== null) return _previous;
    if (_parent === null || _parent.previous === null) return null;

    return _parent.previous.lastChild;
  }

  CodeBlock get next() {
    if (_next != null) return _next;
    if (_parent === null || _parent.next === null) return null;

    return _parent.next.firstChild;
  }

  CodePosition get start() {
    return new CodePosition(this, 0);
  }

  CodePosition get end() {
    return new CodePosition(this, _text.length);
  }

  void parse() {
    final source = new SourceFile('fake.dart', text);
    var p = new Parser(source);
    var cu = p.compilationUnit();
  }

  void markDirty() {
    if (!_dirty) {
      _dirty = true;
      if (_parent != null) {
        _parent._dirty = true;
      }
    }
  }

  String get text() => _text;

  void set text(String newText) {
    _text = newText;
    markDirty();
  }

  int get top() => _top;

  void set top(int newTop) {
    if (newTop != _top) {
      _top = newTop;
      _node.style.setProperty('top', '${_top}px');
    }
  }

  int get height() => _height;

  void set height(int newHeight) {
    if (newHeight != _height) {
      _height = newHeight;
      _node.style.setProperty('height', '${_height}px');
    }
  }

  void insertText(int offset, String newText) {
    _text = _text.substring(0, offset) + newText + _text.substring(offset);
    markDirty();
  }

  void delete(int from, int to) {
    assert(from <= to);
    _text = _text.substring(0, from) + _text.substring(to);
    markDirty();
  }

  // Split redraw and classify???
  // Multi-threading - background workers????

  // Step #1 split top level into different blocks - and add some sort of
  // visual indicator to help make this more clear.

  CodeBlock _addNewBlock(CodeBlock lastChild, int start, Token token,
      List<int> lineStarts, String html) {
    var blockText = _text.substring(start, token.end);
    var newBlock = new CodeBlock(this, blockText, 1, html.toString());
    newBlock._lineStarts = lineStarts.getRange(0, lineStarts.length);
    newBlock.height = (lineStarts.length + 1) * LINE_HEIGHT;
    lineStarts.clear();

    if (lastChild == null) {
      newBlock.top = this.top;
      _firstChild = newBlock;
    } else {
      newBlock.top = lastChild.top + lastChild.height;
      lastChild._next = newBlock;
    }
    _node.appendChild(newBlock._node);
    return newBlock;
  }

  _redraw() {
    if (!_dirty) return;

    _dirty = false;

    if (_text != null && _text.length == 0) {
      _node.innerHTML = '<span> </span>';
      height = LINE_HEIGHT;
      return;
    }

    // container node
    if (_text == null) {
      // recursive on children
      var child = _firstChild;
      var childTop = top;
      _node.innerHTML = '';
      while (child != null) {
        child.top = childTop;
        child._redraw();
        childTop += child.height;
        _node.appendChild(child._node);
        child = child._next;
      }
      height = childTop - top;
      return;
    }

    // classify my text and create children as needed
    // TODO(jimhug): Shared tokenizer with proper source locations?
    final src = new SourceFile('fake.dart', _text);
    var html = new StringBuffer();
    var lineStarts = new List<int>();
    lineStarts.add(0);
    Tokenizer tokenizer = new Tokenizer(src, /*skipWhitespace:*/false);

    int depth = 0;
    int start = 0;
    int indentations = 0; // ???????????
    CodeBlock lastChild = null;

    void addLineStarts(Token token) {
      if (token.kind == TokenKind.COMMENT
          || token.kind == TokenKind.WHITESPACE
          || token.kind == TokenKind.STRING
          || token.kind == TokenKind.STRING_PART) {
        // TODO(jimhug): Should we just make the Tokenizer do this directly?
        final text = token.source.text;
        for (int index = token.start; index < token.end; index++) {
          if (text.charCodeAt(index) == 10/*'\n'*/) {
            lineStarts.add(index - start + 1);
          }
        }
      }
    }

    // TODO(jimhug): Add whitespace blocks?
    while (true) {
      var token = tokenizer.next();
      addLineStarts(token);

      if (token.kind == TokenKind.END_OF_FILE) {
        if (false && _depth == 0) { // TODO(jimhug)
          lastChild = _addNewBlock(lastChild, start, token, lineStarts, html.toString());
          // close this last - possibly incomplete block
          _text = null;
          height = top - (lastChild.top + lastChild.height);
        } else {
          _node.innerHTML = html.toString();
          _lineStarts = lineStarts;
          height = _lineStarts.length * LINE_HEIGHT;
        }
        return;
      }

      if (token.kind == TokenKind.COMMENT) {
        // TODO(jimhug): These may be the most fun blocks to handle...
        // if (depth == 0) - try to make this into its own block
        // and then go wild with markdown -> html.
      }

      final kind = classify(token);
      final stringClass = ''; // TODO!!!
      final text = htmlEscape(token.text);
      if (kind != null) {
        html.add('<span class="$kind $stringClass">$text</span>');
      } else {
        html.add('<span>$text</span>');
      }

      // initially, only one deep
      if (_depth == 0) {
        if (token.kind == TokenKind.CLASS || token.kind == TokenKind.INTERFACE) {
          // Do nothing special for now
        } else if (token.kind == TokenKind.LBRACE) {
          depth += 1;
        } else if (token.kind == TokenKind.SEMICOLON || token.kind == TokenKind.RBRACE) {
          if (token.kind == TokenKind.RBRACE) depth -= 1;
          // TODO(jimhug)
          if (false && depth == 0) {
            // This token must be the last one on its line or at end of file
            token = tokenizer.next();
            if (token.kind == TokenKind.END_OF_FILE) {
            } else if (token.kind == TokenKind.WHITESPACE) {
              // Validate newline!
              //addLineStarts(token);
              //html.add('<span>${htmlEscape(token.text)}</span>');
            } else {
              // ??? what to do???
              continue;
            }
            lastChild = _addNewBlock(lastChild, start, token, lineStarts, html.toString());
            start = token.end;
            lineStarts.clear();
            html = new StringBuffer(); // clear?
          }
        }
      }
    }
  }

  CodePosition positionFromMouse(MouseEvent p) {
    var box = _node.getBoundingClientRect();
    int y = p.clientY - box.top;
    int x = p.clientX - box.left;

    return positionFromPoint(x, y);
  }

  CodePosition positionFromPoint(int x, int y) {
    if (_firstChild != null) {
      CodeBlock child = _firstChild;
      while (child != null) {
        if (y < child.top + child.height) {
          return child.positionFromPoint(x, y - child.top);
        }
      }
      return child.positionFromPoint(x, y - child.top);
    }

    return getPosition((y / LINE_HEIGHT).floor(), (x / CHAR_WIDTH).round());
  }

  CodePosition getPosition(int line, int column) {
    if (_firstChild != null) {
      CodeBlock child = _firstChild;
      int topLine = 0;
      while (child != null) {
        int lines = child._lineStarts.length;
        if (line < topLine + lines) {
          return child.getPosition(line - topLine, column);
        }
        topLine += lines;
        child = child._next;
      }
      return child.getPosition(line, column);
    }

    line = Math.min(Math.max(0, line), _lineStarts.length - 1);
    int maxOffset;
    if (line < _lineStarts.length - 1) {
      maxOffset = _lineStarts[line + 1] - 1;
    } else {
      maxOffset = _text.length;
    }

    final offset = Math.min(_lineStarts[line] + column, maxOffset);

    return new CodePosition(this, offset);
  }

  // These are local line/column to this block
  LineColumn getLineColumn(int offset) {
    if (_firstChild != null) {
      throw "aaaaaaaaaaaa!";
      // somehow iterate through children in a rational way...
      //return _firstChild.getLineColumn(offset);
    }

    // TODO(jimhug): Binary search would be faster but more complicated.
    int previousStart = 0;
    int line = 1;
    for (; line < _lineStarts.length; line++) {
      int start = _lineStarts[line];
      if (start > offset) {
        break;
      }
      previousStart = start;
    }
    return new LineColumn(line - 1, offset - previousStart);
  }

  Point offsetToPoint(int offset) {
    LineColumn lc = null;

    if (_firstChild != null) {
      // walk through my children to find offset
      int start = 0;
      var child = _firstChild;
      while (child != null) {
        if (offset < start + child.text.length) {
          lc = child.getLineColumn(offset - start);
          break;
        }
        start = start + child.text.length;
        child = child._next;
      }
    } else {
      lc = getLineColumn(offset);
    }
    return new Point(lc.column * CHAR_WIDTH, top + (lc.line * LINE_HEIGHT));
  }
}


class KeyBindings {
  static final Map _remap = const {
    'U+001B':'Esc', 'U+0008':'Delete', 'U+0009':'Tab', 'U+0020':'Space',
    'Shift':'',  'Control':'', 'Alt':'', 'Meta':''
  };

  static String _getModifiers(event) {
    String ret = '';
  	if (event.ctrlKey) { ret += 'Control-'; }
  	if (event.altKey) { ret += 'Alt-'; }
  	if (event.metaKey) { ret += 'Meta-'; }
  	if (event.shiftKey) { ret += 'Shift-'; }
  	return ret;
  }

  // TODO(jimhug): Move this to base <= 36 and into shared code.
  static int _hexDigit(int c) {
    if(c >= 48/*0*/ && c <= 57/*9*/) {
      return c - 48;
    } else if (c >= 97/*a*/ && c <= 102/*f*/) {
      return c - 87;
    } else if (c >= 65/*A*/ && c <= 70/*F*/) {
      return c - 55;
    } else {
      return -1;
    }
  }

  static int parseHex(String hex) {
    var result = 0;

    for (int i=0; i < hex.length; i++) {
      var digit = _hexDigit(hex.charCodeAt(i));
      assert(digit != -1);
      result = (result << 4) + digit;
    }

    return result;
  }

  static String translate(event) {
    var ret = _remap[event.keyIdentifier];
    if (ret === null) ret = event.keyIdentifier;

    if (ret == '') {
      return null;
    } else if (ret.startsWith('U+')) {
      // This method only reports "non-text" key presses
      if (event.ctrlKey || event.altKey || event.metaKey) {
  			return _getModifiers(event) +
  			  new String.fromCharCodes([parseHex(ret.substring(2, ret.length))]);
  		} else {
  			return null;
  		}
    } else {
      return _getModifiers(event) + ret;
    }
  }

  var node;
  Map bindings;
  var handleText, handleUnknown;

  KeyBindings(this.node, this.bindings, this.handleText, this.handleUnknown) {
    node.addEventListener('textInput', onTextInput, false);
    node.addEventListener('keydown', onKeydown, false);
  }

  onTextInput(event) {
    var text = event.data;
    var ret;
    if (bindings[text] !== null) {
      ret = bindings[text]();
    } else {
      ret = handleText(text);
    }
    // TODO(jimhug): Unfortunate coupling to shell.
    shell._editor._redraw();
    return ret;
  }

  onKeydown(event) {
    final key = translate(event);
    if (key !== null) {
      if (bindings[key] !== null) {
				bindings[key]();
				event.preventDefault();
			} else {
				if (handleUnknown(key)) {
				  event.preventDefault();
				} else {
				  event.stopPropagation();
				}
			}
    } else {
      event.stopPropagation();
    }
    // TODO(jimhug): Unfortunate coupling to shell.
    shell._editor._redraw();
    return false;
  }
}



// TODO(jimhug): Copy, paste and then modified from dartdoc
/**
 * Kinds of tokens that we care to highlight differently. The values of the
 * fields here will be used as CSS class names for the generated spans.
 */
class Classification {
  static final NONE = null;
  static final ERROR = "e";
  static final COMMENT = "c";
  static final IDENTIFIER = "i";
  static final KEYWORD = "k";
  static final OPERATOR = "o";
  static final STRING = "s";
  static final NUMBER = "n";
  static final PUNCTUATION = "p";

  // A few things that are nice to make different:
  static final TYPE_IDENTIFIER = "t";

  // Between a keyword and an identifier
  static final SPECIAL_IDENTIFIER = "r";

  static final ARROW_OPERATOR = "a";

  static final STRING_INTERPOLATION = 'si';
}

// TODO(rnystrom): should exist in standard lib somewhere
String htmlEscape(String text) {
  return text.replaceAll('&', '&amp;').replaceAll(
      '>', '&gt;').replaceAll('<', '&lt;');
}

bool _looksLikeType(String name) {
  // If the name looks like an UppercaseName, assume it's a type.
  return _looksLikePublicType(name) || _looksLikePrivateType(name);
}

bool _looksLikePublicType(String name) {
  // If the name looks like an UppercaseName, assume it's a type.
  return name.length >= 2 && isUpper(name[0]) && isLower(name[1]);
}

bool _looksLikePrivateType(String name) {
  // If the name looks like an _UppercaseName, assume it's a type.
  return (name.length >= 3 && name[0] == '_' && isUpper(name[1])
    && isLower(name[2]));
}

// These ensure that they don't return "true" if the string only has symbols.
bool isUpper(String s) => s.toLowerCase() != s;
bool isLower(String s) => s.toUpperCase() != s;

String classify(Token token) {
  switch (token.kind) {
    case TokenKind.ERROR:
      return Classification.ERROR;

    case TokenKind.IDENTIFIER:
      // Special case for names that look like types.
      if (_looksLikeType(token.text)
          || token.text == 'num'
          || token.text == 'bool'
          || token.text == 'int'
          || token.text == 'double') {
        return Classification.TYPE_IDENTIFIER;
      }
      return Classification.IDENTIFIER;

    // Even though it's a reserved word, let's try coloring it like a type.
    case TokenKind.VOID:
      return Classification.TYPE_IDENTIFIER;

    case TokenKind.THIS:
    case TokenKind.SUPER:
      return Classification.SPECIAL_IDENTIFIER;

    case TokenKind.STRING:
    case TokenKind.STRING_PART:
    case TokenKind.INCOMPLETE_STRING:
    case TokenKind.INCOMPLETE_MULTILINE_STRING_DQ:
    case TokenKind.INCOMPLETE_MULTILINE_STRING_SQ:
      return Classification.STRING;

    case TokenKind.INTEGER:
    case TokenKind.HEX_INTEGER:
    case TokenKind.DOUBLE:
      return Classification.NUMBER;

    case TokenKind.COMMENT:
    case TokenKind.INCOMPLETE_COMMENT:
      return Classification.COMMENT;

    // => is so awesome it is in a class of its own.
    case TokenKind.ARROW:
      return Classification.ARROW_OPERATOR;

    case TokenKind.HASHBANG:
    case TokenKind.LPAREN:
    case TokenKind.RPAREN:
    case TokenKind.LBRACK:
    case TokenKind.RBRACK:
    case TokenKind.LBRACE:
    case TokenKind.RBRACE:
    case TokenKind.COLON:
    case TokenKind.SEMICOLON:
    case TokenKind.COMMA:
    case TokenKind.DOT:
    case TokenKind.ELLIPSIS:
      return Classification.PUNCTUATION;

    case TokenKind.INCR:
    case TokenKind.DECR:
    case TokenKind.BIT_NOT:
    case TokenKind.NOT:
    case TokenKind.ASSIGN:
    case TokenKind.ASSIGN_OR:
    case TokenKind.ASSIGN_XOR:
    case TokenKind.ASSIGN_AND:
    case TokenKind.ASSIGN_SHL:
    case TokenKind.ASSIGN_SAR:
    case TokenKind.ASSIGN_SHR:
    case TokenKind.ASSIGN_ADD:
    case TokenKind.ASSIGN_SUB:
    case TokenKind.ASSIGN_MUL:
    case TokenKind.ASSIGN_DIV:
    case TokenKind.ASSIGN_TRUNCDIV:
    case TokenKind.ASSIGN_MOD:
    case TokenKind.CONDITIONAL:
    case TokenKind.OR:
    case TokenKind.AND:
    case TokenKind.BIT_OR:
    case TokenKind.BIT_XOR:
    case TokenKind.BIT_AND:
    case TokenKind.SHL:
    case TokenKind.SAR:
    case TokenKind.SHR:
    case TokenKind.ADD:
    case TokenKind.SUB:
    case TokenKind.MUL:
    case TokenKind.DIV:
    case TokenKind.TRUNCDIV:
    case TokenKind.MOD:
    case TokenKind.EQ:
    case TokenKind.NE:
    case TokenKind.EQ_STRICT:
    case TokenKind.NE_STRICT:
    case TokenKind.LT:
    case TokenKind.GT:
    case TokenKind.LTE:
    case TokenKind.GTE:
    case TokenKind.INDEX:
    case TokenKind.SETINDEX:
      return Classification.OPERATOR;

    // Color this like a keyword
    case TokenKind.HASH:

    case TokenKind.ABSTRACT:
    case TokenKind.ASSERT:
    case TokenKind.CLASS:
    case TokenKind.EXTENDS:
    case TokenKind.FACTORY:
    case TokenKind.GET:
    case TokenKind.IMPLEMENTS:
    case TokenKind.IMPORT:
    case TokenKind.INTERFACE:
    case TokenKind.LIBRARY:
    case TokenKind.NATIVE:
    case TokenKind.NEGATE:
    case TokenKind.OPERATOR:
    case TokenKind.SET:
    case TokenKind.SOURCE:
    case TokenKind.STATIC:
    case TokenKind.TYPEDEF:
    case TokenKind.BREAK:
    case TokenKind.CASE:
    case TokenKind.CATCH:
    case TokenKind.CONST:
    case TokenKind.CONTINUE:
    case TokenKind.DEFAULT:
    case TokenKind.DO:
    case TokenKind.ELSE:
    case TokenKind.FALSE:
    case TokenKind.FINALLY:
    case TokenKind.FOR:
    case TokenKind.IF:
    case TokenKind.IN:
    case TokenKind.IS:
    case TokenKind.NEW:
    case TokenKind.NULL:
    case TokenKind.RETURN:
    case TokenKind.SWITCH:
    case TokenKind.THROW:
    case TokenKind.TRUE:
    case TokenKind.TRY:
    case TokenKind.WHILE:
    case TokenKind.VAR:
    case TokenKind.FINAL:
      return Classification.KEYWORD;

    case TokenKind.WHITESPACE:
    case TokenKind.END_OF_FILE:
      return Classification.NONE;

    default:
      return Classification.NONE;
  }
}
