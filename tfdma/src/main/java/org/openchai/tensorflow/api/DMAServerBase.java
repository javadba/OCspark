package org.openchai.tensorflow.api;

import static org.openchai.tensorflow.api.Logger.f;
import static org.openchai.tensorflow.api.Logger.info;

public class DMAServerBase implements TensorFlowIf.DMAServer {
  public String setupChannel(String setupJson) {
    info(f("SetupChannel for %s", setupJson));
    return f("SetupChannel completed for %s", setupJson);
  }

  public String register(TensorFlowIf.DMACallback callbackIf) {
    info(f("register DMACallback invoked for ", callbackIf.getClass().getName()));
    return "register not implemented";
  }

  public DMAStructures.SendResultStruct sendData(String configJson, byte[] dataPtr) {
    info(f("sendData for %s and dataLen=%d", configJson, dataPtr.length));
    return new DMAStructures.SendResultStruct();
  }

  public DMAStructures.RcvResultStruct rcvData(String configJson) {
    info(f("rcvData for %s", configJson));
    return new DMAStructures.RcvResultStruct();
  }

  public String shutdownChannel(String shutdownJson) {
    info(f("ShutdownCannel for %s", shutdownJson));
    return f("ShutdownChannel completed for %s", shutdownJson);
  }

  public byte[] readData(byte[] dataPtr) {
    info(f("readData for datalen=%d", dataPtr.length));
    return "readData not implemented".getBytes();
  }
}
