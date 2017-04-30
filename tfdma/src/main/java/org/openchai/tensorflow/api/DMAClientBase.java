package org.openchai.tensorflow.api;

import static org.openchai.tensorflow.api.JsonUtils.toJson;

public class DMAClientBase implements TensorFlowIf.DMAClient {
  public static class Result {
    public Result(String fn, int rc, String msg) {
      this.fn = fn;
      this.rc = rc;
      this.msg = msg;
    }

    public String fn;
    public int rc;
    public String msg;
  }

  @Override
  public String setupChannel(String setupJson) {
    return toJson(new Result("SetupChannel", 1, "Foo"));
  }

  @Override
  public String prepareWrite(String configJson) {
    return toJson(new Result("prepareWrite", 1, "Foo"));
  }

  @Override
  public DMAStructures.WriteResultStruct write(String configJson, byte[] data, byte[] md5) {
    return new DMAStructures.WriteResultStruct();
  }

  @Override
  public DMAStructures.WriteResultStruct completeWrite(String configJson) {
    return new DMAStructures.WriteResultStruct();
  }

  @Override
  public String prepareRead(String configJson) {
        return toJson(new Result("prepareRead", 1, "Foo"));
  }

  @Override
  public DMAStructures.ReadResultStruct read(String configJson) {
    return null;
  }

  @Override
  public String completeRead(String configJson) {
        return toJson(new Result("completeRead", 1, "Foo"));
  }

  @Override
  public String shutdownChannel(String setupJson) {
        return toJson(new Result("shutdownChannel", 1, "Foo"));
  }

  @Override
  public byte[] readLocal(byte[] dataptr) {
    return new byte[0];
  }
}
