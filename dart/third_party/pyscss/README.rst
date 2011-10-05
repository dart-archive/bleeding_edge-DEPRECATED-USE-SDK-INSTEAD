Python-scss
===========

Python-scss is SCSS_ compiler for python. Documentation available at pypi_ or github_.
This is part of zeta-library_.

.. contents::


Features
========
Python-scss has most of the funcitonality in Sass SCSS_ 3.2 and more. It supports:

* **Nested rules**
* **Keyword arguments**
* **Mixins**: ``@mixin``, ``@include``
* **Functions**: ``@function``, ``@return``
* **Inheritance**: ``@extend``
* **Conditions**: ``@if``, ``@else``, ``@if else``
* **Loops**: ``@for``
* **Variables**: ``$``, ``@variables``, ``@vars``
* **Images**: ``image-url``, ``image-width``, ``image-height``, ..
* **Embedded inline images**: ``inline-image``
* **Colors handling**: ``adjust-color()``, ``scale-color()``, ``opacify()``/``transparentize()``, ``lighten()``/``darken()``, ``mix()``, ...
* **Math functions**: ``sin()``, ``cos()``, ``tan()``, ``round()``, ``ceil()``, ``floor()``, ``pi()``, ...
* **Options**: ``@option compress:true, sort:false;``, ``@option comments:false;``
* **Compass_ helpers**: ``enumerate``, ``type-of``, ...
* **Sorting declarations**: in `Zen sorting order`_

.. note::
   For ``@import`` support you can use zeta-library_, python compass alternative.

   Zeta-library supported ``@import url(path or http)``, but all static files css, scss.

   Also zeta support js import ``require( '../jquery.js' );``. Zeta allow you control all your static files.


Requirements
=============
- python >= 2.5
- pyparsing >= 1.5.5


Installation
============
**python-scss** should be installed using pip or setuptools: ::

    pip install scss

    easy_install scss


Usage
=====

#. **From python source code**: ::

    from scss import parser

    file_path = path_to_file
    src = open( file_path ).read()

    # from file
    print parser.load( 'file_path' )

    # from string
    print parser.parse( 'src' )

    # Create parser object
    p = parser.Stylesheet( options=dict( compress=True ) )
    print p.loads( src )
    p.load( file_path )
    print p

#. **From command line**: ::

    $ scss --help
    Usage: scss [OPTION]... [INFILE] [OUTFILE]

    Compile INFILE or standard input, to OUTFILE or standard output.

    Options:
    --version             show program's version number and exit
    -h, --help            show this help message and exit
    -c, --cache           Create and use cache file. Only for files.
    -i, --interactive     Run in interactive shell mode.
    -m, --compress        Compress css output.
    -w WATCH, --watch=WATCH
                            Watch files or directories for changes. The location
                            of the generated CSS can be set using a colon:
                            scss -w input.scss:output.css
    -S, --no-sorted       Do not sort declaration.
    -C, --no-comments     Clear css comments.
    -W, --no-warnings     Disable warnings.

#. **In interactive mode**: ::

    scss -i

    >>> 25px + 1.5em


Changes
=======

Make sure you`ve read the following document if you are upgrading from previous versions of scss:

http://packages.python.org/scss/changes.html


Examples
========

#. **Nested Rules**
    Example::

	.selector {
	    a {
	        display: block;
	    }
	    strong {
	        color: blue;
	    }
	}

    ...produces::

        .selector a {
            display: block}

        .selector strong {
            color: blue}


#. **Variables**
    Example::

        $main-color: #ce4dd6;
        $style: solid;
        $side: bottom;
        #navbar {
            border-#{$side}: {
            color: $main-color;
            style: $style;
            }
        }

    ...produces::

        #navbar {
            border-bottom-color: #ce4dd6;
            border-bottom-style: solid}

#. **Mixins**
    Example::

        @mixin rounded($side, $radius: 10px) {
            border-#{$side}-radius: $radius;
            -moz-border-radius-#{$side}: $radius;
            -webkit-border-#{$side}-radius: $radius;
        }
        #navbar li { @include rounded(top); }
        #footer { @include rounded(top, 5px); }
        #sidebar { @include rounded(left, 8px); }

    ...produces::

        #navbar li {
                -moz-border-radius-top: 10px;
                -webkit-border-top-radius: 10px;
                border-top-radius: 10px}

        #footer {
                -moz-border-radius-top: 5px;
                -webkit-border-top-radius: 5px;
                border-top-radius: 5px}

        #sidebar {
                -moz-border-radius-left: 8px;
                -webkit-border-left-radius: 8px;
                border-left-radius: 8px}

#. **Extend** (using `@extend`)
    Example::

        .error {
            border: 1px #f00;
            background-color: #fdd;
        }
        .error.intrusion {
            background-image: url("/image/hacked.png");
        }
        .seriousError {
            @extend .error;
            border-width: 3px;
        }

    ...produces::

        .error, .seriousError {
            background-color: #fdd;
            border: 1px #f00}

        .error .intrusion, .seriousError .intrusion {
            background-image: url('/image/hacked.png')}

        .seriousError {
            border-width: 3px}

#. **Interactive mode**
    Example::

	$ python scss.py --interactive
	>>> 25px + 1.5em
        44.5px
        >>> lighten(rgba(130,130,130,.4),10%)
        rgba(155,155,155,0.40)
        >>> .rule { test: red; }
        .rule {
            test: red }
	>>> _


Options
=======

Python-scss has the following options:

- **compress**: Compress ouput css, default is False

- **cache**: Precache compile result, default is False

- **comments**: Leave css comments, default is True

- **sort**: Sort declaration, default is True
  Declaration sorted in `Zen sorting order`_

- **warn**: Enable or disable warnings: unwnown mixin, declaration name, extend ruleset, default is True

Option can be defined... 

#. from command line: ::

    scss -m -S file.scss

#. from python: ::

    parser = Stylesheet( options=dict( compress=True ) )

#. from scss source: ::

    @option compress: true, sort: false;


.. note::
    python-scss is still at early stages of development


Bug tracker
===========

If you have any suggestions, bug reports or
annoyances please report them to the issue tracker
at https://github.com/klen/python-scss/issues


Contributing
============

Development of python-scss happens at github: https://github.com/klen/python-scss

* klen_ (Kirill Klenov)


License
=======

Licensed under a `GNU lesser general public license`_.


Copyright
=========

Copyright (c) 2011 Kirill Klenov (horneds@gmail.com)

Compass_:
    (c) 2009 Christopher M. Eppstein
    http://compass-style.org/

SCSS_:
    (c) 2006-2009 Hampton Catlin and Nathan Weizenbaum
    http://sass-lang.com/


Note
====

**Your feedback are welcome!**

.. _zeta-library: http://github.com/klen/zeta-library
.. _GNU lesser general public license: http://www.gnu.org/copyleft/lesser.html
.. _SCSS: http://sass-lang.com
.. _compass: http://compass-style.org/
.. _python scss git: http://packages.python.org/scss/
.. _pypi: http://packages.python.org/scss/
.. _github: https://github.com/klen/python-scss
.. _Zen sorting order: http://code.google.com/p/zen-coding/wiki/ZenCSSPropertiesEn#Sorting_Methods
.. _klen: https://klen.github.com
