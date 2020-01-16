package com.example.main.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;

public final class QueryExpansion {
	
	static  Dictionary dict;
	static boolean useStemmer = false;
	
	
	static List<String> expandQuery(List<String> a_searchTerms) throws JWNLException {
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
		
		Map<String, Boolean> resolvedTerms = resolveTildaOperator(a_searchTerms);
		List<String> expandedTerms = new ArrayList<String>();
		
		for(Entry<String, Boolean> entry: resolvedTerms.entrySet()){
			if(entry.getValue()) {
				String term = entry.getKey();
				Set<String> synonyms = new HashSet<String>();
				synonyms.addAll(getSynonymsOfWord(dict.getIndexWord(POS.NOUN, term)));
				synonyms.addAll(getSynonymsOfWord(dict.getIndexWord(POS.ADVERB, term)));
				synonyms.addAll(getSynonymsOfWord(dict.getIndexWord(POS.ADJECTIVE, term)));
				synonyms.addAll(getSynonymsOfWord(dict.getIndexWord(POS.VERB, term)));
				expandedTerms.add(concatenateStringForCNF(term, synonyms));
			}
			else expandedTerms.add(entry.getKey());
		}	
		return expandedTerms;
	}


	static Map<String, Boolean> resolveTildaOperator(List<String> a_searchTerms){
		//TODO:
		Map<String, Boolean> terms = new HashMap<String, Boolean>();
		for(String str : a_searchTerms) {
			str = str.trim();
			terms.put(str, true);
		}
		
		return terms;
	}
	
	static Set<String> getSynonymsOfWord(IndexWord a_word) {
		
		Set<String> synonyms = new HashSet<String>();
		if(a_word != null) {
			for(Synset synset : a_word.getSenses()) {
				for(Word synonym: synset.getWords()) {
					String lemma = synonym.getLemma();
					if(useStemmer) {
						Stemmer stemmer = new Stemmer();
						stemmer.add(lemma.toCharArray(), lemma.length());
						stemmer.stem();
						lemma = stemmer.toString();
					}
					synonyms.add(lemma);
				}
			}
		}
		return synonyms;		
	}
	
	static String concatenateStringForCNF(String term, Set<String> terms) {
		String concat = term;
		boolean isFirst = true;
		for(String syn : terms) {
			if(isFirst) {
				isFirst = false;
				concat += "=";
			}
			else concat += ":";
			concat += syn;
		}
		System.out.println(concat);
		return concat;
	}
	
	public static void main(String ...strings) throws JWNLException {
		
		Stemmer stemmer = new Stemmer();
		
		List<String> terms = new ArrayList<String>();
		
		stemmer.add("running".toCharArray(), "running".length());
		stemmer.stem();
		String stemmedWord = stemmer.toString();
		terms.add(stemmedWord);
		
		stemmer = new Stemmer();
		stemmer.add("ran".toCharArray(), "ran".length());
		stemmer.stem();
		stemmedWord = stemmer.toString();
		terms.add(stemmedWord);
		
		stemmer = new Stemmer();
		stemmer.add("runs".toCharArray(), "runs".length());
		stemmer.stem();
		stemmedWord = stemmer.toString();
		terms.add(stemmedWord);
		
		terms.add("run");
		terms.add("runs");
		terms.add("late");
		
		stemmer = new Stemmer();
		stemmer.add("eater".toCharArray(), "eater".length());
		stemmer.stem();
		stemmedWord = stemmer.toString();
		terms.add(stemmedWord);
		
		
		stemmer = new Stemmer();
		stemmer.add("foodie".toCharArray(), "foodie".length());
		stemmer.stem();
		stemmedWord = stemmer.toString();
		terms.add("foodie");
		
		System.out.println(expandQuery(terms));
	}
}
