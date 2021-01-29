package server;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import interfaces.IUser;
import main.WorthSupport;

public class User implements IUser {
	
	@SerializedName("User")

	/**
	 * Overview: Rappresenta lo stato dell' utente nella piattaforma WORTH,
	 * 			 che unisce il client TCP-based con il RMI-bases
				 oltre a gestire le classiche informazioni come il nome e la password
				 mantiene delle informazioni riguardo al codici di ritorno funzionalita' richieste dell'utente
	 * 
	 * Representation invariant: username!=null && password!=null &&  password.length()== 64chars &&
	 * 							 1000 > seed >= 0 && istance >=0
	 */
	
    @Expose 
	private final String username;
    @Expose
	private final String password; // 64byte, password in formato hash
	@Expose 
	private final int seed;
    
    
	
	
	// rappresenta il numero di instaze che il clinet ha aperto se 0 allora � offline
	private int instance = 0;
	// tipo di operazione a cui devo fare ack, se -1 (nessun ack da mandare)
	private short ackcode = -1;
	// Codice di ritorno dell'operazione effettuta,dipende dal tipo di operazione
	private short ackresult = 0;
	
	/**
	 * Crea un nuovo "Stato dell'utente".
	 * @param user
	 * @param pass
	 * @param state
	 * @throws IllegalArgumentException
	 */
	public User(String user,String pass,boolean state) throws IllegalArgumentException {
		
		if(user==null||pass==null) throw new IllegalArgumentException();
		
		username = user;
		seed = (int)(Math.random()*1000);
		password = WorthSupport.encode(pass+seed,"SHA-256");
		instance=0;
	}


	@Override
	public boolean trypassword(String pass) throws IllegalArgumentException  {
		
		pass = WorthSupport.encode(pass+seed,"SHA-256");
		if(pass==null) throw new IllegalArgumentException();
		return this.password.equals(pass);
	}

	@Override
	public void setAck(short ackcode,short ackresult) {
		this.ackcode = ackcode;
		this.ackresult = ackresult;
	}

	@Override
	public short[] getAck() {
		short[] rval = new short[2];
		rval[0] = ackcode;rval[1] = ackresult;
		return rval;
	}
	
	@Override
	public String getUsername() { return username; }
	
	@Override
	public String getPassword() { return password;}
	
	@Override
	public int getSeed() {return seed;}
	
	@Override
	public boolean Status() { return instance>0; }
	
	@Override
	public void putLogin() { instance++; }
	
	@Override
	public void putLogout() { instance--; }

}
