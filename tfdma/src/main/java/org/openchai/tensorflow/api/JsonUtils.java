package org.openchai.tensorflow.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.openchai.tensorflow.api.Logger.eToString;

public class JsonUtils {
  static String toJson(Object obj) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return eToString(e);
    }
  }
}

