package com.mecong.tenderalarm.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AlarmReceiverActivityTest {

  public static int convert(byte[] values) {
    int result = 0;

    for (int i = 0; i < values.length; i++) {
      result = (result >> (5 - i) | ((int) values[i])) << (5 - i);
    }

    return result;
  }

  @Test
  void name() {
    byte[] values = {1, 1, 1, 0, 0, 0};
    assertEquals(56, convert(values));

    byte[] values2 = {0, 1, 1, 0, 1, 0};
    assertEquals(26, convert(values2));
  }
}