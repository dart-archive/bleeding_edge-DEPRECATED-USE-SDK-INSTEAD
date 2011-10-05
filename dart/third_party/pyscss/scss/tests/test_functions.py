import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_math(self):
        src = " 12 * (60px + 20px) "
        test = "960px"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_operations_and_functions(self):
        src = """
        #navbar {
            $navbar-width: 800px;
            $items: 1 + 2;
            $navbar-color: rgb(100, 100, 55);
            $font: "Verdana", monospace;
            width: $navbar-width;
            border-bottom: 2px solid $navbar-color;

            #{enumerate("div", 1, $items)} {
                p & {
                    color: blue }
                color: red; }
            }

            li {
                background-color: $navbar-color - #333;
                background-image: url(test/value.png);
                float: left;
                font: 8px/10px $font;
                margin: 3px + 5.5px auto;
                height: 5px + (4px * (2 + $items));
                width: $navbar-width / $items - 10px;
                &:hover { background-color: $navbar-color - 10%; } }"""
        test = "#navbar{width:800px;border-bottom:2px solid #646437}#navbar div1, #navbar div2, #navbar div3{color:#f00}p #navbar div1, p #navbar div2, p #navbar div3{color:#00f}li{float:left;margin:8.5px auto;width:256.667px;height:25px;background-color:#313104;background-image:url(test/value.png);font:8px / 10px 'Verdana', monospace}li:hover{background-color:#5c5c3e}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_rgb_functions(self):
        src = """
            @option warn:false;

            $color: rgba(23, 45, 67, .4)
            $color2: #fdc;
            .test {
                red_test: red($color);
                blue_test: blue($color);
                green_test: green($color);
                color: mix(#f00, #00f, 25%);
            }
        """
        test = ".test{color:#3f00bf;red_test:23;blue_test:67;green_test:45}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_hsl_functions(self):
        src = """
            @option warn:false;

            $hsl: hsla(0, 100%, 25%, .4);
            .test {
                color: $hsl;
                hue: hue($hsl);
                s: saturation($hsl);
                g: grayscale(#099);
                l: lighten(#333, 50%);
                ah: adjust-hue(#811, 45deg);
            }
        """
        test = ".test{color:rgba(127,0,0,0.40);hue:0;s:100%;g:#4c4c4c;l:#b2b2b2;ah:#886a10}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_opacity_functions(self):
        src = """
            $color: rgba(100, 100, 100, .4);
            .test {
                color: opacify( $color, 60% );
            }
        """
        test = ".test{color:#646464}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_string_functions(self):
        src = """
            $top: 'top';
            $bottom: bottom;
            .test {
                bottom: quote($bottom);
                top: unquote($top);
            }
        """
        test = ".test{top:top;bottom:'bottom'}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_number_functions(self):
        src = """
            @option warn:false;

            $top: 100px;
            $bottom: 50px;
            .test {
                top: percentage($top / $bottom);
                round: round($top);
                ceil: ceil(1.24);
                floor: floor(1.24);
                abs: abs(-1.24);
            }
        """
        test = ".test{top:200%;round:100.0;ceil:2.0;floor:1.0;abs:1.24}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_introspection_functions(self):
        src = """
            @option warn:false;

            $top: 100px;
            $color: #f00;
            .test {
                test: type-of($top);
                test2: type-of($color);
                test3: unit($top);
                test4: unitless($top);
            }
        """
        test = ".test{test:number;test2:color;test3:px;test4:false}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_compass_helpers(self):
        src = """
            #{append-selector(".foo, .bar", ".baz")} {
                color: red;
            }

            .example {

                #{elements-of-type(block)} {
                    border: 1px solid #777777;
                    margin: 1em 3em; }

                #{elements-of-type(inline)} {
                    color: #cc0000; }
            }

            a {
                #{headings(2, 4)} {
                    font-weight: bold;
                }
            }
        """
        test = ".foo .baz, .bar .baz{color:#f00}.example address, .example article, .example aside, .example blockquote, .example center, .example dd, .example dialog, .example dir, .example div, .example dl, .example dt, .example fieldset, .example figure, .example footer, .example form, .example frameset, .example h1, .example h2, .example h3, .example h4, .example h5, .example h6, .example header, .example hgroup, .example hr, .example isindex, .example menu, .example nav, .example noframes, .example noscript, .example ol, .example p, .example pre, .example section, .example ul{margin:1em 3em;border:1px solid #777}.example a, .example abbr, .example acronym, .example b, .example basefont, .example bdo, .example big, .example br, .example cite, .example code, .example dfn, .example em, .example font, .example i, .example img, .example input, .example kbd, .example label, .example q, .example s, .example samp, .example select, .example small, .example span, .example strike, .example strong, .example sub, .example sup, .example textarea, .example tt, .example u, .example var{color:#c00}a h2, a h3, a h4{font-weight:bold}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_image_functions(self):
        src = """
            img.test {
                width: image-width(scss/tests/ + 'bug_64.png');
                height: image-height(scss/tests/ + 'bug_64.png');
                background-image: inline-image(scss/tests/ + 'test.png')

            }
        """
        test = 'img.test{width:64px;height:64px;background-image:url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAMAAAC67D+PAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAAlQTFRF8EFB3AAA////d5xsogAAAAN0Uk5T//8A18oNQQAAAClJREFUeNpiYIIDBkYGCAXEYDaEAFEQKbAQVBEyE6EASRuSYQgrAAIMAB1mAIkfpDEtAAAAAElFTkSuQmCC")}'
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_function_define(self):
        src = """
            @function percent-width($t, $c) {
                $perc: $t / $c * 100%;
                @return $perc;
            }

            .test {
                width: percent-width(5px, 24px);
            }
        """
        test = ".test{width:20.833%}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_misc(self):
        src = """
            $test: center;
            .test {
                margin: 0 if($test == center, auto, 10px);
            }
        """
        test = ".test{margin:0 auto}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)
