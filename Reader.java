import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;


public class Reader extends Thread{

		private DatagramSocket datagramSocket;
		private int ack;				// Sequence number of the next packet
		private static int m = 532;		// Packet size
		private boolean endFile = false;
		private boolean transmissionFinished = false;
		private int myPort=0;			//port
		private static InetAddress incomingAddress;	//address
		private static int incomingPort=0;			//port 

		Reader(DatagramSocket socket){
			this.datagramSocket = socket;
			this.ack = 0;
		}
		
		public void run() {
			PrintWriter outputFile = null;
			int nextPacket = 0;
			
			try {
				//Received data put into a file named DataRecieved.txt
				outputFile = new PrintWriter("DataRecieved.txt","UTF-8");
				
			} catch (IOException e1) {
				e1.printStackTrace();
				System.out.println(e1);
				System.exit(1);
			}
			
			byte[] buffer = new byte[8+m];
			DatagramPacket incomingDatagram = new DatagramPacket(buffer, buffer.length);
		
			
			while(!transmissionFinished || !endFile) {
				
					try {
						// listen for incoming datagram packet
						datagramSocket.receive(incomingDatagram);
						
						byte[] packet = incomingDatagram.getData();

						byte[] numSequence = new byte[4];
						byte[] packetType = new byte[4];
						byte[] data = new byte[m];
						
						//get sequence number
						numSequence[0] = packet[0];
						numSequence[1] = packet[1];
						numSequence[2] = packet[2]; 
						numSequence[3] = packet[3];
						
						//get type of packet
						packetType[0] = packet[4];
						packetType[1] = packet[5];
						packetType[2] = packet[6]; 
						packetType[3] = packet[7];


						
						int seq = ByteBuffer.wrap(numSequence).getInt();
						int type = ByteBuffer.wrap(packetType).getInt();
						
						//The incomming message contain data
						if ( type == 1 && !endFile){
							
							
							
							
							//Transmission finished
							if (seq==-2){
								//System.out.println("Receiver: Transmission finished");
								nextPacket = 0;
								endFile = true;
								
								byte[] arrayNext = ByteBuffer.allocate(4).putInt(-2).array();
								byte[] arrayType = ByteBuffer.allocate(4).putInt(2).array();
								byte[] answer = new byte[8];
								
								//Add the number of sequence to the next expected packet
								answer[0] = arrayNext[0];
								answer[1] = arrayNext[1];
								answer[2] = arrayNext[2];
								answer[3] = arrayNext[3];
								
								//Add the type of packet = 2
								answer[4] = arrayType[0];
								answer[5] = arrayType[1];
								answer[6] = arrayType[2];
								answer[7] = arrayType[3];
								
								DatagramPacket answerDatagram = new DatagramPacket(answer, answer.length, incomingDatagram.getAddress(),incomingDatagram.getPort());
								datagramSocket.send(answerDatagram);
								
							//Expected packet
							}else if (seq == nextPacket){
								setIncomingPort(incomingDatagram.getPort());
								setIncomingAddress(incomingDatagram.getAddress());
								
								for (int i = 8 ; i < m+8 ; i++)
									data[i-8] = packet[i];
								
								String message = new String(data);
								//System.out.println("Receiver: Number of sequence= "+seq);
								//System.out.println("Receiver: Data of packet= "+message);
								outputFile.print(message.replaceAll("\00", ""));
								nextPacket = nextPacket + m;

								byte[] arrayNext = ByteBuffer.allocate(4).putInt(nextPacket).array();
								byte[] arrayType = ByteBuffer.allocate(4).putInt(2).array();
								byte[] answer = new byte[8];
								
								//Add the number of sequence to the next expected packet
								answer[0] = arrayNext[0];
								answer[1] = arrayNext[1];
								answer[2] = arrayNext[2];
								answer[3] = arrayNext[3];
								
								//Add the type of packet = 2
								answer[4] = arrayType[0];
								answer[5] = arrayType[1];
								answer[6] = arrayType[2];
								answer[7] = arrayType[3];
								
								
								DatagramPacket answerDatagram = new DatagramPacket(answer, answer.length, incomingDatagram.getAddress(),incomingDatagram.getPort());
								datagramSocket.send(answerDatagram);
								

							}else{
								//System.out.println("Receiver: I do not get the expected message. Recibido"+seq+ " esperado"+nextPacket);

								byte[] arrayNext = ByteBuffer.allocate(4).putInt(nextPacket).array();
								byte[] arrayType = ByteBuffer.allocate(4).putInt(2).array();
								byte[] answer = new byte[8];
								
								//Add the number of sequence to the next expected packet
								answer[0] = arrayNext[0];
								answer[1] = arrayNext[1];
								answer[2] = arrayNext[2];
								answer[3] = arrayNext[3];
								
								//Add the type of packet
								answer[4] = arrayType[0];
								answer[5] = arrayType[1];
								answer[6] = arrayType[2];
								answer[7] = arrayType[3];
								
								DatagramPacket answerDatagram = new DatagramPacket(answer, answer.length, incomingDatagram.getAddress(),incomingDatagram.getPort());
								datagramSocket.send(answerDatagram);

							}
							
							
							
							
						//The incomming message is an ack
						}else if ( type == 2 && !transmissionFinished){
							

							//System.out.println("Receiver: ACK "+seq);
							if ( seq == -2){
								transmissionFinished = true;
								this.ack = seq;
							}
							if ((seq > this.ack) && (seq % m == 0))
								this.ack = seq;
						}
						
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Error in receive incoming packet");
					}
					
			}
			outputFile.close();
			//System.out.println("Receiver: end");

		}


		public synchronized void setIncomingAddress(InetAddress address){
			this.incomingAddress = address;
			notify();
		}
		
		public synchronized InetAddress getIncomingAddress(){
			try {
				wait();
			}catch(InterruptedException ex){
				ex.printStackTrace();
	        }
			return incomingAddress;
		}
		
		public void setIncomingPort(int port){
			this.incomingPort = port;
		}
		
		public int getIncomingPort(){
			return incomingPort;
		}
		
		public int getACK(){
			return this.ack;
		}
		
	
}
