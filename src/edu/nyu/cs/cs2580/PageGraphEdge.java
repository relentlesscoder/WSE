package edu.nyu.cs.cs2580;

import org.jgrapht.graph.DefaultEdge;

/**
 * Created by Wei Shuai on 11/16/2014.
 */
public class PageGraphEdge extends DefaultEdge {

    public Object getEdgeSource(){
        return getSource();
    }
}
