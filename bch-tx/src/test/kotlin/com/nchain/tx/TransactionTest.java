/*
 * Copyright 2014 Google Inc.
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
import com.nchain.bitcoinkt.core.TransactionSignatureBuilder;
import com.nchain.key.DumpedPrivateKey;
import com.nchain.key.ECKey;
import com.nchain.shared.VerificationException;
import com.nchain.params.MainNetParams;
import com.nchain.params.NetworkParameters;
import com.nchain.params.UnitTestParams;
import com.nchain.shared.Sha256Hash;
import com.nchain.tools.HEX;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.tx.TransactionSignature;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Just check the Transaction.verify() method. Most methods that have complicated logic in Transaction are tested
 * elsewhere, e.g. signing and hashing are well exercised by the wallet tests, the full block chain tests and so on.
 * The verify method is also exercised by the full block chain tests, but it can also be used by API users alone,
 * so we make sure to cover it here as well.
 */
public class TransactionTest {
    private static final NetworkParameters PARAMS = UnitTestParams.INSTANCE;
    private static final CashAddress ADDRESS = ECKey.Companion.create().toCashAddress(PARAMS);

    @Test
    public void regular() throws IOException {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.verify();
    }

    @Test(expected = VerificationException.EmptyInputsOrOutputs.class)
    public void emptyOutputs() throws Exception {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.clearOutputs();
        tx.verify();
    }

    @Test(expected = VerificationException.EmptyInputsOrOutputs.class)
    public void emptyInputs() throws Exception {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.clearInputs();
        tx.verify();
    }

    @Test(expected = VerificationException.LargerThanMaxBlockSize.class)
    public void tooHuge() throws Exception {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.addInput(new TransactionInput(PARAMS, null, new byte[NetworkParameters.MAX_BLOCK_SIZE]));
        tx.verify();
    }

    @Test(expected = VerificationException.DuplicatedOutPoint.class)
    public void duplicateOutPoint() throws Exception {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        TransactionInput input = tx.getInput(0);
        // TODO: review
//        input.setScriptBytes(new byte[1]);
//        tx.addInput(input.duplicateDetached());
        tx.addInput(input);
        tx.verify();
    }


    @Test(expected = VerificationException.NegativeValueOutput.class)
    public void negativeOutput() throws Exception {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.getOutput(0).setValue(Coin.Companion.getNEGATIVE_SATOSHI());
        tx.verify();
    }

    @Test(expected = VerificationException.ExcessiveValue.class)
    public void exceedsMaxMoney2() throws Exception {
        Coin half = Coin.getCOIN().multiply(NetworkParameters.MAX_COINS).divide(2).add(Coin.getSATOSHI());
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.getOutput(0).setValue(half);
        tx.addOutput(half, ADDRESS);
        tx.verify();
    }

    @Test
    public void noExceedsMaxMoney() throws Exception {
        Coin half = Coin.getCOIN().multiply(NetworkParameters.MAX_COINS).divide(2);
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.getOutput(0).setValue(half);
        tx.addOutput(half, ADDRESS);
        tx.verify();
    }
    
    @Test(expected = VerificationException.UnexpectedCoinbaseInput.class)
    public void coinbaseInputInNonCoinbaseTX() throws Exception {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.addInput(Sha256Hash.Companion.getZERO_HASH(), 0xFFFFFFFFL, new ScriptBuilder().data(new byte[10]).build());
        tx.verify();
    }

    @Test(expected = VerificationException.CoinbaseScriptSizeOutOfRange.class)
    public void coinbaseScriptSigTooSmall() throws Exception {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.clearInputs();
        tx.addInput(Sha256Hash.Companion.getZERO_HASH(), 0xFFFFFFFFL, new ScriptBuilder().build());
        tx.verify();
    }

    @Test(expected = VerificationException.CoinbaseScriptSizeOutOfRange.class)
    public void coinbaseScriptSigTooLarge() throws Exception {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        tx.clearInputs();
        TransactionInput input = tx.addInput(Sha256Hash.Companion.getZERO_HASH(), 0xFFFFFFFFL, new ScriptBuilder().data(new byte[99]).build());
        assertEquals(101, input.getScriptBytes().length);
        tx.verify();
    }


