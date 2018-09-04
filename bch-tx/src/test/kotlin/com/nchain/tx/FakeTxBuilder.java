/*
 * Copyright 2011 Google Inc.
 * Copyright 2016 Andreas Schildbach
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

package com.nchain.tx;

import com.nchain.address.CashAddress;
import com.nchain.key.ECKey;
import com.nchain.params.NetworkParameters;
import com.nchain.shared.Randomizer;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.tx.TransactionSignature;

import java.security.SecureRandom;
import java.util.Random;

import static com.google.common.base.Preconditions.checkState;

public class FakeTxBuilder {
    private final static Coin COIN = Coin.Companion.getCOIN();
    private final static Coin FIFTY_COINS = Coin.Companion.getFIFTY_COINS();

    static {
        Randomizer.INSTANCE.setRandom(new SecureRandom(new byte[] {}));
    }

    /** Create a fake transaction, without change. */
    public static Transaction createFakeTx(final NetworkParameters params) {
        return createFakeTxWithoutChangeAddress(params, COIN, ECKey.Companion.create().toCashAddress(params));
    }

    /** Create a fake transaction, without change. */
    public static Transaction createFakeTxWithoutChange(final NetworkParameters params, final TransactionOutput output) {
        Transaction prevTx = FakeTxBuilder.createFakeTx(params, COIN, ECKey.Companion.create().toCashAddress(params));
        TransactionBuilder tx = new TransactionBuilder();
        tx.addOutput(output);
        tx.addInput(prevTx, 0);
        return tx.build();
    }

    /** Create a fake coinbase transaction. */
    public static Transaction createFakeCoinbaseTx(final NetworkParameters params) {
        TransactionInput input = new TransactionInput(new byte[0], TransactionOutPoint.getUNCONNECTED(), TransactionInput.NO_SEQUENCE);
        TransactionBuilder tx = new TransactionBuilder();
        tx.addInput(input);
        TransactionOutput outputToMe = new TransactionOutput(FIFTY_COINS,
                ECKey.Companion.create().toCashAddress(params));
        tx.addOutput(outputToMe);

        Transaction transaction = tx.build();
        checkState(transaction.isCoinBase());
        return transaction;
    }

    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two outputs, one to us, one to somewhere
     * else to simulate change. There is one random input.
     */
    public static Transaction createFakeTxWithChangeAddress(NetworkParameters params, Coin value, CashAddress to, CashAddress changeOutput) {
        TransactionBuilder t = new TransactionBuilder();
        TransactionOutput outputToMe = new TransactionOutput(value, to);
        t.addOutput(outputToMe);
        TransactionOutput change = new TransactionOutput(Coin.Companion.valueOf(1, 11), changeOutput);
        t.addOutput(change);
        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        TransactionBuilder prevTx = new TransactionBuilder();
        TransactionOutput prevOut = new TransactionOutput(value, to);
        prevTx.addOutput(prevOut);
        // Connect it.
        t.addInput(prevTx.build(), 0);
        // Fake signature.
        return t.build();
    }

    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two outputs, one to nobody (return data, 0 coins) and other
     * to us (the change) with all the value expended from the UTXO
     */

    public static Transaction createFakeTxToMeWithReturnData(NetworkParameters params, Coin value, CashAddress to, byte[] data) {
        TransactionBuilder t = new TransactionBuilder();
        t.addData(data);

        TransactionOutput change = new TransactionOutput(value, to);
        t.addOutput(change);

        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        TransactionBuilder prevTx = new TransactionBuilder();
        prevTx.addOutput(new TransactionOutput(value, to));
        // Connect it.
        t.addInput(prevTx.build(), 0);
        // Fake signature.
        return t.build();
    }


    /**
     * Create a fake TX for unit tests, for use with unit tests that need greater control. One outputs, 2 random inputs,
     * split randomly to create randomness.
     */

    public static Transaction createFakeTxWithoutChangeAddress(NetworkParameters params, Coin value, CashAddress to) {
        TransactionBuilder t = new TransactionBuilder();
        TransactionOutput outputToMe = new TransactionOutput(value, to);
        t.addOutput(outputToMe);

        // Make a random split in the output value so we get a distinct hash when we call this multiple times with same args
        long split = new Random().nextLong();
        if (split < 0) { split *= -1; }
        if (split == 0) { split = 15; }
        while (split > value.getValue()) {
            split /= 2;
        }

        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        TransactionBuilder prevTx1 = new TransactionBuilder();
        TransactionOutput prevOut1 = new TransactionOutput(Coin.Companion.valueOf(split), to);
        prevTx1.addOutput(prevOut1);
        // Connect it.
//        t.addInput(prevOut1); // TODO: removed .setScriptSig(ScriptBuilder.createInputScript(TransactionSignature.dummy()));
        t.addInput(prevTx1.build(), 0); // TODO: removed .setScriptSig(ScriptBuilder.createInputScript(TransactionSignature.dummy()));
        // Fake signature.

        // Do it again
        TransactionBuilder prevTx2 = new TransactionBuilder();
        TransactionOutput prevOut2 = new TransactionOutput(Coin.Companion.valueOf(value.getValue() - split), to);
        prevTx2.addOutput(prevOut2);
//        t.addInput(prevOut2); // TODO: removed  .setScriptSig(ScriptBuilder.createInputScript(TransactionSignature.dummy()));
        t.addInput(prevTx2.build(), 0); // TODO: removed  .setScriptSig(ScriptBuilder.createInputScript(TransactionSignature.dummy()));

        return t.build();
    }


    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two outputs, one to us, one to somewhere
     * else to simulate change. There is one random input.
     */
    public static Transaction createFakeTx(NetworkParameters params, Coin value, CashAddress to) {
        return createFakeTxWithChangeAddress(params, value, to, ECKey.Companion.create().toCashAddress(params));
    }

    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two outputs, one to us, one to somewhere
     * else to simulate change. There is one random input.
     */
