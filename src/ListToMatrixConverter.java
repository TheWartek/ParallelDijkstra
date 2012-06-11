import java.util.List;

import pl.mgrproject.api.Edge;
import pl.mgrproject.api.Graph;
import pl.mgrproject.api.plugins.Converter;


public class ListToMatrixConverter implements Converter {
    
    private Graph<?> graph;
    private int infinity = 9999;

    @Override
    public String getName() {
	return "Adjacency List to Matrix";
    }

    @Override
    public void setGraph(Graph<?> g) {
	graph = g;
    }
    
    public int[][] getMatrix() {
	int size = graph.getVertices().size();
	int[][] E = new int[size][size];
	
	for (int i = 0; i < size; ++i) {
	    for (int j = 0; j < size; ++j) {
		E[i][j] = i == j ? 0 : infinity;
	    }
	}
	
	List<Edge<Integer>> edges = (List)graph.getEdges();
	
	for (Edge<Integer> e : edges) {
	    E[e.first][e.last] = e.value;
	}
	
	return E;
    }
    
}
