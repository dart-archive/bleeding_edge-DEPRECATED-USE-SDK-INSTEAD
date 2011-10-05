# To debug the grammar see comment block on how to debug the pyparsing grammar
# in the MEDIA section. 

" SCSS Grammars."
from pyparsing import Word, Suppress, Literal, alphanums, SkipTo, oneOf, ZeroOrMore, Optional, OneOrMore, Forward, cStyleComment, Combine, dblSlashComment, quotedString, Regex, lineEnd, Group, White


__all__ = ("STYLESHEET", "OTHER_VALUE", "quotedString", "EXPRESSION", "IDENT", "PATH", "VARIABLE", "VAR_DEFINITION",
           "VARIABLES", "FUNCTION", "COLOR_VALUE", "SCSS_COMMENT", "CSS_COMMENT", "IMPORT", "STYLET", "RULESET",
           "DECLARATION", "DECLARATION_NAME", "SELECTOR_TREE", "SELECTOR_GROUP", "SELECTOR", "MIXIN", "INCLUDE",
           "MIXIN_PARAM", "EXTEND", "FONT_FACE", "OPTION", "FUNCTION_DEFINITION", "FUNCTION_RETURN",
           "IF", "ELSE", "IF_BODY", "FOR", "FOR_BODY", "CHARSET", "MEDIA", "WARN", "SEP_VAL_STRING", "POINT",
           "PERCENTAGE_VALUE", "ANIMATION_DECLARATIONS", "ANIMATION_BODY",
)


# Base css word and literals
COMMA, COLON, SEMICOLON = [Suppress(c) for c in ",:;"]
OPT_SEMICOLON = Optional(SEMICOLON)
LACC, RACC, LPAREN, RPAREN = [Suppress(c) for c in "{}()"]
LLACC, LRACC, LBRACK, RBRACK = [Literal(c) for c in "{}[]"]

# Comment
CSS_COMMENT = cStyleComment + Optional(lineEnd)
SCSS_COMMENT = dblSlashComment

IDENT = Regex(r"-?[a-zA-Z_][-a-zA-Z0-9_]*")
COLOR_VALUE = Regex(r"#[a-zA-Z0-9]{3,6}")
VARIABLE = Regex(r"-?\$[-a-zA-Z_][-a-zA-Z0-9_]*")
PERCENTAGE_VALUE = Regex(r"-?\d+(?:\.\d*)?|\.\d+") + '%'
OTHER_VALUE = Regex(r"-?\d+(?:\.\d*)?|\.\d+") + Optional(oneOf("em ex px cm mm in pt pc deg fr s "))
NUMBER_VALUE = PERCENTAGE_VALUE | OTHER_VALUE
PATH = Regex(r"[-\w\d_\.]*\/{1,2}[-\w\d_\.\/]*") | Regex(r"((https?|ftp|file):((//)|(\\\\))+[\w\d:#@%/;$()~_?\+-=\\\.&]*)")
POINT_PART = (NUMBER_VALUE | Regex(r"(top|bottom|left|right)"))
POINT = POINT_PART + POINT_PART

# Values
EXPRESSION = Forward()
INTERPOLATION_VAR = Suppress("#") + LACC + EXPRESSION + RACC
SIMPLE_VALUE = NUMBER_VALUE | PATH | IDENT | COLOR_VALUE | quotedString
DIV_STRING = SIMPLE_VALUE + OneOrMore(Literal("/") + SIMPLE_VALUE)

PARAMS = LPAREN + (POINT|EXPRESSION) + ZeroOrMore(COMMA + (POINT|EXPRESSION)) + RPAREN
FUNCTION = Regex(r"-?[a-zA-Z_][-a-zA-Z0-9_]*") + PARAMS
VALUE = FUNCTION | VARIABLE | SIMPLE_VALUE
PARENS = LPAREN + EXPRESSION + RPAREN
MATH_OPERATOR = Regex(r"(\+|-|/|\*|and|or|==|!=|<=|<|>|>=)\s+")
EXPRESSION << ((VALUE | PARENS) + ZeroOrMore(MATH_OPERATOR + (VALUE | PARENS)))

# Declaration
TERM = ( DIV_STRING | EXPRESSION | INTERPOLATION_VAR ) + Optional(",")
DECLARATION_NAME = Optional("*") + OneOrMore(IDENT | INTERPOLATION_VAR)
DECLARATION = Forward()
DECLARATION << (
        DECLARATION_NAME +
        ":" +
        ZeroOrMore(TERM) +
        Optional("!important") +
        Optional(LACC + OneOrMore(DECLARATION | CSS_COMMENT | SCSS_COMMENT) + RACC) +
        OPT_SEMICOLON )

