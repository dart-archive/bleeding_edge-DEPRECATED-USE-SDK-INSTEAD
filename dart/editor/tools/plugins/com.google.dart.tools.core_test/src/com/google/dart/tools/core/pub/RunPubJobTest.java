package com.google.dart.tools.core.pub;

import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.test.util.TestProject;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_LOCK_FILE_NAME;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.IOException;

public class RunPubJobTest extends TestCase {

//  TODO(keertip): enable when sure it will pass on buildbot
//  public void test_runPubScript() throws Exception {
//
//    PlainTestProject project = new PlainTestProject("fooBar");
//
//    RunPubJob pubJob = new RunPubJob(project.getProject(), "help");
//    IStatus status = pubJob.runSilent(new NullProgressMonitor());
//    assertStatus(status, IStatus.OK, null);
//  }

  // Assert normal operation

  private TestProject testProject;

  public void test_runSilent() {
    RunPubJob target = new RunPubJob(testProject.getProject(), RunPubJob.INSTALL_COMMAND, false) {
      @Override
      protected ProcessRunner newProcessRunner(ProcessBuilder builder) {
        ProcessRunner processRunner = new ProcessRunner(builder) {
          @Override
          public int runSync(IProgressMonitor monitor) throws IOException {
            return 0;
          }
        };
        return processRunner;
      }
    };
    IStatus status = target.runSilent(new NullProgressMonitor());
    assertStatus(status, IStatus.OK, null);
  }

  // Assert a process IOException is gracefully handled correctly
  public void test_runSilent_ioException() {
    RunPubJob target = new RunPubJob(testProject.getProject(), RunPubJob.INSTALL_COMMAND, false) {
      @Override
      protected ProcessRunner newProcessRunner(ProcessBuilder builder) {

        // Assert valid builder information
        assertNotNull(builder);
        assertEquals(testProject.getProject().getLocation().toFile(), builder.directory());
        assertTrue(builder.command().size() > 0);

        ProcessRunner processRunner = new ProcessRunner(builder) {
          @Override
          public int runSync(IProgressMonitor monitor) throws IOException {
            throw new IOException("test");
          }
        };
        return processRunner;
      }
    };
    IStatus status = target.runSilent(new NullProgressMonitor());
    assertStatus(status, IStatus.CANCEL, IOException.class);
  }

  // Assert a non-zero exit code generates an error status
  public void test_runSilent_nonZeroExitCode() {
    final int exitCode = 3452;
    RunPubJob target = new RunPubJob(testProject.getProject(), RunPubJob.INSTALL_COMMAND, false) {
      @Override
      protected ProcessRunner newProcessRunner(ProcessBuilder builder) {
        ProcessRunner processRunner = new ProcessRunner(builder) {
          @Override
          public int getExitCode() {
            return exitCode;
          }

          @Override
          public int runSync(IProgressMonitor monitor) throws IOException {
            return exitCode;
          }
        };
        return processRunner;
      }
    };
    IStatus status = target.runSilent(new NullProgressMonitor());
    assertStatus(status, IStatus.ERROR, null);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testProject = new TestProject();
    if (testProject.getProject().exists()) {
      testProject.setFileContent(PUBSPEC_FILE_NAME, "name:  myapp");
      testProject.createFolder(PACKAGES_DIRECTORY_NAME);
      testProject.setFileContent(PUBSPEC_LOCK_FILE_NAME, "packages:");
    }
  }

  private void assertStatus(IStatus status, int severity, Class<?> exceptionClass) {
    assertNotNull("Expected status", status);
    assertEquals(severity, status.getSeverity());
    if (exceptionClass != null) {
      assertNotNull("Expected exception", status.getException());
      assertEquals(exceptionClass, status.getException().getClass());
    }
    assertNotNull("Expected message", status.getMessage());
  }
}
