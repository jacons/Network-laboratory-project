package interfaces;

import java.util.List;
import java.util.Set;

import exceptions.CardNotFoundException;
import exceptions.WrongArgumentException;
import server.Card;
import server.History;

public interface IProject {

	/**
	 * Vengono riscritte tutte quelle carte che hanno subito aggiornamenti
	 */
	public void flushstorage();

	/**
	 * Aggiunge un membro nel progetto, se storage==true,allora aggiorno il file con i membri.
	 * @param name != null
	 * @param storage
	 * @return
	 * @throws IllegalArgumentException
	 * @throws WrongArgumentException
	 */
	public boolean addMember(String name, boolean storage) throws IllegalArgumentException;

	/**
	 * Viene inserita nella lista iniziale(Todo), la card creata con i parametri del metodo.
	 * Restituisce false se la carta è già presente, true se è stata aggiunta. 
	 * @param title != null
	 * @param descr != null
	 * @param lvl< MAXPRIORITY
	 * @throws WrongArgumentException Se non è stato possibile creare un file con il titolo
	 */
	public boolean putcard(String title, String descr, int lvl) throws WrongArgumentException, IllegalArgumentException;

	/**
	 * Viene inserita una carta già presente nel progetto,la carta possiede le informazioni
	 * per inferire quale sia la lista di appartenenza.
	 * @param card != null
	 * @throws IllegalArgumentException
	 */
	public void restoreCard(Card card) throws IllegalArgumentException;

	/**
	 * Effettua uno spostamento di una carte da una lista all'altra,
	 * aggiornando la sua cronologia.
	 * Restituisce false se la card non è stata trovata.
	 * @param title != null
	 * @param from > 0 && from < NUMOFLIST
	 * @param   to > 0 && to < NUMOFLIST
	 * @throws IllegalArgumentException	 
	 */
	public boolean moveCard(String title, int from, int to) throws IllegalArgumentException;

	/**
	 * Rimuova ogni occorrenza della card con il nome "title".
	 * @param title != null
	 * @throws IllegalArgumentException
	 */
	public void deleteCard(String title) throws IllegalArgumentException;

	/**
	 * Restituisce la lista di (copie di) Card appartenti al progetto.
	 */
	public List<Set<Card>> getCards();

	/**
	 * Restituisce la lista di appartenza della Card 
	 * @param card != null
	 * @return
	 * @throws IllegalArgumentException
	 * @throws CardNotFoundException
	 */
	public int getState(Card card) throws IllegalArgumentException, CardNotFoundException;

	/**
	 * Restituisce la cronologia di spostamenti della  Card con il nome title.
	 * @param title != null 
	 * @return
	 * @throws IllegalArgumentException
	 * @throws CardNotFoundException
	 */
	public List<History> getHistoryCard(String title) throws IllegalArgumentException, CardNotFoundException;

	/**
	 * Controlla che tutte le Card siano sulla lista "Done"
	 * @return
	 */
	public boolean isCardComplete();

	public String getTitle();

	public String getPassword();
	
	/**
	 * Controlla che passw sia uguale a quella del progetto
	 * @param pass
	 * @return
	 * @throws IllegalArgumentException
	 */
	public boolean trypassword(String pass) throws IllegalArgumentException;

	/**
	 * Restituisce un array dei membri partecipanti al progetto
	 * @return
	 */
	public String[] getMembers();

	/**
	 * Controlla se uno spefico membro appartiene al Progetto
	 * @param name
	 * @return
	 */
	public boolean memberContains(String name);

	/**
	 * Restituisce il IP del gruppo multicast
	 * @return
	 */
	public String getMulticastIp();

	/**
	 * Restituisce la porta del gruppo multicast
	 * @return
	 */
	public int getMulticastPort();



}