# Selectors
ELEMENT_NAME = Combine(OneOrMore(IDENT | '&')) | Literal("*")
ATTRIB = LBRACK + SkipTo("]") + RBRACK
CLASS_NAME = Word('.', alphanums + "-_")
HASH = Regex(r"#[-a-zA-Z_][-a-zA-Z0-9_]+")
FILTER = HASH | CLASS_NAME | ATTRIB

## PSEUDO = Regex(r':{1,2}[A-Za-z0-9-_]+')
PSEUDO = Regex(r':{1,2}[^\s;{}]+')
COMBINATOR = ZeroOrMore(Word("+>", max=1))

SELECTOR_TREE = Forward()
SELECTOR = OneOrMore(Combine(ELEMENT_NAME | FILTER | INTERPOLATION_VAR | PSEUDO))
SELECTOR.leaveWhitespace()
#SELECTOR_GROUP = SELECTOR + ZeroOrMore((Word("+>", max=1)) + SELECTOR)
SELECTOR_GROUP = SELECTOR + ZeroOrMore(COMBINATOR + SELECTOR)
SELECTOR_GROUP.skipWhitespace = True
#SELECTOR_TREE = SELECTOR_GROUP + ZeroOrMore(COMMA + SELECTOR_GROUP)
##TLL Workaround
SELECTOR_TREE << (
                  SELECTOR_GROUP + ZeroOrMore(Word(",>+", max=1) + SELECTOR_GROUP)
                 )

#@stylet
STYLET = "@stylet" + ELEMENT_NAME

# @debug
DEBUG = "@debug" + EXPRESSION + OPT_SEMICOLON

# @warn
WARN = "@warn" + quotedString + OPT_SEMICOLON

# @include
INCLUDE_MIXIN = IDENT + Optional(PARAMS)
INCLUDE = "@include" + Optional(INCLUDE_MIXIN | quotedString) + OPT_SEMICOLON

# @extend
EXTEND = "@extend" + OneOrMore(ELEMENT_NAME | FILTER | INTERPOLATION_VAR | PSEUDO) + OPT_SEMICOLON

# SCSS variable assigment
SEP_VAL_STRING = EXPRESSION + OneOrMore(COMMA + EXPRESSION)
VAR_DEFINITION = Regex(r"\$[a-zA-Z_][-a-zA-Z0-9_]*") + COLON + (SEP_VAL_STRING | EXPRESSION ) + Optional("!default") + OPT_SEMICOLON

RULESET = Forward()
IF = Forward()
CONTENT = CSS_COMMENT | SCSS_COMMENT | WARN | DEBUG | IF | INCLUDE | VAR_DEFINITION | RULESET | DECLARATION | STYLET

# SCSS control directives
IF_BODY = LACC + ZeroOrMore(CONTENT) + RACC
ELSE = Suppress("@else") + LACC + ZeroOrMore(CONTENT) + RACC
IF << (
        ( Suppress("@if") | Suppress("@else if") ) + EXPRESSION + IF_BODY + Optional(ELSE))

FOR_BODY = ZeroOrMore(CONTENT)
FOR = "@for" + VARIABLE + Suppress("from") + VALUE + (Suppress("through") | Suppress("to")) + VALUE + LACC + FOR_BODY + RACC

RULESET << (
    SELECTOR_TREE +
    LACC + ZeroOrMore(CONTENT | FOR | EXTEND) + RACC )

# SCSS mixin
MIXIN_PARAM = VARIABLE + Optional(COLON + EXPRESSION)
MIXIN_PARAMS = LPAREN + ZeroOrMore(COMMA | MIXIN_PARAM) + RPAREN
MIXIN = "@mixin" + IDENT + Group(Optional(MIXIN_PARAMS)) + LACC + ZeroOrMore(CONTENT | FOR) + RACC

# SCSS function
FUNCTION_RETURN = "@return" + VARIABLE + OPT_SEMICOLON
FUNCTION_BODY = LACC + ZeroOrMore(VAR_DEFINITION) + FUNCTION_RETURN + RACC
FUNCTION_DEFINITION = "@function" + IDENT + Group(MIXIN_PARAMS) + FUNCTION_BODY

