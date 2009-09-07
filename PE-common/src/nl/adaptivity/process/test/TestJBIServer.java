package nl.adaptivity.process.test;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

import javax.jbi.JBIException;
import javax.jbi.messaging.MessageExchange;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.xml.namespace.QName;

import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpoint;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.junit.After;
import org.junit.Before;

import nl.adaptivity.jbi.components.fileServer.FSServiceUnit;
import nl.adaptivity.jbi.components.fileServer.FileServerComponent;
import nl.adaptivity.process.engine.jbi.JBIProcessEngine;


public class TestJBIServer extends JPanel{
  
  private static final long serialVersionUID = -2126657729924810063L;
  private static final URI IN_OUT = URI.create("http://www.w3.org/2004/08/wsdl/in-out");
  private static final QName FILESERVICE = new QName("http://adaptivity.nl/PEUserMessages","File1");
  private static final String FILEENDPOINT = "endpoint";
  private static final QName PESERVICE = new QName("http://adaptivity.nl/ProcessEngine/","ProcessEngine");
  private static final String PEENDPOINT = "http";

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    final TestJBIServer server = new TestJBIServer();
    final Thread shutdownhook = new Thread() {
      @Override
      public void run() {
        try {
          server.tearDown();
        } catch (JBIException e) {
          e.printStackTrace();
        }
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdownhook);
    
    try{
      try {
        server.setUp();
        
        server.addStuff();
      
        server.run();
      
      } finally {
        Runtime.getRuntime().removeShutdownHook(shutdownhook);
        server.tearDown();
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }


  private volatile boolean aStopped;

  
  public TestJBIServer() {
    this.setLayout(new BorderLayout());
    JButton stopButton = new JButton("Stop");
    stopButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent pE) {
        aStopped=true;
        synchronized(TestJBIServer.this) {
          TestJBIServer.this.notifyAll();
        }
        Container frame = getParent();
        while (!(frame instanceof JFrame)) {
          frame = frame.getParent();
        }
        ((JFrame) frame).dispose();
      }
      
    });
    
    add(stopButton, BorderLayout.CENTER);
  }
  
  
  private void addStuff() throws JBIException, MalformedURLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    HttpComponent http = new HttpComponent(); 
    HttpEndpoint ep = new HttpEndpoint();
    ep.setRole(MessageExchange.Role.CONSUMER);
    ep.setService(FILESERVICE);
    ep.setEndpoint(FILEENDPOINT);
    ep.setLocationURI("http://localhost:8192/msgHandler/");
    ep.setDefaultMep(IN_OUT);
    ep.setRest(true);
    http.setEndpoints(new HttpEndpoint[] {ep});
    jbi.activateComponent(http, "servicemix-http-ext");
    
    FileServerComponent fsc = new FileServerComponent();
    FSServiceUnit fsc_su = new FSServiceUnit();
    fsc_su.setName("fileServer1");
    fsc_su.setRootPath(new File("../PEUserMessageHandler/war").toURI().toURL());
    fsc_su.setEndpoint(FILEENDPOINT);
    fsc_su.setService(FILESERVICE);
    fsc.addServiceUnit(fsc_su);
    jbi.activateComponent(fsc, "FileServer");
    fsc.getServiceUnitManager().start(fsc_su.getName());
    
    JBIProcessEngine pe = new JBIProcessEngine();
    jbi.activateComponent(pe, "ProcessEngine");
    
    HttpEndpoint ep2 = new HttpEndpoint();
    ep2.setRole(MessageExchange.Role.CONSUMER);
    ep2.setService(PESERVICE);
    ep2.setEndpoint(PEENDPOINT);
    ep2.setLocationURI("http://localhost:8192/ProcessEngine");
    ep2.setDefaultMep(IN_OUT);
    ep2.setRest(true);
  }


  private void run() {
    JFrame frame = new JFrame();
    frame.add(this);
    frame.pack();
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    
    while (!aStopped) {
      try {
        synchronized(this) {
          wait(300000);// Just do nothing for 5 minutes;
        }
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }


  private JBIContainer jbi;

  @After
  public void tearDown() throws JBIException {
    jbi.shutDown();
  }

  @Before
  public void setUp() throws JBIException {
    jbi = new JBIContainer();
    jbi.setEmbedded(true);
    jbi.setUseMBeanServer(false);
    jbi.init();
    jbi.start();
  }

}
