package org.openchai.tensorflow.api;

import org.junit.*;

import static org.openchai.tensorflow.api.JsonUtils.toJson;
import static org.openchai.tensorflow.api.Logger.info;
import static org.openchai.tensorflow.api.Logger.p;

public class PcieDMAServerTest {

  static PcieDMAServer server = null;
  @BeforeClass
  public static void setUp() throws Exception {
    server = new PcieDMAServer();
  }

  @AfterClass
  public static void tearDown() throws Exception {
//    server.shutdownChannel("blah");
    server = null;
  }

  @Test
  public void battery() throws Exception {
    info("Starting Server battery ..");
    setUp();
    setupChannel();
    prepareWrite();
    write();
    completeWrite();
    prepareRead();
    read();
    completeRead();
    shutdownChannel();
    tearDown();
  }

  public void setupChannel() throws Exception {
    String res = server.setupChannel(toJson("blah"));
    p("setupChannel result: %s", res);
  }

  public void register() throws Exception {
    String res = server.register(new TensorFlowIf.DMACallback() {
      @Override
      public DMAStructures.WriteResultStruct dataSent() {
        return null;
      }

      @Override
      public DMAStructures.ReadResultStruct dataReceived() {
        return null;
      }
    });
    p("register result: %s", res);
  }

  public void prepareWrite() throws Exception {
    String x = server.prepareWrite(toJson("PrepareSend"));
    p("prepareWrite result: %s", x);

  }

  public void write() throws Exception {
    DMAStructures.WriteResultStruct x = server.write(toJson("blah"),
            "hello there".getBytes());
    p("write result: %s", x);
  }

  public void completeWrite() throws Exception {
    String x = server.prepareWrite(toJson("completeSend"));
    p("completeWrite  result: %s", x);

  }

  public void prepareRead() throws Exception {
    String x = server.prepareWrite(toJson("PrepareRcv"));
    p("prepareRead result: %s", x);

  }

  public void read() throws Exception {
    DMAStructures.ReadResultStruct x = server.read(toJson("read"));
    p("read result: %s", x);

  }

  public void completeRead() throws Exception {
    DMAStructures.ReadResultStruct x = server.read(toJson("completeRcv"));
    p("completeRead result: %s", x);

  }

  public void shutdownChannel() throws Exception {
    String x = server.shutdownChannel(toJson("blah"));
    p("shutdownChannel result: %s", x);

  }

  public void readLocal() throws Exception {
    byte[] dataPtr = "I am a dataPointer".getBytes();
    byte[] x = server.readLocal(dataPtr);
    p("readLocal result: %s", x);

  }

}