    @Test
    public void testOptimalEncodingMessageSize() throws IOException {
        Transaction emptyTx = new Transaction(PARAMS);
        int lengthTransactionEmpty = emptyTx.bitcoinSerialize().length;

//        System.err.println(lengthTransactionEmpty);
        Transaction tx = FakeTxBuilder.createFakeTxWithChangeAddress(PARAMS, Coin.getFIFTY_COINS(), ECKey.Companion.create().toCashAddress(PARAMS),ECKey.Companion.create().toCashAddress(PARAMS));
        tx.addOutput(new TransactionOutput(PARAMS, null, Coin.Companion.getCOIN(), ADDRESS));

        int lengthFullTransaction = lengthTransactionEmpty;
        for (TransactionOutput out : tx.getOutputs()) {
//            System.err.println("    "+out.bitcoinSerialize().length);
            lengthFullTransaction += out.bitcoinSerialize().length;
        }

        for (TransactionInput in : tx.getInputs()) {
//            System.err.println("    "+in.bitcoinSerialize().length);
            lengthFullTransaction += in.bitcoinSerialize().length;
        }

//        System.err.println(lengthFullTransaction);
        // optimal encoding size should equal the length we just calculated
        assertEquals(lengthFullTransaction, tx.bitcoinSerialize().length);
    }
    /*
    @Test
    public void testIsMatureReturnsFalseIfTransactionIsCoinbaseAndConfidenceTypeIsNotEqualToBuilding() {
        Transaction tx = FakeTxBuilder.createFakeCoinbaseTx(PARAMS);

        tx.getConfidence().setConfidenceType(ConfidenceType.UNKNOWN);
        assertEquals(tx.isMature(), false);

        tx.getConfidence().setConfidenceType(ConfidenceType.PENDING);
        assertEquals(tx.isMature(), false);

        tx.getConfidence().setConfidenceType(ConfidenceType.DEAD);
        assertEquals(tx.isMature(), false);
    }
    */

    @Test
    public void testCLTVPaymentChannelTransactionSpending() {
        BigInteger time = BigInteger.valueOf(20);

        ECKey from = ECKey.create(), to = ECKey.create(), incorrect = ECKey.create();
        Script outputScript = ScriptBuilder.createCLTVPaymentChannelOutput(time, from, to);

        Transaction tx = new Transaction(PARAMS);
        tx.addInput(new TransactionInput(PARAMS, tx, new byte[] {}));
        tx.getInput(0).setSequenceNumber(0);
        tx.setLockTime(time.subtract(BigInteger.ONE).longValue());
        TransactionSignature fromSig =
                new TransactionSignatureBuilder(tx).calculateSignature(0, from, outputScript, Transaction.SigHash.SINGLE, false);
        TransactionSignature toSig =
                new TransactionSignatureBuilder(tx).calculateSignature(0, to, outputScript, Transaction.SigHash.SINGLE, false);
        TransactionSignature incorrectSig =
                new TransactionSignatureBuilder(tx).calculateSignature(0, incorrect, outputScript, Transaction.SigHash.SINGLE, false);
        Script scriptSig =
                ScriptBuilder.createCLTVPaymentChannelInput(fromSig, toSig);
        Script refundSig =
                ScriptBuilder.createCLTVPaymentChannelRefund(fromSig);
        Script invalidScriptSig1 =
                ScriptBuilder.createCLTVPaymentChannelInput(fromSig, incorrectSig);
        Script invalidScriptSig2 =
                ScriptBuilder.createCLTVPaymentChannelInput(incorrectSig, toSig);

        EnumSet<Script.VerifyFlag> flags = EnumSet.of(Script.VerifyFlag.STRICTENC);
        try {
            scriptSig.correctlySpends(tx, 0, outputScript, flags);
        } catch (ScriptException e) {
            e.printStackTrace();
            fail("Settle transaction failed to correctly spend the payment channel");
        }

        try {
            refundSig.correctlySpends(tx, 0, outputScript, Script.getALL_VERIFY_FLAGS());
            fail("Refund passed before expiry");
        } catch (ScriptException e) { }
        try {
            invalidScriptSig1.correctlySpends(tx, 0, outputScript, Script.getALL_VERIFY_FLAGS());
            fail("Invalid sig 1 passed");
        } catch (ScriptException e) { }
        try {
            invalidScriptSig2.correctlySpends(tx, 0, outputScript, Script.getALL_VERIFY_FLAGS());
            fail("Invalid sig 2 passed");
        } catch (ScriptException e) { }
    }

