package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class WorthSupport {

	public static final int REGISTRY_PORT = 5012;
	
	public static final int TCP_PORT = 8463;
	public static final String TCPIP = "127.0.0.1";
	
	public static final String NAME_RMISERVICES = "RmiServices";
	public static final String NAME_RMICALLBACKS = "RmiCallbacks";
	
	public static final int MULTICASTPORT = 58959;
	public static final int[] MULTICASTIPDEFAULT = {239,0,0,1};
	
		public static final int USERMAX = 20;        // Max chars Utente
		public static final int PRJMAX  = 40;        // Max chars nome dell' progetto
		public static final int CARDMAX = 30;        // Max chars nome della Card
		public static final int DESCMAX = 120;       // Max chars nella descrizione
		public static final int MAXPRIORITY = 5;     // Max priorita' di una card
	
	public static final int CHATBUFFERMAX = 200; // Max caratteri permessi in un messaggio
	
	public static final int NUMOFLIST = 4;       // Numer di liste
	
	// periodo di sleep , del thread che effettua gli aggiornametni sui file
	public static final int TIMEFLUSH = 5000;
	
	public static final boolean RESTORE = true;
	
	public static void deleteDir(File file) {
	    File[] contents = file.listFiles();
	    if (contents != null) for (File f : contents) deleteDir(f);
	    file.delete();
	}
	public static void savefile(Path path, String json) throws IOException {
		Files.deleteIfExists(path);
		Files.write(path,json.getBytes(),StandardOpenOption.CREATE_NEW);
	}
	public static String encode(String str,String method) { // hashing	
		MessageDigest md = null;
		StringBuilder hexString = new StringBuilder();
		try {
			md = MessageDigest.getInstance(method);
		} catch (NoSuchAlgorithmException e) { e.printStackTrace(); } 
		md.update(str.getBytes());
		
		byte[] binhash = md.digest();

		for (int i = 0; i < binhash.length; i++) {
			String hex = Integer.toHexString(0xFF & binhash[i]);
			
			if (hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
	public static String getStringState(int i) {
		switch(i) {
			case 0 : return "To Do";
			case 1 : return "In Progress";
			case 2 : return "To Be Revised";
			case 3 : return "Done";
		}
		return "--";
	}
	/*
	 * CODICI DI RITORNO
	 * 
	 * 28 OK OPERAZIONE EFFETTUATA
	 * 85 ARGOMENTI NON CORRETTI
	 * 44 UTENTE GIA' ESISTENTE
	 * 99 UTENTE INESISTENTE
	 * 29 PROGETTO GIA' ESISTENTE
	 * 95 PROGETTO NON TROVATO
	 * 33 CARD INESISTENTE
	 * 97 CARD GIA' PRESENTE
	 * 19 NUMERO DI CARATTERI SUPERIORI AL TESTO PERMESSO
	 * 22 NON SI DISPONE PERMESSI PER EFFETTURARE L'OPERAZIONE
	 * 11 OPERAZIONE NON PERMESSA	
	 * 14 ERRORE NEL SERVER
	 * 27 TASK DA COMPLETARE
	 * 12 PASSWORD NON CORRETTA
	 * 88 IL PROGETTO NON ESISTE PIU'
	 * 55 IP MUTLICAST TERMINATI
	 * 51 BAD JSON REQUEST
	 * 52 BAD JSON RESPONSE
	 */

}