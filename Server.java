		/* This project was designed by the team SCRATCHES which includes 
		 * team members given below ->
		 * A) Subhankar Bhattacharyya,23
		 * B) Utkarsh Shrivastava,7
		 * C) Yukta Bhartia,2
		 * D) Subarna Mitra,25
		 * work of every teammate is clearly mentioned within the comments in the code and let
		 * me assure you that each of us have done our work with due deligence. Due to one of 
		 * our member (Subarna,25) not having a proper working laptop she managed to show her 
		 * efforts by guiding us verbally and helping us with the flow diagrams and logic 
		 * buildings. All in all it was a total team effort and I hope you would like what we 
		 * have accomplished in this project. 
		 */


import java.net.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;
class MessageFactory {
    
    
    private byte[] RDT = new byte[] { 0x52, 0x44, 0x54 };
    private byte SEQ_0;
    private byte[] END = new byte[] { 0x45, 0x4e, 0x44 };
    private byte[] CRLF = new byte[] { 0x0d, 0x0a };
    private FileInputStream myFIS = null;
    private boolean exit = false;
    private byte[] concatenateByteArrays(byte[] payload) {
		
        byte[] result = new byte[518];
		System.arraycopy(RDT, 0, result, 0, RDT.length);		
		result[RDT.length]=SEQ_0;
		System.arraycopy(payload, 0, result, RDT.length + 1, payload.length);
		System.arraycopy(CRLF, 0, result, RDT.length + payload.length + 1, CRLF.length);		
        return result;
    }
    public void readFile(String file_name) 
    {
		try{
			myFIS = new FileInputStream(file_name);		
			exit=false;
		}
		catch(FileNotFoundException ex)
		{
			System.out.println(ex.getMessage());
		}
    }
    public boolean done()
    {
		return exit;
    }
    public byte[] next_Consignment(int sq) {
		SEQ_0=(byte)sq;
		int bytesRead = 0;
		byte[] myData = new byte[512];
		byte[] refined_data;
		
		try {
			bytesRead = myFIS.read(myData);			
			System.out.println("data consignment has " + bytesRead + " bytes");  
			if (bytesRead > -1) { 				
				if(bytesRead < 510){
					myData[509]=END[0];
					myData[510]=END[1];
					myData[511]=END[2];
					refined_data = concatenateByteArrays(myData); //Will append "END" only if there is space of appending it in the 512 byte consignment
					exit=true; //And then exit will be turnt false, denoting that all the bytes have been read and sent
					myFIS.close();
				} 
				else{
					refined_data = concatenateByteArrays(myData);
				}
			}
			else{
				myData[509]=END[0];
				myData[510]=END[1];
				myData[511]=END[2];
				refined_data = concatenateByteArrays(myData);
				exit=true;
				myFIS.close();
			}
			System.out.println("message has " + myData.length + " bytes");			
			return refined_data;
				
		}catch (Exception ex1) {
			System.out.println(ex1.getMessage());
			return myData;
		}
	}

} 

public class Server {
 
	public static void main(String[] args) {
 
		properServer c1 = new properServer();
		c1.activate(args);
		
	}
}

class properServer {
	void activate(String args[]) {
		
		String CRLF="\r\n";

		DatagramSocket ss = null;//IP datagram socket
		DatagramPacket rp, sp, reservedPacket = null;//empty packets to store received abd sent data
		byte[] rd, sd, reserved;//holds received ack or send packet packets in byte format
		boolean not_sent;

		InetAddress ip=null;
		int port=0;
		boolean exit;
		
		while(true){
			try {
				ss = new DatagramSocket(Integer.parseInt(args[0]));
				
				System.out.println("Server is up....");
				int consignment=0;
				String request;//request requested
				int sq_number = -1;//stores ack seq number
				MessageFactory messageFactory = new MessageFactory();
				not_sent = false;
				
				while(!(messageFactory.done())){	 
					rd=new byte[100];
					sd=new byte[518];
					 
					rp = new DatagramPacket(rd,rd.length);
					
					
					if(consignment==1){
						ss.setSoTimeout(1000);//setting Time Out Timer
					}
					try{
						ss.receive(rp);
						// get client's consignment request from DatagramPacket
						ip = rp.getAddress(); 
						port =rp.getPort();
						System.out.println("Client IP Address = " + ip);
						System.out.println("Client port = " + port);
					}
					catch(SocketTimeoutException e){
						consignment = sq_number+1;
						System.out.println("Acknowledgment lost, resending last packet,i.e consignment #"+consignment);
						not_sent = true;
					}
					
					
					request = new String(rp.getData());
					System.out.println("Client says = " + request.trim());					
					if(request.indexOf("ACK")!=-1){						
						request=request.replace("ACK","");
						request=request.replace(CRLF,"");
						sq_number = Integer.parseInt(request.trim());
					}
					else if(request.indexOf("REQUEST")!=-1){
						request=request.replace("REQUEST","");
						request=request.replace(CRLF,"");
						System.out.println(request.trim());
						messageFactory.readFile(request.trim());
					}
					
					System.out.println("\n=====================================================================\n");
					if(not_sent){
						//resending last sent consignment		
						ss.send(reservedPacket);
						System.out.println("Sent Consignment # " + consignment);
						not_sent = false;
						consignment++;
					}
					else if(consignment==sq_number+1){
					// prepare data
						sd=messageFactory.next_Consignment(consignment);						
						sp=new DatagramPacket(sd,sd.length,ip,port);
						reserved = sd;
						reservedPacket = new DatagramPacket(reserved,reserved.length,ip,port);
						// send data						
						ss.send(sp);
						System.out.println("Sent Consignment #" + consignment);
						 
						if (messageFactory.done()) { //waiting for last byte ACK
							rd=new byte[100];
							sd=new byte[518];					 
							rp = new DatagramPacket(rd,rd.length);	
							not_sent = true;
							do{
								try{
									ss.receive(rp);
									// get client's consignment request from DatagramPacket
									ip = rp.getAddress(); 
									port =rp.getPort();
									System.out.println("Client IP Address = " + ip);
									System.out.println("Client port = " + port);
									request = new String(rp.getData());
									System.out.println("Client says = " + request.trim());	
									not_sent = false;
								}
								catch(SocketTimeoutException e){
									System.out.println("Acknowledgment lost, resending last packet,i.e consignment #"+consignment);
									ss.send(reservedPacket);
									System.out.println("Sent Consignment # " + consignment);								
									//resending last sent consignment										
								}
							}while(not_sent);
							consignment = 0; // reset consignment after last consignment is delivered
							sq_number = -1;
							System.out.println("All packets send successfully");
						} else {
							consignment++;
						}
					}					
					else{
						System.out.println("late packet found and discarded");
					}					
					
				} // end of while messageFactory.done() loop
				ss.close();
				try{
					Thread.sleep(5000);
				}catch(InterruptedException e){System.out.println(e);}		

			}catch (IOException ex) {
				System.out.println(ex.getMessage());
			}
		}
	}
}
 