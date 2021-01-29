package server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import exceptions.CardNotFoundException;
import exceptions.WrongArgumentException;
import interfaces.IProject;
import main.WorthSupport;


public class Project implements IProject {
	/**
	 * Overview: Rappresenta un progetto, ovvero un contenitore di task da svolgere 
	 * 			 dai membri, i quali posso creare ed spostare i task nelle varie liste.
	 * 
	 * Representation invariant : projectitle != null && password != null && 1000 > seed >= 0 &&
	 * 							  lists != null && forall(  map in lists. map!=null &&
	 * 														forall(e in map. e.value()!=null && e.key()=e.value().getTitle()) 
	 * 													  ) &&
	 * 							  members!=null && forall(m in members. m!=null) &&
	 * 
	 * 							  multicastIp !=nul && multicastIp ∈ {ip classe D} && 
	 * 							  multicastPort ∈ {49152...65535} && multicastSocket!=null
	 * 							  multicastGroup!=null 
	 * 
	 */
	
	
	private final String projectitle;
	private final String password;
	private final int seed;

	
	// 0=ToDo 1=In progress  2=Toberevised 3=Done	
	private List<Map<String,Card>> lists;
	
	private Set<String> members;	
	
	private String multicastIp ;
	private int multicastPort;
	
	protected MulticastSocket multicastSocket;
	protected InetAddress multicastGroup;
	protected DatagramPacket packet; // send only "send message"
	
	Gson googleParser = new Gson();
	
	public Project(String title,String passw,String name,String ip,int port,int seed) throws WrongArgumentException {
				
		if(title==null||passw==null||ip==null||port<49152||port>65535) 
			throw new WrongArgumentException("Invalid Arguments");
		
		this.projectitle = title;
		
		if(seed<0) {
			// se è la prima volta che creo il progetto, allora mi scelgo il seed
			this.seed = (int)(Math.random()*1000);
			// ed codifico la password
			this.password = WorthSupport.encode(passw+this.seed,"SHA-256");

		} else {
			// se sto facendo il restore,il seed resta lo stesso e la password pure
			// visto che già è stata codifica.
			this.seed = seed;
			this.password = passw;
		}
		
		
		this.multicastIp = ip;
		this.multicastPort = port;
		
		try {
			multicastSocket = new MulticastSocket(multicastPort);		
			multicastSocket.setTimeToLive(1);
			multicastGroup  = InetAddress.getByName(multicastIp);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			/* Si tenta di creare una cartella con il nome del titolo del progetto
			   se il nome non è valido si genera un eccezione che verrà propagata
			   fino al client, con codice errore: 85 Agomenti non corretti.
			   Si crea anche un file vuoto per inserire i nomi del membri.
			*/
			Files.createDirectories(Paths.get("cache/"+title));
			Files.createFile(Paths.get("cache/"+title+"/member.lst"));
		} catch(FileAlreadyExistsException e) {}
		catch (Exception e) { throw new WrongArgumentException("Error in creatio file"); }
		
		members = new TreeSet<String>();
		if(name!=null) addMember(name,true);
		
		lists = new ArrayList<Map<String,Card>>(WorthSupport.NUMOFLIST);
		for(int i=0;i<WorthSupport.NUMOFLIST;i++) lists.add(new ConcurrentHashMap<String,Card>());
		
		Thread cachestorage = new Thread(new Runnable() {
			// Allo scadere del timeout(sleep) vengono riscritte tutte le card
			// che hanno subito cambiamenti dall'ultima esecuzione dello storage
			public void run(){
				while(true) {
					try { Thread.sleep(WorthSupport.TIMEFLUSH); flushstorage(); } 
					catch (InterruptedException e) { e.printStackTrace(); }
				}
			}
		});
		cachestorage.setDaemon(true);
		cachestorage.start();
	}
	
 	@Override
	public void flushstorage() {
		for(Map<String,Card> l : lists) {
			for(Card card : l.values()) {
				if(!card.isUpdated()) {
					// Se ci sono dei cambiamenti, allora le riscrivo
					Path path = Path.of("cache/"+projectitle+"/"+card.getTitle()+".json");
					
				    Gson build = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
					String json = build.toJson(card,Card.class);	

					try {
						WorthSupport.savefile(path,json);
					} catch (IOException e) { e.printStackTrace(); }
					
					System.out.println("Aggiornata "+card.getTitle());
					card.setUpdated(true);
				}
			}
		}		
 	}
 	
 	@Override
	public boolean addMember(String name, boolean storage) throws IllegalArgumentException {
		
 		if(name==null) throw new IllegalArgumentException();
 		
		boolean flag = members.add(name);
		
		if(flag && storage) try {
			String json = googleParser.toJson(members);
			WorthSupport.savefile(Path.of("cache/"+projectitle+"/member.lst"),json);		
		} catch (IOException e) { e.printStackTrace(); }
		
		if(flag) sendMessage("Il membro "+name+" si e' unito al gruppo");
		return flag; 
	}

