package org.openchai.tensorflow.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.openchai.tensorflow.api.JsonUtils.toJson;
import static org.openchai.tensorflow.api.Logger.info;
import static org.openchai.tensorflow.api.Logger.p;

public class PcieDMAServerTest {

  PcieDMAServer server = null;
  @Before
  public void setUp() throws Exception {
    server = new PcieDMAServer();
  }

  @After
  public void tearDown() throws Exception {
//    server.shutdownChannel("blah");
    server = null;
  }

  @Test
  public void battery() throws Exception {
    info("Starting Server battery ..");
    setUp();
    setupChannel();
    prepareSend();
    sendData();
    completeSend();
    prepareRcv();
    rcvData();
    completeRcv();
    shutdownChannel();
    tearDown();
  }

  @Test
  public void setupChannel() throws Exception {
    String res = server.setupChannel(toJson("blah"));
    p("setupChannel result: %s", res);
  }

  @Test
  public void register() throws Exception {
    String res = server.register(new TensorFlowIf.DMACallback() {
      @Override
      public DMAStructures.SendResultStruct dataSent() {
        return null;
      }

      @Override
      public DMAStructures.RcvResultStruct dataReceived() {
        return null;
      }
    });
    p("register result: %s", res);
  }

  @Test
  public void prepareSend() throws Exception {
    String x = server.prepareSend(toJson("PrepareSend"));
    p("prepareSend result: %s", x);

  }

  @Test
  public void sendData() throws Exception {
    DMAStructures.SendResultStruct x = server.sendData(toJson("blah"),
            "hello there".getBytes());
    p("sendData result: %s", x);
  }

  @Test
  public void completeSend() throws Exception {
    String x = server.prepareSend(toJson("completeSend"));
    p("completeSend result: %s", x);

  }

  @Test
  public void prepareRcv() throws Exception {
    String x = server.prepareSend(toJson("PrepareRcv"));
    p("prepareRcv result: %s", x);

  }

  @Test
  public void rcvData() throws Exception {
    DMAStructures.RcvResultStruct x = server.rcvData(toJson("rcvData"));
    p("rcvData result: %s", x);

  }

  @Test
  public void completeRcv() throws Exception {
    DMAStructures.RcvResultStruct x = server.rcvData(toJson("completeRcv"));
    p("completeRcv result: %s", x);

  }

  @Test
  public void shutdownChannel() throws Exception {
    String x = server.shutdownChannel(toJson("blah"));
    p("shutdownChannel result: %s", x);

  }

  @Test
  public void readLocal() throws Exception {
    byte[] dataPtr = "I am a dataPointer".getBytes();
    byte[] x = server.readLocal(dataPtr);
    p("readLocal result: %s", x);

  }

}