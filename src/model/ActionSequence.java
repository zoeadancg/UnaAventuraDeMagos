package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionSequence {
    private final List<Direccion> seq = new ArrayList<>();
    private final int maxLen;

    public ActionSequence(int maxLen) {
        this.maxLen = maxLen;
    }

    public boolean add(Direccion d) {
        if (seq.size() >= maxLen)
            return false;
        if (!seq.isEmpty() && seq.get(seq.size() - 1) == d)
            return false;
        seq.add(d);
        return true;
    }

    public List<Direccion> getSequence() {
        return Collections.unmodifiableList(seq);
    }
}
