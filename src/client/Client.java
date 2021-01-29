package client;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import exceptions.WrongArgumentException;
import interfaces.IClient;
import interfaces.RMIClientsState;
import interfaces.RMIRegister;
import main.WorthSupport;
import server.Card;
import server.History;

public class Client extends RemoteObject implements RMIClientsState,IClient {

	/*
	 * Overview: Rappresenta un insieme di funzionalità per interagire con server della piattaforma Worth
	 * 			 esponendo graficamente i risultati,la spefica dei metodi è nell'interfaccia.
	 * 
	 * Representation invariant:  socket!=null && users!=null && 
	 * 							  forall(e in users. e.Key()!=null && e.Value()!=null) &&
	 * 							  payload != null && datasent != null && jsonparser!=null &&
	 *							  stubSever != nul && stubCallBack!=null					  	
	 * 
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	private SocketChannel socket; 
	
	private Map<String,Boolean> users;  // Utenti ed il loro stato
	private DefaultTableModel usersTab; // Tabella grafica utenti, null fino ad "login"
	
	private String username;    // null fino al "login"
	private String nameProject; // null fino ad "openProject"
	
	private String multicastAddr;            // null fino ad "openProject" 
	private int multicastPort;               // null fino ad "openProject"
	private ChatHandler chat ;               // null fino ad "GuiApplication" (init in GuiApplication)
	private MulticastSocket multicastSocket; // null fino ad "GuiApplication" (init in GuiApplication)
	private InetAddress multicastGroup;      // null fino ad "GuiApplication" (init in GuiApplication)
	
	private boolean islogged;
		
    protected ByteBuffer datasent; // Di dimenzione 4byte viene usato per il recupero di 
    							   // codici di ritorno ed eventuale dimenzione effettiva del payload			
   
    protected ByteBuffer payload; // Buffer di invio/recezione delle richieste/risposte.
    							  // Alcune funzioni richiedono l'istaurazione di un proprio buffer.

    protected Gson jsonparser;
	
	private RMIRegister stubSever;
	private RMIClientsState stubCallBack;
	
	public Client() throws RemoteException {
		
		users = new HashMap<String,Boolean>();
		islogged = false;
        try {
        	Registry registry = LocateRegistry.getRegistry(WorthSupport.REGISTRY_PORT);
			stubSever = (RMIRegister) registry.lookup(WorthSupport.NAME_RMISERVICES);
			
			RMIClientsState callbackObj = this;
			stubCallBack = (RMIClientsState) UnicastRemoteObject.exportObject(callbackObj, 0);

			socket = SocketChannel.open(new InetSocketAddress(WorthSupport.TCPIP,WorthSupport.TCP_PORT));

		} catch (IOException | NotBoundException e) { 
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(1);
		}
		
        payload  = ByteBuffer.allocateDirect(170); 
        datasent = ByteBuffer.allocateDirect(4);
        jsonparser = new Gson();
	}
	
	/*
	 * Costruisco la richiesta da inviare,con il tipo di operazione che si intende 
	 * seguire ed il payload,restituisce il codice di ritorno del server.
	 */
	private short sendReceiveRequest(int optioncode,Object request) {
			
		if(optioncode<0||request==null) return 85;
		
		payload.clear();
		String tmp = jsonparser.toJson(request);
		payload.putInt(optioncode); // Operation Code
		payload.put(tmp.getBytes());
		payload.flip();
		
		try {	
			while(payload.hasRemaining()) socket.write(payload);
			
			payload.clear();
			// mi metto in attesa dell' ack 4byte : 2byte (code op) + 2byte (return code)
			socket.read(payload);
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(1);
		}
		
		payload.flip(); 
		payload.getShort(); // Operation Code (x debug)
	    return payload.getShort(); // Result Code 
	}

	//-------------------------- RMI CALLBACKS -------------------------
	@Override
	public void notifyNewUser(String user) throws RemoteException {
		//Un nuovo utente si è registrato, lo inserisco nella struttura come offline
		users.put(user, true);
		String[] newuser = {user,"OFFLINE"};
		usersTab.addRow(newuser);
	}