    @Test
    public void testCLTVPaymentChannelTransactionRefund() {
        BigInteger time = BigInteger.valueOf(20);

        ECKey from = ECKey.create(), to = ECKey.create(), incorrect = ECKey.create();
        Script outputScript = ScriptBuilder.createCLTVPaymentChannelOutput(time, from, to);

        Transaction tx = new Transaction(PARAMS);
        tx.addInput(new TransactionInput(PARAMS, tx, new byte[] {}));
        tx.getInput(0).setSequenceNumber(0);
        tx.setLockTime(time.add(BigInteger.ONE).longValue());
        TransactionSignature fromSig =
                new TransactionSignatureBuilder(tx).calculateSignature(0, from, outputScript, Transaction.SigHash.SINGLE, false);
        TransactionSignature incorrectSig =
                new TransactionSignatureBuilder(tx).calculateSignature(0, incorrect, outputScript, Transaction.SigHash.SINGLE, false);
        Script scriptSig =
                ScriptBuilder.createCLTVPaymentChannelRefund(fromSig);
        Script invalidScriptSig =
                ScriptBuilder.createCLTVPaymentChannelRefund(incorrectSig);

        EnumSet<Script.VerifyFlag> flags = EnumSet.of(Script.VerifyFlag.STRICTENC);
        try {
            scriptSig.correctlySpends(tx, 0, outputScript, flags);
        } catch (ScriptException e) {
            e.printStackTrace();
            fail("Refund failed to correctly spend the payment channel");
        }

        try {
            invalidScriptSig.correctlySpends(tx, 0, outputScript, Script.getALL_VERIFY_FLAGS());
            fail("Invalid sig passed");
        } catch (ScriptException e) { }
    }



    @Test
    public void testToStringWhenIteratingOverAnInputCatchesAnException() {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS);
        TransactionInput ti = new TransactionInput(PARAMS, tx, new byte[0]) {
            @Override
            public Script getScriptSig() throws ScriptException {
                throw new ScriptException("");
            }
        };

