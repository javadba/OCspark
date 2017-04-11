package org.openchai.tensorflow.api;

import static org.openchai.tensorflow.api.TensorFlowIf.*;

public class PcieDMAServer extends DMAServerBase implements TensorFlowIf.DMAServer {

  public PcieDMAServer() {
    String libpath = String.format("%s/%s",System.getProperty("user.dir"),"./src/main/cpp/dmaserver.dylib");
//    String libpath = "dmaserver.dylib";
    System.err.println("Loading DMA native library " + libpath + " ..");
    System.load(libpath);

//    System.loadLibrary(libpath);
  }

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
  public String prepareSend(String configJson) {
    super.prepareSend(configJson);
    return prepareSendN(configJson);
  }


  @Override
  public DMAStructures.SendResultStruct sendData(String configJson, byte[] dataPtr) {
    super.sendData(configJson, dataPtr);
    return sendDataN(configJson, dataPtr);
  }

  @Override
  public DMAStructures.SendResultStruct completeSend(String configJson) {
    super.completeSend(configJson);
    return completeSendN(configJson);
  }


  @Override
  public String prepareRcv(String configJson) {
    super.prepareRcv(configJson);
    return prepareRcvN(configJson);
  }


  @Override
  public DMAStructures.RcvResultStruct rcvData(String configJson) {
    super.rcvData(configJson);
    return rcvDataN(configJson);
  }

  @Override
  public DMAStructures.RcvResultStruct completeRcv(String configJson) {
    super.completeRcv(configJson);
    return completeRcvN(configJson);
  }


  @Override
  public String shutdownChannel(String shutdownJson) {
    super.shutdownChannel(shutdownJson);
    return shutdownChannelN(shutdownJson);
  }

  @Override
  public byte[] readLocal(byte[] dataPtr) {
    super.readLocal(dataPtr);
    return readDataN(dataPtr);
  }

  native String setupChannelN(String setupJson);

  native String registerN(DMACallback callbackIf);

  native String prepareSendN(String configJson);

  native DMAStructures.SendResultStruct sendDataN(String configJson, byte[] dataPtr);

  native DMAStructures.SendResultStruct completeSendN(String configJson);

  native String prepareRcvN(String configJson);

  native DMAStructures.RcvResultStruct rcvDataN(String configJson);

  native DMAStructures.RcvResultStruct completeRcvN(String configJson);

  native String shutdownChannelN(String shutdownJson);

  native byte[] readDataN(byte[] dataPtr);

}

