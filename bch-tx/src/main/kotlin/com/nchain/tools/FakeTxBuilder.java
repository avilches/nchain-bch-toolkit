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

package com.nchain.tools;

import com.nchain.address.CashAddress;
import com.nchain.key.ECKey;
import com.nchain.params.NetworkParameters;
import com.nchain.shared.Randomizer;
import com.nchain.tx.*;

import java.security.SecureRandom;
import java.util.Random;

public class FakeTxBuilder {
    private final static Coin COIN = Coin.getCOIN();
    private final static Coin FIFTY_COINS = Coin.getFIFTY_COINS();

    static {
        Randomizer.INSTANCE.setRandom(new SecureRandom(new byte[]{}));
    }

    /**
     * Create a fake transaction, without change.
     */
    public static TransactionBuilder createFakeTx(NetworkParameters params) {
        return createFakeTxWithoutChangeAddress(params, COIN, ECKey.create().toCashAddress(params));
    }

    /**
     * Create a fake transaction, without change.
     */
    public static TransactionBuilder createFakeTxWithoutChange(final NetworkParameters params, final TransactionOutput output) {
        Transaction prevTx = FakeTxBuilder.createFakeTx(params, COIN, ECKey.create().toCashAddress(params)).build();
        TransactionBuilder tx = new TransactionBuilder().addOutput(output).addInput(prevTx, 0);
        return tx;
    }

    /**
     * Create a fake coinbase transaction.
     */
    public static TransactionBuilder createFakeCoinbaseTx(final NetworkParameters params) {
        TransactionBuilder tx = new TransactionBuilder().
                addInput(new byte[0], TransactionOutPoint.getUNCONNECTED(), TransactionInput.NO_SEQUENCE). // coinbase input
                addOutput(FIFTY_COINS, ECKey.create().toCashAddress(params)); // output to me

        // TODO: checkState(tx.build().isCoinBase());
        return tx;
    }

    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two outputs, one to us, one to somewhere
     * else to simulate change. There is one random input.
     */
    public static TransactionBuilder createFakeTxWithChangeAddress(NetworkParameters params, Coin value, CashAddress to, CashAddress changeOutput) {
        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        Transaction prevTx = new TransactionBuilder().addOutput(new TransactionOutput(value, to)).build();

        TransactionBuilder transaction = new TransactionBuilder().
                addOutput(value, to). // output to me
                addOutput(Coin.valueOf(1, 11), changeOutput). // change
                addInput(prevTx, 0);
        return transaction;
    }

    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two outputs, one to nobody (return data, 0 coins) and other
     * to us (the change) with all the value expended from the UTXO
     */

    public static TransactionBuilder createFakeTxToMeWithReturnData(NetworkParameters params, Coin value, CashAddress to, byte[] data) {
        TransactionBuilder tx = new TransactionBuilder().addData(data);
        tx.addOutput(value, to);
        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        TransactionBuilder prevTx = new TransactionBuilder();
        prevTx.addOutput(new TransactionOutput(value, to));
        // Connect it.
        tx.addInput(prevTx.build(), 0);
        return tx;
    }


    /**
     * Create a fake TX for unit tests, for use with unit tests that need greater control. One outputs, 2 random inputs,
     * split randomly to create randomness.
     */

