package org.openchai.tensorflow.api;

import static org.openchai.tensorflow.api.Logger.*;
import static org.openchai.tensorflow.api.TensorFlowIf.*;

public class PcieDMAServer extends DMAServerBase implements TensorFlowIf.DMAServer {
  @Override
  public String setupChannel(String setupJson) {
    super.setupChannel(setupJson);
    return setupChannelN(setupJson);
  }

  @Override
  public String register(DMACallback callbackIf) {
    super.register(callbackIf);
    return registerN(callbackIf);
  }

  @Override
  public DMAStructures.SendResultStruct sendData(String configJson, byte[] dataPtr) {
    super.sendData(configJson, dataPtr);
    return sendDataN(configJson, dataPtr);
  }

  @Override
  public DMAStructures.RcvResultStruct rcvData(String configJson) {
    super.rcvData(configJson);
    return rcvDataN(configJson);
  }

  @Override
  public String shutdownChannel(String shutdownJson) {
    super.shutdownChannel(shutdownJson);
    return shutdownChannelN(shutdownJson);
  }

  @Override
  public byte[] readData(byte[] dataPtr) {
    super.readData(dataPtr);
    return readDataN(dataPtr);
  }

  native String setupChannelN(String setupJson);

  native String registerN(DMACallback callbackIf);

  native DMAStructures.SendResultStruct sendDataN(String configJson, byte[] dataPtr);

  native DMAStructures.RcvResultStruct rcvDataN(String configJson);

  native String shutdownChannelN(String shutdownJson);

  native byte[] readDataN(byte[] dataPtr);

}

