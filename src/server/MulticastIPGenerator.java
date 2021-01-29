package server;

import java.util.ArrayList;
import java.util.Arrays;

import main.WorthSupport;

public class MulticastIPGenerator { 
	/**
	 * Overview: Rappresenta la classe che mi gestisce gli ip multicast di classe D 
	 * 			 da dare ad ogni progetto, secondo principi di unicità ed riuso degl inidirizzi
	 * 
	 * Representation invariant: usedPool!=null && forall(a in usedPool. a!=null)
	 * 
	 */
	
	
	private ArrayList<int[]> usedPool;
    
    public MulticastIPGenerator(){
    	// creo una lista dove mantengo gli ip usati
    	usedPool = new ArrayList<int[]>();
    }
    private Boolean isUsed(int[] ip) {
    	// controllo se l'ip è già stato utilizzato,quindi appartiene alla "pool"
    	for(int[] e : usedPool) {
    		if(e[0]==ip[0] && e[1]==ip[1] && e[2]==ip[2] && e[3]==ip[3]) return true;
    	}
    	
    	return false;
    }
    public String generateIP() {
    	// Mi ristituisce l'indirizzo ip di classe D,che attualmente
    	// non è utilizzato.
    	
    	int[] ip = Arrays.copyOf(WorthSupport.MULTICASTIPDEFAULT,4);
    	
    	while(ip!=null) {

    		if(!isUsed(ip)) {
    			usedPool.add(ip);
    			// restituisce una stringa sempre di dimenzione fissa 3 + 3 + 3 + 3 (più i "dot") 
    			return new String(zeroFill(ip[0])+"."+zeroFill(ip[1])+"."+zeroFill(ip[2])+"."+zeroFill(ip[3]));
    		}
    		
    		if(ip[3]<255) { ip[3]++; }    		
            else if(ip[2]<255){ ip[3]=0; ip[2]++; }     		
            else if(ip[1]<255){ ip[2] = ip[3]=0; ip[1]++; }    		
            else if(ip[0]<239){ ip[1]= ip[2]= ip[3] = 0; ip[0]++; }    		
            else ip = null;
    	}
    	
        return null;
    }
    public void removeIP(String ip) {
    	
    	// Rimuove l'indirizzo ip dalla "pool", così che possa essere riutilizzato
    	int[] ipp = Arrays.stream(ip.split("\\."))
    						.mapToInt(Integer::parseInt)
    						.toArray();
    	
    	if(ipp.length!=4) return;
    	
    	for(int[] e :usedPool) {
    		if(e[0]==ipp[0] && e[1]==ipp[1] && e[2]==ipp[2] && e[3]==ipp[3]) {
    			usedPool.remove(e);
    			return;
    		}
    	}
    	
    	
    }
    /**
     * Aggiungo zeri a sinistra fino che non raggiunge una lunghezza di 3 caratteri
     * @param i
     * @return
     */
    private String zeroFill(int i) {
    	if(i<10) return "00"+i;
    	else if (i<100) return "0"+i;
    	else  return ""+i;
    }
	public static int[] getIpDefault() { return WorthSupport.MULTICASTIPDEFAULT; }
    public int generatePort() { return WorthSupport.MULTICASTPORT ; }
    
}