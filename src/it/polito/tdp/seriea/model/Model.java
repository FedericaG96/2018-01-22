package it.polito.tdp.seriea.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import it.polito.tdp.seriea.db.SerieADAO;

public class Model {
	
	private List<Team> squadre;
	private List<Season> stagioni;
	private Team squadraSelezionata;
	private Map<Season,Integer> punteggi;
	private Map<String, Team> squadreIdMap;
	private Map<Integer, Season> stagioniIdMap;
	
	private Graph<Season,DefaultWeightedEdge> grafo;

	private List<Season> stagioniConsecutive;
	private List<Season> percorsoBest;
	
	public Model() {
		SerieADAO dao = new SerieADAO();
		
		this.squadre = dao.listTeams();
		this.stagioni= dao.listAllSeasons();
		this.squadreIdMap = new HashMap<String, Team> ();
		grafo = new SimpleDirectedWeightedGraph<Season, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		
		for(Team t: squadre) {
			this.squadreIdMap.put(t.getTeam(), t);
		}
		this.stagioniIdMap = new HashMap<Integer, Season>();
		for(Season s: this.stagioni) {
			this.stagioniIdMap.put(s.getSeason(), s);
		}
	}
	
	public List<Team> getSquadre(){
		return this.squadre;
	}
	
	public Map<Season, Integer> calcolaPunteggi(Team squadra){
		this.punteggi = new HashMap<Season, Integer>();
		
		SerieADAO dao = new SerieADAO();
		List<Match> partite = dao.listMatchesForTeam(squadra, stagioniIdMap, squadreIdMap);
		
		for(Match m : partite) {
			//Se la squadra ha vinto una stagione devo incrementare il punteggio di quella stagione
			
			Season stagione = m.getSeason();
			
			int punti = 0;
			if(m.getFtr().equals("D")) { //se è un pareggio
				punti = 1;
			} else {
				if(m.getHomeTeam().equals(squadra) && m.getFtr().equals("H") || //se la squadra giocava in casa e ha vinto la squadra Home
				(m.getAwayTeam().equals(squadra) && m.getFtr().contentEquals("A")) ) //oppure se la squadra è in trasferta e ha vinto la squadra Away
 					{
						punti = 3;
 					}
			}
			
			Integer attuale = punteggi.get(stagione);
			if(attuale == null)
				attuale = 0;
			punteggi.put(stagione, attuale + punti);
		}
		
		return punteggi;
	}

	public void creaGrafo(Team squadraSelezionata2) {
		
		Graphs.addAllVertices(grafo, punteggi.keySet());
		
		for(Season s1 : grafo.vertexSet()) {
			for(Season s2 : grafo.vertexSet()) {
				if(!s1.equals(s2)) {
					if(punteggi.size()!=1) {
					if(punteggi.get(s1) < punteggi.get(s2)) {
						int differenza = punteggi.get(s2) - punteggi.get(s1);
						Graphs.addEdge(grafo, s1, s2, differenza);
					}
					else {
						int differenza = punteggi.get(s1) - punteggi.get(s2);
						Graphs.addEdge(grafo, s2, s1, differenza);
					}
				}
				
				else {
					this.trovaAnnataOro();
				}
				}
			}
		}
	}

	public Season trovaAnnataOro() {
		Season best = null;
		int bestRisultato = Integer.MIN_VALUE;
		
		if(punteggi.size()==1) {
			for(Season s1 : grafo.vertexSet()) {
				best = s1;
			}
		} else {
		for(Season s1 : grafo.vertexSet()) {
			if(this.trovaRisultato(s1) > bestRisultato) {
				best = s1;
				bestRisultato = this.trovaRisultato(s1);
			}
		}
		}
		return best;
	}

	private int trovaRisultato(Season s1) {
		int pesoEntranti = 0;
		int pesoUscenti = 0;
		for(DefaultWeightedEdge e : grafo.incomingEdgesOf(s1)) {
			pesoEntranti += grafo.getEdgeWeight(e);
		}
		for(DefaultWeightedEdge e : grafo.outgoingEdgesOf(s1)) {
			pesoUscenti += grafo.getEdgeWeight(e);
		}
		return pesoEntranti - pesoUscenti;
	}

	public List<Season> getCamminoVirtuoso() {
		
		
		//trova le stagioni consecutive
		this.stagioniConsecutive = new ArrayList<>(punteggi.keySet());
		Collections.sort(stagioniConsecutive);	//ordino la lista delle stagioni secondo l'annata
		
		//Preparo le variabili utili alla ricorsione
		percorsoBest = new ArrayList<Season>();
		List<Season> parziale = new ArrayList<Season>();
		
		//Itera al livello 0 della ricorsione
		for(Season s : grafo.vertexSet()) {
			parziale.add(s);	//avvio una ricorsione per ogni vertice del grafo
			this.cerca(1, parziale);
			parziale.remove(0);
		}
		
		
		return percorsoBest;
	}
	
	/*
	 * RICORSIONE
	 * 
	 * Soluzione parziale : Lista di Season (lista di vertici)
	 * Livello ricorsione: lunghezza della lista
	 * Caso terminale : non trovo altri vertici da aggiungere -> verifica se il cammino ha lunghezza massima
	 * 
	 * Generazione delle soluzioni : vertici connessi all'ultimo vertice del percorso 
	 * (con arco orientato nel verso giusto), non ancora parte del percorso
	 * relativi a stagioni consecutive
	 * 
	 */

	private void cerca(int livello, List<Season> parziale) {
		
		boolean trovato = false;
		
		//genera nuove soluzioni
		Season ultimo = parziale.get(livello -1);
		
		//ciclo sui successori
		for(Season prossimo : Graphs.successorListOf(grafo, ultimo)) {
			if(!parziale.contains(prossimo)) {
				//controllo che prossimo sia nella posizione successiva a ultimo
				if(stagioniConsecutive.indexOf(ultimo)+1 == stagioniConsecutive.lastIndexOf(prossimo)) {
					//candidato accettabile -> fa ricorsione
					trovato = true;
					parziale.add(prossimo);
					this.cerca(livello + 1, parziale);
					parziale.remove(livello);
				}
			}
			
		}
		 
		//valuta caso terminale: se non trovo un prossimo
		if(trovato == false) {
			if(parziale.size()>percorsoBest.size()) {
				percorsoBest = new ArrayList<Season>(parziale);	//clono il best
			}
		}
		
		
	}
}
