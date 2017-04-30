package org.openchai.tensorflow.api;

public class TcpDMAClient extends DMAClientBase implements TensorFlowIf.DMAClient {

  @Override
  public String setupChannel(String setupJson) {
    return super.setupChannel(setupJson);
  }

  @Override
  public String prepareWrite(String configJson) {
    return super.prepareWrite(configJson);
  }

  @Override
  public DMAStructures.WriteResultStruct write(String configJson, byte[] data, byte[] md5) {
    return super.write(configJson, data, md5);
  }

  @Override
  public DMAStructures.WriteResultStruct completeWrite(String configJson) {
    return super.completeWrite(configJson);
  }

  @Override
  public String prepareRead(String configJson) {
    return super.prepareRead(configJson);
  }

  @Override
  public DMAStructures.ReadResultStruct read(String configJson) {
    return super.read(configJson);
  }

  @Override
  public String completeRead(String configJson) {
    return super.completeRead(configJson);
  }

  @Override
  public String shutdownChannel(String setupJson) {
    return super.shutdownChannel(setupJson);
  }

  @Override
  public byte[] readLocal(byte[] dataptr) {
    return super.readLocal(dataptr);
  }
}