# Root elements
OPTION = "@option" + OneOrMore(IDENT + COLON + IDENT + Optional(COMMA)) + OPT_SEMICOLON
IMPORT = "@import" + FUNCTION + OPT_SEMICOLON
#MEDIA = "@media" + IDENT + ZeroOrMore("," + IDENT) + LLACC + ZeroOrMore( CONTENT | MIXIN | FOR ) + LRACC
FONT_FACE = "@font-face" + LLACC + ZeroOrMore(DECLARATION) + LRACC
VARIABLES = ( Literal("@variables") | Literal('@vars') ) + LLACC + ZeroOrMore(VAR_DEFINITION) + RACC
PSEUDO_PAGE = ":" + IDENT
PAGE = "@page" + Optional(IDENT) + Optional(PSEUDO_PAGE) + LLACC + ZeroOrMore(DECLARATION) + LRACC
CHARSET = "@charset" + IDENT + OPT_SEMICOLON

#TODO(terry): Added MEDIA syntax from CSS3
#media
#  : MEDIA_SYM S* media_query_list '{' S* ruleset* '}' S*
#  ;
#media_query_list
# : S* [media_query [ ',' S* media_query ]* ]?
# ;
#media_query
# : [ONLY | NOT]? S* media_type S* [ AND S* expression ]*
# | expression [ AND S* expression ]*
# ;
#media_type
# : IDENT
# ;
#expression
# : '(' S* media_feature S* [ ':' S* expr ]? ')' S*
# ;
#media_feature
# : IDENT
MEDIA_NEXT_EXPRESSION = Literal(":") + EXPRESSION
MEDIA_EXPRESSION = LPAREN + IDENT + Optional(MEDIA_NEXT_EXPRESSION) + RPAREN
MEDIA_AND_QUERY = Optional(Literal("and") | COMMA) + MEDIA_EXPRESSION
MEDIA_QUERY_LIST = Optional(Optional(Literal("ONLY") | Literal('NOT')) + IDENT + ZeroOrMore(MEDIA_AND_QUERY)) | Optional(MEDIA_EXPRESSION + ZeroOrMore(MEDIA_EXPRESSION))
MEDIA = "@media" + MEDIA_QUERY_LIST + LLACC + ZeroOrMore( CONTENT | MIXIN | FOR ) + RACC

# Below example on how to debug grammar built with pyparsing:
#
#MEDIA.setName("@media").setDebug()
#MEDIA_QUERY_LIST.setName("MEDIA_QUERY_LIST").setDebug()
#MEDIA_QUERY.setName("MEDIA_QUERY").setDebug()
#MEDIA_AND_QUERY.setName("MEDIA_AND_QUERY").setDebug()
#MEDIA_EXPRESSION.setName("MEDIA_EXPRESSION").setDebug()
#MEDIA_NEXT_EXPRESSION.setName("MEDIA_NEXT_EXPRESSION").setDebug()
#EXPRESSION.setName("EXPRESSION").setDebug()
#
# Output character offset of origijnal line parsed all pyparsing are offset from
# this input.
#
#print (' '*9).join(map(str,range(11)))
#print (''.join(map(str,range(10))))*15
#print "@media all and (min-device-width: 769px) and (max-device-width: 1280px) {_  .sm-root {_    left: 310px;_  }_}"
#print

#TODO(terry): Added webit-keyframes animation this needs to be @keyframes with
#             @-webkit-keyframes mapping to @keyframes.  Also, need more stuff
#             for animation like timing functions for keyframes see:
#
#      http://www.w3.org/TR/css3-animations/#timing-functions-for-keyframes-

KEYFRAME_SELECTORS = PERCENTAGE_VALUE | IDENT | Literal("from")  | Literal("to")
ANIMATION_DECLARATIONS = LLACC + ZeroOrMore( CONTENT | MIXIN | FOR ) + LRACC
ANIMATION_PARAMS =  KEYFRAME_SELECTORS + ANIMATION_DECLARATIONS
ANIMATION_BODY = LLACC + ZeroOrMore(ANIMATION_PARAMS) + LRACC
ANIMATION = "@-webkit-keyframes" + White() + IDENT + ANIMATION_BODY
# TODO(jmesserly): Need to support other browser prefixes.

# Css stylesheet
STYLESHEET = ZeroOrMore(
    FONT_FACE
    | CHARSET
    | OPTION
    | MEDIA
    | PAGE
    | CONTENT
    | FUNCTION_DEFINITION
    | MIXIN
    | INCLUDE
    | FOR
    | IMPORT
    | VARIABLES
    | EXPRESSION
    | ANIMATION
)
