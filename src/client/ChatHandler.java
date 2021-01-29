package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

import javax.swing.JTextArea;

import exceptions.WrongArgumentException;
import main.WorthSupport;

public class ChatHandler extends Thread {

	/*
	 * Overview : Rapprsenta un thread che si occupa di ricevere i visualizzare i messaggi
	 * 			  nella chat del progetto. 
	 * 
	 * Representation invariant :  ipMulticast!=null && ipMulticast ∈ {ip classe D} && 
	 * 							   portMulticast ∈ {49152...65535} && group!=null &&
	 * 							   msgTab != null
	 * 
	 */
	
	private InetAddress ipMulticast;
	private int portMulticast;

	private MulticastSocket group;
	private JTextArea msgTab;
	
	
	@SuppressWarnings("deprecation")
	public ChatHandler(String ip, int port, JTextArea msgTab) throws IOException,WrongArgumentException {
		
		if(ip==null||port<49152||port>65535||msgTab==null)
			new WrongArgumentException();
		
		this.portMulticast = port;
		this.msgTab = msgTab;
		ipMulticast = InetAddress.getByName(ip);
		group = new MulticastSocket(port);
		group.joinGroup(ipMulticast);
		
	}

	@Override
	public void run() {
			
		DatagramPacket packet = new DatagramPacket(new byte[WorthSupport.CHATBUFFERMAX],0,WorthSupport.CHATBUFFERMAX);
		
		while(!isInterrupted() && !group.isClosed()) {
		
			try {
				group.receive(packet); // Riceve il pacchetto
				// Aggiorna la chat
				msgTab.setText(msgTab.getText()+new String(packet.getData()));
				// inizializza di nuovo il pachetto, per il prossimo messaggio
				packet.setData(new byte[WorthSupport.CHATBUFFERMAX]);
			} catch(SocketException e) {}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	public void CloseChat() {
		try {
			group.leaveGroup(ipMulticast);
			group.close();
		} catch (IOException e) { e.printStackTrace(); }
		
	}
	public int getPort() { return portMulticast; }
	
}