	@Override
	public void notifyEvent(String user, boolean state) throws RemoteException {
		//Un client è entrato/uscito dalla piattaforma, aggiorno la struttura dati gli utenti
		users.replace(user, state);

		// aggiornamento grafico dello stato dell' utente
		for(int i=0;i<usersTab.getRowCount();i++) {
			if(((String)usersTab.getValueAt(i,0)).compareTo(user)==0) {
				String newstate = state?"ONLINE":"OFFLINE";
				usersTab.setValueAt(newstate,i,1);
			}
		}
	}
	//-------------------------- RMI CALLBACKS -------------------------
	
	//-------------------------- RMI SERVER -------------------------
	@Override
	public short register(String user,String passw) {
		
		if(user.trim().isBlank() || passw.trim().isEmpty()) return 85;
		if(user.length()>WorthSupport.USERMAX) return 19;  // supera la lunghezza massima
		
		short Code=0;
		try {
			Code = stubSever.register(user,WorthSupport.encode(passw,"SHA-256"));
		} catch (RemoteException e) { e.printStackTrace(); }

		return Code;
	}
	//-------------------------- RMI SERVER -------------------------

	@Override
	public short login(String user,String passw, DefaultTableModel tablemodel) {
		
		if(islogged) return 28;
		if(user==null||passw==null||tablemodel==null) return 85;
		if(user.length()>WorthSupport.USERMAX) return 19;
	
		// max payload 95byte = 20 + 64 + 4 +7
		payload.clear();
		payload.putInt(0); // Operation Code
		String[] request = {user,WorthSupport.encode(passw,"SHA-256")};
		payload.put(jsonparser.toJson(request).getBytes());
		payload.flip();
		
		try {	
			while(payload.hasRemaining()) socket.write(payload);
			
			datasent.clear();
			// Leggo solo i primi 4 byte (nella relazione approfondisco)
			socket.read(datasent); 
			datasent.flip();
			
			int dimbuff = datasent.getInt();
	    	if(dimbuff<0) return (short) Math.abs(dimbuff);// vedi login

	    	
	    	ByteBuffer buffer = ByteBuffer.allocate(dimbuff);
			socket.read(buffer); // leggo il restante payload
			
			buffer.flip();
			byte[] bytes = new byte[dimbuff];
			buffer.get(bytes);
			
			
			// json to List of string
			Type cardstype = new TypeToken<ArrayList<String[]>>(){}.getType();
			ArrayList<String[]> response = jsonparser.fromJson(new String(bytes),cardstype);

			this.username = user;
			this.usersTab = tablemodel;
				
			boolean isonline;
			for(String[] us : response) {
				
				isonline =  us[1].equals("1")?true:false;
				users.put(us[0],isonline);
					
				us[1] = (isonline)?"ONLINE":"OFFLINE";
				usersTab.addRow(us); // aggiorno graficamente la tabella
			}	
			stubSever.registerForCallback(stubCallBack); // rmi callbacks
				
		} catch (JsonSyntaxException e) { 
				return 52; // Bad response
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(1);
		}
		return 28;
	}
	
	@Override
	public void logout() {
		
		try {
			stubSever.unregisterForCallback(stubCallBack); // rmi callbacks
			
			payload.clear();
			payload.putInt(1); // Operation code
			payload.flip(); 
			socket.write(payload);			
			
			socket.close();
			
			// se non ho ancora aperto il progetto il thread della chat non è ancora avviato
			if(chat!=null) {
				chat.CloseChat();
				chat.interrupt();					
			}
				
			if(multicastSocket!=null) multicastSocket.close();
	
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(1);
		}
		System.exit(0);
	}

