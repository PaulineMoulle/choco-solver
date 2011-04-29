/**
 *  Copyright (c) 2010, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package choco.kernel.memory;

import choco.kernel.memory.structure.IndexedBipartiteSet;
import choco.kernel.memory.structure.S64BitSet;

/**
 * Super class of all environments !
 */
public abstract class AbstractEnvironment implements IEnvironment {

    protected int currentWorld = 0;

    private static final int SIZE = 128;

    /**
     * Shared BitSet
     */
    public IndexedBipartiteSet currentBitSet;
    /**
     * Nex free bit in the shared BitSet
     */
    protected int nextOffset;

    public final int getWorldIndex() {
        return currentWorld;
    }

    /**
     * Factory pattern: new IStateBitSet objects are created by the environment
     *
     * @param size initail size of the IStateBitSet
     * @return IStateBitSet
     */
    @Override
    public IStateBitSet makeBitSet(int size) {
        return new S64BitSet(this, size);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void worldPopUntil(int w) {
        while (currentWorld > w) {
            worldPop();
        }
    }

    public final void createSharedBipartiteSet(int size) {
        currentBitSet = new IndexedBipartiteSet(this, size);
        nextOffset = -1;
    }

    /**
     * Factory pattern : shared StoredBitSetVector objects is return by the environment
     *
     * @return
     */
    @Override
    public final IndexedBipartiteSet getSharedBipartiteSetForBooleanVars() {
        if (currentBitSet == null) {
            createSharedBipartiteSet(SIZE);
        }
        nextOffset++;
        if (nextOffset > currentBitSet.size() - 1) {
            increaseSizeOfSharedBipartiteSet(currentBitSet.size() * 2);
        }
        return currentBitSet;
    }

    /**
     * Increase the size of the shared bi partite set,
     * it HAS to be called before the end of the environment creation
     * BEWARE: be sure you are correctly calling this method
     *
     * @param gap the gap the reach the expected size
     */
    @Override
    public void increaseSizeOfSharedBipartiteSet(int gap) {
        currentBitSet.increaseSize(gap);
    }

    /**
     * Return the next free bit in the shared StoredBitSetVector object
     *
     * @return
     */
    @Override
    public final int getNextOffset() {
        return nextOffset;
    }
}
