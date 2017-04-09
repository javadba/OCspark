package org.openchai.tensorflow.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openchai.tcp.util.FileUtils;

import static org.openchai.tensorflow.api.JsonUtils.*;
import static org.openchai.tensorflow.api.Logger.*;

import static org.junit.Assert.*;

public class PcieDMAClientTest {
  PcieDMAServer server = null;
  PcieDMAClient client = null;

  public static void main(String[] args) throws Exception {
    PcieDMAClientTest test = new PcieDMAClientTest();
    test.battery();
  }

  @Test
  public void battery() throws Exception {
    info("Starting battery ..");
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

  @Before
  public void setUp() throws Exception {
    // TODO: set up server properly
    server = new PcieDMAServer();
    server.setupChannel(toJson(new TestJson("setupChannel","something")));
    server.register(new TensorFlowIf.DMACallback() {
                      @Override
                      public DMAStructures.SendResultStruct dataSent() {
                        return null;
                      }

                      @Override
                      public DMAStructures.RcvResultStruct dataReceived() {
                        return null;
                      }
                    });
    Thread.sleep(100);
    client = new PcieDMAClient();

  }

  @After
  public void tearDown() throws Exception {
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

  @Test
  public void setupChannel() throws Exception {
    client.setupChannel(toJson(new TestJson("setupChannel","something")));
  }

  @Test
  public void prepareWrite() throws Exception {
    client.prepareWrite(toJson(new TestJson("prepareWrite","something")));

  }

  @Test
  public void write() throws Exception {
    byte[] data = "some data to write".getBytes();
    client.write(toJson(new TestJson("write","something")), data,
            FileUtils.md5(data));
  }

  @Test
  public void completeWrite() throws Exception {
    client.completeWrite(toJson(new TestJson("completeWrite","something")));

  }

  @Test
  public void prepareRead() throws Exception {
    client.prepareRead(toJson(new TestJson("prepareRead","something")));

  }

  @Test
  public void read() throws Exception {

  }

  @Test
  public void completeRead() throws Exception {
    client.completeRead(toJson(new TestJson("completeRead","something")));

  }

  @Test
  public void shutdownChannel() throws Exception {
    client.shutdownChannel(toJson(new TestJson("shutdownChannel","something")));

  }

  @Test
  public void readData() throws Exception {
    // TODO: I'm not certain how to represent this dataptr
    client.readData("some dataptr".getBytes());
  }


}