	@Override
	public void updateProjectList(DefaultListModel<String> listProject) {
		
		if(listProject==null) return;
		
		payload.clear();
		payload.putInt(2); // Operation code
		payload.flip();
		
		try {
			while(payload.hasRemaining()) socket.write(payload); // Invio la richiesta
	    	
	    	datasent.clear();
			socket.read(datasent);
			datasent.flip();
	
			
	    	int dimbuff = datasent.getInt(); // Leggo solo i 4 byte che mi rappresentano il payload
	    	ByteBuffer buffer = ByteBuffer.allocate(dimbuff);
			
			socket.read(buffer); // Leggo il payload
			buffer.flip();
			byte[] bytes = new byte[dimbuff];
			buffer.get(bytes);
			
			// json to list string
			Type historytype = new TypeToken<List<String>>(){}.getType();
			List<String> response = jsonparser.fromJson(new String(bytes),historytype);
			
			listProject.clear(); // Aggiungo il nuovo utente alla tabella grafica
			for(String s : response) listProject.addElement(s);
	
		} catch(JsonSyntaxException e) {
			JOptionPane.showMessageDialog(null,"Bad request");
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(1);
		}
		
	}

	@Override
	public short addProject(String nameproject,String password) {
		// max buffer 115byte 40 + 64 + 4 + 7 
		if(nameproject==null||password==null) return 85;
		if(nameproject.trim().isBlank() || password.trim().isBlank()) return 85;
		if(nameproject.length()>WorthSupport.PRJMAX) return 19;
		
		String[] request = {nameproject,WorthSupport.encode(password,"SHA-256")};
		return sendReceiveRequest(3,request); // Invia la richiesta e restituisce il codice di ritorno del server	
	}
	
	@Override
	public short openProject(String project) {
		
		if(project==null||project.trim().isBlank()) return 85;
		// supera la lunghezza massima
		if(project.length()>WorthSupport.PRJMAX) return 19;		
	
		// max payload 48byte = 4+40+4
		payload.clear();
		payload.putInt(4); // Code op
		payload.put(jsonparser.toJson(project).getBytes());
		payload.flip();
				
		try {	
			while(payload.hasRemaining()) socket.write(payload);

			datasent.clear();
			socket.read(datasent);
			datasent.flip();
			int dimbuff = datasent.getInt();// Come sopra, recupero prima numero di byte del payload
			
			payload.clear();
			socket.read(payload); // Poi leggo effettivamente il payload
			payload.flip();
			
			byte[] bytes = new byte[dimbuff];
			payload.get(bytes);
			
			// json to string array
			String[] response = jsonparser.fromJson(new String(bytes),String[].class);
				
			// Se c'è un codice di errore lo visualizzo. A differenza nelle funzioni che devono restituire 
			// SOLO i codici di ritorno, questo meotodo coneterrà nel json
			// come primo campo il codice di ritorno poi le informazioni effettive.
			if(!response[0].equals("28")) return Short.parseShort(response[0]);
				
			multicastAddr = response[1]; // rappresenta IP
			multicastPort = Integer.valueOf(response[2]); // la port
			
		} catch(JsonSyntaxException e) {
			return 52; // Bad request
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(1);
		}
		this.nameProject = project;
		return 28;
	}	
	
	@Override
	public short deleteProject(String passw) {
		
		// Se il progetto è stato effettivamente cancellato, effetto anche il logout.
		if(passw==null||passw.trim().isBlank()) return 85;
		String[] request = {nameProject,WorthSupport.encode(passw,"SHA-256")};
		short code =  sendReceiveRequest(5,request);
		if(code==28) {logout(); return 28;} 
		
		else return code;
	}

	@Override 
	public short addMember(String name) {
		//max buffer 71byte 20+40+4+7
		if(name==null||name.trim().isBlank()) return 85;
		// supera la lunghezza massima
		if(name.length()>WorthSupport.USERMAX) return 19;		
		
		String[] request = {nameProject,name};
		return sendReceiveRequest(6,request);		
	}

	@Override	
	public short updateMember(DefaultListModel<String> listMembers) {
		if(listMembers==null) return 85;
		// max payload 46byte = 4+40+2
		payload.clear();
		payload.putInt(7); // Code op
		
		String rqst_title = jsonparser.toJson(nameProject,String.class);
		payload.put(rqst_title.getBytes()); // name progetto
		
		payload.flip();
		
		try {
			while(payload.hasRemaining()) socket.write(payload);
			
			datasent.clear();
			socket.read(datasent);
			datasent.flip();
			
	    	int dimbuff = datasent.getInt();
	    	if(dimbuff<0) return (short) Math.abs(dimbuff);// vedi login
	    	
	    	ByteBuffer buffer = ByteBuffer.allocate(dimbuff);
			
			socket.read(buffer);
			buffer.flip();
			byte[] bytes = new byte[dimbuff];
			buffer.get(bytes);
		
			String[] response = jsonparser.fromJson(new String(bytes),String[].class);
			
			listMembers.clear();
			for(String s : response) listMembers.addElement(s);
	
		} catch(JsonSyntaxException e) {
			return 52; // Bad request
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(1);
		}
		return 28;
	}

