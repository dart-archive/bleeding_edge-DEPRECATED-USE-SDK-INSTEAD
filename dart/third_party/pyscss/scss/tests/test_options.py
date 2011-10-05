import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet()

    def test_default(self):
        src = """
        @option compress:false;
        // SCSS comment
        /* CSS Comment */
        #navbar {
            height: 100px;
            color: #ff0033;
            border: 2px solid magenta;
            @warn "Test";

            li {
                background-color: red - #333;
                float: left;
                font: 8px/10px verdana;
                margin: 3px + 5.5px auto;
                height: 5px + (4px * 2);
            }
        }
        """
        test = "/* CSS Comment */\n#navbar {\n\theight: 100px;\n\tborder: 2px solid #f0f;\n\tcolor: #f03;}\n\n#navbar li {\n\tfloat: left;\n\tmargin: 8.5px auto;\n\theight: 13px;\n\tbackground-color: #c00;\n\tfont: 8px / 10px verdana;}\n\n"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_compress(self):
        src = """
        @option compress:true;
        // SCSS comment
        /* CSS Comment */
        #navbar, p {
            height: 100px;
            color: #ff0033;
            border: 2px solid magenta;

            li {
                background-color: red - #333;
                float: left;
                font: 8px/10px verdana;
                margin: 3px + 5.5px auto;
                height: 5px + (4px * 2);
            }
        }
        """
        test = "#navbar, p{height:100px;border:2px solid #f0f;color:#f03}#navbar li, p li{float:left;margin:8.5px auto;height:13px;background-color:#c00;font:8px / 10px verdana}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_comments(self):
        src = """
        @option comments:false, compress: false;
        // SCSS comment
        /* CSS Comment */
        #navbar, p {
            height: 100px;
            color: #ff0033;
            border: 2px solid magenta;

            li {
                background-color: red - #333;
                float: left;
                font: 8px/10px verdana;
                margin: 3px + 5.5px auto;
                height: 5px + (4px * 2);
            }
        }
        """
        test = "#navbar, p {\n\theight: 100px;\n\tborder: 2px solid #f0f;\n\tcolor: #f03;}\n\n#navbar li, p li {\n\tfloat: left;\n\tmargin: 8.5px auto;\n\theight: 13px;\n\tbackground-color: #c00;\n\tfont: 8px / 10px verdana;}\n\n"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

    def test_sortings(self):
        src = """
        @option comments:true, compress: false, sort: false;
        // SCSS comment
        /* CSS Comment */
        #navbar, p {
            height: 100px;
            color: #ff0033;
            border: 2px solid magenta;

            li {
                background-color: red - #333;
                float: left;
                font: 8px/10px verdana;
                margin: 3px + 5.5px auto;
                height: 5px + (4px * 2);
            }
        }
        """
        test = "/* CSS Comment */\n#navbar, p {\n\theight: 100px;\n\tcolor: #f03;\n\tborder: 2px solid #f0f;}\n\n#navbar li, p li {\n\tbackground-color: #c00;\n\tfloat: left;\n\tfont: 8px / 10px verdana;\n\tmargin: 8.5px auto;\n\theight: 13px;}\n\n"
        out = self.parser.loads(src)
        self.assertEqual(test, out)
