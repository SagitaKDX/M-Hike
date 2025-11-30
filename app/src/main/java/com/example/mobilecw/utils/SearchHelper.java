package com.example.mobilecw.utils;

import com.example.mobilecw.database.entities.Hike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * SearchHelper provides advanced search algorithms including:
 * - Partial matching
 * - Case-insensitive search
 * - Fuzzy matching (Levenshtein distance)
 * - Relevance scoring
 */
public class SearchHelper {
    
    private static final int FUZZY_THRESHOLD = 3; // Max edit distance for fuzzy match
    
    /**
     * Search hikes with fuzzy matching and relevance scoring
     * @param hikes List of all hikes
     * @param query Search query
     * @return List of matching hikes sorted by relevance
     */
    public static List<Hike> fuzzySearch(List<Hike> hikes, String query) {
        if (query == null || query.trim().isEmpty()) {
            return hikes;
        }
        
        String normalizedQuery = query.toLowerCase().trim();
        List<HikeScore> scoredHikes = new ArrayList<>();
        
        for (Hike hike : hikes) {
            int score = calculateRelevanceScore(hike, normalizedQuery);
            if (score > 0) {
                scoredHikes.add(new HikeScore(hike, score));
            }
        }
        
        // Sort by score (highest first)
        Collections.sort(scoredHikes, new Comparator<HikeScore>() {
            @Override
            public int compare(HikeScore o1, HikeScore o2) {
                return Integer.compare(o2.score, o1.score);
            }
        });
        
        // Extract sorted hikes
        List<Hike> results = new ArrayList<>();
        for (HikeScore hs : scoredHikes) {
            results.add(hs.hike);
        }
        
        return results;
    }
    
    /**
     * Calculate relevance score for a hike based on search query
     * Higher score = more relevant
     */
    private static int calculateRelevanceScore(Hike hike, String query) {
        int score = 0;
        
        String name = hike.getName() != null ? hike.getName().toLowerCase() : "";
        String location = hike.getLocation() != null ? hike.getLocation().toLowerCase() : "";
        String description = hike.getDescription() != null ? hike.getDescription().toLowerCase() : "";
        String difficulty = hike.getDifficulty() != null ? hike.getDifficulty().toLowerCase() : "";
        
        // Exact match (highest score)
        if (name.equals(query)) {
            score += 100;
        }
        if (location.equals(query)) {
            score += 80;
        }
        
        // Starts with query (high score)
        if (name.startsWith(query)) {
            score += 50;
        }
        if (location.startsWith(query)) {
            score += 40;
        }
        
        // Contains query (medium score)
        if (name.contains(query)) {
            score += 30;
        }
        if (location.contains(query)) {
            score += 25;
        }
        if (description.contains(query)) {
            score += 15;
        }
        if (difficulty.contains(query)) {
            score += 10;
        }
        
        // Word-by-word matching (for multi-word queries)
        String[] queryWords = query.split("\\s+");
        for (String word : queryWords) {
            if (word.length() < 2) continue;
            
            if (name.contains(word)) {
                score += 10;
            }
            if (location.contains(word)) {
                score += 8;
            }
            if (description.contains(word)) {
                score += 5;
            }
        }
        
        // Fuzzy matching (Levenshtein distance)
        String[] nameWords = name.split("\\s+");
        String[] locationWords = location.split("\\s+");
        
        for (String nameWord : nameWords) {
            if (nameWord.length() >= 3) {
                int distance = levenshteinDistance(nameWord, query);
                if (distance <= FUZZY_THRESHOLD) {
                    score += (FUZZY_THRESHOLD - distance) * 5;
                }
            }
        }
        
        for (String locationWord : locationWords) {
            if (locationWord.length() >= 3) {
                int distance = levenshteinDistance(locationWord, query);
                if (distance <= FUZZY_THRESHOLD) {
                    score += (FUZZY_THRESHOLD - distance) * 4;
                }
            }
        }
        
        return score;
    }
    
    /**
     * Calculate Levenshtein distance (edit distance) between two strings
     * This measures how many single-character edits are needed to change one word into another
     */
    public static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        // If one string is empty, distance is length of the other
        if (len1 == 0) return len2;
        if (len2 == 0) return len1;
        
        // Create distance matrix
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        // Initialize first row and column
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        // Fill the matrix
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                
                dp[i][j] = Math.min(
                    Math.min(
                        dp[i - 1][j] + 1,      // deletion
                        dp[i][j - 1] + 1       // insertion
                    ),
                    dp[i - 1][j - 1] + cost    // substitution
                );
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * Check if two strings are similar based on fuzzy matching
     */
    public static boolean isFuzzyMatch(String s1, String s2, int threshold) {
        if (s1 == null || s2 == null) return false;
        return levenshteinDistance(s1.toLowerCase(), s2.toLowerCase()) <= threshold;
    }
    
    /**
     * Helper class to hold hike and its relevance score
     */
    private static class HikeScore {
        Hike hike;
        int score;
        
        HikeScore(Hike hike, int score) {
            this.hike = hike;
            this.score = score;
        }
    }
    
    /**
     * Filter hikes by advanced criteria
     */
    public static List<Hike> advancedFilter(List<Hike> hikes, String nameQuery, String locationQuery, 
                                           Double minLength, Double maxLength, Long startDate, Long endDate,
                                           String difficulty, String parkingAvailable) {
        List<Hike> filtered = new ArrayList<>();
        
        for (Hike hike : hikes) {
            boolean matches = true;
            
            // Name filter
            if (nameQuery != null && !nameQuery.trim().isEmpty()) {
                String hikeName = hike.getName() != null ? hike.getName().toLowerCase() : "";
                if (!hikeName.contains(nameQuery.toLowerCase().trim())) {
                    matches = false;
                }
            }
            
            // Location filter
            if (locationQuery != null && !locationQuery.trim().isEmpty()) {
                String hikeLocation = hike.getLocation() != null ? hike.getLocation().toLowerCase() : "";
                if (!hikeLocation.contains(locationQuery.toLowerCase().trim())) {
                    matches = false;
                }
            }
            
            // Length filter
            if (minLength != null && hike.getLength() < minLength) {
                matches = false;
            }
            if (maxLength != null && hike.getLength() > maxLength) {
                matches = false;
            }
            
            // Date filter
            if (hike.getDate() != null) {
                long hikeTime = hike.getDate().getTime();
                if (startDate != null && hikeTime < startDate) {
                    matches = false;
                }
                if (endDate != null && hikeTime > endDate) {
                    matches = false;
                }
            }
            
            // Difficulty filter
            if (difficulty != null && !difficulty.trim().isEmpty()) {
                String hikeDifficulty = hike.getDifficulty() != null ? hike.getDifficulty() : "";
                if (!hikeDifficulty.equals(difficulty)) {
                    matches = false;
                }
            }
            
            // Parking filter
            if (parkingAvailable != null && !parkingAvailable.trim().isEmpty()) {
                boolean hikeHasParking = hike.isParkingAvailable();
                if (parkingAvailable.equalsIgnoreCase("yes") && !hikeHasParking) {
                    matches = false;
                } else if (parkingAvailable.equalsIgnoreCase("no") && hikeHasParking) {
                    matches = false;
                }
            }
            
            if (matches) {
                filtered.add(hike);
            }
        }
        
        return filtered;
    }
}