	@Override
	public short addCard(String name, String descr, int lvl) {
		// max 208byte =  40+30+120+1+4+13
		if(name==null||descr==null|lvl<0||lvl>WorthSupport.MAXPRIORITY) return 85;
		if(name.trim().isBlank() || descr.trim().isBlank()) return 85;		
		// supera la lunghezza massima
		if(name.length()>WorthSupport.CARDMAX || descr.length() > WorthSupport.DESCMAX) return 19;
		
		String[] request = {nameProject,name,descr,String.valueOf(lvl)};
		return sendReceiveRequest(8,request);
	}

	@Override
	public short showCards(DefaultTableModel cardListModel) {
		
		if(cardListModel==null) return 85;
		
		// max payload 47byte = 4+40+3
		payload.clear();
		payload.putInt(9); // Code op
		
		payload.put(jsonparser.toJson(nameProject).getBytes());
		payload.flip();
		
		try {	
			while(payload.hasRemaining()) socket.write(payload);

			datasent.clear();
			socket.read(datasent);
			datasent.flip();
			
	    	int dimbuff = datasent.getInt();
	    	if(dimbuff<0) return (short) Math.abs(dimbuff);// vedi login
	    	
	    	ByteBuffer buffer = ByteBuffer.allocate(dimbuff);
			
			socket.read(buffer);
			buffer.flip();
			byte[] bytes = new byte[dimbuff];
			buffer.get(bytes);
		
			// json ti list of set (card)
			Type cardstype = new TypeToken<ArrayList<HashSet<Card>>>(){}.getType();	
			List<Set<Card>> response = jsonparser.fromJson(new String(bytes),cardstype);
						
			// Elimino la vecchia tabella 
			int num = cardListModel.getRowCount();
			for(int i=0;i<num;i++) cardListModel.removeRow(0);
				
			// graficamente aggiungo quella nuova
			for(int i=0;i<response.size();i++) {
				for(Card s: response.get(i)) {
					String[] row = {s.getTitle(),String.valueOf(s.getPriority()),WorthSupport.getStringState(i),s.getDescr()};
					cardListModel.addRow(row);
				}
			}
		} catch(JsonSyntaxException e) {
			return 52; // Bad request
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(1);
		}
		return 28;
	}

	@Override
	public short moveCard(String name, int from, int to) {
		//max payload 85byte  40+30+1+1+13
		if(name==null||name.trim().isBlank()||from<0||to<0) return 85;
		
		if(name.length()>WorthSupport.CARDMAX) return 19;
		
		// le operazioni permesse sono speficate nell'progetto
		if(from==0 && to!=1) return 11;
		if(from==1 && to!=2 && to!=3) return 11;
		if(from==2 && to!=1 && to!=3) return 11;
		if(from==3) return 11;
				
		String[] request = {nameProject,String.valueOf(from),String.valueOf(to),name};
		return sendReceiveRequest(10,request);
	}
	
