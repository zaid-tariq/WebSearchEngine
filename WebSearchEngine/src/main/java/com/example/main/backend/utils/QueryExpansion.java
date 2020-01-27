package com.example.main.backend.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;

public final class QueryExpansion {
	
	static  Dictionary dict;
	
	
	public static List<String> expandQuery(List<String> a_tildaTerms, List<String> a_nonTildaTerms) throws JWNLException {
		/*
		 * function to fetch all synonyms from the wordnet and return CNF
			stem the terms ?
				Problem here, cuz we dont have entity matching for now. So the term "new york city" which is one entity will also be tokenized and stemmed.
				Also, if I stem, I lose some words, for example foodie turn to foodi, which gives no synonyms.
					but if I dont stem then I still lose words. For example "runs" gives no synonyms
			create CNF of the query with these terms
			send to database
			database will get the collection_frequency of each expanded term from the collection_vocabulary table and get the k most frequent terms
			discard other expanded terms
			search the database and rank results
		 * 
		 * */
		 
		if(dict == null) 
			dict = Dictionary.getDefaultResourceInstance();
		List<String> expandedTerms = new ArrayList<String>();
		
		for(String term: a_tildaTerms){
				Set<String> synonyms = new HashSet<String>();
				synonyms.addAll(getSynonymsOfWord(dict.getIndexWord(POS.NOUN, term)));
				synonyms.addAll(getSynonymsOfWord(dict.getIndexWord(POS.ADVERB, term)));
				synonyms.addAll(getSynonymsOfWord(dict.getIndexWord(POS.ADJECTIVE, term)));
				synonyms.addAll(getSynonymsOfWord(dict.getIndexWord(POS.VERB, term)));
				expandedTerms.add(concatenateStringForCNF(term, synonyms, true));
		}	
		
		for(String term : a_nonTildaTerms)
			expandedTerms.add(Utils.stemTerm(term));
		return expandedTerms;
	}
	
	static List<String> expandGermanQuery(List<String> a_tildaTerms, List<String> a_nonTildaTerms, Connection con) throws JWNLException, SQLException {

		List<String> expandedTerms = new ArrayList<String>();
		
		for(String term: a_tildaTerms){
			Set<String> synonyms = new HashSet<String>();
			synonyms.addAll(GermanDict.getSynonymsForGermanTerm(term, con));
			expandedTerms.add(concatenateStringForCNF(term, synonyms, false));
		}	
		
		for(String term : a_nonTildaTerms)
			expandedTerms.add(term);
		return expandedTerms;
	}
	
	static Set<String> getSynonymsOfWord(IndexWord a_word) {
		
		Set<String> synonyms = new HashSet<String>();
		if(a_word != null) {
			for(Synset synset : a_word.getSenses()) {
				for(Word synonym: synset.getWords()) {
					String lemma = synonym.getLemma();
					synonyms.add(lemma);
				}
			}
		}
		return synonyms;		
	}
	
	static String concatenateStringForCNF(String term, Set<String> terms, boolean stemTerms) {
		String concat = stemTerms ? Utils.stemTerm(term): term;
		boolean isFirst = true;
		for(String syn : terms) {
			if(isFirst) {
				isFirst = false;
				concat += "=";
			}
			else concat += ":";
			concat += stemTerms ? Utils.stemTerm(syn): syn;
		}
		System.out.println(concat);
		return concat;
	}
	
}
