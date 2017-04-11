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
  public String prepareSend(String configJson) {
    info(f("prepareSend for %s", configJson));
    return toJson(f("prepareSend completed for %s", configJson));
  }

  @Override
  public DMAStructures.SendResultStruct completeSend(String configJson) {
    info(f("completeSend for %s", configJson));
    return new DMAStructures.SendResultStruct();
  }

  @Override
  public DMAStructures.SendResultStruct sendData(String configJson, byte[] dataPtr) {
    info(f("sendData for %s and dataLen=%d", configJson, dataPtr.length));
    return new DMAStructures.SendResultStruct();
  }

  @Override
  public String prepareRcv(String configJson) {
    info(f("prepareRcv for %s", configJson));
    return toJson(f("prepareRcv completed for %s", configJson));
  }

  public DMAStructures.RcvResultStruct rcvData(String configJson) {
    info(f("rcvData for %s", configJson));
    return new DMAStructures.RcvResultStruct();
  }

  @Override
  public DMAStructures.RcvResultStruct completeRcv(String configJson) {
    info(f("completeRcv for %s", configJson));
    return new DMAStructures.RcvResultStruct();
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
