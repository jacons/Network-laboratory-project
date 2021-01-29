package interfaces;

public interface IUser {

	/**
	 * Verifica la correttezza della password byte x byte,
	 * restituisce true se la passwors  e' corretta, false altrimenti
	 * @param pass
	 * @throws IllegalArgumentException	  
	 */
	public boolean trypassword(String pass) throws IllegalArgumentException;

	/**
	 * Imposta la funzione a cui fare ack ed il suo relatovo risultato
	 * @param ackcode
	 * @param ackresult
	 */
	public void setAck(short ackcode, short ackresult);

	/**
	 * Restitusce la coppia < Funzione a cui fare ack , Codice di ritorno >
	 */
	public short[] getAck();

	public String getUsername();

	public String getPassword();

	public int getSeed();

	/*
	 * Restituisce lo stato attuale dell'utente sotto forma di valore di verita'
	 */
	public boolean Status();

	public void putLogin();

	public void putLogout();

}