	@Override
	public short deleteCard(String name) {
		// max 77byte = 40+30+7
		if(name==null||name.trim().isBlank()) return 85;		
		// supera la lunghezza massima
		if(name.length()>WorthSupport.CARDMAX) return 19;
		
		String[] request = {nameProject,name};
		return sendReceiveRequest(11,request);	
	}
	@Override
	public short historyCard(String name, DefaultTableModel historyCardModel) {
		// max payload 77byte 40+30+7
		if(name==null||name.trim().isEmpty()) return 85;

		if(name.length()>WorthSupport.CARDMAX) return 19;
		
		String[] request = {nameProject,name};
		String tmp = jsonparser.toJson(request,String[].class);
		payload.clear();
		payload.putInt(12);
		payload.put(tmp.getBytes());
		payload.flip();
		
		try {
			
			while(payload.hasRemaining()) socket.write(payload);
	    	
	    	datasent.clear();
			socket.read(datasent);
			datasent.flip();
			
	    	int dimbuff = datasent.getInt();
	    	if(dimbuff<0) return (short) Math.abs(dimbuff); // vedi login
	    	ByteBuffer buffer = ByteBuffer.allocate(dimbuff);
			
			socket.read(buffer);
			buffer.flip();
			byte[] bytes = new byte[dimbuff];
			buffer.get(bytes);
			
			// json to list of history
			Type historytype = new TypeToken<List<History>>(){}.getType();
			List<History> response = jsonparser.fromJson(new String(bytes),historytype);
			
			// Elimino la vecchia tabella 
			int num = historyCardModel.getRowCount();
			for(int i=0;i<num;i++) historyCardModel.removeRow(0);
			// Graficamente aggiungo quella nuova
			if(response!=null)for(History h : response) {
				String[] s = {WorthSupport.getStringState(h.getFrom()),WorthSupport.getStringState(h.getTo()),String.valueOf(h.getDate())};
				historyCardModel.addRow(s);	    	
			}

		} catch(JsonSyntaxException e) {
			return 52; // Bad request
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,e.getMessage());
			System.exit(1);
		}
		return 28;
	}
	@Override
	public void readytoChat(JTextArea msgHistory) {
		
		// le informazioni per il gruppo e la porte le ho recuperate in "login"
		try {
			multicastSocket = new MulticastSocket(multicastPort);		
			multicastSocket.setTimeToLive(1);
			multicastGroup  = InetAddress.getByName(multicastAddr);
			
			// Gli passo l'oggetto dove graficamente andrà a scrivere i messaggi.
			chat = new ChatHandler(multicastAddr,multicastPort,msgHistory);
			chat.setDaemon(true);
		} catch (WrongArgumentException|IOException e) { e.printStackTrace(); }
		chat.start();
		
	}
	@Override
	public short sendMessage(String msg) {
			
		if(msg==null||msg.trim().isBlank()) return 85;
		
		String text = username+": "+msg+".\n";
		if(text.length()>WorthSupport.CHATBUFFERMAX) return 19;
		
		byte[] buffer = new byte[WorthSupport.CHATBUFFERMAX];
		buffer = text.getBytes();
		
		try {
			DatagramPacket packet = new DatagramPacket(buffer,text.length(),multicastGroup,multicastPort);
		
			multicastSocket.send(packet);
		} catch (IOException e) { e.printStackTrace(); }
			
		return 28;
	
	}
	public String codeToText(short code,String correctText) {
		switch(code) {
		
		case 28: return correctText;
		case 85: return "Gli argomenti non sono corretti";
		case 44: return "L'utente è già inscritto alla piattaforma";
		case 99: return "L'utente non è presente nella piattaforma";
		case 29: return "E' già stato creato un progetto con questo nome";
		case 95: return "Non sono riuscito a trovare il progetto";
		case 33: return "La card non è presente nel progetto o nella lista selezionata";
		case 97: return "E' già presente una card con questo nome nel progetto";
		case 19: return "Il numero di caratteri è superiore a quello permesso";
		case 22: return "Non si dispone dei permessi per effettuare l'operazione";
		case 11: return "L'operazione non è permessa";	
		case 14: return "E' stato riscontrato un errore nel server";
		case 27: return "Ci sono ancora Card che devono essere completate";
		case 12: return "La password non è corretta";
		case 88: return "Il progetto è stato cancellato";
		case 51: return "Bad json request";
		case 52: return "Bas json response";
		case 55: return "Ip multicast terminati";
		default: return "Errore non riconosciuto "+code; 
		}
	}
		
	public String getNameProject() { return nameProject; }
	
	public String getUsername() { return username; }
	
	public boolean isIslogged() { return islogged; }

	public String getIpChat() { return multicastAddr; }

	public int getPortChat() { return multicastPort; }

}