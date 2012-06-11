import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import pl.mgrproject.api.Graph;
import pl.mgrproject.api.plugins.RoutingAlgorithm;


public class ParallelDijkstra implements RoutingAlgorithm {
    private int[][] E = new int[0][0];
    private List<Point> V;
    private List<Integer> Q;
    private int[] p;
    private int[] d;
    private static final int INF = 9999;
    private int start;
    private int threads;
    
    private synchronized int getD(int i) {
	return d[i];
    }
    
    private synchronized void setD(int i, int val) {
	d[i] = val;
    }

    @Override
    public String getName() {
	return "Dijkstra - zrównoleglony";
    }

    @Override
    public List<Point> getPath(int stop) throws Exception {
	List<Point> path = new ArrayList<Point>();

	int index = stop;
	while (index != start) {
	    path.add(V.get(index));
	    index = p[index];
	}
	path.add(V.get(start));

	return path;
    }

    @Override
    public void run(int start) throws Exception {
	this.start = start;
	int n = E.length;
	Q = new ArrayList<Integer>(n);
	p = new int[n];
	d = new int[n];
	Integer u = -1;

	for (int i = 0; i < n; ++i) {
	    p[i] = -1;
	    d[i] = INF;
	    Q.add(i);
	}
	d[start] = 0;
	p[start] = 0;
	
	ExecutorService exec = Executors.newFixedThreadPool(threads);
	ArrayList<Future<Integer>> result  = new ArrayList<Future<Integer>>();
	ArrayList<Future<Boolean>> result2  = new ArrayList<Future<Boolean>>();
	
	while (!Q.isEmpty()) {
	    // szukanie wierzcholka o najmniejszym d.
	    for (int i = 0; i < threads - 1; ++i) {
		result.add(exec.submit(new FindMinD(i * (V.size() / threads), (i + 1) * (V.size() / threads))));
	    }
	    //ostatni watek bierze reszte wektora
	    result.add(exec.submit(new FindMinD((threads - 1) * (V.size() / threads), V.size())));
	    
	    int minD = INF + 1;

	    for (Future<Integer> f : result) {
		try {
		    Integer i = f.get();
		    if (i == -1)
			continue;
		    if (d[i] < minD) {
			minD = d[i];
			u = i;
		    }
		} catch (InterruptedException e) {
		    System.out.println("[InterruptedException] " + e);
		} catch (ExecutionException e) {
		    System.out.println("[ExecutionException] " + e);
		}
	    }
	    
	    result.clear();
	    Q.remove(u);
	    
	    // sprawdzanie sasiadow analizowanego wierzcholka.
	    for (int i = 0; i < threads - 1; ++i) {
		result2.add(exec.submit(new CheckNeighbors(u, i * (V.size() / threads), (i + 1) * (V.size() / threads))));
	    }
	    result2.add(exec.submit(new CheckNeighbors(u, (threads - 1) * (V.size() / threads), V.size())));
	    
	    // oczekiwanie na zakonczenie dzialania watkow
	    try {
		for (Future<Boolean> f : result2) {
		    f.get();
		}
	    } catch (InterruptedException e) {
		System.out.println(e);
	    } catch (ExecutionException e) {
		System.out.println(e);
	    }

	    result2.clear();
	}
	
	exec.shutdown();
    }

    @Override
    public void setGraph(Graph<?> graph) {
	ListToMatrixConverter c = new ListToMatrixConverter();
	c.setGraph(graph);
	V = graph.getVertices();
	E = c.getMatrix();
	threads = Runtime.getRuntime().availableProcessors();
    }
    
    private class FindMinD implements Callable<Integer> {
	private int begin;
	private int end;
	
	public FindMinD(int begin, int end) {
	    this.begin = begin;
	    this.end = end;
	}
	
	@Override
	public Integer call() {
	    int min = INF + 1;
	    int index = -1;
	    try {
		for (int i = begin; i < end; ++i) {
		    if (d[i] < min && Q.contains(new Integer(i))) {
			min = d[i];
			index = i;
		    }
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    return index;
	}
    }
    
    private class CheckNeighbors implements Callable<Boolean> {
	private int begin;
	private int end;
	private int u;
	
	public CheckNeighbors(int u, int begin, int end) {
	    this.begin = begin;
	    this.end = end;
	    this.u = u;
	}

	@Override
	public Boolean call() {
	    try {
		for (int v = begin; v < end; ++v) {
		    if (E[u][v] != INF) {
			if (d[u] + E[u][v] < d[v]) {
			    d[v] = d[u] + E[u][v];
			    p[v] = u;
			}
		    }
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    return true;
	}
	
    }
}
