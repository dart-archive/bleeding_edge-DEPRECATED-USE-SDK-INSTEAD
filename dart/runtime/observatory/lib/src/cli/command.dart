// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of cli;

// Splits a line into a list of string args.
List<String> _splitLine(String line) {
  var args = line.split(' ').where((arg) {
      return arg != ' ' && arg != '';
    }).toList();
  return args;
}

// Concatenates the first 'count' args.
String _concatArgs(List<String> args, int count) {
  if (count == 0) {
    return '';
  }
  return '${args.sublist(0, count).join(" ")} ';
}

// Shared functionality for RootCommand and Command.
abstract class _CommandBase {
  _CommandBase(List<Command> children) {
    assert(children != null);
    _children.addAll(children);
    for (var child in _children) {
      child._parent = this;
    }
  }

  // A command may optionally have sub-commands.
  List<Command> _children = [];

  _CommandBase _parent;
  int get _depth => (_parent == null ? 0 : _parent._depth + 1);

  // Override in subclasses to provide command-specific argument completion.
  //
  // Given a list of arguments to this command, provide a list of
  // possible completions for those arguments.
  List<String> complete(List<String> args) => [];

  // Override in subclasses to provide command-specific execution.
  Future run(List<String> args);

  // Returns a list of local subcommands which match the args.
  List<Command> _matchLocal(String arg, bool preferExact) {
    var matches = new List<Command>();
    for (var child in _children) {
      if (child.name.startsWith(arg)) {
        if (preferExact && child.name == arg) {
          return [child];
        }
        matches.add(child);
      }
    }
    return matches;
  }

  // Returns the set of commands could be triggered by a list of
  // arguments.
  List<Command> _match(List<String> args, bool preferExact) {
    if (args.isEmpty) {
      return [];
    }    
    bool lastArg = (args.length == 1);
    var matches = _matchLocal(args[0], !lastArg || preferExact);
    if (matches.isEmpty) {
      return [];
    } else if (matches.length == 1) {
      var childMatches =  matches[0]._match(args.sublist(1), preferExact);
      if (childMatches.isEmpty) {
        return matches;
      } else {
        return childMatches;
      }
    } else {
      return matches;
    }
  }

  // Builds a list of completions for this command.
  List<String> _buildCompletions(List<String> args,
                                 bool addEmptyString) {
    var completions = complete(args.sublist(_depth, args.length));
    if (addEmptyString && completions.isEmpty && args[args.length - 1] == '') {
      // Special case allowance for an empty particle at the end of
      // the command.
      completions = [''];
    }
    var prefix = _concatArgs(args, _depth);
    return completions.map((str) => '${prefix}${str}').toList();
  }

}

// The root of a tree of commands.
class RootCommand extends _CommandBase {
  RootCommand(List<Command> children) : super(children);

  // Provides a list of possible completions for a line of text.
  List<String> completeCommand(String line) {
    var args = _splitLine(line);
    bool showAll = line.endsWith(' ') || args.isEmpty;
    if (showAll) {
      // Adding an empty string to the end causes us to match all
      // subcommands of the last command.
      args.add('');
    }
    var commands =  _match(args, false);
    if (commands.isEmpty) {
      // No matching commands.
      return [];
    }
    int matchLen = commands[0]._depth;
    if (matchLen < args.length) {
      // We were able to find a command which matches a prefix of the
      // args, but not the full list.
      if (commands.length == 1) {
        // The matching command is unique.  Attempt to provide local
        // argument completion from the command.
        return commands[0]._buildCompletions(args, true);
      } else {
        // An ambiguous prefix match leaves us nowhere.  The user is
        // typing a bunch of stuff that we don't know how to complete.
        return [];
      }
    }

    // We have found a set of commands which match all of the args.
    // Return the completions strings.
    var prefix = _concatArgs(args, args.length - 1);
    var completions =
        commands.map((command) => '${prefix}${command.name} ').toList();
    if (showAll && matchLen == args.length) {
      // If we are showing all possiblities, also include local
      // completions for the parent command.
      completions.addAll(commands[0]._parent._buildCompletions(args, false));
    }
    return completions;
  }

  // Runs a command.
  Future runCommand(String line) {
    var args = _splitLine(line);
    var commands =  _match(args, true);
    if (commands.isEmpty) {
      // TODO(turnidge): Add a proper exception class for this.
      return new Future.error('notfound');
    } else if (commands.length == 1) {
      return commands[0].run(args.sublist(commands[0]._depth));
    } else {
      // TODO(turnidge): Add a proper exception class for this.
      return new Future.error('ambiguous');
    }
  }

  Future run(List<String> args) {
    throw 'should-not-execute-the-root-command';
  }

  toString() => 'RootCommand';
}

// A node in the command tree.
abstract class Command extends _CommandBase {
  Command(this.name, List<Command> children) : super(children);

  final name;

  toString() => 'Command(${name})';
}
