package server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import exceptions.CardNotFoundException;
import exceptions.FullStackMulticastIpException;
import exceptions.ProjectNotFoundException;
import exceptions.WrongArgumentException;
import interfaces.IProject;
import interfaces.RMIClientsState;
import main.WorthSupport;

public class TCPServices extends Thread {
	
	/*
	 * Overview: Rappresenta la parte del server dove vengono ricevute le richieste di
	 * 			 operazioni da parte dei client attraverso la comunicazione TCP esplicita.
	 * 			 Le richieste/risposte sono rappresentate da informazioni strutturare 
	 * 			 secondo lo standard json. 
	 * 
	 * Representation invariant: clients!=null &&
	 * 							 forall(c in clients. c.Key()!=null && c.Value()!=null) &&
	 * 
	 * 							 subscriber!= null &&
	 * 							 forall(cs in subscriber. cs!=null) &&
	 * 
	 * 							 codeOp!= null && payload!=null && socket!=null && selector!=null &&
	 * 							 ipGenerator!=null && jsonparser!=null &&
	 * 
	 * 							 projects!=null && forall(p in projects. p.Key()!=null && p.value()!=null)
	 * 
	 * 
	 * 
	 */
	
	private  Map<String,User> clients; // inizilizzata in "WorthServer" 

	private List<RMIClientsState> subscriber;   // inizilizzata in "WorthServer" 
	
    protected ByteBuffer codeOp;  // Operazioni in entrata & ack in uscita
    
    protected ByteBuffer payload; // Buffer di invio/recezione delle richieste/risposte.
	                              // Alcune funzioni richiedono l'istaurazione di un proprio buffer.

	private ServerSocketChannel socket;
	private Selector selector;
	
	protected MulticastIPGenerator ipGenerator; // Gestisce l'indirizzamento multicast
	
	private Map<String,Project> projects; // Struttura per mantenere i progetti

	protected Gson jsonparser;
	
	public TCPServices(Map<String,User> clients, List<RMIClientsState> sub) throws IOException {
		
		this.clients = clients;
		this.subscriber = sub;
		
		projects = new TreeMap<String,Project>();
		
		
		try {
			selector = Selector.open();
			
			socket = ServerSocketChannel.open();
            ServerSocket ss = socket.socket();

            ss.bind(new InetSocketAddress(WorthSupport.TCP_PORT));
            
            socket.configureBlocking(false);
            int op = socket.validOps();
            socket.register(selector,op,null);
            
		} catch (IOException e) { throw new IOException(e.getMessage());}
			
		try {
			//inizializzo la struttura nel fs
			Files.createDirectories(Paths.get("cache"));	
			Files.createFile(Paths.get("cache/clients.lst"));
			Files.createFile(Paths.get("cache/project.lst"));
		} catch (FileAlreadyExistsException e2 ) {} 
		catch (IOException e) { e.printStackTrace(); }
		
		codeOp =  ByteBuffer.allocateDirect(4);
		payload = ByteBuffer.allocateDirect(209); 
		
		ipGenerator = new MulticastIPGenerator();
		jsonparser = new Gson();
		
		if(WorthSupport.RESTORE) restore();
	}
	
