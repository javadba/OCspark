package org.openchai.tensorflow.api;

import static org.openchai.tensorflow.api.Logger.f;
import static org.openchai.tensorflow.api.Logger.*;
import static org.openchai.tensorflow.api.TensorFlowIf.*;

public class TcpDMAServer extends DMAServerBase implements TensorFlowIf.DMAServer {

  @Override
  public String setupChannel(String setupJson) {
    return super.setupChannel(setupJson);
  }

  @Override
  public String register(DMACallback callbackIf) {
    return super.register(callbackIf);
  }

  @Override
  public DMAStructures.SendResultStruct sendData(String configJson, byte[] dataPtr) {
    return super.sendData(configJson, dataPtr);
  }

  @Override
  public DMAStructures.RcvResultStruct rcvData(String configJson) {
    return super.rcvData(configJson);
  }

  @Override
  public String shutdownChannel(String shutdownJson) {
    return super.shutdownChannel(shutdownJson);
  }

  @Override
  public byte[] readData(byte[] dataPtr) {
    return super.readData(dataPtr);
  }
}

