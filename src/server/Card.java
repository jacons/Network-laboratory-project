package server;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import main.WorthSupport;


public class Card implements Cloneable {
	/**
	 * Overview : Rappresenta task che deve essere svolto,al suo interno sono presenti 
	 * 			  oltre alle altre informazioni classiche come il titolo, la descrizione ecc...
	 * 			  una lista di movimenti da una lista di lavoro all' altra.
	 * 
	 * Representation invariant : title!=null && descr!=null && 0<= priority <= MAXPRIORITY &&
	 * 							  myhistory !=null && forall(h in myhistory. h!=null)
	 */
	
	@SerializedName("Card")
    @Expose
	private final String title;
    @Expose
	private final String descr;
    @Expose
	private final int priority;

    
    // utilizzo questo flag per segnalare che ci sono stati cambiamenti dall'ultima
    // volta che ï¿½ stata messa a true: true-> Aggiornato .... false-> Da aggiornare
    private boolean isUpdated = true ;
    
    @Expose
	private ArrayList<History> myhistory;
	
	public Card(String title, String descr, int priority) throws IllegalArgumentException {
		
		if(title==null||descr==null||priority<0||priority>WorthSupport.MAXPRIORITY) 
			throw new IllegalArgumentException();  
		
		this.title = title;
		this.descr = descr;
		this.priority = priority;
		
		myhistory = new ArrayList<History>();
		// Aggiungo alla cronologia che "a che tempo" la card è stata creata
		myhistory.add(new History(-1,0));
	}
	public Card(String title, String descr, int priority,ArrayList<History> histories) throws IllegalArgumentException {
		
		if(title==null||descr==null||priority<0||priority>WorthSupport.MAXPRIORITY) 
			throw new IllegalArgumentException();  
		
		this.title = title;
		this.descr = descr;
		this.priority = priority;
		
		myhistory = new ArrayList<History>(histories);
	}
	
	/**
	 * Aggiunge un nuovo movimento di lista, ed segnala che c'e' stata un cambiamento
	 */
	public void addHistory(int from,int to) {
		isUpdated  = false ;
		myhistory.add(new History(from,to));	
	}
	
	/**
	 * Aggiungo piu' movimenti di lista, se il parametro e' diverso da null
	 * inoltre segnalo che c'e' stato un cambiamento
	 * @param histories != null
	 */
	public void addHistory(ArrayList<History> histories) {
		if(histories==null) return;
		isUpdated  = false ;
		myhistory.addAll(histories);
	}
	
	@Override
	public Card clone() { return new Card(title,descr,priority,myhistory); }
	
	@Override 
	public int hashCode() { return title.hashCode(); }
	
	@Override
	public boolean equals(Object o) {
		
	    if (o == this) { return true;  } 
	    if (!(o instanceof Card)) { return false;  }
	    Card c = (Card) o; 
	    return title.equals(c.getTitle());
	}
	
	/**
	 * Inferisco in quale lista la carta appartiene attraverso l'ultimo
	 * movimento fatto nella cronologia
	 */
	public int inferState() { return myhistory.get(myhistory.size()-1).getTo(); }
	
 	public String getTitle() { return title; }

	public String getDescr() { return descr; }

	public int getPriority() { return priority; }
	
	/**
	 * Restituisco una NUOVA lista contente tutti gli spostamenti effettuati
	 */
	public List<History> getHistory() { return new ArrayList<History>(myhistory); }
	
	public boolean isUpdated() { return isUpdated; }
	
	public void setUpdated(boolean isUpdated) { this.isUpdated = isUpdated; }
}
