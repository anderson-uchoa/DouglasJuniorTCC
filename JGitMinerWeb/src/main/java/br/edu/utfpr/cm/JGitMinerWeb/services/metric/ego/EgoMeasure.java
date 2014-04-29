package br.edu.utfpr.cm.JGitMinerWeb.services.metric.ego;

import br.edu.utfpr.cm.JGitMinerWeb.services.metric.Measure;

/**
 * Stores the eigenvector measure of the vertex <code>V</code>.
 * 
 * @author Rodrigo T. Kuroda
 * @param <V> Class of vertex
 */
public class EgoMeasure<V> extends Measure<V> {

    private final long size;
    private final long ties;
    private final long pairs;
    private final double density;
    private final double betweennessCentrality;
    
    public EgoMeasure(final V vertex, final long size, final long ties, 
            final double betweennessCentrality) {
        super(vertex);
        this.size = size;
        this.ties = ties;
        this.pairs = size * (size - 1);
        this.density = pairs == 0 ? 1 : (double) ties / (double) pairs;
        this.betweennessCentrality = betweennessCentrality;
    }

    public long getSize() {
        return size;
    }

    public long getTies() {
        return ties;
    }

    public long getPairs() {
        return pairs;
    }

    public double getDensity() {
        return density;
    }

    public double getBetweennessCentrality() {
        return betweennessCentrality;
    }

    @Override
    public String toString() {
        return super.toString() + ", size: " + size + ", ties: " + ties 
                + ", pairs: " + pairs + ", density: " + density 
                + ", ego betweeness centrality: " + betweennessCentrality;
    }
}
