import cPickle
import os.path
import sys
from collections import defaultdict

from pyparsing import ParseBaseException

from scss import SORTING
from scss.base import Node, Empty, ParseNode, ContentNode, IncludeNode
from scss.control import Variable, Expression, Function, Mixin, Include, MixinParam, Extend, Variables, Option, FunctionDefinition, FunctionReturn, If, For, SepValString, Stylet
from scss.function import warn, _nest
from scss.grammar import *
from scss.value import NumberValue, StringValue, ColorValue, QuotedStringValue, PointValue, RepeatValue


class Comment(Node):
    """ Comment node.
    """
    delim = ''
    def __str__(self):
        """ Clean comments if option `comments` disabled
            or enabled option `compress`
        """
        if self.root.get_opt('comments') and not self.root.get_opt('compress'):
            return super(Comment, self).__str__()
        return ''


class Warn(Empty):
    """ Warning node @warn.
    """
    def parse(self, target):
        """ Write message to stderr.
        """
        if self.root.get_opt('warn'):
            warn(self.data[1])


class Import(Node):
    """ Import node @import.
    """
    def __str__(self):
        """ Write @import to outstring.
        """
        return "%s;\n" % super(Import, self).__str__()

# TODO(terry): Need to eliminate the { } as literal and consume with notion of
#              DECLARATION_BODY, ANIMATION_BODY, AnimationDecls, etc.  This will
#              make the formatting code general and succinct.
class AnimationDecls(ContentNode):
    def __str__(self):
      nl, ws, ts = self.root.cache['delims']

      # Formatting for animation declarations.  Each declaration body is
      # wrapped by curly braces with one level of indentation (2 spaces).
      # Within a curly brace each declaration is indented 2 spaced and each
      # declaration ends with a semi-colon. 
      declLen = len(self.data)
      if (declLen > 0):
        declLen -= 1
        assert self.data[0] == '{' and self.data[declLen] == '}'
        return ''.join(['%s{%s' % (ws, nl),
               '%s%s' % (ws, ws),
               '%s%s%s;' % (ws, ws,
                           ''.join(str(decl) for decl in self.data[1:declLen])),
               '%s%s%s}%s%s%s' % (nl, ws, ws, nl, ws, ws)])

class AnimationBody(ContentNode):
    def __str__(self):
       nl, ws, ts = self.root.cache['delims']

      # Formatting for the body of the animation.  The body is surrounded by an
      # open curly brace and newline followed by a repeated field selector
      # (indented by 2 spaces) and the animation declaration.  When all
      # animation declarations are emitted add the end curly brace for the
      # entire animation.
       bodyLen = len(self.data)
       if bodyLen > 0:
         bodyLen -= 1
         assert self.data[0] == '{' and self.data[bodyLen] == '}'
         return ''.join(['%s{%s' % (ws, nl),
                '%s%s' % (ws, ws),
                ''.join(str(body) for body in self.data[1:bodyLen]),
                '%s' % nl,
                '}%s%s' % (nl, nl)])

class Ruleset(ContentNode):
    """ Rule node.
    """
    def parse(self, target):
        """ Parse nested rulesets
            and save it in cache.
        """
        if isinstance(target, ContentNode):
            if target.name:
                self.parent = target
                self.name.parse(self)
                self.name += target.name
            target.ruleset.append(self)
        self.root.cache['rset'][str(self.name).split()[0]].add(self)
        super(Ruleset, self).parse(target)


class Declaration(ParseNode):
    """ Declaration node.
    """
    def __init__(self, s, n, t):
        """ Add self.name and self.expr to object.
        """
        super(Declaration, self).__init__(s, n, t)
        self.name = self.expr = ''

    def parse(self, target):
        """ Parse nested declaration.
        """
        if not isinstance(target, Node):
            parent = ContentNode(None, None, [])
            parent.parse(target)
            target = parent

        super(Declaration, self).parse(target)
        self.name = str(self.data[0])
        while isinstance(target, Declaration):
            self.name = '-'.join(( str(target.data[0]), self.name))
            target = target.parent

        self.expr = ' '.join(str(n) for n in self.data[2:] if not isinstance(n, Declaration))
        if self.expr:
            target.declareset.append(self)

    def __str__(self):
        """ Warning on unknown declaration
            and write current in outstring.
        """
        if ( not SORTING.has_key(self.name.strip('*_'))
                and self.root.get_opt('warn') ):
            warn("Unknown declaration: %s" % self.name)

        return (":%s" % self.root.cache['delims'][1] ).join(
                (self.name, self.expr))


