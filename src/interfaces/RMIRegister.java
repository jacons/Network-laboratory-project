package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIRegister extends Remote {

	/**
	 * Registra un nuovo utente nell'applicazione l'utente appena registrato apparita' come offline
	 * Codici risultato:
	 * 28 Operazione effettuta, 85 Argomenti non corretti, 44 Utente gia' Esistente
	 * @param utente != null
	 * @param password != null
	 * @return Code
	 * @throws RemoteException
	 */
	public short register(String utente,String password) throws RemoteException;
	
	/**
	 * Definisce la volonta di un client, di essere aggiornato sullo stato degli utenti della piattaforma
	 * @param ClientInterface 
	 * @throws RemoteException
	 */
	public void registerForCallback(RMIClientsState ClientInterface) throws RemoteException;
	
	
	/**
	 * Definisce la volonta di un client, di disiscriversi all'aggiornamento sullo stato degli utenti
	 * della piattaforma
	 * @param ClientInterface
	 * @throws RemoteException
	 */
	public void unregisterForCallback(RMIClientsState ClientInterface) throws RemoteException;

	
}
