package net.es.nsi.pce.pf;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

/**
 *
 * @author hacksaw
 */
public class SimpleStpPairCollection implements Iterable<SimpleStpPair> {
    private final String srcId;
    private final String dstId;

    private final LinkedList<Pair> list = new LinkedList<>();

    public SimpleStpPairCollection(String srcId, Set<SimpleLabel> srcLabels, String dstId, Set<SimpleLabel> dstLabels) {
        this.srcId = srcId;
        this.dstId = dstId;

        // We want to process common labels first, then the remaining mismatched.
        SetView<SimpleLabel> commonLabels = Sets.intersection(srcLabels, dstLabels);
        if (!commonLabels.isEmpty()) {
            SimpleLabel[] toArray = (SimpleLabel[]) commonLabels.toArray();
            for (int i = 0; i < toArray.length; i++) {
                Pair pair = new Pair(toArray[i], toArray[i]);
                list.addFirst(pair);
            }
        }

        // Now we add the remaining pair combinations.
        SimpleLabel[] srcArray = (SimpleLabel[]) srcLabels.toArray();
        SimpleLabel[] dstArray = (SimpleLabel[]) dstLabels.toArray();
        for (int i = 0; i < srcArray.length; i++) {
            for (int j = 0; j < dstArray.length; j++) {
                // If they are equal labels then we skip them.
                if (!srcArray[i].equals(dstArray[j]) &&
                        srcArray[i].getType().equalsIgnoreCase(dstArray[j].getType())) {
                    Pair pair = new Pair(srcArray[i], dstArray[j]);
                    list.addLast(pair);
                }
            }
        }
    }

    @Override
    public Iterator<SimpleStpPair> iterator() {
        return new SimpleStpPairIterator();
    }

    private class SimpleStpPairIterator implements Iterator<SimpleStpPair> {
        private ListIterator<Pair> iterator = list.listIterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public SimpleStpPair next() {
            Pair next = iterator.next();
            SimpleStp src = new SimpleStp(srcId, next.src);
            SimpleStp dst = new SimpleStp(dstId, next.dst);
            return new SimpleStpPair(src, dst);
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    private class Pair {
        SimpleLabel src;
        SimpleLabel dst;

        public Pair(SimpleLabel src, SimpleLabel dst) {
            this.src = src;
            this.dst = dst;
        }
    }
}