// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
#import('../lang.dart');
#import('../file_system_node.dart');

/** Path to starting library or application. */
final libPath = '../client/samples/swarm/swarm.dart';

/** Path to corePath library. */
final corePath = 'lib';

/** Path to generate html files into. */
final outdir = './docs';

/** The file currently being written to. */
StringBuffer _file;

/**
 * The cached lookup-table to associate doc comments with spans. The outer map
 * is from filenames to doc comments in that file. The inner map maps from the
 * token positions to doc comments. Each position is the starting offset of the
 * next non-comment token *following* the doc comment. For example, the position
 * for this comment would be the position of the "Map" token below.
 */
Map<String, Map<int, String>> _comments;

// TODO(jimhug): This generates really ugly output with lots of holes.

/**
 * Run this from the frog/samples directory.  Before running, you need
 * to create a docs dir with 'mkdir docs' - since Dart currently doesn't
 * support creating new directories.
 */
void main() {
  // TODO(rnystrom): Get options and homedir like frog.dart does.
  final files = new NodeFileSystem();
  parseOptions('.', [] /* args */, files);

  initializeWorld(files);

  world.withTiming('parsed', () {
    world.processScript(libPath);
  });

  world.withTiming('resolved', () {
    world.resolveAll();
  });

  world.withTiming('generated docs', () {
    _comments = <String, Map<int, String>>{};

    for (var library in world.libraries.getValues()) {
      docLibrary(library);
    }

    docIndex(world.libraries.getValues());
  });
}

startFile() {
  _file = new StringBuffer();
}

write(String s) {
  _file.add(s);
}

writeln(String s) {
  write(s);
  write('\n');
}

endFile(String outfile) {
  world.files.writeString(outfile, _file.toString());
  _file = null;
}

/** Turns a library name into something that's safe to use as a file name. */
sanitize(String name) => name.replaceAll(':', '_').replaceAll('/', '_');

docIndex(List<Library> libraries) {
  startFile();
  writeln('<html><head><title>Index</title></head>');
  writeln('<body>');
  for (var library in libraries) {
    writeln('<a href="${sanitize(library.name)}.html">${library.name}</a>');
  }
  writeln('</body></html>');
  endFile('$outdir/index.html');
}

docLibrary(Library library) {
  startFile();
  writeln('<html><head><title>${library.name}</title></head>');
  writeln('<body>');
  writeln('<h1>Library ${library.name}</h1>');

  for (var type in library.types.getValues()) {
    // TODO(rnystrom): The unnamed type is the container of top-level
    // definitions. Should eventually include those in generated docs.
    if (type.name == null) continue;

    docType(type);
  }

  // TODO(rnystrom): Need to figure out where the doc comment for an entire
  // library goes. Before the #library()?

  writeln('</body></html>');
  endFile('$outdir/${sanitize(library.name)}.html');
}

docType(Type type) {
  write('<h2 id="${type.name}">${type.isClass ? "Class" : "Interface"} ');
  write('${type.name}</h2>');

  writeComment(type.span);

  // Show the superclass and superinterface(s).
  if (type.parent != null ||
      (type.interfaces != null && type.interfaces.length > 0)) {
    writeln('<h3>Inheritance</h3>');
    writeln('<p>');

    if (type.parent != null) {
      write('Extends ${typeRef(type.parent)}. ');
    }

    if (type.interfaces != null) {
      var interfaces = [];
      switch (type.interfaces.length) {
        case 0:
          // Do nothing.
          break;

        case 1:
          write('Implements ${typeRef(type.interfaces[0])}.');
          break;

        case 2:
          write('''Implements ${typeRef(type.interfaces[0])} and
              ${typeRef(type.interfaces[1])}.''');
          break;

        default:
          write('Implements ');
          for (var i = 0; i < type.interfaces.length; i++) {
            write('${typeRef(type.interfaces[i])}');
            if (i < type.interfaces.length - 1) {
              write(', ');
            } else {
              write(' and ');
            }
          }
          write('.');
          break;
        }
      }
    }

  writeln('<h3>Constructors</h3>');
  writeln('<dl>');
  for (var name in type.constructors.getKeys()) {
    var constructor = type.constructors[name];
    docMethod(constructor, namedConstructor: name);
  }
  writeln('</dl>');

  writeln('<h3>Members</h3>');
  writeln('<dl>');
  for (var member in orderValuesByKeys(type.members)) {
    if (member.isMethod &&
        (member.definition != null) &&
        !member.name.startsWith('_')) {
      docMethod(member);
    } else if (member.isProperty) {
      if (member.canGet) docMethod(member.getter);
      if (member.canSet) docMethod(member.setter);
    }
  }
  writeln('</dl>');

  writeln('<h3>Fields</h3>');
  writeln('<dl>');
  for (var member in type.members.getValues()) {
    if (!member.isField) continue;
    if (member.name.startsWith('_')) continue; // Skip privates
    docField(member);
  }
  writeln('</dl>');
}

