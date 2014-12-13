package edu.nyu.cs.cs2580.Index;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ExtentList implements Serializable {
  private static final long serialVersionUID = 1L;

  public static enum DocumentField {
    TITLE,
    CONTENT
  }

  // This map contains all extent lists information
  // Key: Field name
  // Value: Extent list which contains the start position and end position of that field within the document
  private Map<DocumentField, FieldPositionRange> extentListMap;

  public ExtentList() {
    extentListMap = new HashMap<DocumentField, FieldPositionRange>();
  }

  public boolean addExtList(DocumentField docField, int startPos, int endPos) {
    if (!extentListMap.containsKey(docField)) {
      FieldPositionRange extRange = new FieldPositionRange(startPos, endPos);
      extentListMap.put(docField, extRange);
      return true;
    } else {
      return false;
    }
  }

  public FieldPositionRange getFieldPositionRange(DocumentField docField) {
    return extentListMap.get(docField);
  }

  public boolean hasField(DocumentField docField) {
    return extentListMap.containsKey(docField);
  }
}