class DeclarationName(ParseNode):
    """ Name of declaration node.
        For spliting it in one string.
    """
    delim = ''


class SelectorTree(ParseNode):
    """ Tree of selectors in ruleset.
    """
    delim = ', '

    def extend(self, target):
        """ @extend selectors tree.
        """
        self_test = ', '.join(map(str, self.data))
        target_test = ', '.join(map(str, target.data))
        self.data = (self_test + ', ' + self_test.replace(str(self.data[0].data[0]), target_test)).split(', ')

    def __add__(self, target):
        """ Add selectors from parent nodes.
        """
        if isinstance(target, SelectorTree):
            self_test = ', '.join(map(str, self.data))
            target_test = ', '.join(map(str, target.data))
            self.data = _nest(target_test, self_test).split(', ')
        return self

    def __str__(self):
      selOut = ''
      for entry in self.data:
        if (isinstance(entry, str)):
          # Combinator and commas are as raw strings.
          selOut += ' %s ' % entry
        else:
          for selEntry in entry.data:
            if (isinstance(selEntry, str)):
              selOut += ' %s ' % selEntry
            else:
              # Construct the pseudo elements etc. multiple regex tokens as str.
              selOut += ''.join(str(n) for n in selEntry.data);
            selOut += ' '
      return selOut


class Selector(ParseNode):
    """ Simple selector node.
    """
    delim = ''

    def __str__(self):
        """ Write to output.
        """
        return ''.join(StringValue(n).value for n in self.data)


class VarDefinition(ParseNode, Empty):
    """ Variable definition.
    """
    def __init__(self, s, n, t):
        """ Save self.name, self.default, self.expression
        """
        super(VarDefinition, self).__init__(s, n, t)
        self.name = t[0][1:]
        self.default = len(t) > 2
        self.expression = t[1]

    def parse(self, target):
        """ Update root and parent context.
        """
        super(VarDefinition, self).parse(target)
        if isinstance(self.parent, ParseNode):
            self.parent.ctx.update({ self.name: self.expression.value })
        self.root.set_var(self)


class Media(ParseNode):
    def __str__(self):
      selOut = ''
      # TODO(terry): Need to retain parenthesis in expression for the media
      #              selector.  Parenthesis are important and need a better
      #              grammar.  For now hack is to wrap the 'and' operator and
      #              comma operator to use emit parentheses around expressions.
      anyOper = False
      openParen = 0
      for entry in self.data:
        if isinstance(entry, str) and entry == 'and':
          if openParen > 0:
            selOut += ') and '
            openParen -= 1
          else:
            selOut += ' and '
          anyOper = True
        elif isinstance(entry, str) and entry == '{':
          selOut += ''.join([')' for n in xrange(openParen)])
          selOut += ' {\n'
        # Handle the expression.
        elif isinstance(entry, StringValue):
          if openParen > 0:
            selOut += ') , (%s' % entry
          elif anyOper:
            selOut += '(%s' % entry
            anyOper = False
            openParen += 1
          else:
            selOut += ' %s ' % entry
        else:
          selOut += ' %s ' % entry

      selOut += '}\n'
      return selOut