docMethod(MethodMember method, [String namedConstructor = null]) {
  write('<dt><code>');

  if (method.isStatic) {
    write('static ');
  }

  if (namedConstructor == null) {
    write(optionalTypeRef(method.returnType));
  }

  // Translate specially-named methods: getters, setters, operators.
  var name = method.name;
  if (name.startsWith('get\$')) {
    // Getter.
    name = 'get ${name.substring(4)}';
  } else if (name.startsWith('set\$')) {
    // Setter.
    name = 'set ${name.substring(4)}';
  } else {
    // See if it's an operator.
    name = TokenKind.rawOperatorFromMethod(name);
    if (name == null) {
      name = method.name;
    } else {
      name = 'operator $name';
    }
  }

  write(name);

  // Named constructors.
  if (namedConstructor != null && namedConstructor != '') {
    write('.');
    write(namedConstructor);
  }

  write('(');
  var paramList = [];
  if (method.parameters == null) print(method.name);
  for (var p in method.parameters) {
    paramList.add('${optionalTypeRef(p.type)}${p.name}');
  }
  write(Strings.join(paramList, ", "));
  write(')');
  write('</code></dt><dd>');

  writeComment(method.span);
  writeln('</dd>');
}

docField(FieldMember field) {
  write('<dt><code>');
  if (field.isStatic) {
    write('static ');
  }

  if (field.isFinal) {
    write('final ');
  } else if (field.type.name == 'Dynamic') {
    write('var ');
  }

  write(optionalTypeRef(field.type));
  write('${field.name}');
  write('</code></dt><dd>');

  writeComment(field.span);
  writeln('</dd>');
}

typeRef(Type type) {
  if (type.library != null) {
    var library = sanitize(type.library.name);
    return '<a href="${library}.html#${type.name}">${type.name}</a>';
  } else {
    return type.name;
  }
}

/**
 * Creates a linked string for an optional type annotation. Returns an empty
 * string if the type is Dynamic.
 */
optionalTypeRef(Type type) {
  if (type.name == 'Dynamic') {
    return '';
  } else {
    return typeRef(type) + ' ';
  }
}

writeComment(SourceSpan span) {
  var comment = findComment(span);
  if (comment != null) {
    writeln('<p>$comment</p>');
  }
}

/** Finds the doc comment preceding the given source span, if there is one. */
findComment(SourceSpan span) {
  if (span == null) return null;

  // Get the doc comments for this file.
  var fileComments = _comments.putIfAbsent(span.file.filename,
    () => parseDocComments(span.file));

  return fileComments[span.start];
}

parseDocComments(SourceFile file) {
  var comments = <int, String>{};

  var tokenizer = new Tokenizer(file, false);
  var lastComment = null;
  while (true) {
    var token = tokenizer.next();
    if (token.kind == TokenKind.END_OF_FILE) break;

    if (token.kind == TokenKind.COMMENT) {
      var text = token.text;
      if (text.startsWith('/**')) {
        // Remember that we've encountered a doc comment.
        lastComment = stripComment(token.text);
      }
    } else {
      if (lastComment != null) {
        // We haven't attached the last doc comment to something yet, so stick
        // it to this token.
        comments[token.start] = lastComment;
        lastComment = null;
      }
    }
  }

  return comments;
}

/**
 * Pulls the raw text out of a doc comment (i.e. removes the comment
 * characters.
 */
// TODO(rnystrom): Should handle [name] and [:code:] in comments. Should also
// break empty lines into multiple paragraphs. Other formatting?
// See dart/compiler/java/com/google/dart/compiler/backend/doc for ideas.
// (/DartDocumentationVisitor.java#180)
stripComment(comment) {
  StringBuffer buf = new StringBuffer();

  for (var line in comment.split('\n')) {
    line = line.trim();
    if (line.startsWith('/**')) line = line.substring(3, line.length);
    if (line.endsWith('*/')) line = line.substring(0, line.length-2);
    line = line.trim();
    while (line.startsWith('*')) line = line.substring(1, line.length);
    line = line.trim();
    buf.add(line);
    buf.add(' ');
  }

  return buf.toString();
}