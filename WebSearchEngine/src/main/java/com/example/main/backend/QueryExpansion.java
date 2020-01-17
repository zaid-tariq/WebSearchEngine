package com.example.main.backend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import com.example.main.backend.utils.Utils;

import java.util.Set;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;

public final class QueryExpansion {
	
	static  Dictionary dict;
	
	
	static List<String> expandQuery(List<String> a_tildaTerms, List<String> a_nonTildaTerms) throws JWNLException {
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
				expandedTerms.add(concatenateStringForCNF(term, synonyms));
		}	
		
		for(String term : a_nonTildaTerms)
			expandedTerms.add(Utils.stemTerm(term));
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
	
	static String concatenateStringForCNF(String term, Set<String> terms) {
		String concat = Utils.stemTerm(term);
		boolean isFirst = true;
		for(String syn : terms) {
			if(isFirst) {
				isFirst = false;
				concat += "=";
			}
			else concat += ":";
			concat += Utils.stemTerm(syn);
		}
		System.out.println(concat);
		return concat;
	}
	
}
