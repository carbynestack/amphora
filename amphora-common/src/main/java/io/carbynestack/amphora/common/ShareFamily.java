package io.carbynestack.amphora.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public enum ShareFamily {
  HEMI("Hemi", false, 16),
  COWGEAR("CowGear", true, 32);

  private static Map<String, ShareFamily> mapping = loadMapping();

  @Getter private final String familyName;

  @Getter private final boolean verifiable;

  @Getter private final int shareSize;

  public static ShareFamily getShareFamilyByName(String familyName) {
    return mapping.get(familyName);
  }

  private static Map<String, ShareFamily> loadMapping() {
    Map<String, ShareFamily> mapping = new HashMap<>();
    mapping.put(HEMI.familyName, HEMI);
    mapping.put(COWGEAR.familyName, COWGEAR);

    return mapping;
  }
}
