	package server;

import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import interfaces.RMIClientsState;
import interfaces.RMIRegister;
import main.WorthSupport;

public class RMIServerServices extends RemoteObject implements RMIRegister {

	private static final long serialVersionUID = 1L;
	
	private Map<String,User> clients; // inizilizzata in "WorthServer" 

	protected List<RMIClientsState> subscriber;
	
	public RMIServerServices(Map<String, User> clients,List<RMIClientsState> subscriber) throws RemoteException {
		this.clients = clients;
		this.subscriber = subscriber;	

	}
	@Override
	public short register(String utente,String password) throws RemoteException {
		// controllo che i parametri sono corretti
		if(utente==null||password==null||utente.trim().isEmpty()) return 85; 

		
		// se l'utente e' gia' presente lo segnalo
		if(clients.containsKey(utente)) return 44;
				
		clients.putIfAbsent(utente,new User(utente,password,false));
		
		try {
		    Gson build = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			String json = build.toJson(clients.values());
			
			WorthSupport.savefile(Path.of("cache/clients.lst"), json);
		} catch (Exception e) { e.printStackTrace(); }
				
		// per ogni utente iscritto al public-subscribe notifico 
		//che si � aggiunto un nuovo utente
		for(RMIClientsState user : subscriber) {
			
			try { user.notifyNewUser(utente);
			} catch (RemoteException e) { e.printStackTrace(); System.exit(1); }
		}
		System.out.println("Aggiunto un nuovo utente");
		return 28;
	}
	@Override
	public void unregisterForCallback(RMIClientsState ClientInterface) throws RemoteException {
		
		if(ClientInterface==null) throw new NullPointerException();
		// remove l'utente alla struttura public-subscribe
		if (subscriber.remove(ClientInterface)) System.out.println("Client unregistered");
		
	}

	@Override
	public void registerForCallback(RMIClientsState ClientInterface) throws RemoteException {
		if(ClientInterface==null) throw new NullPointerException();
		// aggiungo l'utente alla struttura public-subscribe
		if (!subscriber.contains(ClientInterface)) subscriber.add(ClientInterface);
		System.out.println("New client registered to callback" );		
	}
}