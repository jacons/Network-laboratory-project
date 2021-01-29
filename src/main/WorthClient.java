package main;

import java.rmi.RemoteException;

import client.Client;
import graphics.GUILogin;

public class WorthClient {

	public WorthClient() {
		
		Client client = null;
		try {
			client = new Client();
		} catch (RemoteException e) { e.printStackTrace(); }
		
		GUILogin.start(client);
	}

	public static void main(String[] args) {

		new WorthClient();		
	}

}