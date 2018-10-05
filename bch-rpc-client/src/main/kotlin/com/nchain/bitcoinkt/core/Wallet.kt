package com.nchain.bitcoinkt.core

import com.nchain.tx.Transaction

/*
 * @author Alberto Vilches
 * @date 19/09/2018
 */

open class Wallet {

    open fun receiveFromBlock(tx: Transaction,
                         block: StoredBlock,
                         blockType: AbstractBlockChain.NewBlockType,
                         relativityOffset: Int) {

    }
}
