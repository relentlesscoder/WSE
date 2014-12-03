package edu.nyu.cs.cs2580.Utils;

import java.util.ArrayList;
import java.util.List;

/**************************************************************************
 * V-Byte utility codes...
 *************************************************************************/

public class VByteUtil {
  private VByteUtil() {}

  /**
   * Extract i + 1 first 7 bits from {@code val} (From right to left) e.g. If i
   * = 0 and val = 101 0101 1010 (In binary form) The return byte will be 0101
   * 1010...
   *
   * @param i   The i first 7 bits (From right to left)
   * @param val the number...
   * @return the extracted 7 bits
   */
  private static byte extract7Bits(int i, long val) {
    return (byte) ((val >> (7 * i)) & ((1 << 7) - 1));
  }

  /**
   * If {@code val} has n bits, return the last n % 7 bit. (From right to left)
   * e.g. If i = 0 and val = 101 0101 1010 (In binary form) The return byte will
   * be 0000 1010...
   *
   * @param i   The last n % 7 (n - 7 * i) bits (From right to left)
   * @param val the number...
   * @return the extracted 7 bits
   */
  private static byte extractLastBits(int i, long val) {
    return (byte) ((val >> (7 * i)));
  }

  /**
   * Check if the byte is the end of the number.
   *
   * @param b A v-byte encoded byte.
   * @return true if the byte is the last byte of the number (the highest bit is
   * 1), otherwise false.
   */
  public static boolean isEndOfNum(byte b) {
    return b < 0;
  }

  /**
   * Encode a integer number to a list of bytes
   *
   * @param num The number needed to be encoded
   * @return a list of v-byte encoded byte represents the input number
   */
  public static List<Byte> vByteEncoding(int num) {
    List<Byte> bytes = new ArrayList<Byte>();

    if (num < (1 << 7)) {
      bytes.add((byte) (num | (1 << 7)));
    } else if (num < (1 << 14)) {
      bytes.add(extract7Bits(0, num));
      bytes.add((byte) (extractLastBits(1, (num)) | (1 << 7)));
    } else if (num < (1 << 21)) {
      bytes.add(extract7Bits(0, num));
      bytes.add(extract7Bits(1, num));
      bytes.add((byte) (extractLastBits(2, (num)) | (1 << 7)));
    } else if (num < (1 << 28)) {
      bytes.add(extract7Bits(0, num));
      bytes.add(extract7Bits(1, num));
      bytes.add(extract7Bits(2, num));
      bytes.add((byte) (extractLastBits(3, (num)) | (1 << 7)));
    } else {
      bytes.add(extract7Bits(0, num));
      bytes.add(extract7Bits(1, num));
      bytes.add(extract7Bits(2, num));
      bytes.add(extract7Bits(3, num));
      bytes.add((byte) (extractLastBits(4, (num)) | (1 << 7)));
    }

    return bytes;
  }

  /**
   * Decode a list of byteList to one integer number
   *
   * @param byteList A list of v-byte encoded bytes needed to be decoded
   * @return The decoded number
   */
  public static int vByteDecoding(List<Byte> byteList) {
    int num = 0;

    for (int i = 0; i < byteList.size(); i++) {
      num = ((byteList.get(i) & 0x7F) << 7 * i) | num;
    }

    return num;
  }

  /**
   * Encode a list of Integers to a list of Bytes by using v-byte
   *
   * @param list A list of integer numbers needed to be encoded
   * @return a list of v-byte encoded bytes
   */
  public static List<Byte> vByteEncodingList(List<Integer> list) {
    List<Byte> res = new ArrayList<Byte>();

    for (int i : list) {
      res.addAll(vByteEncoding(i));
    }

    return res;
  }

  /**
   * Decode a list of Bytes to a list of Integers by using v-byte
   *
   * @param byteList A list of v-byte encoded bytes which represents a list of number
   * @return a list of decoded integer numbers
   */
  public static List<Integer> vByteDecodingList(List<Byte> byteList) {
    List<Integer> res = new ArrayList<Integer>();
    int i = 0;

    while (i < byteList.size()) {
      List<Byte> tmpByteList = new ArrayList<Byte>();

      while (!isEndOfNum(byteList.get(i))) {
        tmpByteList.add(byteList.get(i++));
      }

      tmpByteList.add(byteList.get(i++));

      res.add(vByteDecoding(tmpByteList));
    }

    return res;
  }

  public static List<Integer> vByteDecodingList(byte[] byteArray) {
    List<Integer> res = new ArrayList<Integer>();
    int i = 0;

    while (i < byteArray.length) {
      List<Byte> tmpByteList = new ArrayList<Byte>();

      while (!isEndOfNum(byteArray[i])) {
        tmpByteList.add(byteArray[i++]);
      }

      tmpByteList.add(byteArray[i++]);

      res.add(vByteDecoding(tmpByteList));
    }

    return res;
  }
}