    public static TransactionBuilder createFakeTxWithoutChangeAddress(NetworkParameters params, Coin value, CashAddress to) {

        // Make a random split in the output value so we get a distinct hash when we call this multiple times with same args
        long split = (long)Math.random() * value.getValue();

        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        Transaction prevTx1 = new TransactionBuilder().addOutput(Coin.valueOf(split), to).build();
        Transaction prevTx2 = new TransactionBuilder().addOutput(Coin.valueOf(value.getValue() - split), to).build();

        TransactionBuilder tx = new TransactionBuilder().
                addInput(prevTx1, 0).
                addInput(prevTx2, 0).
                addOutput(value, to); // output to me

        Transaction transaction = tx.build();

        // TODO:
//        checkState(transaction.getInputSum().getValue() == value.getValue(), "Error in inputSum");
//        checkState(transaction.getOutputSum().getValue() == value.getValue(), "Error in outputSum");
//        checkState(transaction.getFee().getValue() == 0, "Error in fee");

        return tx;
    }


    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two outputs, one to us, one to somewhere
     * else to simulate change. There is one random input.
     */
    public static TransactionBuilder createFakeTx(NetworkParameters params, Coin value, CashAddress to) {
        return createFakeTxWithChangeAddress(params, value, to, ECKey.create().toCashAddress(params));
    }

    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two outputs, one to us, one to somewhere
     * else to simulate change. There is one random input.
     */
/*
    public static TransactionBuilder createFakeTx(NetworkParameters params, Coin value, ECKey to) {
        TransactionBuilder t = new TransactionBuilder(params);
        TransactionOutput outputToMe = new TransactionOutput(params, t, value, to);
        t.addOutput(outputToMe);
        TransactionOutput change = new TransactionOutput(params, t, Coin.valueOf(1, 11), ECKey.create());
        t.addOutput(change);
        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        TransactionBuilder prevTx = new TransactionBuilder(params);
        TransactionOutput prevOut = new TransactionOutput(params, prevTx, value, to);
        prevTx.addOutput(prevOut);
        // Connect it.
        t.addInput(prevOut);
        return roundTripTransaction(params, t);
    }
*/

    /**
     * TransactionBuilder[0] is a feeder transaction, supplying BTC to TransactionBuilder[1]
     */
/*
    public static TransactionBuilder[] createFakeTx(NetworkParameters params, Coin value,
                                             CashAddress to, CashAddress from) {
        // Create fake TXes of sufficient realism to exercise the unit tests. This transaction send BTC from the
        // from address, to the to address with to one to somewhere else to simulate change.
        TransactionBuilder t = new TransactionBuilder(params);
        TransactionOutput outputToMe = new TransactionOutput(params, t, value, to);
        t.addOutput(outputToMe);
        TransactionOutput change = new TransactionOutput(params, t, Coin.valueOf(1, 11), ECKey.create().toCashAddress(params));
        t.addOutput(change);
        // Make a feeder tx that sends to the from address specified. This feeder tx is not really valid but it doesn't
        // matter for our purposes.
        TransactionBuilder feederTx = new TransactionBuilder(params);
        TransactionOutput feederOut = new TransactionOutput(params, feederTx, value, from);
        feederTx.addOutput(feederOut);

        // make a previous tx that sends from the feeder to the from address
        TransactionBuilder prevTx = new TransactionBuilder(params);
        TransactionOutput prevOut = new TransactionOutput(params, prevTx, value, to);
        prevTx.addOutput(prevOut);

        // Connect up the txes
        prevTx.addInput(feederOut);
        t.addInput(prevOut);

        // roundtrip the tx so that they are just like they would be from the wire
        return new TransactionBuilder[]{roundTripTransaction(params, prevTx), roundTripTransaction(params,t)};
    }
*/

    public static class DoubleSpends {
        public TransactionBuilder t1, t2, prevTx;
    }

    /**
     * Creates two transactions that spend the same (fake) output. t1 spends to "to". t2 spends somewhere else.
     * The fake output goes to the same address as t2.
     */
/*
    public static DoubleSpends createFakeDoubleSpendTxns(NetworkParameters params, CashAddress to) {
        DoubleSpends doubleSpends = new DoubleSpends();
        Coin value = COIN;
        CashAddress someBadGuy = ECKey.create().toCashAddress(params);

        doubleSpends.prevTx = new TransactionBuilder(params);
        TransactionOutput prevOut = new TransactionOutput(params, doubleSpends.prevTx, value, someBadGuy);
        doubleSpends.prevTx.addOutput(prevOut);

        doubleSpends.t1 = new TransactionBuilder(params);
        TransactionOutput o1 = new TransactionOutput(params, doubleSpends.t1, value, to);
        doubleSpends.t1.addOutput(o1);
        doubleSpends.t1.addInput(prevOut);

        doubleSpends.t2 = new TransactionBuilder(params);
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
