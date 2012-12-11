import re
import sys
import gsutil
import tempfile
import optparse

def main():
  parser = _ParseOptions()
  (options, args) = parser.parse_args()
  # Determine which targets to build. By default we build the "all" target.
  if args:
    print 'only options should be passed to this script'
    parser.print_help()
    return 1

  revision = options.revision
  src_rev = options.srcrev

  gsu = gsutil.GsUtil(running_on_buildbot=False)
  elements = ['DartBuild-linux.gtk.x86_64.zip',
              'DartBuild-linux.gtk.x86.zip',
              'DartBuild-macosx.cocoa.x86_64.zip',
              'DartBuild-macosx.cocoa.x86.zip',
              'DartBuild-win32.win32.x86_64.zip',
              'DartBuild-win32.win32.x86.zip',
              'dart-editor-linux.gtk.x86_64.zip',
              'dart-editor-linux.gtk.x86.zip',
              'dart-editor-macosx.cocoa.x86_64.zip',
              'dart-editor-macosx.cocoa.x86.zip',
              'dart-editor-win32.win32.x86_64.zip',
              'dart-editor-win32.win32.x86.zip']
  os_names = []
  re_filename = re.compile(
      r'^(dart-editor|DartBuild)-(\w.+)\.(\w.+)\.(\w+)\.zip')
  for element in elements:
    matcher = re_filename.match(element)
    os_name = matcher.group(2)
    if os_name in 'macosx':
      os_name = os_name[:-1]
    if os_name not in os_names:
      os_names.append(os_name)
    print os_name
    gsu.Copy('gs://dart-editor-archive-continuous/{0}/{1}'.format(src_rev,
                                                                  element),
             'gs://dart-editor-archive-testing/'
             '{0}/{1}'.format(revision, element))
    gsu.SetAclFromFile('gs://dart-editor-archive-testing/'
                       '{0}/{1}'.format(revision, element),
                       'acl.xml')
    print 'copied {0}'.format(element)

  f = None
  try:
    f = tempfile.NamedTemporaryFile(suffix='.txt', prefix='tag')
    for os_name in os_names:
      gsu.Copy(f.name,
               'gs://dart-editor-archive-testing-staging/tags'
               '/done-{0}-{1}'.format(revision, os_name))
  finally:
    if f is not None:
      f.close()


def _ParseOptions():
  result = optparse.OptionParser()
  result.set_default('srcrev', 'latest')
  result.set_default('revision', '123')
  result.add_option('-r', '--revision',
                    help='SVN Revision.',
                    action='store')
  result.add_option('-s', '--srcrev',
                    help='source SVN Revision.',
                    action='store')

  return result

if __name__ == '__main__':
  sys.exit(main())
