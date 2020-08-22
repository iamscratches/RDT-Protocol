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

//byte[] slice = Arrays.copyOfRange(myArray, 5, 10);
import java.net.*;
import java.io.*;
import java.util.*;
 
public class Client {
 
	public static void main(String[] args) {
	 //CleanClient c1 = new CleanClient();
	 DirtyClient c1 = new DirtyClient();
	 //test c1 = new test();
	 c1.activate(args);
	 //c1.show();
	    
	}
 
}
class test{
	public void show(){
		FileInputStream myFIS = null;
		FileOutputStream fos = null;
		
		try{
			myFIS = new FileInputStream("test.txt");
			fos = new FileOutputStream("copyoftest.txt");
		}
		catch(FileNotFoundException ex)
		{
			System.out.println(ex.getMessage());
		}
		byte[] myData = new byte[512];
		int bytesRead = 0;
		try{
			bytesRead = myFIS.read(myData);			
			fos.write(myData);
		}catch(IOException e){
			System.out.println(e);
		}
		System.out.println("data consignment has " + bytesRead + " bytes");  
		while(bytesRead != -1){			
			System.out.println(new String(myData));
			myData = new byte[512];
			try{
				bytesRead = myFIS.read(myData);
				fos.write(myData);
			}catch(IOException e){
				System.out.println(e);
			}
			System.out.println("data consignment has " + bytesRead + " bytes");  
		}
	}
}

class DirtyClient {
	private byte[] RDT = new byte[] { 0x52, 0x44, 0x54 };
    private byte[] END = new byte[] { 0x45, 0x4e, 0x44 };
	void activate(String args[]) {
		DatagramSocket cs = null;
		FileOutputStream fos = null;				
		String CRLF="\r\n";
		String filename=args[2];

		try {
			cs = new DatagramSocket();//IP datagram socket
			cs.setSoTimeout(7000);//server respond failure safe

			Random random= new Random();
			
			double AVERAGE_DELAY = 1;
			
			byte[] rd, sd;//holds received packet or send ack packets in byte format

			fos = new FileOutputStream("copyof"+filename);
			
			boolean request_sent = false;
			byte[] payload = null;//received payload message
			int sq_number = -1;//stores current received seq_no
			int prev_sq_no = -1;//stores last received packet seq_no
			DatagramPacket sp,rp;//datagram packets that payload along with source and detination IP addresses and port no.s
			boolean end = false;//indicates all packets are sent and received properly
					
			while(!end)
			{   
				//initialisation of the variables to be used latter in the code
				
				rd=new byte[518];
				rp=new DatagramPacket(rd,rd.length);	
					
				if(AVERAGE_DELAY >= 0.2){//Dont't send packets if no packets received the last time
				// send request till the client receives a response from the server 
					if(!request_sent){
						sd=("REQUEST" + filename + "\r\n").getBytes();									
					}
					else{																			//send acknowledgement for the last packet received
						sd=("ACK" + sq_number + "\r\n").getBytes();					
					}
					sp=new DatagramPacket(sd,sd.length,InetAddress.getByName(args[0]),Integer.parseInt(args[1]));	
					System.out.println("sent " + (new String(sp.getData())).trim());	
					cs.send(sp);
				}
				try{
					AVERAGE_DELAY = (random.nextDouble());
					cs.receive(rp);																//wait for incoming packets from server
					if(AVERAGE_DELAY >= 0.2){
						System.out.println("packet received");
						//System.out.println(new String(rp.getData()));
						request_sent = true;
					}								
					try{
						if(AVERAGE_DELAY < 0.2){//packet not received or ack sent but lost in network
							System.out.println("packet lost or ack not sent");
							continue;
						}
						else if(AVERAGE_DELAY < 0.4){//packet received but ack send late
							System.out.println("ack send late");
							Thread.sleep(1500);			
						}							
						else//network delay
							Thread.sleep(500);
					}catch(InterruptedException e){System.out.println(e);}		
				}catch(SocketTimeoutException e){//server fail safe
					System.out.println("Server is taking too long to respond!!\nPlease try again after some time");
					cs.close();					
					break;
				}
				
				//Extract payload and sequence no. from the received packet
				if(request_sent){																
					sq_number = Integer.parseInt(String.valueOf(rd[3]));
					payload = Arrays.copyOfRange(rp.getData(), 4, 516);
				}
				System.out.println("sq_number = " + sq_number);
				
				//Extract payload for the last packet received from server indicated by "END"
				if(rd[509+4]==END[0] && rd[510+4]==END[1] && rd[511+4]==END[2]){ // last consignment
					end = true;
					payload = Arrays.copyOfRange(rp.getData(), 4, 513);	
					int i = 0;
					while(payload[i] != 0 && payload.length > i)
						i++;
					byte[] temp = payload;
					payload = new byte[i];
					payload = Arrays.copyOfRange(temp,0,i);
					sd=("ACK" + sq_number + "\r\n").getBytes();	
					sp=new DatagramPacket(sd,sd.length,InetAddress.getByName(args[0]),Integer.parseInt(args[1]));	
					System.out.println("sent " + (new String(sp.getData())).trim());	
					cs.send(sp);
				}
				
				//client will only accept the packet with seq_no 1 higher than the last accepted seq_no from the server
				if(prev_sq_no + 1 == sq_number){					
					fos.write(payload);
					prev_sq_no += 1;
				}
				else{																				//discard duplicate packets
					System.out.println("duplicate packet discarded");
				}
				
				System.out.println("***********************************");
			}//end of while loop
			
			//close and free up the port sockets after all packets are received
		}catch (IOException ex) {
			System.out.println(ex.getMessage());
		}finally {

			try {
				if (fos != null)
					fos.close();
				if (cs != null)
					cs.close();
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			}
		}
	}
}
