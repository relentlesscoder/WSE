package edu.nyu.cs.cs2580.Utils;

import java.io.*;

public class SerializeUtil {
  /**
   * Serialize the object.
   * @param obj object
   * @return array of bytes
   * @throws java.io.IOException
   */
  public static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream os = new ObjectOutputStream(out);
    os.writeObject(obj);
    return out.toByteArray();
  }

  /**
   * Deserialize the object
   * @param data object represented in array of bytes
   * @return object
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    ByteArrayInputStream in = new ByteArrayInputStream(data);
    ObjectInputStream is = new ObjectInputStream(in);
    return is.readObject();
  }
}