	/**
	 * La mia implementazinoe prevede una cartella "cache", dove al suo interno sono presenti 2 file
	 * "client.lst" e "project.lst" tutti e due contengono informazioni in formato json.
	 * (per la spiegazione completa si rimanda alla relazione)
	 */
	protected void restore() {
		
		if(Files.exists(Paths.get("cache/clients.lst"))) {
			try {
				// Funziona per file di dimenzione minore di 2GB (vedi specifica)
				byte[] bytes = Files.readAllBytes(Paths.get("cache/clients.lst"));
				
				if(bytes.length==0) return;
				
				Type usertype = new TypeToken<Collection<User>>(){}.getType();
				Collection<User> users = jsonparser.fromJson(new String(bytes),usertype);
				
				if(users==null) throw new NullPointerException();
				
				for(User u : users) {
					
					if(u.getUsername().length()>WorthSupport.USERMAX && u.getPassword().length()!=64 &&
							u.getSeed()>1000 && u.getSeed()<0) continue;					
					clients.putIfAbsent(u.getUsername(), u);
					System.out.println("Ripristinato client "+u.getUsername());
				}
			} catch (JsonSyntaxException|NullPointerException e) {
				System.out.println("Il file dei client � corrotto! "+e.getMessage());
				return;
			} catch (IOException e) {
				System.out.println(e.getCause());
				return;
			} catch (IllegalArgumentException e) {
				System.out.println("Il file dei progetti e' corrotto.");
				return;
			}
			
		} else {
			System.out.println("File dei clienti non trovato");
			return;
		}
		
		if(Files.exists(Paths.get("cache/project.lst"))) {
			
			String[][] projects=null;
			try {
				byte[] bytes = Files.readAllBytes(Paths.get("cache/project.lst"));
				if(bytes.length==0) return;

				// Gli indirizzi multicast sono creati dinamicaminamente
				projects = jsonparser.fromJson(new String(bytes),String[][].class);
				
				if(projects==null) throw new NullPointerException();
			} catch (IOException e) {
				System.out.println("Non e' stato possibile leggere il file dei progetti.");
				return;
			} catch (JsonSyntaxException|NullPointerException e) {
				System.out.println("Il file dei progetti e' corrotto.");
				return;
			}
					
			for(String[] project_arg : projects) { // per ogni progetto trovato
				// Se i parametri non sono giusti salta questo progetto.
				if(	project_arg[0]!=null && project_arg.length==3 &&!project_arg[0].trim().isBlank() &&
					project_arg[0].length()<=WorthSupport.PRJMAX &&
					Integer.valueOf(project_arg[1].length())==64 &&
					Integer.valueOf(project_arg[2])>=0 && Integer.valueOf(project_arg[2])<1000) {

					// PRIMA DI CREARE IL PROGETTO, CONTROLLO CHE RIESCA A RECUPERARE I MEMBRI
					
					// Tento il ripristino dei membri
					if(Files.exists(Paths.get("cache/"+project_arg[0]+"/member.lst"))) {
						
						String[] members=null;boolean memberCheck = true;
						try {
							byte[] bin_member = Files.readAllBytes(Paths.get("cache/"+project_arg[0]+"/member.lst"));
							
							members = jsonparser.fromJson(new String(bin_member),String[].class);
							if(members==null) throw new NullPointerException();
							
							for(String m : members) {
								if(m==null) throw new NullPointerException();
								if(m.length()>WorthSupport.USERMAX)  throw new IllegalArgumentException();
							}
							
						} catch (IOException e) {
							System.out.println("Non e' stato possibile leggere il file dei membri.");
							memberCheck=false;
						} catch (JsonSyntaxException|NullPointerException e) {
							System.out.println("Il file dei membri e' corrotto.");
							memberCheck=false;
						} catch (IllegalArgumentException e) {
							System.out.println("Il file dei membri non rispetta le spefiche.");
							memberCheck=false;
						}
						// ORA CHE SONO SICURO DI AVERE I MEMBRI CORRETTI, CREO IL PROGETTO E RECUPERO LE CARD
						if(memberCheck) {
							short code = 0;
							try {
								// 28 -> tutto ok 
								code = newInsideProject(project_arg[0],project_arg[1],null,Integer.valueOf(project_arg[2]));																
							} catch (IOException e) {
								System.out.println("Progetto non e' stato ripristinato "+project_arg[0]+"\n"+e.getMessage());
							}
							if(code!=28) {
								System.out.println("Progetto non e' stato ripristinato "+project_arg[0]);
							} else {
								System.out.println("Progetto ripristinato: "+project_arg[0]);
								
								// Se l'utente non esiste nella piattaforma non viene aggiunto.
								for(String m : members) addInsideMember(project_arg[0],m,false);
								
								// Se il progetto � stato ripristinato allora ripristino alle le cards
								File cards = new File("cache/"+project_arg[0]);
								for (File fcard : cards.listFiles()) {
							        if (fcard.isFile() && fcard.getName().endsWith(".json")) {
							        								        	
										try {
											byte[] bin_card = Files.readAllBytes(Path.of("cache/"+project_arg[0]+"/"+fcard.getName()));
											
											try {
												Card card = jsonparser.fromJson(new String(bin_card),Card.class);
												
												if(card!=null) {
													this.projects.get(project_arg[0]).restoreCard(card);
									        		System.out.println("Ripristinata card "+card.getTitle());									
												}											
											} catch (JsonSyntaxException e) {
												System.out.println("Il file della card � corrotto.");
											} catch (IllegalArgumentException e) {
												System.out.println("La card non rispetta i parametri.");
											}													
										} catch (IOException e) {
											System.out.println("Non e' stato possibile recuperare la card "+fcard.getName());
										}						        	
							        }
							    }	
							}						
						}
					} else {
						System.out.println("Il file dei membri non e' stato trovato,"
											+ "il progetto non e'  stato caricato "+project_arg[0]);
					}		
				} else {
					System.out.println("Parametri dei progetto non corretti");
				}
			}
		} else {
			System.out.println("File del progetti non trovato");
		}			
	}
	
