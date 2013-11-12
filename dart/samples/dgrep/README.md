## Dgrep: Searching Files for Content

A bare-bones implementation of the the Unix `grep` utility.

### Usage

Use from the command line in the `bin` directory in the following way:

    dart dgrep.dart [-rnS] patterns file_or_directory

### Examples

The `data` directory inside `bin` contains the starting lines of a popular
nursery rhyme ('one banana, two banana, three banana, four') spread over
several files and directories. Here are some examples of `dgrep` usage using
that data:

Search in the current directory (no match):

    $ dart dgrep.dart one .

Search recursively using `-r` (one match):

    $ dart dgrep.dart -r one .
    ./data/one/one.txt:one banana

Search recursively and print line numbers using `-rn` (three matches):

    $ dart dgrep.dart -rn banana .
    ./data/one/one.txt:1:one banana
    ./data/three_four/three/three.txt:1:three banana
    ./data/two/two.txt:1:two banana

Search in the packages directory without following symlinks (no match):

    $ dart dgrep.dart -rn "args.removeAt(0)" .


Search in the packages directory following symlinks using `-S` (eight matches):

    $ dart dgrep.dart -rnS "args.removeAt(0)" .
    ./packages/args/src/parser.dart:72:     args.removeAt(0);
    ./packages/args/src/parser.dart:81:     var commandName = args.removeAt(0);
    ./packages/args/src/parser.dart:100:    rest.add(args.removeAt(0));
    ./packages/args/src/parser.dart:134:    args.removeAt(0);
    ./packages/args/src/parser.dart:155:    args.removeAt(0);
    ./packages/args/src/parser.dart:206:    args.removeAt(0);
    ./packages/args/src/parser.dart:239:    args.removeAt(0);
    ./packages/args/src/parser.dart:262:    args.removeAt(0);


Please report any [bugs or feature requests](http://dartbug.com/new).
