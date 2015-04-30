/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms. 
 */
package jdk.dio.example;

import java.io.IOException;


//import javax.microedition.midlet.MIDlet;

import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.gpio.GPIOPort;
import jdk.dio.gpio.PinEvent;
import jdk.dio.gpio.PinListener;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackImpl;
import javax.microedition.midlet.MIDlet;
/**
 * Demostrates basic GPIO functionality.
 * 
 * <p><b>Target Platforms</b>: emulator.</p>
 */ 
//public class GPIODemo  extends MIDlet implements MqttCallback{
public class GPIODemo extends MIDlet{
    static final String LED5_PIN_NAME = "LED 5";
    
    static final String LED6_PIN_NAME = "LED 6";
    
    static final int LED_PORT_ID = 8;
    static final String LED_PORT_NAME = "LEDS";
    
    boolean bFirst = false;
    boolean loopFlag = true;
    
    private GPIOPin led5 = null;
    private GPIOPort ledPort = null;
    private GPIOPin button2 = null;
    private GPIOPin button3 = null;
    
 // Private instance variables
    private MqttClient client;
    private MqttClient pubClinet;
    private MqttConnectOptions conOpt;
	private int qos = 2;
	private String broker = "localhost";
	private int port = 1883;
	private int sslport = 8890;
	private boolean SSL = false;
    