        tx.addInput(ti);
        assertEquals(tx.toString().contains("[exception: "), true);
    }

    @Test
    public void testToStringWhenThereAreZeroInputs() {
        Transaction tx = new Transaction(PARAMS);
        assertEquals(tx.toString().contains("No inputs!"), true);
    }
                             /*
    @Test
    public void testTheTXByHeightComparator() {
        Transaction tx1 = FakeTxBuilder.createFakeTx(PARAMS);
        tx1.getConfidence().setAppearedAtChainHeight(1);

        Transaction tx2 = FakeTxBuilder.createFakeTx(PARAMS);
        tx2.getConfidence().setAppearedAtChainHeight(2);

        Transaction tx3 = FakeTxBuilder.createFakeTx(PARAMS);
        tx3.getConfidence().setAppearedAtChainHeight(3);

        SortedSet<Transaction> set = new TreeSet<Transaction>(Transaction.getSORT_TX_BY_HEIGHT());
        set.add(tx2);
        set.add(tx1);
        set.add(tx3);

        Iterator<Transaction> iterator = set.iterator();

        assertEquals(tx1.equals(tx2), false);
        assertEquals(tx1.equals(tx3), false);
        assertEquals(tx1.equals(tx1), true);

        assertEquals(iterator.next().equals(tx3), true);
        assertEquals(iterator.next().equals(tx2), true);
        assertEquals(iterator.next().equals(tx1), true);
        assertEquals(iterator.hasNext(), false);
    }


    @Test(expected = ScriptException.class)
    public void testAddSignedInputThrowsExceptionWhenScriptIsNotToRawPubKeyAndIsNotToAddress() {
        ECKey key = ECKey.create();
        CashAddress addr = key.toCashAddress(PARAMS);
        Transaction fakeTx = FakeTxBuilder.createFakeTx(PARAMS, Coin.getCOIN(), addr);

        Transaction tx = new Transaction(PARAMS);
        tx.addOutput(fakeTx.getOutput(0));

        Script script = ScriptBuilder.createOpReturnScript(new byte[0]);

        tx.addSignedInput(fakeTx.getOutput(0).getOutPointFor(), script, key);
    }
                                    */
    /*
    @Test
    public void testPrioSizeCalc() throws Exception {
        Transaction tx1 = FakeTxBuilder.createFakeTx(PARAMS, Coin.getCOIN(), ADDRESS);
        int size1 = tx1.getMessageSize();
        int size2 = tx1.getMessageSizeForPriorityCalc();
        assertEquals(113, size1 - size2);
        tx1.getInput(0).setScriptSig(new Script(new byte[109]));
        assertEquals(78, tx1.getMessageSizeForPriorityCalc());
        tx1.getInput(0).setScriptSig(new Script(new byte[110]));
        assertEquals(78, tx1.getMessageSizeForPriorityCalc());
        tx1.getInput(0).setScriptSig(new Script(new byte[111]));
        assertEquals(79, tx1.getMessageSizeForPriorityCalc());
    }
      
    @Test
    public void testCoinbaseHeightCheck() throws VerificationException {
        // Coinbase transaction from block 300,000
        final byte[] transactionBytes = Utils.INSTANCE.getHEX().decode("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4803e09304062f503253482f0403c86d53087ceca141295a00002e522cfabe6d6d7561cf262313da1144026c8f7a43e3899c44f6145f39a36507d36679a8b7006104000000000000000000000001c8704095000000001976a91480ad90d403581fa3bf46086a91b2d9d4125db6c188ac00000000");
        final int height = 300000;
        final Transaction transaction = PARAMS.getDefaultSerializer().makeTransaction(transactionBytes);
        transaction.checkCoinBaseHeight(height);
    }
*/

    /**
     * Test a coinbase transaction whose script has nonsense after the block height.
     * See https://github.com/bitcoinj/bitcoinj/issues/1097
     */
/*
    @Test
    public void testCoinbaseHeightCheckWithDamagedScript() throws VerificationException {
        // Coinbase transaction from block 224,430
        final byte[] transactionBytes = Utils.INSTANCE.getHEX().decode(
            "010000000100000000000000000000000000000000000000000000000000000000"
            + "00000000ffffffff3b03ae6c0300044bd7031a0400000000522cfabe6d6d0000"
            + "0000000000b7b8bf0100000068692066726f6d20706f6f6c7365727665726aac"
            + "1eeeed88ffffffff01e0587597000000001976a91421c0d001728b3feaf11551"
            + "5b7c135e779e9f442f88ac00000000");
        final int height = 224430;
        final Transaction transaction = PARAMS.getDefaultSerializer().makeTransaction(transactionBytes);
        transaction.checkCoinBaseHeight(height);
    }
*/

    /**
     * Ensure that hashForSignature() doesn't modify a transaction's data, which could wreak multithreading havoc.
     */