/*
    public static Transaction createFakeTx(NetworkParameters params, Coin value, ECKey to) {
        Transaction t = new Transaction(params);
        TransactionOutput outputToMe = new TransactionOutput(params, t, value, to);
        t.addOutput(outputToMe);
        TransactionOutput change = new TransactionOutput(params, t, Coin.Companion.valueOf(1, 11), ECKey.Companion.create());
        t.addOutput(change);
        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        Transaction prevTx = new Transaction(params);
        TransactionOutput prevOut = new TransactionOutput(params, prevTx, value, to);
        prevTx.addOutput(prevOut);
        // Connect it.
        t.addInput(prevOut);
        return roundTripTransaction(params, t);
    }
*/

    /**
     * Transaction[0] is a feeder transaction, supplying BTC to Transaction[1]
     */
/*
    public static Transaction[] createFakeTx(NetworkParameters params, Coin value,
                                             CashAddress to, CashAddress from) {
        // Create fake TXes of sufficient realism to exercise the unit tests. This transaction send BTC from the
        // from address, to the to address with to one to somewhere else to simulate change.
        Transaction t = new Transaction(params);
        TransactionOutput outputToMe = new TransactionOutput(params, t, value, to);
        t.addOutput(outputToMe);
        TransactionOutput change = new TransactionOutput(params, t, Coin.Companion.valueOf(1, 11), ECKey.Companion.create().toCashAddress(params));
        t.addOutput(change);
        // Make a feeder tx that sends to the from address specified. This feeder tx is not really valid but it doesn't
        // matter for our purposes.
        Transaction feederTx = new Transaction(params);
        TransactionOutput feederOut = new TransactionOutput(params, feederTx, value, from);
        feederTx.addOutput(feederOut);

        // make a previous tx that sends from the feeder to the from address
        Transaction prevTx = new Transaction(params);
        TransactionOutput prevOut = new TransactionOutput(params, prevTx, value, to);
        prevTx.addOutput(prevOut);

        // Connect up the txes
        prevTx.addInput(feederOut);
        t.addInput(prevOut);

        // roundtrip the tx so that they are just like they would be from the wire
        return new Transaction[]{roundTripTransaction(params, prevTx), roundTripTransaction(params,t)};
    }
*/

    public static class DoubleSpends {
        public Transaction t1, t2, prevTx;
    }

    /**
     * Creates two transactions that spend the same (fake) output. t1 spends to "to". t2 spends somewhere else.
     * The fake output goes to the same address as t2.
     */
/*
    public static DoubleSpends createFakeDoubleSpendTxns(NetworkParameters params, CashAddress to) {
        DoubleSpends doubleSpends = new DoubleSpends();
        Coin value = COIN;
        CashAddress someBadGuy = ECKey.Companion.create().toCashAddress(params);

        doubleSpends.prevTx = new Transaction(params);
        TransactionOutput prevOut = new TransactionOutput(params, doubleSpends.prevTx, value, someBadGuy);
        doubleSpends.prevTx.addOutput(prevOut);

        doubleSpends.t1 = new Transaction(params);
        TransactionOutput o1 = new TransactionOutput(params, doubleSpends.t1, value, to);
        doubleSpends.t1.addOutput(o1);
        doubleSpends.t1.addInput(prevOut);

        doubleSpends.t2 = new Transaction(params);
        doubleSpends.t2.addInput(prevOut);
        TransactionOutput o2 = new TransactionOutput(params, doubleSpends.t2, value, someBadGuy);
        doubleSpends.t2.addOutput(o2);

        try {
//            doubleSpends.t1 = params.getDefaultSerializer().makeTransaction(doubleSpends.t1.bitcoinSerialize());
//            doubleSpends.t2 = params.getDefaultSerializer().makeTransaction(doubleSpends.t2.bitcoinSerialize());
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        }
        return doubleSpends;
    }
*/

}
