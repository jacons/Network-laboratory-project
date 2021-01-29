package main;

import interfaces.RMIClientsState;
import interfaces.RMIRegister;
import server.RMIServerServices;
import server.TCPServices;
import server.User;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WorthServer {
	
	
	private  ConcurrentHashMap<String,User> clients;

	protected List<RMIClientsState> subscriber;
	
	private TCPServices tcpservices; // Thread per le richieste TCP
	private RMIServerServices rmiservices; // Oggetto esportato per la registrazione
	
	
	public WorthServer() {
		
		clients = new  ConcurrentHashMap<String,User>();
		subscriber = Collections.synchronizedList(new ArrayList<RMIClientsState>());
		
		try {
			tcpservices = new TCPServices(clients,subscriber);
		} catch (IOException e) {
			System.out.print(e.getMessage());
			System.exit(1);
		}
        try{
    		rmiservices = new RMIServerServices(clients,subscriber);

            RMIRegister stub = (RMIRegister) UnicastRemoteObject.exportObject(rmiservices, 0);
            LocateRegistry.createRegistry(WorthSupport.REGISTRY_PORT);
            Registry registry = LocateRegistry.getRegistry(WorthSupport.REGISTRY_PORT);
            registry.rebind(WorthSupport.NAME_RMISERVICES,stub);
            System.out.println("Server ready for register");
            
        } catch (RemoteException e) {
            System.out.println("RMI error :" + e.toString());
            System.exit(1);
        }	
        
        tcpservices.start();
	}
	
	public static void main(String[ ] args) { new WorthServer(); }
	
}