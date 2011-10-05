import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_font_face(self):
        src = """
            @font-face {
                    font-family: 'MyMinionPro';
                src: url('minion-webfont.eot?') format('eot'),
                    url('minion-webfont.woff') format('woff'),
                    url('minion-webfont.ttf') format('truetype');
                    font-weight: normal;
                    font-style: normal;
                    font-size: ( 12px / 16px ) * 100%;
            }

            @font-face {
                    font-family: 'MyMinionProItalic';
                src: url('minionpro-it-webfont.eot?') format('eot'),
                    url('minionpro-it-webfont.woff') format('woff'),
                    url('minionpro-it-webfont.ttf') format('truetype');
                    font-weight: normal;
                    font-style: italic;
            }

            h1,h2,h3, time, ol#using .number {
                    font-weight: normal;
                    font-family: 'MyMinionPro';
            }
        """
        test = "@font-face{font-weight:normal;font-style:normal;font-size:75%;font-family:'MyMinionPro';src:url('minion-webfont.eot?') format('eot') , url('minion-webfont.woff') format('woff') , url('minion-webfont.ttf') format('truetype')}@font-face{font-weight:normal;font-style:italic;font-family:'MyMinionProItalic';src:url('minionpro-it-webfont.eot?') format('eot') , url('minionpro-it-webfont.woff') format('woff') , url('minionpro-it-webfont.ttf') format('truetype')}h1, h2, h3, time, ol#using .number{font-weight:normal;font-family:'MyMinionPro'}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)
