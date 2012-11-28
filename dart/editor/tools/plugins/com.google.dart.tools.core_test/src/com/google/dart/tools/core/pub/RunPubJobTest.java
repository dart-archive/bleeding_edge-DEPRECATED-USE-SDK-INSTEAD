package com.google.dart.tools.core.pub;

import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.mock.MockProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.File;
import java.io.IOException;

public class RunPubJobTest extends TestCase {

  private static final MockProject PROJECT = new MockProject(RunPubJobTest.class.getSimpleName()) {
    @Override
    public IPath getLocation() {
      return ResourcesPlugin.getWorkspace().getRoot().getLocation().append(getName());
    };
  };

  // Assert normal operation
  public void test_runSilent() {
    RunPubJob target = new RunPubJob(PROJECT, RunPubJob.INSTALL_COMMAND) {
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
    RunPubJob target = new RunPubJob(PROJECT, RunPubJob.INSTALL_COMMAND) {
      @Override
      protected ProcessRunner newProcessRunner(ProcessBuilder builder) {

        // Assert valid builder information
        assertNotNull(builder);
        assertEquals(PROJECT.getLocation().toFile(), builder.directory());
        String sdkPath = builder.environment().get("DART_SDK");
        assertNotNull(sdkPath);
        assertTrue(new File(sdkPath).exists());
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
    RunPubJob target = new RunPubJob(PROJECT, RunPubJob.INSTALL_COMMAND) {
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
