## Dcat: Concatenating Files

A bare-bones implementation of the the Unix `cat` utility.

### Usage

Use from the command line in the `bin` directory in the following way:

    dart dcat.dart [-n] patterns [files]

`dcat` reads `files` sequentially, writing them to standard output. The
file operands are processed in command-line order.
If `files` is absent, `dcat` reads from the standard input until `EOF`.

Unlike the *nix `cat`, `dcat` does not support single dash ('-') arguments.

### Examples

The `data` directory inside `bin` contains two files and a directory. Here
are some examples of `dcat` usage using that data.

reading from the command line (no files provided as args):

    $ dart dcat.dart
    line1
    line1
    line2
    line2
    done typing
    done typing

a single file:

    $ dart dcat.dart data/foo
    foo1
    foo2
    foo3

multiple files with line numbers:

    $ dart dcat.dart -n data/foo data/bar
    1 foo1
    2 foo2
    3 foo3
    4
    1 bar1
    2 bar2
    3 bar3
    4

with a file that does not exist:

    $ dart dcat.dart -n data/foo data/nonexistent_file data/bar
    1 foo1
    2 foo2
    3 foo3
    4
    error: data/nonexistent_file not found
    1 bar1
    2 bar2
    3 bar3
    4

with a directory:

    $ dart dcat.dart data/empty_dir
    error: data/empty_dir is a directory


Please report any [bugs or feature requests](http://dartbug.com/new).
