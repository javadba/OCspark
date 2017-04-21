package org.openchai.tensorflow.api;

import static org.openchai.tensorflow.api.JsonUtils.toJson;
import static org.openchai.tensorflow.api.Logger.f;
import static org.openchai.tensorflow.api.Logger.info;

public class DMAServerBase implements TensorFlowIf.DMAServer {
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

  public String setupChannel(String setupJson) {
    info(f("SetupChannel for %s", setupJson));
    return toJson(new Result("SetupChannel", 1, "Foo"));
  }

  public String register(TensorFlowIf.DMACallback callbackIf) {
    info(f("register DMACallback invoked for ", callbackIf.getClass().getName()));
    return toJson("register not implemented");
  }

  @Override
  public String prepareWrite(String configJson) {
    info(f("prepareWrite for %s", configJson));
    return toJson(f("prepareWrite completed for %s", configJson));
  }

  @Override
  public DMAStructures.WriteResultStruct completeWrite(String configJson) {
    info(f("completeWrite  for %s", configJson));
    return new DMAStructures.WriteResultStruct();
  }

  @Override
  public DMAStructures.WriteResultStruct write(String configJson, byte[] dataPtr) {
    info(f("write for %s and dataLen=%d", configJson, dataPtr.length));
    return new DMAStructures.WriteResultStruct();
  }

  @Override
  public String prepareRead(String configJson) {
    info(f("prepareRead for %s", configJson));
    return toJson(f("prepareRead completed for %s", configJson));
  }

  public DMAStructures.ReadResultStruct read(String configJson) {
    info(f("read for %s", configJson));
    return new DMAStructures.ReadResultStruct();
  }

  @Override
  public DMAStructures.ReadResultStruct completeRead(String configJson) {
    info(f("completeRead for %s", configJson));
    return new DMAStructures.ReadResultStruct();
  }

  public String shutdownChannel(String shutdownJson) {
    info(f("ShutdownCannel for %s", shutdownJson));
    return toJson(f("ShutdownChannel completed for %s", shutdownJson));
  }

  public byte[] readLocal(byte[] dataPtr) {
    info(f("readLocal for datalen=%d", dataPtr.length));
    return toJson("readLocal not implemented").getBytes();
  }
}