/*
    @Test
    public void testHashForSignatureThreadSafety() {
        Block genesis = UnitTestParams.INSTANCE.getGenesisBlock();
        Block block1 = genesis.createNextBlock(ECKey.create().toAddress(UnitTestParams.INSTANCE),
                    genesis.getTransactions().get(0).getOutput(0).getOutPointFor());

        final Transaction tx = block1.getTransactions().get(1);
        final String txHash = tx.getHashAsString();
        final String txNormalizedHash = new TransactionSignatureBuilder(tx).hashForSignature(0, new byte[0], Transaction.SigHash.ALL.byteValue()).toString();

        for (int i = 0; i < 100; i++) {
            // ensure the transaction object itself was not modified; if it was, the hash will change
            assertEquals(txHash, tx.getHashAsString());
            new Thread(){
                public void run() {
                    assertEquals(txNormalizedHash, new TransactionSignatureBuilder(tx).hashForSignature(0, new byte[0], Transaction.SigHash.ALL.byteValue()).toString());
                }
            };
        }
    }
    */

    @Test
    public void testHashForSignature()
    {
        String dumpedPrivateKey = "KyYyHLChvJKrM4kxCEpdmqR2usQoET2V1JbexZjaxV36wytPw7v1";
        DumpedPrivateKey dumpedPrivateKey1 = DumpedPrivateKey.fromBase58(MainNetParams.INSTANCE, dumpedPrivateKey);
        ECKey key = dumpedPrivateKey1.getKey();

        String txConnectedData = "020000000284ff1fbdee5aeeaf7976ddfb395e00066c150d4ed90da089f5b47e46215dc23c010000006b4830450221008e1f85698b5130f2dd56236541f2b2c1f7676721acebbbdc3c8711a345d2f96b022065f1f2ea915b8844319b3e81e33cb6a26ecee838dc0060248b10039e994ab1e641210248dd879c54147390a12f8e8a7aa8f23ce2659a996fa7bf756d6b2187d8ed624ffeffffffefd0db693d73d8087eb1f44916be55ee025f25d7a3dbcf82e3318e56e6ccded9000000006a4730440221009c6ba90ca215ce7ad270e6688940aa6d97be6c901a430969d9d88bef7c8dc607021f51d088dadcaffbd88e5514afedfa9e2cac61a1024aaa4c88873361193e4da24121039cc4a69e1e93ebadab2870c69cb4feb0c1c2bfad38be81dda2a72c57d8b14e11feffffff0230c80700000000001976a914517abefd39e71c633bd5a23fd75b5dbd47bc461b88acc8911400000000001976a9147b983c4efaf519e9caebde067b6495e5dcc491cb88acba4f0700";
        Transaction txConnected = new Transaction(MainNetParams.INSTANCE, txConnectedData);

        String txData = "0200000001411d29708a0b4165910fbc73b6efbd3d183b1bf457d8840beb23874714c41f61010000006a47304402204b3b868a9a966c44fb05f2cfb3c888b5617435d00ebe1dfe4bd452fd538592d90220626adfb79def08c0375de226b77cefbd3c659aad299dfe950539d01d2770132a41210354662c29cec7074ad26af8664bffdb7f540990ece13a872da5fdfa8be019563efeffffff027f5a1100000000001976a914dcbfe1b282c167c1942a2bdc927de8b4a368146588ac400d0300000000001976a914fb57314db46dd11b4a99c16779a5e160858df43888acd74f0700";
        Transaction tx = new Transaction(MainNetParams.INSTANCE, txData);

        Script sig = tx.getInput(0).getScriptSig();

        EnumSet<Script.VerifyFlag> flags = EnumSet.of(Script.VerifyFlag.STRICTENC, Script.VerifyFlag.SIGHASH_FORKID);
        sig.correctlySpends(tx, 0, txConnected.getOutput(1).getScriptPubKey(), txConnected.getOutput(1).getValue(), flags);


    }

  /*

    @Test
    public void testOpReturn() {
        Address goodAddress = Address.fromBase58(PARAMS, "mrj2K6txjo2QBcSmuAzHj4nD1oXSEJE1Qo");

        final byte[] bytes = "hello".getBytes();
        Transaction withReturnData = FakeTxBuilder.createFakeTxToMeWithReturnData(PARAMS, Coin.getZERO(), goodAddress, bytes);

        assertEquals(true, withReturnData.isOpReturn());
        assertArrayEquals(bytes, withReturnData.getOpReturnData());

        Transaction withoutReturnData = FakeTxBuilder.createFakeTx(PARAMS);
        assertEquals(false, withoutReturnData.isOpReturn());
        assertEquals(null, withoutReturnData.getOpReturnData());

    }
*/

    @Test
    public void testRawParseAndExport() {
        NetworkParameters params = PARAMS;

        // https://blockchain.info/tx/ed27cf72886af7c830faeff136b3859185310334330a4856f60c768ab46b9c1c
        String rawTx1 = "010000000193e3073ecc1d27f17e3d287ccefdfdba5f7d8c160242dbcd547b18baef12f9b31a0000006b483045022100af501dc9ef2907247d28a5169b8362ca494e1993f833928b77264e604329eec40220313594f38f97c255bcea6d5a4a68e920508ef93fd788bcf5b0ad2fa5d34940180121034bb555cc39ba30561793cf39a35c403fe8cf4a89403b02b51e058960520bd1e3ffffffff02b3bb0200000000001976a914f7d52018971f4ab9b56f0036958f84ae0325ccdc88ac98100700000000001976a914f230f0a16a98433eca0fa70487b85fb83f7b61cd88ac00000000";

        Transaction tx1 = new Transaction(params, rawTx1);
        assertEquals(rawTx1, HEX.encode(tx1.bitcoinSerialize()));

        // https://blockchain.info/tx/0024db8e11da76b2344e0722bf9488ba2aed611913f9803a62ac3b41f5603946
        String rawTx2 = "01000000011c9c6bb48a760cf656480a33340331859185b336f1effa30c8f76a8872cf27ed000000006a47304402201c999cf44dc6576783c0f55b8ff836a1e22db87ed67dc3c39515a6676cfb58e902200b4a925f9c8d6895beed841db135051f8664ab349f2e3ea9f8523a6f47f93883012102e58d7b931b5d43780fda0abc50cfd568fcc26fb7da6a71591a43ac8e0738b9a4ffffffff029b010100000000001976a9140f0fcdf818c0c88df6860c85c9cc248b9f37eaff88ac95300100000000001976a9140663d2403f560f8d053a25fbea618eb47071617688ac00000000";
        Transaction tx2 = new Transaction(params, rawTx2);
        assertEquals(rawTx2, HEX.encode(tx2.bitcoinSerialize()));

//        https://blockchair.com/bitcoin-cash/transaction/0eab89a271380b09987bcee5258fca91f28df4dadcedf892658b9bc261050d96
        String rawTx3 = "01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff2c03ccec051f4d696e656420627920416e74506f6f6c20626a3515d2158520566e53850b00110000008c7a0900ffffffff01e170f895000000001976a9149524440a5b54cca9c46ef277c34739e9b521856d88ac00000000";
        Transaction tx3 = new Transaction(params, rawTx3);
        assertEquals(rawTx3, HEX.encode(tx3.bitcoinSerialize()));

//        https://blockchair.com/bitcoin-cash/transaction/1e24eaaa72b6c10a4d57084ab3acb612bd123bbf64c2a5746b6221b02202090e
        String rawTx4 = "0200000001a73374e059d610c0f8ee6fcbc1f89b54ebf7b109426b38d8e3e744e698abf8a5010000006a47304402200dfc3bacafb825c0c457ff3756e9c243965be45d5d490e70c5dfb2f6060445870220431e3d9f852d4b5803ab0d189d8931dc6c35f3724d6be3e8928043b7c789f66a4121022e46d40245e27e8ef260f8d724838c850a5447b81ae9f77d2d5e28fd2640a36a0000000001d4092800000000001976a9147775f3423eb410a4184d9d3ef93f7ed4d1c1d4e988ac00000000";
        Transaction tx4 = new Transaction(params, rawTx4);
        assertEquals(rawTx4, HEX.encode(tx4.bitcoinSerialize()));
    }

}
