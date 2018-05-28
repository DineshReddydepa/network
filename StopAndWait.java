

import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class StopAndWait extends JFrame {
	
	private static int m = 532;					// Maximum payload size
	private static InetAddress receiverAddress;	// Receiver address
	private static int receiverPort=0;			// Receiver port 
	private static int myPort=0 ;					// My port
	private static DatagramSocket datagramSocket;
	private static int packetLosses;			// Percentage of packet losses
	private static int timeout = 1;				// Timeout
	
	private Button bttnStart;
	private JLabel lblAddress, lblPort, lblPacketLosses, lblStatistic, lblTime, lblUNit, lblLosses;
	private JTextField txtAddress, txtPort, txtPacketLosses;
	
	
	public StopAndWait(){
		super("StopAndWait");
		setLayout(null);
		
		lblAddress = new JLabel("IPAddress:");
		lblAddress.setBounds(10, 10, 100, 20);
		add(lblAddress);
		txtAddress = new JTextField("");
		txtAddress.setBounds(100, 10, 130, 20);
		add(txtAddress);
		
		lblPort = new JLabel("Port:");
		lblPort.setBounds(10, 40, 100, 20);
		add(lblPort);
		txtPort = new JTextField("");
		txtPort.setBounds(60, 40, 55, 20);
		add(txtPort);
		
		lblPacketLosses = new JLabel("% Packet Losses:");
		lblPacketLosses.setBounds(10, 70, 150, 20);
		add(lblPacketLosses);
		txtPacketLosses = new JTextField("");
		txtPacketLosses.setBounds(150, 70, 40, 20);
		add(txtPacketLosses);
		
		bttnStart = new Button("Start");
		bttnStart.setBounds(230, 100,80,30);
		bttnStart.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
				    	int port = Integer.parseInt(txtPort.getText());
				    	int losses = Integer.parseInt(txtPacketLosses.getText());
				    	String address = txtAddress.getText();
				    	
				    	if (txtPort.getText().replaceAll(" ", "").length()==0 || 
				    			txtPacketLosses.getText().replaceAll(" ", "").length()==0){
					    	JOptionPane.showMessageDialog(null,
								    "The Port and % Packet Losses fields can not be empty",
								    "Error",
								    JOptionPane.ERROR_MESSAGE);
					    	
				    	}else if (port < 0){
				    		JOptionPane.showMessageDialog(null,
								    "Invalid port",
								    "Error",
								    JOptionPane.ERROR_MESSAGE);
				    		
				    	}else if (losses < 0 || losses > 99){
				    		JOptionPane.showMessageDialog(null,
								    "Invalid % Packet Losses",
								    "Error",
								    JOptionPane.ERROR_MESSAGE);
				    	}else{
				    		
				    		if (address.equals("")){
				    			myPort = port;
				    		}else{
				    			try {
				    				receiverAddress = InetAddress.getByName(address);
				    			} catch (UnknownHostException exc) {
				    				exc.printStackTrace();
				    				System.out.println("Invalid address: "+receiverAddress);
				    				System.exit(1);
				    			}
				    			receiverPort = port;
				    		}
				    		packetLosses = losses;
				    		start();
				    		
				    	}
				    	
				    }});
					
		add(bttnStart);
		
		lblStatistic = new JLabel("Statistic Data");
		lblStatistic.setBounds(10, 150, 300, 20);
		add(lblStatistic);
		
		lblTime = new JLabel("");
		lblTime.setBounds(10, 180, 300, 20);
		add(lblTime);
		
		lblUNit = new JLabel("");
		lblUNit.setBounds(10, 210, 300, 20);
		add(lblUNit);
		
		lblLosses = new JLabel("");
		lblLosses.setBounds(10, 240, 300, 20);
		add(lblLosses);
		
		setSize(340,310);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	
	
	public void start(){
			int losses = 0;
			
			try {
				
				//Read from a file named DataSent.txt
				File inputFile = new File("COSC635_P2_DataSent.txt");
				InputStream input = new FileInputStream(inputFile);
				int inputfileLength = (int) inputFile.length();

				
				if(myPort!=0)
					datagramSocket = new DatagramSocket(myPort);
				else
					datagramSocket = new DatagramSocket();
					
				
				int nextPacket = 0;
				int w=0;
				int next = 0;
				
				//Reader of incoming messages
				Reader reader;
				reader = new Reader(datagramSocket);
				reader.start();
				
				long initTime = System.currentTimeMillis();
				
				byte[] buffer = new byte[m];
				while (next < inputfileLength){
					
					if (next == nextPacket + m || w == 0) {
						buffer = new byte[m];
						nextPacket = next;
						//Copy packet from the file to the buffer
						input.read(buffer);			
					}

					if (receiverPort == 0){
						receiverAddress= reader.getIncomingAddress();
						receiverPort = reader.getIncomingPort();
						
					}
						
					Random generator = new Random(System.currentTimeMillis());
					int randomInt = generator.nextInt(100);
					
					//If the random number generated is less than the user input number
					// then the current protocol data unit wont be sent to simulate a packet loss.
					if ( randomInt >= packetLosses){
						
						byte[] arrayNumSeq = ByteBuffer.allocate(4).putInt(nextPacket).array();
						byte[] packetType = ByteBuffer.allocate(4).putInt(1).array();
						byte[] packet = new byte[8+m];

						//Add the number of sequence to the packet
						packet[0] = arrayNumSeq[0];
						packet[1] = arrayNumSeq[1];
						packet[2] = arrayNumSeq[2];
						packet[3] = arrayNumSeq[3];
						
						//Add the type of packet = 1
						packet[4] = packetType[0];
						packet[5] = packetType[1];
						packet[6] = packetType[2];
						packet[7] = packetType[3];

						//Add the data to the packet
						for(int j = 8; j < m+8 ; j++)
							packet[j] = buffer[j-8];

						
						//System.out.println("Sender: Packet Number of sequence "+nextPacket);
						String p = new String(buffer);
						//System.out.println("Sender: Data of packet= "+p+"\n");

						//Send packet
						DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, receiverAddress,receiverPort);
						datagramSocket.send(datagramPacket);
						w++;
						
					}else
						losses++;
						
					
					
					// Wait for the ack
					try{
					    Thread.sleep(timeout*1000);
					    
					}catch(InterruptedException e){
						e.printStackTrace();
						System.out.println("Error with Thread.sleep");
						System.exit(1);
					}

					next = reader.getACK();
					
				}
				
				//System.out.println("Sender: Transmission finished");
				long endTime = System.currentTimeMillis();
				
				//Send Transmission finished
				while(reader.getACK() != -2){
					byte[] arrayNumSeq = ByteBuffer.allocate(4).putInt(-2).array();
					byte[] packetType = ByteBuffer.allocate(4).putInt(1).array();
					byte[] packet = new byte[8+m];
					
					//Add the number of sequence to the packet
					packet[0] = arrayNumSeq[0];
					packet[1] = arrayNumSeq[1];
					packet[2] = arrayNumSeq[2];
					packet[3] = arrayNumSeq[3];
					
					//Add the type of packet = 1
					packet[4] = packetType[0];
					packet[5] = packetType[1];
					packet[6] = packetType[2];
					packet[7] = packetType[3];
					
					DatagramPacket answer = new DatagramPacket(packet, packet.length,receiverAddress,receiverPort);
					datagramSocket.send(answer);
					
					// Wait for the ack
					try{
					    Thread.sleep(timeout*1000);
					
					}catch(InterruptedException e){
						e.printStackTrace();
						System.out.println("Error with Thread.sleep");
						System.exit(1);
					}
				}
				
				//Close files
				input.close();
				int time = ((int)(endTime - initTime)/1000);
				int days = time / 86400;
				int res = time % 86400;
				int hours = res / 3600;
				res = res % 3600;
				int min = res / 60;
				int sec = res % 60;
				
				//Show Statictic data
				lblTime.setText("Time: "+days+"days "+hours+"hours "+min+"minutes "+sec+"seconds");
				lblUNit.setText("Data units sent: "+w);
				lblLosses.setText("Packet losses: "+losses);
				SwingUtilities.updateComponentTreeUI(this);
				
				
				System.out.println("\n\n-------Statistic-------");
				System.out.println("Time: "+days+"days "+hours+"hours "+min+"minutes "+sec+"seconds");
				System.out.println("Data units sent: "+w);
				System.out.println("Packet losses: "+losses);
				
				
			} catch (SocketException e) {
				e.printStackTrace();
				System.out.println("Error with DatagramSocket");
				System.exit(1);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out.println("Error open the file: DataSent.txt");
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(e);
				System.exit(1);
			}
			
			//System.out.println("Sender: end");
	}
	public static void main(String[] args){
		StopAndWait inter = new StopAndWait();
	}
}