    public void startApp() {
        if(bFirst == false) {

            System.out.println("Starting GPIO Demo");
            try {
            	led5 = (GPIOPin)DeviceManager.open(new GPIOPinConfig(
                		6, 6, GPIOPinConfig.DIR_OUTPUT_ONLY, GPIOPinConfig.MODE_OUTPUT_PUSH_PULL,
                		GPIOPinConfig.TRIGGER_NONE, false));
                button2 = (GPIOPin)DeviceManager.open(new GPIOPinConfig(
                		2, 13, GPIOPinConfig.DIR_INPUT_ONLY, GPIOPinConfig.MODE_INPUT_PULL_UP,
                		GPIOPinConfig.TRIGGER_BOTH_EDGES, true));
                button3 = (GPIOPin)DeviceManager.open(new GPIOPinConfig(
                		6, 15, GPIOPinConfig.DIR_INPUT_ONLY, GPIOPinConfig.MODE_INPUT_PULL_UP,
                		GPIOPinConfig.TRIGGER_BOTH_EDGES, true));
                ledPort = (GPIOPort)DeviceManager.open(LED_PORT_ID);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Open pin or port fail");
                return;
            }
            
            System.out.println("set listener for button 2,3");
            try {
                button2.setInputListener(button2Listener);
                button3.setInputListener(button3Listener);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 

            bFirst = true;
        } else {
            System.out.println("GPIO Demo is already started...");
        }
    }
    
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
        bFirst = false;
        if(led5 != null) {
            try {
                led5.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            led5 = null;
        }
        if(ledPort != null){
            try {
                ledPort.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            ledPort = null;
        }
        if(button2 != null){
            try {
                button2.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            button2 = null;
        }
        if(button3 != null){
            try {
                button3.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            button3 = null;
        }
    }
    
    PinListener button2Listener = new PinListener() {
        public void valueChanged(PinEvent event) {
            GPIOPin pin = (GPIOPin)event.getDevice();
            System.out.println("listener2");
            try {
                System.out.println("value:  "+ pin.getValue());
                led5.setValue(!pin.getValue());
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        }
    };    
    
    PinListener button3Listener = new PinListener() {
        public void valueChanged(PinEvent event) {
            GPIOPin pin = (GPIOPin)event.getDevice();
            System.out.println("listener3");
            try {
                System.out.println("value:  "+ pin.getValue());
                if(!pin.getValue()) {
                    ledPort.setValue(3 - (led5.getValue() ? 1 : 0));
                }else{
                    ledPort.setValue(0);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
        }
    };
    
    
    //---------------MQTT-----------------------����
    
    /**
     * Publish / send a message to an MQTT server
     * @throws MqttException, IOException
     */
    protected void publish() throws MqttException, IOException {
    	String payload = "Message from blocking MQTTv3 Java client sample";
		String clientId = "SampleJavaV3_pubMIDP";
		String pubTopic = "Sample/Java/v3";
		
		String url = "tcp://" + broker + ":" + port;
		if (SSL) {
			url = "ssl://"  + broker + ":" + sslport;
		}

		pubClinet = new MqttClient(url, clientId);
		MqttCallbackImpl mcb = new MqttCallbackImpl();
		pubClinet.setCallback(mcb);
		pubClinet.connect();
		
    	// Create and configure a message
   		MqttMessage message = new MqttMessage(payload.getBytes());
    	message.setQos(qos);
 
    	// Send the message to the server, control is not returned until
    	// it has been delivered to the server meeting the specified
    	// quality of service.
    	pubClinet.publish(pubTopic, message);
    	
    	// Disconnect the client from the server
    	pubClinet.disconnect();
    	
    	// Reassign the pubClient to null
    	pubClinet = null;
    }

    /**
     * Subscribe to a topic on an MQTT server
     * Once subscribed this method waits for the messages to arrive from the server 
     * that match the subscription. It continues listening for messages until pin 3
     * is pressed
     * @throws IOException, MqttException
     */
	protected void subscribe() throws IOException, MqttException {
		String clientId = "SampleJavaV3_subMIDP";
		String subTopic = "Sample/#";
		String url = "tcp://" + broker + ":" + port;
		
		if (SSL) {
			url = "ssl://"  + broker + ":" + sslport;
		}
		
		if (client == null) {
			// Construct an MQTT blocking mode client
			client = new MqttClient(url, clientId);
			
			// Set this wrapper as the callback handler
			MqttCallbackImpl mcb = new MqttCallbackImpl();
			client.setCallback(mcb);
			
			// Construct the connection options object that contains connection parameters 
    		// such as cleansession and LWAT
			conOpt = new MqttConnectOptions();
			
			// Connect to the MQTT server
			client.connect(conOpt);
		}
		
    	// Subscribe to the requested topic
    	// The QOS specified is the maximum level that messages will be sent to the client at. 
    	// For instance if QOS 1 is specified, any messages originally published at QOS 2 will 
    	// be downgraded to 1 when delivering to the client but messages published at 1 and 0 
    	// will be received at the same level they were published at. 
		client.subscribe(subTopic, qos);
	}
	
    /**
     * Disconnect the client that has subscribed from the server
     * @throws MqttException
     */
	private void disconnect() throws MqttException {
		if (client != null) {
			// Disconnect the client from the server
			client.disconnect();
			client = null;
		}
	}

    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
	public void connectionLost(Throwable cause) {
		// Called when the connection to the server has been lost.
		// An application may choose to implement reconnection
		// logic at this point. This sample simply exits.
		System.out.println("Connection to " + broker + "lose!" + cause);
		System.exit(1);
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		// Called when a message has been delivered to the
		// server. The token passed in here is the same one
		// that was passed to or returned from the original call to publish.
		// This allows applications to perform asynchronous 
		// delivery without blocking until delivery completes.
		//
		// This sample demonstrates asynchronous deliver and 
		// uses the token.waitForCompletion() call in the main thread which
		// blocks until the delivery has completed. 
		// Additionally the deliveryComplete method will be called if 
		// the callback is set on the client
		// 
		// If the connection to the server breaks before delivery has completed
		// delivery of a message will complete after the client has re-connected.
		// The getPendinTokens method will provide tokens for any messages
		// that are still to be delivered.
	}

	/****************************************************************/
	/* End of MqttCallback methods                                  */
	/****************************************************************/


	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		// Called when a message arrives from the server that matches any
		// subscription made by the client		
		String time = "now";
		System.out.println("Time:\t" +time +
                           "  Topic:\t" + topic + 
                           "  Message:\t" + new String(message.getPayload()) +
                           "  QoS:\t" + message.getQos());
	}
}
