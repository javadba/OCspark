package org.openchai.tensorflow.api;

import org.junit.*;

import static org.openchai.tensorflow.api.JsonUtils.*;
import static org.openchai.tensorflow.api.Logger.*;

import java.security.MessageDigest;

public class PcieDMAClientTest {
  static PcieDMAServer server = null;
  static PcieDMAClient client = null;

  public static void main(String[] args) throws Exception {
    PcieDMAClientTest test = new PcieDMAClientTest();
    test.setUp();
    test.battery();
    test.tearDown();
  }


  byte[] md5(byte[] arr) {
    MessageDigest md = null;
    try {                               
      md = MessageDigest.getInstance("MD5");
    } catch(Exception e) {
      e.printStackTrace();
    }
    md.update(arr);
    return md.digest();
  }

  @Test
  public void battery() throws Exception {
    info("Starting battery ..");
    // setUp();
    setupChannel();
    prepareWrite();
    write();
    completeWrite();
    prepareRead();
    read();
    completeRead();
//    shutdownChannel();
  }

  @BeforeClass
  public static void setUp() throws Exception {
    // TODO: set up server properly
    server = new PcieDMAServer();
    server.setupChannel(toJson(new TestJson("setupChannel","something")));
    server.register(new TensorFlowIf.DMACallback() {
                      @Override
                      public DMAStructures.WriteResultStruct dataSent() {
                        return null;
                      }

                      @Override
                      public DMAStructures.ReadResultStruct dataReceived() {
                        return null;
                      }
                    });
    Thread.sleep(100);
    client = new PcieDMAClient();

  }

  @AfterClass
  public static void tearDown() throws Exception {
    client.shutdownChannel("blah");
    client = null;
  }

  public static class TestJson {
    public TestJson(String fn, String payload) {
      this.fn = fn;
      this.payload = payload;
    }

    public String fn;
    public String payload;
  }

  public void setupChannel() throws Exception {
    client.setupChannel(toJson(new TestJson("setupChannel","something")));
  }

  public void prepareWrite() throws Exception {
    client.prepareWrite(toJson(new TestJson("prepareWrite","something")));

  }

  public void write() throws Exception {
    byte[] data = "some data to write".getBytes();
    client.write(toJson(new TestJson("write","something")), data,
            md5(data));
  }

  public void completeWrite() throws Exception {
    client.completeWrite(toJson(new TestJson("completeWrite","something")));

  }

  public void prepareRead() throws Exception {
    client.prepareRead(toJson(new TestJson("prepareRead","something")));

  }

  public void read() throws Exception {

  }

  public void completeRead() throws Exception {
    client.completeRead(toJson(new TestJson("completeRead","something")));

  }

  public void shutdownChannel() throws Exception {
    client.shutdownChannel(toJson(new TestJson("shutdownChannel","something")));

  }

  public void readLocal() throws Exception {
    // TODO: I'm not certain how to represent this dataptr
    client.readLocal("some dataptr".getBytes());
  }


}