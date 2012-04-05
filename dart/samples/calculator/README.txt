A. Building the Calculator application for ChromeOS:
====================================================

cd dart/samples/calculator
./build_app ~/www


B. Installing calculator in ChromeOS:
=====================================

Copy the manifest to point to Dart calculator application and application icon:

1. Run the File Manager
2. Create a new folder (e.g., calculator)
3. Open a browser tab type in the URL of where you built the calculator app (e.g., http://www.corp.google.com/~terry/dart/calculator)
4. Select the file manifest.json
5. When the file's content is displayed click on the file's content and select the "Save as..." menu item
6. Choose the new folder created in the File Manager (e.g., calculator)
7. Again type in the URL of where you built the calculator app (e.g., http://www.corp.google.com/~terry/dart/calculator)
8. Select the file calc_128.png (the application icon)
9. When the icon is displayed, click on the icon and select "Save image as..." menu item
10. Choose the new folder craeted in the File Manager (e.g., calculator)


C. Installing the app (bring up the extensions management page):
================================================================

1. Click the wrench icon and choosing Tools > Extensions.
2. If Developer mode has a + by it, click the +. 
3. Click the Load unpacked extension button. 
4. A file dialog appears.
5. Choose the new folder created in section B (e.g., calculator)

