package com.google.dart.tools.core.utilities.download;

import com.google.dart.tools.core.DartCore;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import java.util.UUID;

public class UpdateDecoratorForUsageCountTest extends TestCase {
  public void test_cidGeneration() throws CoreException {

    String preState = getCidPrefHelper();
    try {

      //Force the state of the Cid Pref to empty
      setCidPrefHelper("");

      //Verify
      String preTestValue = getCidPrefHelper();
      Assert.isTrue(preTestValue.isEmpty());

      //Generate new CID
      String newCID = UpdateDecoratorForUsageCount.getCIDParam();
      UUID newUUID = UUID.fromString(newCID);
      Assert.isNotNull(newUUID);
      Assert.isTrue(!newCID.isEmpty());

      //Readback 1
      String newCID2 = UpdateDecoratorForUsageCount.getCIDParam();
      UUID newUUID2 = UUID.fromString(newCID2);

      Assert.isTrue(newCID.equals(newCID2));
      Assert.isTrue(newUUID.equals(newUUID2));

      //Readback 2
      String newCID3 = UpdateDecoratorForUsageCount.getCIDParam();
      UUID newUUID3 = UUID.fromString(newCID3);

      Assert.isTrue(newCID.equals(newCID3));
      Assert.isTrue(newUUID.equals(newUUID3));

      //Force the state of the Cid Pref to empty
      setCidPrefHelper("");

      //Generate new second CID
      String newCIDPass2 = UpdateDecoratorForUsageCount.getCIDParam();
      UUID newUUIDPass2 = UUID.fromString(newCIDPass2);

      //Verify it worked
      Assert.isNotNull(newUUIDPass2);
      Assert.isTrue(!newCIDPass2.isEmpty());

      //Verify it's different
      Assert.isTrue(!newCIDPass2.equals(newCID));
      Assert.isTrue(!newUUIDPass2.equals(newUUID));

    } finally {
      //Push original value back
      setCidPrefHelper(preState);

      //Verify that we're back to initial state
      Assert.isTrue(preState.equals(getCidPrefHelper()));
    }

  }

  private String getCidPrefHelper() {
    return DartCore.getPlugin().getPrefs().get(UpdateDecoratorForUsageCount.PREF_USER_CID, "");
  }

  private void setCidPrefHelper(String cid) throws CoreException {
    DartCore.getPlugin().getPrefs().put(UpdateDecoratorForUsageCount.PREF_USER_CID, cid);
    DartCore.getPlugin().savePrefs();
  }

}
