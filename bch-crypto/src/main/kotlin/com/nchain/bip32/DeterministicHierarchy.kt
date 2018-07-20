/*
 * Copyright 2013 Matija Mazi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nchain.bip32

import java.util.Locale


// TODO: This whole API feels a bit object heavy. Do we really need ChildNumber and so many maps, etc?
// TODO: Should we be representing this using an actual tree arrangement in memory instead of a bunch of hashmaps?

/**
 *
 * A DeterministicHierarchy calculates and keeps a whole tree (hierarchy) of keys originating from a single
 * root key. This implements part of the BIP 32 specification. A deterministic key tree is useful because
 * Bitcoin's privacy system require new keys to be created for each transaction, but managing all these
 * keys quickly becomes unwieldy. In particular it becomes hard to back up and distribute them. By having
 * a way to derive random-looking but deterministic keys we can make wallet backup simpler and gain the
 * ability to hand out [DeterministicKey]s to other people who can then create new addresses
 * on the fly, without having to contact us.
 *
 *
 * The hierarchy is started from a single root key, and a location in the tree is given by a path which
 * is a list of [ChildNumber]s.
 */


/**
 * Constructs a new hierarchy rooted at the given key. Note that this does not have to be the top of the tree.
 * You can construct a DeterministicHierarchy for a subtree of a larger tree that you may not own.
 */
class DeterministicHierarchy(rootKey: DeterministicKey) {
    private val keys = hashMapOf<List<ChildNumber>, DeterministicKey>()
    private val rootPath: List<ChildNumber>
    // Keep track of how many child keys each node has. This is kind of weak.
    private val lastChildNumbers = hashMapOf<List<ChildNumber>, ChildNumber>()

    /**
     * Returns the root key that the [DeterministicHierarchy] was created with.
     */
    val rootKey: DeterministicKey
        get() = get(rootPath, false, false)

    init {
        putKey(rootKey)
        rootPath = rootKey.path
    }

    /**
     * Inserts a key into the heirarchy. Used during deserialization: you normally don't need this. Keys must be
     * inserted in order.
     */
    fun putKey(key: DeterministicKey) {
        val path = key.path
        // Update our tracking of what the next child in each branch of the tree should be. Just assume that keys are
        // inserted in order here.
        val parent = key.parent
        if (parent != null)
            lastChildNumbers[parent.path] = key.childNumber
        keys[path] = key
    }

    /**
     * Returns a key for the given path, optionally creating it.
     *
     * @param path the path to the key
     * @param relativePath whether the path is relative to the root path
     * @param create whether the key corresponding to path should be created (with any necessary ancestors) if it doesn't exist already
     * @return next newly created key using the child derivation function
     * @throws IllegalArgumentException if create is false and the path was not found.
     */
    operator fun get(path: List<ChildNumber>, relativePath: Boolean, create: Boolean): DeterministicKey {
        val list = arrayListOf<ChildNumber>()
        if (relativePath) {
            list.addAll(rootPath)
        }
        list.addAll(path)
        val absolutePath = list.toList()

        if (!keys.containsKey(absolutePath)) {
            if (!create)
                throw IllegalArgumentException(String.format(Locale.US, "No key found for %s path %s.",
                        if (relativePath) "relative" else "absolute", HDUtils.formatPath(path)))
            check(absolutePath.size > 0, {"Can't derive the master key: nothing to derive from."})
            val subList = absolutePath.subList(0, absolutePath.size - 1)
            val parent = get(subList, false, true)
            putKey(HDKeyDerivation.deriveChildKey(parent, absolutePath[absolutePath.size - 1]))
        }
        return keys[absolutePath]!!
    }

    /**
     * Extends the tree by calculating the next key that hangs off the given parent path. For example, if you pass a
     * path of 1/2 here and there are already keys 1/2/1 and 1/2/2 then it will derive 1/2/3.
     *
     * @param parentPath the path to the parent
     * @param relative whether the path is relative to the root path
     * @param createParent whether the parent corresponding to path should be created (with any necessary ancestors) if it doesn't exist already
     * @param privateDerivation whether to use private or public derivation
     * @return next newly created key using the child derivation funtcion
     * @throws IllegalArgumentException if the parent doesn't exist and createParent is false.
     */
    fun deriveNextChild(parentPath: List<ChildNumber>, relative: Boolean, createParent: Boolean, privateDerivation: Boolean): DeterministicKey {
        val parent = get(parentPath, relative, createParent)
        var nAttempts = 0
        while (nAttempts++ < HDKeyDerivation.MAX_CHILD_DERIVATION_ATTEMPTS) {
            try {
                val createChildNumber = getNextChildNumberToDerive(parent.path, privateDerivation)
                return deriveChild(parent, createChildNumber)
            } catch (ignore: HDDerivationException) {
            }

        }
        throw HDDerivationException("Maximum number of child derivation attempts reached, this is probably an indication of a bug.")
    }

    private fun getNextChildNumberToDerive(path: List<ChildNumber>, privateDerivation: Boolean): ChildNumber {
        val lastChildNumber = lastChildNumbers[path]
        val nextChildNumber = ChildNumber(if (lastChildNumber != null) lastChildNumber.num() + 1 else 0, privateDerivation)
        lastChildNumbers[path] = nextChildNumber
        return nextChildNumber
    }

    fun getNumChildren(path: List<ChildNumber>): Int {
        val cn = lastChildNumbers[path]
        return if (cn == null)
            0
        else
            cn.num() + 1   // children start with zero based childnumbers
    }

    /**
     * Extends the tree by calculating the requested child for the given path. For example, to get the key at position
     * 1/2/3 you would pass 1/2 as the parent path and 3 as the child number.
     *
     * @param parentPath the path to the parent
     * @param relative whether the path is relative to the root path
     * @param createParent whether the parent corresponding to path should be created (with any necessary ancestors) if it doesn't exist already
     * @return the requested key.
     * @throws IllegalArgumentException if the parent doesn't exist and createParent is false.
     */
    fun deriveChild(parentPath: List<ChildNumber>, relative: Boolean, createParent: Boolean, createChildNumber: ChildNumber): DeterministicKey {
        return deriveChild(get(parentPath, relative, createParent), createChildNumber)
    }

    private fun deriveChild(parent: DeterministicKey, createChildNumber: ChildNumber): DeterministicKey {
        val childKey = HDKeyDerivation.deriveChildKey(parent, createChildNumber)
        putKey(childKey)
        return childKey
    }

    companion object {

        val BIP32_STANDARDISATION_TIME_SECS = 1369267200
    }
}
