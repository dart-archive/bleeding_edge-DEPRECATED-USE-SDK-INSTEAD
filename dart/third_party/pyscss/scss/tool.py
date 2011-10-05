""" Command-line tool to parse scss file.
"""
import optparse
import sys, os
import time

from os.path import abspath, dirname, join, normpath

COMMANDS = ['import', 'option', 'mixin', 'include', 'for', 'if', 'else']


def complete(text, state):
    """ Auto complete scss constructions
        in interactive mode.
    """
    for cmd in COMMANDS:
        if cmd.startswith(text):
            if not state:
                return cmd
            else:
                state -= 1

def main(argv=None):
    from scss import parser, VERSION

    try:
        # Upgrade shell in interactive mode
        import atexit
        import readline
        history = os.path.join(os.environ['HOME'], ".scss-history")
        atexit.register(readline.write_history_file, history)
        readline.parse_and_bind("tab: complete")
        readline.set_completer(complete)
        readline.read_history_file(history)
    except ( ImportError, IOError ):
        pass

    # Create options
    p = optparse.OptionParser(
        usage="%prog [OPTION]... [INFILE] [OUTFILE] [DARTCLASS]",
        version="%prog " + VERSION,
        epilog="SCSS compiler.",
        description="Compile INFILE or standard input, to OUTFILE or standard output, if --dart specified DARTCLASS is the dart file name and class name.")

    p.add_option(
        '-c', '--cache', action='store_true', dest='cache',
        help="Create and use cache file. Only for files.")

    p.add_option(
        '-i', '--interactive', action='store_true', dest='shell',
        help="Run in interactive shell mode.")

    p.add_option(
        '-m', '--compress', action='store_true', dest='compress',
        help="Compress css output.")

    p.add_option(
        '-d', '--dart', action='store_true', dest='dart',
        help="Pre-process css output css file and dart class file.")

    p.add_option(
        '-w', '--watch', dest='watch',
        help="""Watch files or directories for changes.
The location of the generated CSS can be set using a colon:
    scss -w input.scss:output.css
""")

    p.add_option(
        '-S', '--no-sorted', action='store_false', dest='sort',
        help="Do not sort declaration.")

    p.add_option(
        '-C', '--no-comments', action='store_false', dest='comments',
        help="Clear css comments.")

    p.add_option(
        '-W', '--no-warnings', action='store_false', dest='warn',
        help="Disable warnings.")

    opts, args = p.parse_args(argv or sys.argv[1:])
    precache = opts.cache

    # TODO(terry): Handle dart option in interactive shell.
    # Interactive mode
    if opts.shell:
        p = parser.Stylesheet()
        print 'SCSS v. %s interactive mode' % VERSION
        print '================================'
        print 'Ctrl+D or quit for exit'
        while True:
            try:
                s = raw_input('>>> ').strip()
                if s == 'quit':
                    raise EOFError
                print p.loads(s)
            except (EOFError, KeyboardInterrupt):
                print '\nBye bye.'
                break

        sys.exit()

    # Watch mode
    elif opts.watch:
        self, target = opts.watch.partition(':')
        files = []
        if not os.path.exists(self):
            print >> sys.stderr, "Path don't exist: %s" % self
            sys.exit(1)

        if os.path.isdir(self):
            for f in os.listdir(self):
                path = os.path.join(self, f)
                if os.path.isfile(path) and f.endswith('.scss'):
                    tpath = os.path.join(target or self, f[:-5] + '.css')
                    files.append([ path, tpath, 0 ])
        else:
            files.append([ self, target or self[:-5] + '.css', 0 ])

        s = parser.Stylesheet(
            options=dict(
                comments = opts.comments,
                compress = opts.compress,
                dart = opts.dart,
                warn = opts.warn,
                sort = opts.sort,
                cache = precache,
            ))

        def parse(f):
            infile, outfile, mtime = f
            ttime = os.path.getmtime(infile)
            if mtime < ttime:
                print " Parse '%s' to '%s' .. done" % ( infile, outfile )
                out = s.load(open(infile, 'r'))
                open(outfile, 'w').write(out)
                f[2] = os.path.getmtime(outfile)

        print 'SCSS v. %s watch mode' % VERSION
        print '================================'
        print 'Ctrl+C for exit\n'
        while True:
            try:
                for f in files:
                    parse(f)
                time.sleep(0.3)
            except OSError:
                pass
            except KeyboardInterrupt:
                print "\nSCSS stoped."
                break

        sys.exit()

    # Default compile files
    elif not args:
        infile = sys.stdin
        outfile = sys.stdout
        precache = False

    elif len(args) == 1:
        try:
            infile = open(args[0], 'r')
            outfile = sys.stdout
        except IOError, e:
            sys.stderr.write(str(e))
            sys.exit()

    elif len(args) == 2 or len(args) == 3:
        try:
            infile = open(os.path.abspath(args[0]), 'r')
            outfile = open(os.path.abspath(args[1]), 'w')
        except IOError, e:
            sys.stderr.write(str(e))
            sys.exit()
    else:
        p.print_help(sys.stdout)
        sys.exit()

    try:
        s = parser.Stylesheet(
            options=dict(
                comments = opts.comments,
                compress = opts.compress,
                dart = opts.dart,
                warn = opts.warn,
                sort = opts.sort,
                cache = precache,
            ))
        if (opts.dart and len(args) == 3):
          try:
              dartClass = args[2]
              dartfn = os.path.abspath('%s.dart' % dartClass)
              dartfile = open(dartfn, 'w')
          except IOError, e:
              sys.stderr.write(str(e))
              sys.exit()

          # Parse the scss file.
          nodes = s.loadReturnNodes(infile)

          # Add the main CSS file to list of files pre-processed.
          s.addInclude(args[0], nodes)

          # Write out CSS file.
          print 'Generating CSS file %s' % os.path.abspath(args[1])

          cssIncludes = []
          # Output all includes first.
          for include in s.scssIncludes:
            cssIncludes.append(
                '/* ---------- Included %s file ---------- */\n\n' % include[0])
            cssIncludes.append(''.join(map(str, include[1])))

          outfile.write('/* File generated by SCSS from source %s\n' % args[0])
          outfile.write(' * Do not edit.\n')

          outfile.write(' */\n\n%s' % ''.join(cssIncludes))

          #Write out dart class file.
          dartfile.write(s.dartClass(args[0], dartClass, s.scssIncludes))
          print 'Generating Dart Class %s' % dartfn
        else:
          outfile.write(s.load(infile))
    except ValueError, e:
        raise SystemExit(e)


if __name__ == '__main__':
    # Setup PYTHONPATH when starting tool (from main) to needed modules.
    TOOL_PATH = dirname(dirname(abspath(__file__)))
    THIRDPARTY_PATH = normpath('{0}/..'.format(TOOL_PATH))
    sys.path.append(join(THIRDPARTY_PATH, 'pyparsing/src'))
    sys.path.append(TOOL_PATH)

    main()
