package com.google.dart.tools.core.mobile;

/**
 * Class representing a connected android device as returned by
 * {@link AndroidDebugBridge#getConnectedDevice()}.
 */
public class AndroidDevice {

  public static boolean isEqual(AndroidDevice d1, AndroidDevice d2) {
    return d1 == null ? d2 == null : d1.equals(d2);
  }

  private String deviceId;

  private boolean authorized;

  public AndroidDevice(String deviceId, boolean authorized) {
    if (deviceId == null) {
      throw new IllegalArgumentException();
    }
    this.deviceId = deviceId;
    this.authorized = authorized;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof AndroidDevice) {
      AndroidDevice d = (AndroidDevice) other;
      return deviceId.equals(d.getDeviceId()) && authorized == d.isAuthorized();
    }
    return false;
  }

  public String getDeviceId() {
    return deviceId;
  }

  @Override
  public int hashCode() {
    return deviceId.hashCode() + (authorized ? 1 : 0);
  }

  public boolean isAuthorized() {
    return authorized;
  }
}
