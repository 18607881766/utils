package utils.crypto.adv.bulletproof.innerproduct;

import utils.crypto.adv.bulletproof.linearalgebra.FieldVector;

/**
 * Created by buenz on 6/28/17.
 */
public class InnerProductWitness {
    private final FieldVector a;
    private final FieldVector b;

    public InnerProductWitness(FieldVector a, FieldVector b) {
        this.a = a;
        this.b = b;
    }

    public FieldVector getA() {
        return a;
    }

    public FieldVector getB() {
        return b;
    }
}
