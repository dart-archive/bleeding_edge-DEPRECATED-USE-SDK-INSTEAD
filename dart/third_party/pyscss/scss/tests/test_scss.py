import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_bugs(self):
        src = """
            .bug {
                background: -webkit-gradient(linear, top left, 100% 100%, from(#ddd), to(#aaa));
                background: -moz-linear-gradient (top, #DDD, #AAA);
                margin: 2px -5em -1px 0;
            }
        """
        test = ".bug{margin:2px -5em -1px 0;background:-webkit-gradient(linear, top left, 100% 100%, from(#ddd), to(#aaa));background:-moz-linear-gradient(top, #ddd, #aaa)}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_base(self):
        src = """
            @charset utf-8;
            @import url(test);

            @warn "Test warnings!"
            @mixin z-base {
                a:hover, a:active { outline: none; }
                a, a:active, a:visited { color: #607890; }
                a:hover { color: #036; }
                @debug test; }

            @media print { @include z-base; }

            // Test comment
            /* Css comment */
            body:not(.test) {
                $font: Georgia;

                margin-bottom: .5em;
                font-family: $font, sans-serif;
                *font:13px/1.231 sans-serif; }

            ::selection {
                color: red;
            }

            .test:hover {
                color: red;
                &:after {
                    content: 'blue'; }}

            pre, code, kbd, samp {
                font: 12px/10px;
                font-family: monospace, sans-serif; }

            abbr[title], dfn[title] {
                border:2px; }

            """
        test = "@charset utf-8;\n@import url(test);\n@media print { a:hover, a:active{outline:none}a, a:active, a:visited{color:#607890}a:hover{color:#036} }body:not(.test){margin-bottom:.5em;font-family:Georgia , sans-serif;*font:13px / 1.231 sans-serif}::selection{color:#f00}.test:hover{color:#f00}.test:hover:after{content:#00f}pre, code, kbd, samp{font:12px / 10px;font-family:monospace , sans-serif}abbr[title], dfn[title]{border:2px}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_nesting_2(self):
        src = """#navbar {
          width: 80%;
          height: 23px;
          ul { list-style-type: none; }
          li { float: left;
            a .test .main{ font-weight: bold; }
          } }"""
        test = "#navbar{width:80%;height:23px}#navbar ul{list-style-type:none}#navbar li{float:left}#navbar li a .test .main{font-weight:bold}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_nestproperties(self):
        src = """.fakeshadow {
            border: {
                style: solid;
                left: { width: 4px; color: #888; }
                right: { width: 2px; color: #ccc; }
            } }"""
        test = ".fakeshadow{border-style:solid;border-right-width:2px;border-right-color:#ccc;border-left-width:4px;border-left-color:#888}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_parent_references(self):
        src = """a { color: #ce4dd6;
            &:hover { color: #ffb3ff; }
            &:visited { color: #c458cb; }
            .test & { color: red; }}"""
        test = "a{color:#ce4dd6}a:hover{color:#ffb3ff}a:visited{color:#c458cb}.test a{color:#f00}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_variables(self):
        src = """$main-color: #ce4dd6;
            $style: solid;
            $def_test: first;
            $def_test: second;
            $def_test: beep-beep !default;
            #navbar { border-bottom: { color: $main-color; style: $style; } }
            a.#{$def_test} { color: $main-color; &:hover { border-bottom: $style 1px; } }"""
        test = "#navbar{border-bottom-style:solid;border-bottom-color:#ce4dd6}a.second{color:#ce4dd6}a.second:hover{border-bottom:solid 1px}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_interpolation(self):
        src = """$side: top;
            $radius: 10px;
            div.rounded-#{$side} p {
            border-#{$side}-radius: $radius;
            -moz-border-radius-#{$side}: $radius;
            -webkit-border-#{$side}-radius: $radius; }"""
        test = "div.rounded-top p{border-top-radius:10px;-moz-border-radius-top:10px;-webkit-border-top-radius:10px}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_mixin_arg(self):
        src = """@mixin rounded($side, $radius: 10px, $dummy: false) {
            border-#{$side}-radius: $radius;
            -moz-border-radius-#{$side}: $radius;
            -webkit-border-#{$side}-radius: $radius; }
            #navbar li { @include rounded(top); }
            #footer { @include rounded(top, 5px); }
            #sidebar { @include rounded(left, 8px); }"""
        test = "#navbar li{border-top-radius:10px;-moz-border-radius-top:10px;-webkit-border-top-radius:10px}#footer{border-top-radius:5px;-moz-border-radius-top:5px;-webkit-border-top-radius:5px}#sidebar{border-left-radius:8px;-moz-border-radius-left:8px;-webkit-border-left-radius:8px}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_extend_rule(self):
        src = """
            .error { border: 1px #f00; background-color: #fdd; }
            a:hover {text-decoration: underline}
            .hoverlink {@extend a:hover}
            .error .intrusion { background-image: url(/image/hacked.png); }
            .seriousError { @extend .error; border-width: 3px; }
            """
        test = ".error, .seriousError{border:1px #f00;background-color:#fdd}a:hover{text-decoration:underline}.error .intrusion, .seriousError .intrusion{background-image:url(/image/hacked.png)}.seriousError{border-width:3px}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)