class Stylesheet(object):
    """ Root stylesheet node.
    """

    def_delims = '\n', ' ', '\t'

    # Known stylet base classes.
    styletClasses = []

    # List of known CSS class names emitted.  We only want to emit one constant
    # per CSS class.
    knownClassNames = []

    # Dart class to output only class name selectors with style properties.
    cssClass = []

    # Dart class to output only class name selectors with no style properties.
    cssStateSelectors = []

    currentOptions = []
    currentFile = ''

    scssIncludes = []

    exportedClassName = []
    classNameAsState = []

    def __init__(self, cache = None, options=None):
        self.cache = cache or dict(

            # Variables context
            ctx = dict(),

            # Mixin context
            mix = dict(),

            # Rules context
            rset = defaultdict(set),

            # Options context
            opts = dict(
                comments = True,
                warn = True,
                sort = True,
                path = os.getcwd(),
            ),

            # CSS delimeters
            delims = self.def_delims,

        )

        if options:
            self.currentOptions = options
            for option in options.items():
                self.set_opt(*option)

        self.setup()
        Node.root = self

    def setup(self):

        # Values
        OTHER_VALUE.setParseAction(NumberValue)
        PERCENTAGE_VALUE.setParseAction(NumberValue)
        IDENT.setParseAction(StringValue)
        PATH.setParseAction(StringValue)
        POINT.setParseAction(PointValue)
        COLOR_VALUE.setParseAction(ColorValue)
        REPEAT_VALUE.setParseAction(RepeatValue)
        quotedString.setParseAction(QuotedStringValue)
        EXPRESSION.setParseAction(Expression)
        SEP_VAL_STRING.setParseAction(SepValString)

        # Vars
        VARIABLE.setParseAction(Variable)
        VAR_DEFINITION.setParseAction(VarDefinition)
        VARIABLES.setParseAction(Variables)
        FUNCTION.setParseAction(Function)
        FUNCTION_DEFINITION.setParseAction(FunctionDefinition)
        FUNCTION_RETURN.setParseAction(FunctionReturn)

        # Coments
        SCSS_COMMENT.setParseAction(lambda x: '')
        CSS_COMMENT.setParseAction(Comment)

        # At rules
        IMPORT.setParseAction(Import)
        CHARSET.setParseAction(Import)
        MEDIA.setParseAction(Media)
        STYLET.setParseAction(Stylet)
        ANIMATION_DECLARATIONS.setParseAction(AnimationDecls)
        ANIMATION_BODY.setParseAction(AnimationBody)

        # Rules
        RULESET.setParseAction(Ruleset)
        DECLARATION.setParseAction(Declaration)
        DECLARATION_NAME.setParseAction(DeclarationName)
        SELECTOR.setParseAction(Selector)
        SELECTOR_GROUP.setParseAction(ParseNode)
        SELECTOR_TREE.setParseAction(SelectorTree)
        FONT_FACE.setParseAction(ContentNode)

        # SCSS Directives
        MIXIN.setParseAction(Mixin)
        MIXIN_PARAM.setParseAction(MixinParam)
        INCLUDE.setParseAction(Include)
        EXTEND.setParseAction(Extend)
        OPTION.setParseAction(Option)
        IF.setParseAction(If)
        IF_BODY.setParseAction(IncludeNode)
        ELSE.setParseAction(IncludeNode)
        FOR.setParseAction(For)
        FOR_BODY.setParseAction(IncludeNode)
        WARN.setParseAction(Warn)

    @property
    def ctx(self):
        return self.cache['ctx']

    def set_var(self, vardef):
        """ Set variable to global stylesheet context.
        """
        if not(vardef.default and self.cache['ctx'].get(vardef.name)):
            self.cache['ctx'][vardef.name] = vardef.expression.value

    def set_opt(self, name, value):
        """ Set option.
        """
        self.cache['opts'][name] = value

        if name == 'compress':
            self.cache['delims'] = self.def_delims if not value else ('', '', '')

    def get_opt(self, name):
        """ Get option.
        """
        return self.cache['opts'].get(name)

    def update(self, cache):
        """ Update self cache from other.
        """
        self.cache['delims'] = cache.get('delims')
        self.cache['opts'].update(cache.get('opts'))
        self.cache['rset'].update(cache.get('rset'))
        self.cache['mix'].update(cache.get('mix'))
        map(self.set_var, cache['ctx'].values())

    def scan(self, src):
        """ Scan scss from string and return nodes.
        """
        assert isinstance(src, basestring)
        try:
            nodes = STYLESHEET.parseString(src, parseAll=True)
            return nodes
        except ParseBaseException:
            err = sys.exc_info()[1]
            print >> sys.stderr, err.line
            print >> sys.stderr, " "*(err.column-1) + "^"
            print >> sys.stderr, err
            sys.exit(1)

    def parse(self, nodes):
        map(lambda n: n.parse(self) if isinstance(n, Node) else None, nodes)

    def loads(self, src):
        """ Compile css from scss string.
        """
        assert isinstance(src, basestring)
        nodes = self.scan(src.strip())
        self.parse(nodes)
        return ''.join(map(str, nodes))

    def load(self, f, precache=None):
        """ Compile scss from file.
            File is string path of file object.
        """
        precache = precache or self.get_opt('cache') or False
        nodes = None
        if isinstance(f, file):
            path = os.path.abspath(f.name)

        else:
            path = os.path.abspath(f.name)
            f = open(f)

        cache_path = os.path.splitext(path)[0] + '.ccss'

        if precache and os.path.exists(cache_path):
            ptime = os.path.getmtime(cache_path)
            ttime = os.path.getmtime(path)
            if ptime > ttime:
                dump = open(cache_path, 'rb').read()
                nodes = cPickle.loads(dump)

        if not nodes:
            src = f.read()
            nodes = self.scan(src.strip())

        if precache:
            f = open(cache_path, 'wb')
            cPickle.dump(nodes, f)

        self.parse(nodes)
        return ''.join(map(str, nodes))

    # TODO(terry): For now special function for returning nodes.
    def loadReturnNodes(self, f, precache=None):
        """ Compile scss from file.
            File is string path of file object.
        """
        self.currentFile = f

        precache = precache or self.get_opt('cache') or False
        nodes = None
        if isinstance(f, file):
            path = os.path.abspath(f.name)

        else:
            path = os.path.abspath(f.name)
            f = open(f)

        cache_path = os.path.splitext(path)[0] + '.ccss'

        if precache and os.path.exists(cache_path):
            ptime = os.path.getmtime(cache_path)
            ttime = os.path.getmtime(path)
            if ptime > ttime:
                dump = open(cache_path, 'rb').read()
                nodes = cPickle.loads(dump)

        if not nodes:
            src = f.read()
            nodes = self.scan(src.strip())

        if precache:
            f = open(cache_path, 'wb')
            cPickle.dump(nodes, f)

        self.parse(nodes)
        return nodes

    def __str__(self):
      return 'media';

    # Format of the Dart CSS class:
    #
    #    class Layout1 {
    #      // selector, properties<propertyName, value>
    #      static final selectors = const {
    #        '#elemId' : const {
    #          'left' : '20px'
    #        },
    #        '.className' : const {
    #          'color' : 'red'
    #        }
    #      };
    #    } 
    #
    # Emit pre-defined classes for Dart CSS.
    #
    def emitPreDefineDart(self, sourceFile):
      return ['// File generated by SCSS from source file %s\n' % sourceFile,
              '// Do not edit.\n\n'
             ]

    def emitCssClass(self, cssName):
      self.cssClass.append('class {0} {{\n'.format(cssName))
      self.cssClass.append('  // CSS class selectors:\n')

    # Emit stylet instance:
    #
    #  static final CssLayout layout = new CssLayout();
    def emitStyletInstance(self, styletName):
      styletClass = 'STYLET_{0}'.format(styletName)
      return '  static final {0} {1} = new {0}();\n\n'.format(styletClass,
                                                              styletName)

    # Emit each exported stylet.
    #
    #    class {stylet name} {
    #      // selector, properties<propertyName, value>
    #      static final selectors = const {
    def emitStyletClass(self, stylet):
      self.styletClasses.append('class {0} {{\n'.
                                format(stylet.name))
      self.styletClasses.append('  // selector, properties<propertyName, value>\n')
      self.styletClasses.append('  static final selectors = const {\n')
      return stylet.name

    def emitCloseCSSStyletSelector(self):
      self.styletClasses.append("\n    }")    
   
    # Emit the selector for this style:
    #   where selectorValue is valid selector (e.g., #elemId, .className, etc.)
    #
    #         'selectorValue' : const {
    def emitCSSStyletSelector(self, selectorTree, anySelectors):
      if anySelectors:
        self.styletClasses.append(",\n");
      self.styletClasses.append("    '%s' : const {\n" % selectorTree.data[0])

    # Emit each property in the stylet property map:
    #    'propName' : 'propValue'
    def emitStyletProperty(self, decl, anyProps):
      if anyProps:
        self.styletClasses.append(',\n')
      propValues = len(decl)
      if propValues > 0:
        values = []
        first = True
        for value in decl[-(propValues - 2):]:
          values.append(('{0}' if first else ' {0}').format(value))
          first = False
        self.styletClasses.append("      '{0}' : \"{1}\"".
                                  format(decl[0], ''.join(values)))
        anyProps = True
      return anyProps

    def emitEndStyletClass(self):
      self.styletClasses.append('\n  };\n')
      self.styletClasses.append('}\n\n')

    # Emit each CSS class name as a constant string in the generated Dart class,
    # but only once.
    def emitDartCSSClassName(self, selectorTree):
      # Only process selectors ignore declarations associated with the
      # selectors.
      for parseNode in selectorTree.data:
        if isinstance(parseNode, ParseNode):
          assert isinstance(parseNode.data[0], ParseNode)
  
          if len(parseNode.data) == 1:
            for selector in parseNode.data:
              # Tree will contain selector and strings (e.g., commas, etc.).
              if isinstance(selector, Selector):
                if len(selector.data) == 1:
                  selectName = selector.data[0]
                  if selectName[0] == '.' and selectName.rfind('.') == 0:
                    # static final String ARTICLE = 'article';
                    className = selectName[1:]
                    if not(className in self.knownClassNames):
                      # Class name hasn't been emitted; emit now.

                      # TODO(terry): Better mechanism then replacing -
                      #              with _.  Possibly add export name
                      #              attribute.
                      exportedName = className.upper().replace('-', '_')
                      self.cssClass.append(
                        "  static final String {0} = '{1}';\n".
                        format(exportedName, className))
                      # Add to list of known class names emitted.
                      self.knownClassNames.append(className)

    # Decide if class selector should be as exported class name (that has a
    # a real CSS style) or as a class name used for state (no CSS style).
    def addToKnownClassList(self, selectorTree):
      for parseNode in selectorTree.data:
        if isinstance(parseNode, ParseNode):
          if len(parseNode.data) == 1:
            name = parseNode.data[0].data[0]
            if name[0] == '.':
              cssName = name[1:]
              if not(cssName in self.exportedClassName):
                self.exportedClassName.append(cssName)
          else:
            for node in parseNode.data:
              if isinstance(node, Selector):
                for name in node.data:
                  if name[0] == '.':
                    cssName = name[1:]
                    if not(cssName in self.exportedClassName) and not(
                        cssName in self.classNameAsState):
                      self.classNameAsState.append(cssName)

    def emitCssSelector(self, name):
      exportedName = name.upper().replace('-', '_')
      self.cssClass.append("  static final String {0} = '{1}';\n".
                           format(exportedName, name))

    def emitCssStateSelectors(self):
      self.cssStateSelectors.append('\n  // CSS state class selectors:\n')

    def emitCssStateSelector(self, name):
      exportedName = name.upper().replace('-', '_')
      self.cssStateSelectors.append("  static final String {0} = '{1}';\n".
                                    format(exportedName, name))

    #class cssTest {
    #  static final String FRONTVIEW = 'frontview';
    #  static final String QUERY = 'query';
    #
    #  static final CssLayout layout = new CssLayout();
    #}
    def dartClass(self, sourceFile, cssName, cssFiles):
      self.styletClasses = self.emitPreDefineDart(sourceFile)
      self.knownClassNames = []
      self.cssClass = []

      # One class per CSS file.
      self.emitCssClass(cssName)

      # Iterate though all rulesets.
      stylets = []
      anyProps = False

      for file in cssFiles:
        nodes = file[1]
        for node in nodes:
          if isinstance(node, Ruleset):
            ruleset = node
            for selectorTree in ruleset.data:
              if isinstance(selectorTree, SelectorTree):
                # Process both class selectors with style properties and class
                # selectors used as state.
                self.addToKnownClassList(selectorTree)
          elif isinstance(node, Stylet):
            # We've hit a stylet emit the stylet class definition.
            stylet = node
            stylets.append(self.emitStyletClass(stylet))
            styletRulesets = stylet.data[2:]
            styletSelectorOpen = False;
            anySelectors = False
            for styletRuleset in styletRulesets:
              assert isinstance(styletRuleset, Ruleset)
              for selectorTree in styletRuleset.data:
                if isinstance(selectorTree, SelectorTree):
                  if styletSelectorOpen:
                    self.emitCloseCSSStyletSelector();
                    styletSelectorOpen = False;
                    anyProps = False;
                  self.emitCSSStyletSelector(selectorTree, anySelectors);
                  anySelectors = True;
                  styletSelectorOpen = True;
                elif isinstance(selectorTree, Declaration):
                  anyProps = self.emitStyletProperty(selectorTree.data, anyProps)

            if styletSelectorOpen:
              self.emitCloseCSSStyletSelector();
              anySelectors = False;
              anyProps = False;

            # Close the CSSStylet properties array and the style class we've
            # been emitting.
            self.emitEndStyletClass()
            anyProps = False

      # Emit all CSS class selectors that have a real CSS properties.
      for cssClassSelector in self.exportedClassName:
        self.emitCssSelector(cssClassSelector)

      # Emit the CSS_State class for CSS class selectors that have not CSS
      # properties associated with the class name.
      self.emitCssStateSelectors()
      for cssStateName in self.classNameAsState:
        if not(cssStateName in self.exportedClassName):
          self.emitCssStateSelector(cssStateName)

      # Emit CSS selectors that have no CSS style properties (used for state).
      self.cssClass.extend(self.cssStateSelectors)

      # Close the CSS class we're done.
      self.cssClass.append('}\n')

      self.styletClasses.extend(self.cssClass)
      return "".join(self.styletClasses)

    def addInclude(self, filename, nodes):
      self.scssIncludes.append([filename, nodes])

def parse( src, cache=None ):
    """ Parse from string.
    """
    parser = Stylesheet(cache)
    return parser.loads(src)


def load(path, cache=None, precache=False):
    """ Parse from file.
    """
    parser = Stylesheet(cache)
    return parser.load(path, precache=precache)
