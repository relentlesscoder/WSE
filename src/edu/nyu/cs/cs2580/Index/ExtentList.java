package edu.nyu.cs.cs2580.Index;

import java.util.HashMap;
import java.util.Map;

public class ExtentList {
  // This map contains all extent lists information
  // Key: Field name
  // Value: Extent list which contains the start position and end position of that field within the document
  private Map<String, FieldPositionRange> extentListMap;

  public ExtentList() {
    extentListMap = new HashMap<String, FieldPositionRange>();
  }

  public boolean addExtList(String field, int startPos, int endPos) {
    if (!extentListMap.containsKey(field)) {
      FieldPositionRange extRange = new FieldPositionRange(startPos, endPos);
      extentListMap.put(field, extRange);
      return true;
    } else {
      return false;
    }
  }

  public FieldPositionRange getFieldPositionRange(String field) {
    return extentListMap.get(field);
  }

  public boolean hasField(String field) {
    return extentListMap.containsKey(field);
  }
}
