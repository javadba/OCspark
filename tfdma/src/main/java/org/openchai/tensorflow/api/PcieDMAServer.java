package org.openchai.tensorflow.api;

import static org.openchai.tensorflow.api.TensorFlowIf.*;

public class PcieDMAServer extends DMAServerBase implements TensorFlowIf.DMAServer {

  public PcieDMAServer() {
    String parentDir = System.getProperty("user.dir");
    if (!parentDir.contains("tfdma")) {
      parentDir += "/tfdma";
    }
    String libpath = String.format("%s/%s",parentDir,"src/main/cpp/dmaserver.dylib");
    System.err.println("Loading DMA native library " + libpath + " ..");
    System.load(libpath);

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
  public String prepareWrite(String configJson) {
    super.prepareWrite(configJson);
    return prepareWriteN(configJson);
  }


  @Override
  public DMAStructures.WriteResultStruct write(String configJson, byte[] dataPtr) {
    super.write(configJson, dataPtr);
    return writeN(configJson, dataPtr);
  }

  @Override
  public DMAStructures.WriteResultStruct completeWrite(String configJson) {
    super.completeWrite(configJson);
    return completeWriteN(configJson);
  }


  @Override
  public String prepareRead(String configJson) {
    super.prepareRead(configJson);
    return prepareReadN(configJson);
  }


  @Override
  public DMAStructures.ReadResultStruct read(String configJson) {
    super.read(configJson);
    return readN(configJson);
  }

  @Override
  public DMAStructures.ReadResultStruct completeRead(String configJson) {
    super.completeRead(configJson);
    return completeReadN(configJson);
  }


  @Override
  public String shutdownChannel(String shutdownJson) {
    super.shutdownChannel(shutdownJson);
    return shutdownChannelN(shutdownJson);
  }

  @Override
  public byte[] readLocal(byte[] dataPtr) {
    super.readLocal(dataPtr);
    return readLocalN(dataPtr);
  }

  native String setupChannelN(String setupJson);

  native String registerN(DMACallback callbackIf);

  native String prepareWriteN(String configJson);

  native DMAStructures.WriteResultStruct writeN(String configJson, byte[] dataPtr);

  native DMAStructures.WriteResultStruct completeWriteN(String configJson);

  native String prepareReadN(String configJson);

  native DMAStructures.ReadResultStruct readN(String configJson);

  native DMAStructures.ReadResultStruct completeReadN(String configJson);

  native String shutdownChannelN(String shutdownJson);

  native byte[] readLocalN(byte[] dataPtr);

}

