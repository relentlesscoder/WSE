package edu.nyu.cs.cs2580;

import org.junit.Test;

import static org.junit.Assert.*;

public class QueryPhraseTest {

    @Test
    public void testProcessQuery() throws Exception {
        Query query = new Query("\"new york city\"film");
        query.processQuery();
    }
}