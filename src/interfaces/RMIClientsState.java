package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientsState extends Remote {

	/**
	 * Aggiorna l'utente che c'e' un nuovo iscritto alla piattaforma
	 * @param user
	 * @throws RemoteException
	 */
	public void notifyNewUser(String user) throws RemoteException;
	
	/**
	 * Aggiorna l'utente che un altro utente ha cambiato stato
	 * @param user
	 * @param state
	 * @throws RemoteException
	 */
	public void notifyEvent(String user,boolean state) throws RemoteException;
}
