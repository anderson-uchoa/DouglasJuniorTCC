package br.edu.utfpr.cm.minerador.services.matrix.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Rodrigo T. Kuroda
 */
public class FilePairReleasesOccurenceCounter {

    private final FilePair filePair;
    private final Set<String> releasesOcurrences;

    public FilePairReleasesOccurenceCounter(FilePair filePair) {
        this.filePair = filePair;
        this.releasesOcurrences = new HashSet<>();
    }

    public FilePair getFilePair() {
        return filePair;
    }

    public int getReleasesOcurrences() {
        return releasesOcurrences.size();
    }

    public boolean addReleaseOcurrence(String release) {
        return releasesOcurrences.add(release);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + Objects.hashCode(this.filePair);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FilePairReleasesOccurenceCounter other = (FilePairReleasesOccurenceCounter) obj;
        if (!Objects.equals(this.filePair, other.filePair)) {
            return false;
        }
        return true;
    }

}
