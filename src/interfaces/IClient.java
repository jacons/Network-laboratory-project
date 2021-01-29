package interfaces;

import javax.swing.DefaultListModel;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

public interface IClient {
	 
	/**
	 * Richiede la registrazione dell'utente al server.
	 * Codici risultato:
	 * 28 Operazione effettuta, 85 Argomenti non corretti, 44 Utente gia' Esistente,19 Argomenti troppo lunghi
	 * @param user != null
	 * @param passw != null
	 * @return Code
	 */
	public short register(String user,String passw);

	/**
	 * Richiede l'accesso all piattaforma.
	 * Codici risultato:
	 * 28 Operazione effettuta, 85 Argomenti non corretti,19 Argomenti troppo lunghi
	 * @param user != null
	 * @param passw != null
	 * @param tablemodel != null
	 * @return Code
	 */
	public short login(String user, String passw, DefaultTableModel tablemodel);
	
	/**
	 * Effettua la disconnessione dalla piattaforma,notificando il server,
	 * chiudendo tutte le risorse utilizzate.
	 */
	public void logout();

	/**
	 * Richiede la creazione di un nuovo progetto.
	 * Codici risultato:
	 * 85 Argomenti non corretti, 29 Progetto gia' esistente, 28 Operazione effettutata,
	 * 14 Errore nel server, 19 Overflow testo 55 Ip multicast terminati
	 * @param nameproject != null
	 * @param Password != null
	 * @return Code
	 */
	public short addProject(String nameproject,String Password);
	
	/**
	 * Richiede l'accesso ad un progetto.
	 * Codici risultato:
	 * 28 Operazione effettuta, 95 Progetto inesistente, 22 Operazione non permessa,
	 * 85 Argomenti non corretti, 19 Overflow testo
	 * @param nameproject != null
	 * @return Code
	 */
	public short openProject(String nameproject);
	
	/**
	 * Richiede al server la lista di progetto a cui il client e' iscritto.
	 */
	public void updateProjectList(DefaultListModel<String> listMembers);

	/**
	 * Richiedere di rimuovere,il progetto dalla piattaforma.
	 * 27 Task da completare 12 Password scorretta 28 Operazione effettutata 14 Errore nel server
	 * 85 Argomenti non corretti 88 Il progetto non esiste piu'
	 * @param passw != null
	 * @return Code
	 */
	public short deleteProject(String passw);

	/**
	 * Richiede al Server la list di utenti che fanno parte dal progetto.
	 * Codici risultato:
	 * 88 Progetto inesistente, 28 Operazione effettuata, 85 Argomenti non corretti
	 * @param listMembers != null
	 * @return Code
	 */
	public short updateMember(DefaultListModel<String> listMembers);
	
	/**
	 * Richiede al server l'aggiunta di un nuovo membro al progetto.
	 * Codici risultato:
	 * 99 Utente non iscritto, 14 Errore nel server, 28 Operazione effettuata, 85 Argomenti non corretti
	 * 88 Progetto inesistente.
	 * @param name !=null
	 * @return Code
	 */
	public short addMember(String name);
	
	/**
	 * Richiede al server di aggiungere una carta al progetto, se l'operazione va a buon fine 
	 * allora la inserisce nella lista di card "to do".
	 * Codici risultato:
	 * 97 Card gi� presente, 85 Argomenti non corretti, 28 Operazione effettuata,
	 * 14 Errore nel server, 19 Argomenti troppo lunghi, 88 Il progetto non esiste piu'
	 * @param title != null
	 * @param descr != null
	 * @param lvl>0  && lvl<= WorthSupport.MAXPRIORITY
	 * @return Code
	 */
	public short addCard(String title, String descr, int lvl) ;
	
	/**
	 * Richiede al server di spostare la carta, da una lista all' altra. 
	 * Codici risultato:
	 * 28 Operazione effettuta, 33 Card non trovata, 85 Argomenti non corretti,
	 * 11 Operazione non permessa, 19 Argomenti troppo lunghi, 88 Il progetto non esiste piu'
	 * @param title != null
	 * @param  from >=0 && from<=3
	 * @param  to >=0 && to<=3
	 * @return Code
	 */
	public short moveCard(String title,int from, int to) ;
	
	/**
	 * Richiede al server l'eliminazione di una carta presente nel progetto, se e' presente la cancella.
	 * Codici risultato:
	 * 28 Operazione effettuta, 85 Argomenti non corretti, 19 Argomenti troppo lunghi,
	 * 88 Il progetto non esiste piu'
	 * @param title != null
	 * @return Code
	 */
	public short deleteCard(String title) ;
	
	
	/**
	 * Richiede al server la lista delle carte che attualmente sono presenti nel progetto.
	 * Codici risultato:
	 * 88 Se il progetto non esiste piu', 28 Operazione effettuata, 85 Argomenti non corretti
	 * @param cardListModel  != null
	 * @return Code
	 */
	public short showCards(DefaultTableModel cardListModel);
	
	/**
	 * Dato un nome di una carta come parametro richiede al server, la cronologia di quella carta 
	 * Codici risultato:
	 * 28 Operazione effettuata,19 Argomenti troppo lunghi, 85 Argomenti non corretti,
	 * 88 Il progetto non esiste piu'
	 * @param name != null
	 * @param historyCardModel != null
	 * @return Code
	 */
	public short historyCard(String name, DefaultTableModel historyCardModel);
	
	/*
	 * Inizializza le componenti necessarie per l'utilizzo della chat di progetto.
	 */
	public void readytoChat(JTextArea msgHistory);
	
	/**
	 * Manda un messaggio nella chat di progetto.
	 * Codici risultato:
	 * 28 Operazione effettuata,19 Argomenti troppo lunghi, 85 Argomenti non corretti, 
	 * @param msg != null
	 * @return Code
	 */
	public short sendMessage(String msg);
}