	@Override
	public void run() {
		while(true) {
			System.out.println("---In attesa di richieste---");
			try { selector.select(); } 
	        catch (IOException ex) {ex.printStackTrace();break;}
			
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator <SelectionKey> iterator = readyKeys.iterator();

			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				try {
					if (key.isAcceptable()) {				
						ServerSocketChannel server = (ServerSocketChannel) key.channel();
						SocketChannel client = server.accept();
						System.out.println("Accepted connection from " + client);
						client.configureBlocking(false);
						client.register(selector,SelectionKey.OP_READ);
					} 
					else if (key.isReadable()) {				
						 // client ha dati per il server (richiesta di un servizio)							
						 SocketChannel client = (SocketChannel) key.channel();
						 // inizializzo il buffer e mi recupero il codice dell'operazione richiesta
						 codeOp.clear();   
						 client.read(codeOp);
						 codeOp.flip();
						 
						 int code = codeOp.getInt(); // recupero il codice dell'operazine richiesta						 
						 services(code,client,key);
						 
					 } else if (key.isWritable()) {
						 SocketChannel client = (SocketChannel) key.channel();
						 
						 String nameclient = (String) key.attachment();
						 
						 // recuero l'operazione che deve essere "confermata"
						 // se -1 non devo confermare niente altrimenti l'operazione da confermare
						 // corrisponde esattamente al code della fuzione richiesta
						 short[] ack = clients.get(nameclient).getAck();
		           			
						 codeOp.clear();   
						 codeOp.putShort(ack[0]); // ack dell operazione 
						 codeOp.putShort(ack[1]); // Code dell' esito dell'operazione
						 codeOp.flip();
						 
						 client.write(codeOp);
							 
						 // segno che ack � stato gi� mandato
						 clients.get(nameclient).setAck((short)-1,(short)0);							 
						 
						 client.register(selector,SelectionKey.OP_READ);
						 key.attach(nameclient);               	    
					 }
               } catch (IOException ex) { key.cancel();
               
               try { key.channel().close(); } catch (IOException cex) {} }
           }
       }			
	}
	
	/**
	 * Da il primi 4 byte castati come int, definisco le operazioni che devo eseguire.
	 * La classe e'  protected per un eventuale estenzione per uleriori funzionalita'.
	 */
	protected void services(int code, SocketChannel client, SelectionKey key) throws IOException {
		 // Decido che operazione eseguire in base al codice corrispondente
		switch(code) {
		 	
			 case  0 : login(client,key);              break;
			 case  1 : logout(client,key);			   break;
			 case  2 : projectList(client,key);        break;
			 case  3 : newProject(client,key);         break;
			 case  4 : openProject(client,key);        break;							
			 case  5 : projectDelete(client,key);      break;
			 case  6 : addMember(client,key);          break;
			 case  7 : memberList(client,key);         break;
			 case  8 : newCard(client,key);            break;
			 case  9 : cardList(client,key);           break;
			 case 10 : moveCard(client,key);           break;
			 case 11 : cardDelete(client,key);         break;
			 case 12 : historiesCardList(client,key);  break;
		 }	
	}
	/**
	 * Manda la risposta al client.
	 * il formato di msg viene stabilito all'occorrenza(in generale json). 
	 * @param client -> Socket
	 * @param msg -> array di byte da inviare
	 * @throws IOException
	 */
	private void sendJsonResponse(SocketChannel client,String msg) throws IOException {
		
	    payload.clear();
	    payload.putInt(msg.length()); //Prima viene mandata la dimenzione del payload successivo
	    payload.flip();
	    
	    client.write(payload); 
	    ByteBuffer tmp = ByteBuffer.wrap(msg.getBytes()); 
	    while(tmp.hasRemaining()) client.write(tmp);	
	}
	/**
	 * Manda un codice di ritorno al client,il codice di ritorno sara' sempre c<0.
	 * @param client
	 * @param Code
	 * @throws IOException
	 */
	private void sendNegativeCodeResponce(SocketChannel client,int Code) throws IOException  {
		
		if(Code>0) Code = -Code;
		codeOp.clear();
		codeOp.putInt(Code);
		codeOp.flip();
		client.write(codeOp);
	}

	private void login(SocketChannel client, SelectionKey key) throws IOException {
		
		// in  -> max buffer 95byte 20(user) + 64(pass) + 7(json) + 4(codeop)
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);
		
		try {
			// nome + password
			String[] request = jsonparser.fromJson(new String(bytes),String[].class);
				    
			if(request==null) throw new NullPointerException();
			else {
				
				if(request[0]==null||
				   request[1]==null||
				   !clients.containsKey(request[0]) ||
				   !clients.get(request[0]).trypassword(request[1])) throw new WrongArgumentException();
				
				// ok il client ha le credenziali giuste
				
				clients.get(request[0]).putLogin(); // metto online
				
				// mi creo la lista di utenti 
				List<String[]> registeruser = new ArrayList<String[]>(clients.size());
				for(Entry<String,User> e : clients.entrySet()) {	
					String[] user = {e.getKey(),(e.getValue().Status())?"1":"0"};
					registeruser.add(user);
				}
				
				String json = jsonparser.toJson(registeruser);
				sendJsonResponse(client,json);
				
				for(RMIClientsState c : subscriber) c.notifyEvent(request[0],clients.get(request[0]).Status());
				
				key.attach(request[0]);
			}
		} catch(WrongArgumentException e) {
			sendNegativeCodeResponce(client,85);
			return;
			
		} catch(JsonSyntaxException|NullPointerException e) {
			sendNegativeCodeResponce(client,51);
			return;
		}
	}
	/**
	 * Effettua il logout, principalmente serve per aggiornare gli altri utenti ed rimuovere la chiave
	 * @param client
	 * @param key
	 * @throws IOException
	 */
	private void logout(SocketChannel client, SelectionKey key) throws IOException {
		// 0 byte
	    String nameclient = (String)key.attachment();
	    
	    // nel caso non abbia effettutato il login, nameclinet == null 
	    // cancello solo la chiave
	    if(nameclient!=null) {  
		    if(clients.containsKey(nameclient)) {
				clients.get(nameclient).putLogout();
				clients.get(nameclient).setAck((short)-1,(short)0);	  	    	
		    }
		    
			for(RMIClientsState user : subscriber) {	
				try { user.notifyEvent(nameclient,clients.get(nameclient).Status());
				} catch (RemoteException e) { e.printStackTrace(); System.exit(1); }
			}    	
	    }

		key.cancel();
	}
	/**
	 * Crea un nuovo progetto se non esistente,ed aggiunge come primo membro 
	 * colui che l'ha creato. 
	 * 85 Argomenti non corretti 29 Progetto gia' esistente 28 Operazione effettutata
	 * 14 Errore nel server 55 Ip multicast terminati 51 Bad request
	 * @param client
	 * @param key
	 * @throws IOException
	 */
	private void newProject(SocketChannel client,SelectionKey key) throws IOException {
		// in  -> max buffer 115byte 40(project) + 64(pass) + 4(codeop) + 7(json)
		// out -> code op
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);
		
		String nameclient = (String)key.attachment();
		try {
			String[] request = jsonparser.fromJson(new String(bytes),String[].class);
						
			short code = newInsideProject(request[0],request[1],nameclient,-1);
			clients.get(nameclient).setAck((short) 1,code);

			if(code==28) { // se effettivamente � stato creato un nuovo progetto
				// Aggiorno la lista dei progetti
				String[][] project = new String[projects.size()][3];
				int i=0;
				for(Entry<String, Project> entry : projects.entrySet()) {
					project[i][0]     = entry.getKey();
					project[i][1]   = entry.getValue().getPassword();
					project[i++][2]   = String.valueOf(entry.getValue().getSeed());
					
				}
				String json = jsonparser.toJson(project);
				WorthSupport.savefile(Path.of("cache/project.lst"), json);	
			}
		} catch(JsonSyntaxException|NullPointerException e) {
			clients.get(nameclient).setAck((short) 1,(short)51);
		}
		  
        client.register(selector,SelectionKey.OP_WRITE);
        key.attach(nameclient);

	}	
	/**
	 * Crea effettivamente il progetto restituendo i codici di ritorno.
	 * usato anche per effettuare il restore senza passare per TCP.
	 */
	private short newInsideProject(String title,String passw,String member,int seed) throws IOException {
		
		if(title==null||passw==null) return 51;
		
	    if(projects.containsKey(title)) return 29; //progetto gi� esistente	 
	    else {
	    	try {
	    		String ipmulticast = ipGenerator.generateIP();
	    		if(ipmulticast==null) throw new FullStackMulticastIpException();
	    		// aggiungo al progetto come primo membro, il creatore
	    		Project p = new Project(title,passw,member,ipmulticast,ipGenerator.generatePort(),seed);
		    	projects.put(title,p); 
		    	return 28; // ok 
	    	} catch (WrongArgumentException e) {
	    		System.out.println(e.getMessage()); // x debug
	    		return 85; // errore per la creazione dello storage
	    	} catch (FullStackMulticastIpException e) {
	    		return 55;
			}
	    }	
	}
	
	/**
	 * Controlla che il progetto esista e se l'utente abbia a disposizione i permessi per accedere al progetto.
	 * 95 Progetto inesistente 22 Non e' membro 28 Operazione effettutata 88 Il progetto non esiste piu'
	 * 51 Bad request
	 * @param client
	 * @param key
	 * @throws IOException
	 */
	private void openProject(SocketChannel client, SelectionKey key) throws IOException {
		// in  -> max buffer 44byte 40(project) + 2(json) + 4(codeop)
		// out -> max buffer 19byte 2(code op) + 5(port) + 12(ip) (string)
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);
		
	    String nameclient = (String)key.attachment();
	    String[] response = new String[3];
	    
		try {
			String title = jsonparser.fromJson(new String(bytes),String.class);
			
		    if(title==null) throw new NullPointerException();
		    
		    if(!projects.containsKey(title)) throw new ProjectNotFoundException();
		    
		    // se esiste verifichiamo che � membro
		    if(projects.get(title).memberContains(nameclient)) {
		    	// ok sei entrato
		    	response[0] = String.valueOf(28);
		    	response[1] = String.valueOf(projects.get(title).getMulticastIp());
		    	response[2] = String.valueOf(projects.get(title).getMulticastPort());
		    	if(response[2].length()!=5) response[2]="0"+response[2];
		    } else { 
		    	// il client non � membro
		    	response[0] = String.valueOf(22);
		    	response[1] = null;
		    	response[2] = null;
		    }
		} catch(JsonSyntaxException|NullPointerException e) {
	    	response[0] = String.valueOf(51); // Bad request 
	    	response[1] = null;
	    	response[2] = null;
		} catch (ProjectNotFoundException e) {
		    // se il progetto non esisite restituisco il messaggio errore
	    	response[0] = String.valueOf(95);
	    	response[1] = null;
	    	response[2] = null;
		}	    
	    String json = jsonparser.toJson(response);
	    sendJsonResponse(client,json);
	    
	}
	/**
	 * Restituisce l'insieme dei progetti di un determinatato utente.
	 */
	private void projectList(SocketChannel client, SelectionKey key) throws IOException {
		// 0 byte (l'informazione che gli serve � nella key)
		payload.clear();
		
	    String nameclient = (String)key.attachment();
	
	    List<String> myprj = new ArrayList<String>();
	    
	    for(IProject p : projects.values()) {
	    	if(p.memberContains(nameclient)) myprj.add(p.getTitle());
	    }
	    
	    String json = jsonparser.toJson(myprj);   	    
	    sendJsonResponse(client,json);	
	}
	/**
	 * Rimuove in progetto dalla piattaforma se e solo se tutte le card sono completate, e se la passoword corrispone.
	 * 27 Task da completare 12 Password scorretta 28 Operazione effettutata 14 Errore nel server 88 Il progetto non esiste piu'
	 * 51 Bad request
	 * @param client
	 * @param key
	 * @throws IOException
	 */
	private void projectDelete(SocketChannel client,SelectionKey key) throws IOException {
		// in  -> 104byte  40(project) + 64(pass) 
		// out -> Code op
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);

	    String nameclient = (String)key.attachment();
	    try {
	   
	    	String[] request = jsonparser.fromJson(new String(bytes),String[].class);
			
			if(request==null||request[0]==null||request[1]==null)
				throw new NullPointerException();
			
			if(!projects.containsKey(request[0]))
				throw new ProjectNotFoundException();

			if(!projects.get(request[0]).trypassword(request[1]))
				throw new WrongArgumentException();
			
			
		    if(!projects.get(request[0]).isCardComplete())  {
		    	// Ci sono ancora task da completare
		    	clients.get(nameclient).setAck((short) 11,(short)27);

		    } else {
		    	try {
		    		
		    		WorthSupport.deleteDir(new File("cache/"+request[0]));
		    		
		    		ipGenerator.removeIP(projects.get(request[0]).getMulticastIp());
		    		projects.remove(request[0]);
		    		
		    		// Aggiorno la lista dei progetti
		    		String[][] project = new String[projects.size()][2];
		    		int i=0;
		    		for(Entry<String, Project> entry : projects.entrySet()) {
		    			project[i][0]   = entry.getKey();
		    			project[i][1]   = entry.getValue().getPassword();
		    			project[i++][2]   = String.valueOf(entry.getValue().getSeed());
		    		}
		        	String json = jsonparser.toJson(project);
		        	WorthSupport.savefile(Path.of("cache/project.lst"), json);
		        	
		    		clients.get(nameclient).setAck((short) 11,(short)28);
		    	} catch(Exception e) {
		    		clients.get(nameclient).setAck((short) 11,(short)14);
		    	}
		    }	
	    } catch (JsonSyntaxException|NullPointerException e ) {
	    	// bad request
	    	clients.get(nameclient).setAck((short) 11,(short)51); 
	    } catch (ProjectNotFoundException e1) {
	    	// Il progetto non esiste pi�
	    	clients.get(nameclient).setAck((short) 11,(short)88);
		} catch (WrongArgumentException e1) {
	    	// la password non � corretta
	    	clients.get(nameclient).setAck((short) 11,(short)12);
		}
		
	    client.register(selector,SelectionKey.OP_WRITE);
	    key.attach(nameclient);	
	}

	
	/**
	 * Aggiunge un membro nel progetto se non gia' esistente,altrimenti lo ignora
	 * 99 Non e' iscritto 14 Errore nel server 28 Operazione effettuata 88 Il progetto non esiste piu'
	 * 51 Bad request
	 * @throws IOException
	 */
	private void addMember(SocketChannel client,SelectionKey key) throws IOException {
		// in  -> max buffer 71byte 20(name) + 40(project) + 4(Code op) + 7(json)
		// out -> Code op
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);
		
	    String nameclient = (String)key.attachment();	
		try {
			
			String[] request = jsonparser.fromJson(new String(bytes),String[].class);	
			
			if(request==null) throw new NullPointerException();
				
		    short code = addInsideMember(request[0],request[1],true);
			clients.get(nameclient).setAck((short) 4,code);
	
		} catch (JsonSyntaxException|NullPointerException e ) {
			clients.get(nameclient).setAck((short) 4,(short)51);	
		}
	    client.register(selector,SelectionKey.OP_WRITE);
	    key.attach(nameclient);	
	}
	
	/**
	 * Effettua effettivamente l'aggiunta del membro restituendo i codici di ritorno.
	 * usato anche per effettuare il restore senza passare per TCP.
	 * se Storage == false, non viene modificato il file dei membri
	 * @return
	 */
	private short addInsideMember(String project,String name,boolean storage) {
		
		if(project==null||name==null) return 51; // Bad request
		
 		if(!projects.containsKey(project)) return 88; // Il progetto non esiste
 		
		if(!clients.containsKey(name)) return 99; // non � iscritto 
	    	
	    try { projects.get(project).addMember(name,storage); } 
	    catch(IllegalArgumentException e) {return 14;}
	    	
	    return 28; // ok
	}
	
	/**
	 * Restituisce la lista dei membri appartenenti ad un progetto 
	 * 88 Il progetto non esiste piu'
	 * 51 Bad request
	 * 
	 */
	private void memberList(SocketChannel client,SelectionKey key) throws IOException {
		//in -> max buffer 46byte 40(project) + 4(code op) + 2(json) 
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();
	
		byte[] bytes = new byte[lenght];
		payload.get(bytes);
		 
		try {
			String request = jsonparser.fromJson(new String(bytes),String.class);
			
			if(request==null) throw new NullPointerException();
			
			else if(!projects.containsKey(request))
				throw new ProjectNotFoundException();
			
			String[] members = projects.get(request).getMembers();
			
			String json = jsonparser.toJson(members, String[].class);
		    sendJsonResponse(client,json);	

		} catch (JsonSyntaxException|NullPointerException e ) {
			// Bad request
			sendNegativeCodeResponce(client,51);
			return;
		} catch (ProjectNotFoundException e) {
			// Se il progetto non esiste restituisco il codice di errore
			sendNegativeCodeResponce(client,88);
			return;
		}
		

	}
	/**
	 * Aggiunge una carta al progetto, se non esiste.
	 * 28 Operazione effettuata 97 Gia' esistente 85 Argomenti non corretti
	 * 14 Errore nel server 88 Il progetto non esiste piu'
	 * 51 Bad request
	 */
	private void newCard(SocketChannel client,SelectionKey key) throws IOException {
		//in -> max 208byte =  40(project) + 30(Card name)+ 120(descr) + 1(lvl) + 4(code op) + 13(json)
		//out -> Code op
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);
			
	    String nameclient = (String)key.attachment();
		
		try {
			String[] request = jsonparser.fromJson(new String(bytes),String[].class);
			// project -> name -> descr -> lvl
			
			if(request==null||request[0]==null||request[1]==null||request[2]==null||request[3]==null)
				throw new NullPointerException();
				
			if(!projects.containsKey(request[0]))
				throw new ProjectNotFoundException();
			
			boolean added = projects.get(request[0]).putcard(request[1],request[2],Integer.valueOf(request[3]));			
			clients.get(nameclient).setAck((short) 5,(added)?(short)28:(short)97);	// ok aggiunta / gia esiste
							
		} catch (WrongArgumentException e) {
			// errore nella creazione del file
			clients.get(nameclient).setAck((short) 5,(short)85);
		} catch (IllegalArgumentException e) {
			// errore nelle requires
			clients.get(nameclient).setAck((short) 5,(short)14);
		} catch (JsonSyntaxException|NullPointerException e ) {
			// bad request
		    clients.get(nameclient).setAck((short) 5,(short)51);
		} catch (ProjectNotFoundException e) {
	    	// Progetto non trovato
	    	clients.get(nameclient).setAck((short) 5,(short)88);
		}

	    client.register(selector,SelectionKey.OP_WRITE);
	    key.attach(nameclient);	
	}
	/*
	 * Sposta una Card da una lista all' altra
	 * 28 Operazione Effettutata 33 Card non trovata 88 Il progetto non esiste pi�
	 * 51 Bad request
	 */
	private void moveCard(SocketChannel client,SelectionKey key) throws IOException {
		//in ->max payload 85byte  40(project)+ 30(card name) + 1(from) + 1(to) + 13(json)
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);
		
		String nameclient = (String)key.attachment();
		try {		
			String[] request = jsonparser.fromJson(new String(bytes),String[].class);
			// project -> from -> to -> name
			
			if(request==null||request[0]==null||request[1]==null||request[2]==null||request[3]==null)
				throw new NullPointerException();
			
			if(!projects.containsKey(request[0]))
				throw new ProjectNotFoundException();
			
			
			boolean moved = projects.get(request[0]).moveCard(request[3],Integer.valueOf(request[1]),Integer.valueOf(request[2]));
			clients.get(nameclient).setAck((short) 6,(moved)?(short)28:(short)33);	// ok / non trovata
			
		} catch(IllegalArgumentException e) {
			// Errore nelle requires
			clients.get(nameclient).setAck((short) 6,(short)14);	
		} catch (JsonSyntaxException|NullPointerException e ) {
			// bad request
			clients.get(nameclient).setAck((short) 6,(short)51);
		} catch (ProjectNotFoundException e) {
	    	// Progetto non trovato
	    	clients.get(nameclient).setAck((short) 5,(short)88);
		}
		
	    client.register(selector,SelectionKey.OP_WRITE);
	    key.attach(nameclient);	
	}
	/**
	 * Rimuove una Card dal progetto.
	 * 28 Operazione effettutata 85 Argomenti non corretti 
	 * 88 Il progetto non esiste piu' 51 Bad request
	 * @param client
	 * @param key
	 * @throws IOException
	 */
	private void cardDelete(SocketChannel client,SelectionKey key) throws IOException {
		// in -> max 77byte = 40(project) + 30(Card name) + 7(json)
		// out -> Code op
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);
		
		String nameclient = (String)key.attachment();
				
		try {
			String[] request = jsonparser.fromJson(new String(bytes),String[].class);
			// project -> name
			
			if(request==null||request[0]==null||request[1]==null)
				throw new NullPointerException();
			
			if(!projects.containsKey(request[0]))
				throw new ProjectNotFoundException();
			
			projects.get(request[0]).deleteCard(request[1]);   
	    	Files.delete(Path.of("cache/"+request[0]+"/"+request[1]+".json"));
	    	clients.get(nameclient).setAck((short) 7,(short)28); // ok fatto 

		} catch (IllegalArgumentException|NoSuchFileException e) {
			// Errore nell requires  
		    clients.get(nameclient).setAck((short) 7,(short)85); 
		} catch (JsonSyntaxException|NullPointerException e ) {
			 //bad request
	    	clients.get(nameclient).setAck((short) 7,(short)51);
		} catch (ProjectNotFoundException e) {
	    	// Progetto non trovato
	    	clients.get(nameclient).setAck((short) 5,(short)88);
		}
		
	    client.register(selector,SelectionKey.OP_WRITE);
	    key.attach(nameclient);	
	}
	/**
	 * Restituisce l'insieme delle Card del progetto, ed tutte le loro relative informazioni
	 * 
	 */
	private void cardList(SocketChannel client,SelectionKey key) throws IOException {
		//in ->max payload 46byte = 4(Code op) + 40(project) + 2(json)
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);
		try {
			String request = jsonparser.fromJson(new String(bytes),String.class);
			
			if(request==null) throw new NullPointerException();
			
			if(!projects.containsKey(request))
				throw new ProjectNotFoundException();
		    
		    IProject project = projects.get(request);
		 
		    Gson build = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			String json = build.toJson(project.getCards());   
		    sendJsonResponse(client,json);	
	    
		} catch (JsonSyntaxException|NullPointerException e ) {
			sendNegativeCodeResponce(client,51);
			return;
		} catch (ProjectNotFoundException e) {
			sendNegativeCodeResponce(client,88);
			return;
		}
	}
	/**
	 * Restituisce l'insieme dei movimenti di una card.
	 * 
	 */
	private void historiesCardList(SocketChannel client, SelectionKey key) throws IOException   {
		// max payload 77byte 40(project) + 30(Card name) + 7(json)
		payload.clear();
		int lenght = client.read(payload);
		payload.flip();

		byte[] bytes = new byte[lenght];
		payload.get(bytes);
		try {
			
			String[] request = jsonparser.fromJson(new String(bytes),String[].class);	
			//project -> cardName 
			
			if(request==null||request[0]==null||request[1]==null)
				throw new NullPointerException();
			
			if(!projects.containsKey(request[0]))
				throw new ProjectNotFoundException();
		    
		    List<History> list = null;
			try {
				list = projects.get(request[0]).getHistoryCard(request[1]);
			} catch (IllegalArgumentException | CardNotFoundException e) { list = null;}
		    
		    String json = jsonparser.toJson(list);   
		    sendJsonResponse(client,json);	
		    
		} catch (JsonSyntaxException|NullPointerException e ) {
			sendNegativeCodeResponce(client,51);
			return;
		} catch (ProjectNotFoundException e) {
			sendNegativeCodeResponce(client,88);
			return;
		}
	}
}