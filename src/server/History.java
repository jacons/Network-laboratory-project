package server;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.annotations.Expose;

public class History {
	/**
	 * Overview : Piccola struttura dove sono contenute le informazioni
	 * 			  "temporali" del passaggio di una carta da una lista
	 * 			  all'altra.
	 * 
	 * Representation invariant : date != null
	 * 
	 * Oss. Vedo la classe history come una struttura per mantenere la cronologia
	 * degli spostamenti,indipendentemente da quale lista si spostano
	 */	
	
    @Expose
	private final int from;
    @Expose
	private final int to;
    @Expose
	private final String date;
	
	public History(int from,int to) throws IllegalArgumentException {
		
		this.from = from;
		this.to   = to;
		this.date = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss").format(new Date());
	}

	public int getFrom() {return from; }

	public int getTo() { return to; }
	
	public String getDate() { return date; }
	
	@Override
	public String toString() { return "from: "+from+" to: "+to; }
}