	@Override
	public boolean putcard(String title,String descr,int lvl) throws WrongArgumentException,IllegalArgumentException {
		
		if(title==null||descr==null||lvl<0||lvl>WorthSupport.MAXPRIORITY)
			throw new IllegalArgumentException();
		
		//Per ogni lista cerca la carta, restituisce false se già presente. 
		for(Map<String,Card> l : lists) if(l.containsKey(title)) return false; 
		
		// La card con quel nome non è prenente
		
		Path path;
		try {
			// Sei il nome della card non è valido per creare il file corrispondente
			// genero un eccezione
			path = Path.of("cache/"+projectitle+"/"+title+".json");	
		} catch (InvalidPathException e) { throw new WrongArgumentException();}
		
		
		// Se esiste un file con lo stesso nome lo cancella.
		if(Files.exists(path))  
			try { Files.delete(path); } 
			catch (IOException e) { e.printStackTrace(); }			
		

		Card card = new Card(title,descr,lvl);
		String json = googleParser.toJson(card,Card.class);// serializzo la classe in json
		
		try {
			WorthSupport.savefile(path,json);
		} catch (IOException e) { throw new WrongArgumentException(); }

		// Aggiungo la carta alla lista ToDo
		lists.get(0).putIfAbsent(title,card);
		
		sendMessage("La card "+title+" e' stata aggiunta al progetto");
		
		return true;
	}
	

	@Override
	public void restoreCard(Card card) throws IllegalArgumentException  {		
		 // dopo aver veficato la correttezza dei parametri, effettuo "inferenza"
		 // sulla cronologia per individuare la lista(mappa) di appartenenza corretta
		if(card==null           || card.getTitle()==null  ||card.getDescr()==null  ||
		   card.getPriority()<0 || card.getPriority()>WorthSupport.MAXPRIORITY)
			throw new IllegalArgumentException();
		
		try {
			// Se non riesce ad inferire lo stato viene messa nella lista To Do
			lists.get(card.inferState()).putIfAbsent(card.getTitle(),card);
		} catch(Exception e) {
			lists.get(0).putIfAbsent(card.getTitle(),card);
		}
	}
	

	@Override
	public boolean moveCard(String title,int from,int to) throws IllegalArgumentException {
		
		if(from > WorthSupport.NUMOFLIST || from < 0 || to >WorthSupport.NUMOFLIST || to < 0 || title==null) 
			throw new IllegalArgumentException();
		
		// Rimuovo il puntatore alla card da una mappa e lo aggiungo all'altra
		if(lists.get(from).containsKey(title)) {
			
			Card card = lists.get(from).get(title);
			
			lists.get(to).putIfAbsent(title,card);
			lists.get(from).remove(title);
			
			card.addHistory(from, to);
			
			sendMessage("La card "+title+" e' stata spostata da "+WorthSupport.getStringState(from)+
														   " a "+WorthSupport.getStringState(to));
			return true;
		} else return false;
	}

	@Override
	public void deleteCard(String title) throws IllegalArgumentException {
		
		if(title==null) throw new IllegalArgumentException();
		// per ogni lista(mappa) effetto l'eliminazione
		for(Map<String,Card> m : lists) 
			if(m.remove(title) != null) {
				sendMessage("La card "+title+" e' stata eliminata");
				return;
			}		
	}


	@Override
	public List<Set<Card>> getCards() {
		
		List<Set<Card>> result = new ArrayList<Set<Card>>(WorthSupport.NUMOFLIST);
		
		for(int i=0;i<WorthSupport.NUMOFLIST;i++) {
			result.add(new HashSet<Card>());
			for(Card e : lists.get(i).values()) result.get(i).add(e.clone());
			
		}
		return result;
		
	}

	@Override
	public int getState(Card card) throws IllegalArgumentException,CardNotFoundException {
		
		if(card == null ) throw new IllegalArgumentException();
	
		for(int i=0;i<WorthSupport.NUMOFLIST;i++) if(lists.get(i).containsKey(card.getTitle())) return i;
		
		throw new CardNotFoundException();
	}
	

	@Override
	public List<History> getHistoryCard(String title) throws IllegalArgumentException,CardNotFoundException {
		
		if(title == null) throw new IllegalArgumentException();
		
		Card card;
		// Scorro ogni lista
		for(Map<String,Card> m : lists) if((card = m.get(title))!=null) return card.getHistory();
		
		throw new CardNotFoundException();
	}
	private void sendMessage(String msg) {
		
		if(msg==null||msg.trim().isBlank()) return;
		
		String text = "Server: "+msg+".\n";
		
		if(text.length()>WorthSupport.CHATBUFFERMAX) {
			// Se il messaggio supera i limiti viene trocato
			text = text.substring(0, WorthSupport.CHATBUFFERMAX-1);
		}
		
		byte[] buffer = new byte[WorthSupport.CHATBUFFERMAX];
		buffer = text.getBytes();
		
		packet = new DatagramPacket(buffer,text.length(),multicastGroup,multicastPort);
		try {
			multicastSocket.send(packet);
		} catch (IOException e) { e.printStackTrace(); }	
	}

	@Override
	public boolean isCardComplete() { return lists.get(0).isEmpty() && lists.get(1).isEmpty() && lists.get(2).isEmpty() ; }
		
	@Override
	public String getTitle() { return projectitle; }
	
	@Override
	public String getPassword() { return password; }	

	@Override
	public boolean trypassword(String pass) throws IllegalArgumentException  {
		
		pass = WorthSupport.encode(pass+seed,"SHA-256");
		if(pass==null) throw new IllegalArgumentException();
		return this.password.equals(pass);
	}
	
	public int getSeed() { return seed;}
	
	@Override
	public String[] getMembers() { return members.toArray(new String[members.size()]); }
	
	@Override
	public boolean memberContains(String name) {return members.contains(name); }

	@Override
	public String getMulticastIp() { return multicastIp; }

	@Override
	public int getMulticastPort() { return multicastPort; }	
}