package utils.crypto.adv.bulletproof.linearalgebra;

import cyclops.collections.immutable.VectorX;
import utils.crypto.adv.bulletproof.algebra.Group;
import utils.crypto.adv.bulletproof.algebra.GroupElement;

import java.math.BigInteger;

/**
 * Created by buenz on 7/2/17.
 */
public class PeddersenBase<T extends GroupElement<T>> extends GeneratorVector<T> {
    public final T g;
    public final T h;

    public PeddersenBase(T g, T h, Group<T> group) {
        super(VectorX.of(g, h), group);
        this.g = g;
        this.h = h;

    }

    public T commit(BigInteger x, BigInteger r) {
        return g.multiply(x).add(h.multiply(r));
    }

}
