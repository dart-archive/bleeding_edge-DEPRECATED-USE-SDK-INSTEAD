import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_variables(self):
        src = """
            table.hl {
                margin: 2em 0;
                td.ln {
                    text-align: right;

                    li {
                        color: red;
                    }
                    &:hover {
                        width: 20px;
                    }
                }
            }

            li {
                font: {
                    family: serif;
                    weight: bold;
                    size: 1.2em;
                }
            }
            """
        test = "table.hl{margin:2em 0}table.hl td.ln{text-align:right}table.hl td.ln li{color:#f00}table.hl td.ln:hover{width:20px}li{font-weight:bold;font-size:1.2em;font-family:serif}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)

