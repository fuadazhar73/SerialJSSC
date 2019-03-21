/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vimana.serial;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 *
 * @author Sora
 */
public class SerialJssc {

    static SerialPort serialPort;
    
    static String MYSQL_UNAME = "ttys";
    static String MYSQL_PASSWORD = "V1m@n@l0gy123!";
//    static String MYSQL_UNAME = "root";
//    static String MYSQL_PASSWORD = "rootadmin";
    static String MYSQL_URL = "jdbc:mysql://localhost:3306/ttys?autoReconnect=true&useSSL=false";
    
    String SERIAL_PORT = "/dev/ttyS0";
    int BAUD_RATE = 9600;
    int DATA_BITS = 8;
    int STOP_BITS = 1;
    int PARITY = 0;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println("Serial JSSC Start");
        SerialJssc serialJssc = new SerialJssc();
        serialJssc.printAvailablePort();
        
        try{  
            Class.forName("com.mysql.jdbc.Driver");  
        } catch (ClassNotFoundException cnfe) {
            System.out.println(cnfe);
        }
        
        serialJssc.readSerialPort();
        
        //File file = new File("C:\\Users\\Sora\\Documents\\inputsukses.txt");
        
        //System.out.println("Size : " + file.length());
    }
    
    private void printAvailablePort() {
        String[] portNames = SerialPortList.getPortNames();
        for(int i = 0; i < portNames.length; i++){
            System.out.println(portNames[i]);
        }
    }
    
    private void readSerialPort() {
        serialPort = new SerialPort("/dev/ttyS3"); 
        try {
            serialPort.openPort();//Open port
            serialPort.setParams(this.BAUD_RATE, this.DATA_BITS, this.STOP_BITS, this.PARITY);//Set params
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT); 
            int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;//Prepare mask
            serialPort.setEventsMask(mask);//Set mask
            serialPort.addEventListener(new SerialPortReader());//Add SerialPortEventListener
            System.out.println("Waiting bits on " + serialPort.getPortName());
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
        }
    }
    
    
    /*
    * In this class must implement the method serialEvent, through it we learn about 
    * events that happened to our port. But we will not report on all events but only 
    * those that we put in the mask. In this case the arrival of the data and change the 
    * status lines CTS and DSR
    */
   static class SerialPortReader implements SerialPortEventListener {
       
       StringBuilder message = new StringBuilder();
       ExecutorService executor = Executors.newSingleThreadExecutor();
       @Override
       public void serialEvent(SerialPortEvent event) {
           if(event.isRXCHAR()){//If data is available
               if (event.getEventValue() > 0) {
                   try {
                        byte buffer[] = serialPort.readBytes();
                        
                        for (byte b: buffer) {
                                System.out.println((char)b);
                                if ( (b == '\r' || b == '\n') && message.length() > 0) {
                                    String toProcess = message.toString();
                                    executor.submit(() -> {
                                        String threadName = Thread.currentThread().getName();
                                        System.out.println("Insert Line to DB in " + threadName + " with values " + toProcess);
                                        try {
                                            Connection conn = DriverManager.getConnection(MYSQL_URL,MYSQL_UNAME,MYSQL_PASSWORD);
                                            
                                            Calendar calendar = Calendar.getInstance();
                                            java.sql.Date now = new java.sql.Date(calendar.getTime().getTime());

                                            // the mysql insert statement
                                            String query = " insert into serial_data (data, created_date, downloaded)"
                                              + " values (?, ?, ?)";

                                            // create the mysql insert preparedstatement
                                            PreparedStatement preparedStmt = conn.prepareStatement(query);
                                            preparedStmt.setBytes(1, toProcess.getBytes(StandardCharsets.US_ASCII));
                                            preparedStmt.setTimestamp(2, new java.sql.Timestamp(now.getTime()));
                                            preparedStmt.setInt(3, 0);

                                            // execute the preparedstatement
                                            preparedStmt.execute();

                                            conn.close();
                                        } catch (SQLException sqle) {
                                            System.out.println(sqle);
                                        }
                                    });
                                    message.setLength(0);
                                }
                                else {
                                    message.append((char)b);
                                }
                        }                
                    }
                    catch (SerialPortException ex) {
                        System.out.println(ex);
                        System.out.println("serialEvent");
                    }
               }
               
           }
           else if(event.isCTS()){//If CTS line has changed state
               if(event.getEventValue() == 1){//If line is ON
                   System.out.println("CTS - ON");
               }
               else {
                   System.out.println("CTS - OFF");
               }
           }
           else if(event.isDSR()){///If DSR line has changed state
               if(event.getEventValue() == 1){//If line is ON
                   System.out.println("DSR - ON");
               }
               else {
                   System.out.println("DSR - OFF");
               }
           }
       }
   }
    
}
