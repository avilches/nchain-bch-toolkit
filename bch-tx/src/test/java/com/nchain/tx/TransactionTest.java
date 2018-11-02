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
import com.nchain.bitcoinkt.core.TransactionSignatureService;
import com.nchain.key.DumpedPrivateKey;
import com.nchain.key.ECKey;
import com.nchain.params.MainNetParams;
import com.nchain.params.NetworkParameters;
import com.nchain.params.UnitTestParams;
import com.nchain.shared.Sha256Hash;
import com.nchain.shared.VerificationException;
import com.nchain.tools.FakeTxBuilder;
import com.nchain.tools.HEX;
import com.nchain.script.Script;
import com.nchain.script.ScriptBuilder;
import com.nchain.script.ScriptException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.EnumSet;

import static org.junit.Assert.*;

/**
 * Just check the Transaction.verify() method. Most methods that have complicated logic in Transaction are tested
 * elsewhere, e.g. signing and hashing are well exercised by the wallet tests, the full block chain tests and so on.
 * The verify method is also exercised by the full block chain tests, but it can also be used by API users alone,
 * so we make sure to cover it here as well.
 */
public class TransactionTest {
    private static final NetworkParameters PARAMS = UnitTestParams.INSTANCE;
    private static final CashAddress ADDRESS = ECKey.create().toCashAddress(PARAMS);

    @Test
    public void regular() throws IOException {
        FakeTxBuilder.createFakeTx(PARAMS).
                build().verify();
    }

    @Test(expected = VerificationException.EmptyInputsOrOutputs.class)
    public void emptyOutputs() throws Exception {
        FakeTxBuilder.createFakeTx(PARAMS).
                clearOutputs().
                build().verify();
    }

    @Test(expected = VerificationException.EmptyInputsOrOutputs.class)
    public void emptyInputs() throws Exception {
        FakeTxBuilder.createFakeTx(PARAMS).
                clearInputs().
                build().verify();
    }

    @Test(expected = VerificationException.LargerThanMaxBlockSize.class)
    public void tooHuge() throws Exception {
        FakeTxBuilder.createFakeTx(PARAMS).
                addInput(new TransactionInput(new byte[NetworkParameters.MAX_BLOCK_SIZE])).
                build().verify();
    }

    @Test(expected = VerificationException.DuplicatedOutPoint.class)
    public void duplicateOutPoint() throws Exception {
        TransactionBuilder tx = FakeTxBuilder.createFakeTx(PARAMS);
        // Create a new input with the some outpoint of this transaction
        final TransactionOutPoint outpoint = tx.getInputs().get(0).getOutpoint();
        tx.addInput(new TransactionInput(new byte[]{}, outpoint));
        tx.build().verify();
    }


    @Test(expected = VerificationException.NegativeValueOutput.class)
    public void negativeOutput() throws Exception {
        FakeTxBuilder.createFakeTx(PARAMS).
                addOutput(Coin.valueOf(-2), ECKey.create()).
                build().verify();
    }

    @Test(expected = VerificationException.ExcessiveValue.class)
    public void exceedsMaxMoney2() throws Exception {
        Coin half = Coin.getCOIN().multiply(NetworkParameters.MAX_COINS).divide(2).add(Coin.getSATOSHI());
        FakeTxBuilder.createFakeTx(PARAMS).
                clearOutputs().
                addOutput(half, ADDRESS).
                addOutput(half, ADDRESS).
                build().verify();
    }

    @Test
    public void noExceedsMaxMoney() throws Exception {
        Coin half = Coin.getCOIN().multiply(NetworkParameters.MAX_COINS).divide(2);
        FakeTxBuilder.createFakeTx(PARAMS).
                clearOutputs().
                addOutput(half, ADDRESS).
                addOutput(half, ADDRESS).
                build().verify();
    }

    @Test(expected = VerificationException.UnexpectedCoinbaseInput.class)
    public void coinbaseInputInNonCoinbaseTX() throws Exception {
        FakeTxBuilder.createFakeTx(PARAMS).
                addInput(Sha256Hash.getZERO_HASH(), TransactionInput.NO_SEQUENCE, new ScriptBuilder().data(new byte[10]).build()).
                build().verify();
    }

    @Test(expected = VerificationException.CoinbaseScriptSizeOutOfRange.class)
    public void coinbaseScriptSigTooSmall() throws Exception {
        FakeTxBuilder.createFakeTx(PARAMS).
                clearInputs().
                addInput(Sha256Hash.getZERO_HASH(), TransactionInput.NO_SEQUENCE, new ScriptBuilder().build()).
                build().verify();
    }

    @Test(expected = VerificationException.CoinbaseScriptSizeOutOfRange.class)
    public void coinbaseScriptSigTooLarge() throws Exception {
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS).
                clearInputs().
                addInput(Sha256Hash.getZERO_HASH(), TransactionInput.NO_SEQUENCE, new ScriptBuilder().data(new byte[99]).build()).build();
        assertEquals(101, tx.getInput(0).getScriptBytes().length);
        tx.verify();
    }


    @Test
    public void testOptimalEncodingMessageSize() throws IOException {
        Transaction emptyTx = new TransactionBuilder().build();
        final int lengthTransactionEmpty = emptyTx.bitcoinSerialize().length;

        final CashAddress address = ECKey.create().toCashAddress(PARAMS);
        Transaction tx = FakeTxBuilder.createFakeTxWithChangeAddress(PARAMS, Coin.getFIFTY_COINS(), address, address).
                addOutput(Coin.getCOIN(), ADDRESS).
                build();

        int lengthFullTransaction = lengthTransactionEmpty;
        for (TransactionOutput out : tx.getOutputs()) {
            lengthFullTransaction += out.bitcoinSerialize().length;
        }

        for (TransactionInput in : tx.getInputs()) {
            lengthFullTransaction += in.bitcoinSerialize().length;
        }

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

        Transaction tx = new TransactionBuilder(1, time.subtract(BigInteger.ONE).longValue()).
                addInput(new TransactionInput(new byte[]{}, null, 0L)).
                build();

        TransactionSignature fromSig =
                TransactionSignatureService.INSTANCE.calculateSignature(tx, 0, from, outputScript, Transaction.SigHash.SINGLE, false);
        TransactionSignature toSig =
                TransactionSignatureService.INSTANCE.calculateSignature(tx,0, to, outputScript, Transaction.SigHash.SINGLE, false);
        TransactionSignature incorrectSig =
                TransactionSignatureService.INSTANCE.calculateSignature(tx,0, incorrect, outputScript, Transaction.SigHash.SINGLE, false);
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
        } catch (ScriptException e) {
        }
        try {
            invalidScriptSig1.correctlySpends(tx, 0, outputScript, Script.getALL_VERIFY_FLAGS());
            fail("Invalid sig 1 passed");
        } catch (ScriptException e) {
        }
        try {
            invalidScriptSig2.correctlySpends(tx, 0, outputScript, Script.getALL_VERIFY_FLAGS());
            fail("Invalid sig 2 passed");
        } catch (ScriptException e) {
        }
    }

    @Test
    public void testCLTVPaymentChannelTransactionRefund() {
        BigInteger time = BigInteger.valueOf(20);

        ECKey from = ECKey.create(), to = ECKey.create(), incorrect = ECKey.create();
        Script outputScript = ScriptBuilder.createCLTVPaymentChannelOutput(time, from, to);

        Transaction tx = new TransactionBuilder(1, time.add(BigInteger.ONE).longValue()).
                addInput(new TransactionInput(new byte[]{}, null, 0L)).
                build();
        TransactionSignature fromSig =
                TransactionSignatureService.INSTANCE.calculateSignature(tx,0, from, outputScript, Transaction.SigHash.SINGLE, false);
        TransactionSignature incorrectSig =
                TransactionSignatureService.INSTANCE.calculateSignature(tx,0, incorrect, outputScript, Transaction.SigHash.SINGLE, false);
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
        } catch (ScriptException e) {
        }
    }


    @Test
    public void testToStringWhenIteratingOverAnInputCatchesAnException() {
        TransactionInput ti = new TransactionInput(new byte[0]) {
            @Override
            public Script getScriptSig() throws ScriptException {
                throw new ScriptException("");
            }
        };
        Transaction tx = FakeTxBuilder.createFakeTx(PARAMS).addInput(ti).build();
        assertEquals(tx.toString().contains("[exception: "), true);
    }

    @Test
    public void testToStringWhenThereAreZeroInputs() {
        Transaction tx = new TransactionBuilder().build();
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
    public void testHashForSignature() {
        String dumpedPrivateKey = "KyYyHLChvJKrM4kxCEpdmqR2usQoET2V1JbexZjaxV36wytPw7v1";
        DumpedPrivateKey dumpedPrivateKey1 = DumpedPrivateKey.fromBase58(MainNetParams.INSTANCE, dumpedPrivateKey);
        ECKey key = dumpedPrivateKey1.getKey();

        String txConnectedData = "020000000284ff1fbdee5aeeaf7976ddfb395e00066c150d4ed90da089f5b47e46215dc23c010000006b4830450221008e1f85698b5130f2dd56236541f2b2c1f7676721acebbbdc3c8711a345d2f96b022065f1f2ea915b8844319b3e81e33cb6a26ecee838dc0060248b10039e994ab1e641210248dd879c54147390a12f8e8a7aa8f23ce2659a996fa7bf756d6b2187d8ed624ffeffffffefd0db693d73d8087eb1f44916be55ee025f25d7a3dbcf82e3318e56e6ccded9000000006a4730440221009c6ba90ca215ce7ad270e6688940aa6d97be6c901a430969d9d88bef7c8dc607021f51d088dadcaffbd88e5514afedfa9e2cac61a1024aaa4c88873361193e4da24121039cc4a69e1e93ebadab2870c69cb4feb0c1c2bfad38be81dda2a72c57d8b14e11feffffff0230c80700000000001976a914517abefd39e71c633bd5a23fd75b5dbd47bc461b88acc8911400000000001976a9147b983c4efaf519e9caebde067b6495e5dcc491cb88acba4f0700";
        Transaction txConnected = Transaction.parse(txConnectedData);

        String txData = "0200000001411d29708a0b4165910fbc73b6efbd3d183b1bf457d8840beb23874714c41f61010000006a47304402204b3b868a9a966c44fb05f2cfb3c888b5617435d00ebe1dfe4bd452fd538592d90220626adfb79def08c0375de226b77cefbd3c659aad299dfe950539d01d2770132a41210354662c29cec7074ad26af8664bffdb7f540990ece13a872da5fdfa8be019563efeffffff027f5a1100000000001976a914dcbfe1b282c167c1942a2bdc927de8b4a368146588ac400d0300000000001976a914fb57314db46dd11b4a99c16779a5e160858df43888acd74f0700";
        Transaction tx = Transaction.parse(txData);

        Script sig = tx.getInput(0).getScriptSig();

        EnumSet<Script.VerifyFlag> flags = EnumSet.of(Script.VerifyFlag.STRICTENC, Script.VerifyFlag.SIGHASH_FORKID);
        sig.correctlySpends(tx, 0, txConnected.getOutput(1).getScriptPubKey(), txConnected.getOutput(1).getValue(), flags);
    }

    @Test
    public void testOpReturn() {
        CashAddress goodAddress = CashAddress.fromBase58(PARAMS, "mrj2K6txjo2QBcSmuAzHj4nD1oXSEJE1Qo");

        final byte[] bytes = "hello".getBytes();
        Transaction withReturnData = FakeTxBuilder.createFakeTxToMeWithReturnData(PARAMS, Coin.getZERO(), goodAddress, bytes).build();

        assertEquals(true, withReturnData.isOpReturn());
        assertArrayEquals(bytes, withReturnData.getOpReturnData());

        Transaction withoutReturnData = FakeTxBuilder.createFakeTx(PARAMS).build();
        assertEquals(false, withoutReturnData.isOpReturn());
        assertEquals(null, withoutReturnData.getOpReturnData());
    }

    @Test
    public void testRawParseAndExport() {
        NetworkParameters params = PARAMS;

        // https://blockchain.info/tx/ed27cf72886af7c830faeff136b3859185310334330a4856f60c768ab46b9c1c
        String rawTx1 = "010000000193e3073ecc1d27f17e3d287ccefdfdba5f7d8c160242dbcd547b18baef12f9b31a0000006b483045022100af501dc9ef2907247d28a5169b8362ca494e1993f833928b77264e604329eec40220313594f38f97c255bcea6d5a4a68e920508ef93fd788bcf5b0ad2fa5d34940180121034bb555cc39ba30561793cf39a35c403fe8cf4a89403b02b51e058960520bd1e3ffffffff02b3bb0200000000001976a914f7d52018971f4ab9b56f0036958f84ae0325ccdc88ac98100700000000001976a914f230f0a16a98433eca0fa70487b85fb83f7b61cd88ac00000000";

        Transaction tx1 = Transaction.parse(rawTx1);
        assertEquals(rawTx1, HEX.encode(tx1.bitcoinSerialize()));

        // https://blockchain.info/tx/0024db8e11da76b2344e0722bf9488ba2aed611913f9803a62ac3b41f5603946
        String rawTx2 = "01000000011c9c6bb48a760cf656480a33340331859185b336f1effa30c8f76a8872cf27ed000000006a47304402201c999cf44dc6576783c0f55b8ff836a1e22db87ed67dc3c39515a6676cfb58e902200b4a925f9c8d6895beed841db135051f8664ab349f2e3ea9f8523a6f47f93883012102e58d7b931b5d43780fda0abc50cfd568fcc26fb7da6a71591a43ac8e0738b9a4ffffffff029b010100000000001976a9140f0fcdf818c0c88df6860c85c9cc248b9f37eaff88ac95300100000000001976a9140663d2403f560f8d053a25fbea618eb47071617688ac00000000";
        Transaction tx2 = Transaction.parse(rawTx2);
        assertEquals(rawTx2, HEX.encode(tx2.bitcoinSerialize()));

//        https://blockchair.com/bitcoin-cash/transaction/0eab89a271380b09987bcee5258fca91f28df4dadcedf892658b9bc261050d96
        String rawTx3 = "01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff2c03ccec051f4d696e656420627920416e74506f6f6c20626a3515d2158520566e53850b00110000008c7a0900ffffffff01e170f895000000001976a9149524440a5b54cca9c46ef277c34739e9b521856d88ac00000000";
        Transaction tx3 = Transaction.parse(rawTx3);
        assertEquals(rawTx3, HEX.encode(tx3.bitcoinSerialize()));

//        https://blockchair.com/bitcoin-cash/transaction/1e24eaaa72b6c10a4d57084ab3acb612bd123bbf64c2a5746b6221b02202090e
        String rawTx4 = "0200000001a73374e059d610c0f8ee6fcbc1f89b54ebf7b109426b38d8e3e744e698abf8a5010000006a47304402200dfc3bacafb825c0c457ff3756e9c243965be45d5d490e70c5dfb2f6060445870220431e3d9f852d4b5803ab0d189d8931dc6c35f3724d6be3e8928043b7c789f66a4121022e46d40245e27e8ef260f8d724838c850a5447b81ae9f77d2d5e28fd2640a36a0000000001d4092800000000001976a9147775f3423eb410a4184d9d3ef93f7ed4d1c1d4e988ac00000000";
        Transaction tx4 = Transaction.parse(rawTx4);
        assertEquals(rawTx4, HEX.encode(tx4.bitcoinSerialize()));
    }

    @Test
    public void basicBlockParseTest() {
        String hash = "00000000000000a04990202e38466845fe1ec7e704ac69c7007643ddb5a07d70";
        // Real content of the block!
        // You can check it http://chainquery.com/bitcoin-api/getblock/00000000000000a04990202e38466845fe1ec7e704ac69c7007643ddb5a07d70/false
        String rawBlock = "02000000df762998c01c90c354c3b005dd078783abfcda60f04924801a00000000000000f1e6b85d81fe54306723f467ad5f742d9062f536ef9a8ff574d6576588cb7431b2b5db514ec9001aa9b132652401" +
                "000000010000000000000000000000000000000000000000000000000000000000000000ffffffff530372bf030400003a3e01124d696e656420627920425443204775696c642cfabe6d6d7f3a1c9aaa62b86347450280" +
                "9c264016a82ccbc673fbafada4ad8b2aa07666c801000000000000000800001dda0000026cffffffff0160be1e95000000001976a91427a1f12771de5cc3b73941664b2537c15316be4388ac0000000001000000018f8b" +
                "529bc0df8cbb004fd5af26f8e5963d574f520c581dbe1d093feb1947ef47000000006a47304402204333166ad9b4d45399ee6dc9887198b8b1edf28765ef0160f290eec31bc43609022020082db7d5a39732049f0c6e0c" +
                "1ea8bc8e4127c96da209d2dd7aa4fd28ea82440121036f0857417f9e9b59f2db481d8569bc936f873da4830e864a0d89719f00e11fa9ffffffff02b063b829000000001976a9147d2b085ad5e5089558aae4ccccf52d02" +
                "008f349088ac00943577000000001976a9147c9817b1f5869cac68956bc9a8267960d76cb48188ac000000000100000001086066d815c142e3756bc3ca184e6ad0d05d31c0b26c7cee722a794c60e6d2c6000000006c49" +
                "3046022100ac78d3cb0e62d3dde2c15312be787d028ee53cf6119fdd36965a0adbc1dc8b6802210087ec7226d27a7927f9b75d940f717bae4663ef27a326c86ed9f0d65459861ebc01210373552cc06e1bf7c2e830def2" +
                "4b28a6ccb573d945fbe3c79fa42d63c9238be92effffffff0240aeeb02000000001976a9143152e44f6c676fe7a0df2608bff9ec5f15cc979c88ac40420f00000000001976a91430f62dea6f63c6ee67c6eb20e1289a24" +
                "8c73d47c88ac00000000010000000201a1e7290e00957c7261a4607549c3ed26d6e3ae752c731b359eec1b0ac949ae010000008c4930460221009ba83edbb555cd4fb753ff3618c87008abc4f4072e10c07c65074b0648" +
                "221e350221008decdb5ab043deeb63bab20d31700fcbcf17eb7b632fe1b47576e180769fc02a014104d03d5a2d260af7e66d8b19d56ca84fc4e131894eb3afce5640ea58cc3d78c2642eaad4d0b6cd987373c30e29fcc7" +
                "e40f38f6043abd8a17d402ca048c3fa205cdffffffff847cb470e05f66b3e0cb57e56647aa1905c2bb6fec1ca89808d61ba77e82783e160000008b4830450220715cc58fcce35e951b28c0ea088e7f0dd71e7a9cd4fc58" +
                "7c23704f21b9c1cdc60221008b1758eeff5323a42d19e930c966961b3ebd427105475c6c56b5b3a7fe891379014104acf44d51a1a5d15288ba039c72e7d6946649ade1b9c9647427909a59ddfb66c2610db8d5aaf3fff8" +
                "45b792af40ff808e0fbd3d986f2fde9dc24726e606e582b8ffffffff02005b2e1c000000001976a914af7608bac5fa813fea9f5c29467dd2cc2052ef3e88ac1aa9ea8d000000001976a914af9392981e3b972af49939e4" +
                "e169effae8c58b8d88ac000000000100000001766f900359db4761bb6632044c4ea4f305353001c54c67b2a48d08469138f51b000000008b483045022100a019283686fad8e1d51636f0166b9533d8f9fd3bfc56917ee5" +
                "2ffc48b6f0a14f02202a03ed18e26d30726c31a6f8dbd0d1ec9cb2a78e1f6990f615e8846caa02b0d50141047f525f430c4b48eccec671ceaf7c334d1498d7f00bf7067729f24b42f2b80252e2b4e7533313be710573dc" +
                "c7b126bfbdf360ff1184a9da120cca439a067364beffffffff0100ac23fc060000001976a91445a948203264e01efe25786fb616a7605098142e88ac0000000001000000040491e00fb0fcab6c7c7b0ba0ebb1ce37a53a" +
                "8d0d8ddde41bd32e31b6e54954cb000000006b4830450220454f3c96e902ec5579f366cddee3db7191e1dccea7139b7e5eeef2343066a587022100f992da80251ba8e8f7a73cf5aac923298d0e8dec15a78bc685854f06" +
                "589cb2650121035c094ccd4d958c2ec26318507bece5ef230eac82cba53bc940fba539519752b3ffffffff86f79225d18c2db5a649d92ec0656026bd9039abde806b62289ff7f2b5a07171000000006c493046022100ab" +
                "d1aaa62ed5775dfa1b5626cf5889a7347721241b57523dea7c6c4ba4041ee7022100ff11e59889437d47f2811f961dc9215039eac2d09658ffa3b3fac597987ca3700121026e338dfeaa60ff1ba7526194153562c19136" +
                "6782d6312d97584ced22df12478effffffff705be27970c05d91009860460ff392d39418e93d6b55345687a079ffe3fc88cb000000006b483045022067a223909d09426e436fb07b8bdc3f9199409ff27506ab808dafc5" +
                "68cfd484bf022100d17d205fb912d00d30583d998c801455e4150b1d0d86d33e2b281c2662ef1f5f0121034576b809104c27d076b5a2ba6d83d6a31a19822d6f826b5c05ff50461c037e11ffffffffea419b9627e212a0" +
                "f989acd57c2c2e861aa2508a40dace1f72c4cfe28aa30092000000006b483045022100d1352032ae884bfdac00f70f7fa373e75e77b7f8e4a04934e33ca48d37e2811802201246137fda1a32e1834e7b977b32467f65ba" +
                "ddee1168a41d9b3ac86220ce84d90121024312a258a591f248bd068aeb890c2161a378a1baf4413100d84d33cd6e54f821ffffffff020b850f00000000001976a9148eaa7ba0e36ed9898022569b369686ee74640ba688ac" +
                "60decb1d000000001976a914bf147bba7fb8ac59c764e1ef80d1919589f4f42888ac0000000001000000041c2abefa628f9db504f05e21d2116f9ed57e35b33c8cf405ce236991aa07d52d000000006a4730440220373785e" +
                "5200b46c7473b7b1d592ef7cc02bf8a4891417337a189206fb7b6ee5b0220215f5dbd6c4dc35fe1cdb0870b4fa4074a10bc936c39345802d3465a597c0eeb0121036c92328f17abdcc4dfc12060edbbbcd86759d678d7a72a" +
                "f4d91254312ee03f7effffffffd4ada44bff4c820ff6e67b3344c35a91f9f0954b7bc3cd6a9d96d56472c25a30010000006a473044022050372a8c4ce8affd6df876589cecce53fb918bb82f1d67fe266230f7784c6f260220" +
                "20ce497465efa7e21037f89ceb9abfd54c9751407a4ebeea9e83ef91698ec68c0121021872e85ba7e680b5de34afeee990fb77ca23657c604453ecfd161ba37435ed3fffffffffdc5ededab0bbe6a82834300e47f05fa16d9a" +
                "352e0ea506a8ecc78aedd15e223e000000006b48304502202fb165e129181a52e9cb324a6e2126252710505300c29be9e1fccaba86aa15b90221008d3d8fd4170e792e2455caf350a47bd81553e35328a8cee5c0fa74dc8fe8" +
                "0d510121030d8f52b90b95eda0fade968f9d2bf9c8258f457b2abe34e60c3d3dcf836cdfb3ffffffff590a1b602ae03edbe8df8451eff6e85ec3e24544db2f657f795830b083ffc56e000000006a473044022054e80dc50438b768" +
                "98d06e006730936b9df57c3e44a7e079c9923097eeb85a260220027d15be8a046f5e87d969b2990c3c89a0ee40c458ed57d7792b45238bc9bfc6012103771616c4e991ae745f921b1726ff0f1eb9bf57b39efabe1a6421796819511" +
                "931ffffffff025f2dc187000000001976a914ca1f6b5d1ae2dd346ec269d7e8afe5253da0482988ac17626b00000000001976a9149badd4e7488ad4977cc66898925757fd1083a7c588ac0000000001000000014ebd67e8a9bbd9a370" +
                "12607390c7028164767956a37b60a6d73ed11e2fd2d22a000000008a4730440220630c5eff" +
                "c72f091a4fa2212145a7289100a6910afd4e1b955c6148b0d1334d7702201010c5636d3a942e3d12cafad7988375cb8c7bef0285a885e728d0bdb82d527f014104d2b7f4ebc7f19bd57caf9b21a21daf112206aa7c770d8" +
                "91ec18ba3fc9060436043acb6d84bc7fba43878d8333d7418597a5e8f0bdbf9051a3db1210aa94e27a2ffffffff0280ac9a06000000001976a914591b396cda577aea97aa6a7feb7a0a8e670e8f9e88ac61d7ae1c000000001" +
                "976a91459e9988336e4c931bdd3a62f9711d1bc90aa22a688ac000000000100000004f20b39b2ef5e1dbe91aa7f1436e0c0f60616408cb787efdbe908be658fda7328030000006b48304502202bf942da83ee597486eef12a787484" +
                "a971762811297dd4dfd50ca6a496cc4541022100f624400cfc3aedafeb2d0d6cf982cb490fbb2960c80c77ab40ea141e9df3495c012103dfb167c623e0769e9c40fed09741ee75d557785aeda12d097ec69baad09e5179ffffffffd03f" +
                "b20e25737298a75c40553b0a9ae934005d897727d6eb52217da73ff740e6040000006b483045022067ebd6eb87880e86094f9a4d4295d2680a38cc1eabddc9aa6c6b63d11f10962102210083e87889d3cdff8a3b0cce0dd33c2cc130f9" +
                "51004ec2f983dddee6a316f82228012103dfb167c623e0769e9c40fed09741ee75d557785aeda12d097ec69baad09e5179ffffffffd0918db306d03696825c2f2b41344c2f591a1ef273557584a3ed4eb15d6fe82e030000006a47" +
                "3044022019e3009d6e709625361eaa39b1734f5d90d62cd85aeb15f8b12b9e8f128f613502203f9edfabe21bf13842daff00ffbfd8ab5b837578752cc215778a4389f0ee8bbf012103dfb167c623e0769e9c40fed09741ee75d55778" +
                "5aeda12d097ec69baad09e5179ffffffff9848aa9aff958514c738c5f4e0389f16fd3926d52281c3a1c1aee01e320b4107000000006b48304502202d66995b1273449d8bfc22fa6887ba369a7a99665f2e010d64838573593f535e0" +
                "22100e2e12ce94c3c3252bcf0532a72b9c93157aa3ad158129a6cb7f2a7a7be5a7549012102d1913ffbd35c75831b167a841f3ab63a86306060cf562e84ee20ef4fcbfab17effffffff017d92a707000000001976a91442c1036a8b" +
                "8ab25e903830d0e364418db81f725488ac000000000100000001e37e338b539530945cd24fdb366f6dfddca7dec5addab4c7a53c8bd8bd516a8c000000006c493046022100e0f961b5ad0280c334ad8fc03d1d36b868cec43ad8159bce" +
                "fbc04b389f20bc9a022100beaa5987b2ff25e5fb6e40f87b0b54673f68d2934e5574dfc19b346ad6c8a1dd01210309ecba988807f7fd361d729ce45ecb771a187e685043de2cc56bd14ed230ab5affffffff02200815b3000000001" +
                "976a91455a2c7325836f27ca21dfd5cfe4fee4d54177c4188ac00c63e05000000001976a9146b469df0072a328f49d5fb21d5dffaf40006ee2c88ac00000000010000000143ca266deb9ca0cf731fc67c36c637187bc2de5c3fb7e07169aa29190db3b099010000006b48304502210096939553c142b12cc9c24d7b7a74170d6bed83ad45aa665a867bdc45edd998ef02204be9a5aaed52f48f69157bc0b476e1e968182578b6e3905d12c8eef7b1ea0481012103a117ad13149a7e22eac49ee115dc4c97786859e0d31f374d0df237ea4a77e225ffffffff02a0853e23000000001976a91430d6017988faa02665392665532a0b1dd24ca1e088ac30c93147080000001976a914804c6bed23685ff1a7b4241c5e62cbaf4275833d88ac000000000100000004b9ff1db6c7b73afe395b4152de7cf03db11e4ffc7b9ff6abef73c7e36d378c1c010000006a4730440220189115343276905b0fa2f7dc1a28b1f81f39b16ce3c1b97e454539b2758a26a702201491c3686281817d82de7dc5bf6ec35cda8f9ac366d3b2b73e1185d27a47f584012102226d6a9e2d4332c7bb5b9d40b43ce9a3fa4a35cd6c558d0b5070f0e1acbfda0fffffffffedc790015accbae5115483f2f8181b014b36659657c7af43680d54f54048e7f2000000006b483045022100fcd0131cd7ef1d3da551a8e5059e19a34912e4ed2a09f27082d9116ab361a7fe022033108f11c8e1de92de23763aa22830a795243f1844c8795c5a174047282bce1c012103611ae5dcf02c1556a1b7774b1715b7223db9dee84d8e86cc2b4a5552edfdb08fffffffff6654da3734a9c61600ab28998007c736afbada4532aefd3f8c656755eb38dd20010000006b48304502203efc885f4ceac91829644e79b9b4e7569376739d9999cfe6f850cbbd45dd68d2022100c147bffb2220fff44bc1a7458e506319574e3b88873b5fb839040004ca095fff0121023025a914d12d7a04397ca5ec11e02a3f5e7924a6958b79e0c05137620cb604f5ffffffffcdd25f104283045326cd9e847f111fca1e9eac0dd052a035175f3c9f80efc264000000006c493046022100a88e5e941eb4ba621505c612d4e043b94c23b53c8cae5371367ca843e7a011b5022100b3f15b01ac5f4974d49f53f42acb5f732e90959962878342aa20b017d4360f510121021cdd06684c4939df6645be24693ab5cf5f6a78e5933502e479260a2ca649c848ffffffff02d0a0e128020000001976a914a792fd9e17e54da763a201e000266be6160a5ba488acc02f5501000000001976a9143a5d4ea500313a890ef1c577a9f6f6545cef6f5388ac00000000010000000b144a086bfb8fc8c60b3875ea1fe090d2c91e0c1b4491ee81067da23b87f70bfb190100008c4930460221008c174e3089c9750b4ef898a8b14d73aa9acbf2a4d4c957f53443fd83308c50aa022100d7f4bb010eb660fcc397df35da95ec1cea37ed5522ef76b54370199c2bcb1275014104fd18960529aa49a5db1efe50a0a2d3902533348848ef19bbed31e2db6a6e751bd85b8aaad230924800662788a6eaf7e7c71c060c8c3addaea46958172b8cf50dffffffffb83503654e45cac2d92c00adcc5ad41691aeaa9af28d423f79524552864bf0e41b0000008a47304402204ad9e9324b276008cbfe1bae52ea3c46a6cfc35f5710dbff0883008ba93214d90220392ad2e425da9f9d9746c3c2e399ab66eebee4a8b5ef3526f19f4972b9a840c1014104eef3e78a5b71c2d4f645fc24ae6b23b39c3f537545f66c225ccc41dfdce9572643a0527b8c771f8daebb3cc16717d77160ca67c7fec9ee78cc4f1ab6b83e9b38ffffffff0d749b9ecc9d1cae8afaf40c70f3d4046583a48e878ff94e0b3eb3d412fe917b0d0000008a47304402205ff2212f4806921722812ba12be1e082efe3b5c96cbc6e0a90eadee5424daaae022050688cb925a6e19def73cd645c9485c21ccf49a070a046e17952e675f8572aca0141049b823375995512cb351de3a24ac87b3971dc4f5f920b780d2f60b693887122f539b4a84f19c24328745c4ea92812b367a43953e41bf1f172e59eb0c0a0070476ffffffff733627ca9c2cb764ffb6719bf15d82ffb9cbb7e7c074435553537fad4208dece000000008b483045022004ffd94ee7b4eee7d7640f77afeebb038dd9a7b9f05853b5221a19fedd459c2802210084777c9bb0ea66c09f75b2f209be8ea3505a30687783d673f481cfb49a0a6dc3014104325a3029d6bf7d39e741b245f47352f46acb2f720fa61b4a83829a08edf22326fa5eb0e4108b5f40eff085c1b013cbf6322edd8226ad44b8aad24cf88bfc85daffffffff4f6a72feaffe7b11bba4a7010183478946621e41d44f2846e1aaba03c7a9133b080000008a47304402202d80e2b7d43cb22eeee262f3902567ef3da3935a61a9900f6b4ec7e775f9381102200052f6bb152389853280cfcfc79b8e0125a6774ed275a26cad8e239d20aa906e014104351267099182f409343263f30b77ab03b5dca3762f2315403929d1bc7aa9ff45b3541f4b331f7da3284143c196d1c29667ca5c6c0ba367c57b4eec8bfd496fe9ffffffffb83503654e45cac2d92c00adcc5ad41691aeaa9af28d423f79524552864bf0e43f0000008b48304502204739e167b68d7f0c2679fa7a6db24a0b5775df7f3e2256250b97b963c83ee156022100fbb155d8298a492aac969e365b1fd9aa51d60653fb43fd39c1a77221b73136d0014104eee1af9499c627296dd4f323f1eb8767b02969c4ed9ace6fb775a5e5d8879f0ee58957d799122a3dbb98f61418d8e3de13456b225f4de2e2d70f0cdedb76ce5effffffffb83503654e45cac2d92c00adcc5ad41691aeaa9af28d423f79524552864bf0e4160000008b483045022100d2a130215ac15a239db59bf9da7e438752e3562edc91ae1382f36500de4d4b7902205deffc9f52bb98cccef75cb4cdf6518c367544f787198f89780791dc4ff2a1da0141042ea81892130333584c044a9461abb1de35483f9a73103d207d0729b58344a34cef4bda4b5ceb1cd5893d5b11b87d0cd6727e1d843c8e58b3c06e7867e6468696ffffffff144a086bfb8fc8c60b3875ea1fe090d2c91e0c1b4491ee81067da23b87f70bfb230000008b4830450220120ce20136336b7fd603ec317e1cff5739b4a29849d76b9284c1f1d1b888be5f022100f5bc3d7b20227e3e3d644bf643671dc5de7a687ebd196c3ef0888502e513c8c701410404b809e4e285069f7a25107bc54fb349de6434a18ca88d344aaa22138ffb169b38f840560ce75c64e72513ab31c8f954fe0f86d1706cba54683a077875c8d8e9ffffffffb83503654e45cac2d92c00adcc5ad41691aeaa9af28d423f79524552864bf0e4360000008c493046022100a51528b80f32a0a6ba71820b92a59e2cd1ea3fd45e0a792bc82e0baa99e65dd8022100fc8c8d8c576f88e6ff69b6edddea2efed595acdd71cfff8ce2cca9a13cc6dacc0141048193e580d768d55e32184e0a91632df10dd186f6694f2c3552a938599457ad1562b3f2f637151cc83a81034c014a6233543f7745298091642de1b5adc3fa3fe9ffffffff7d7fadf63a2b1ec9c7338219b51e49c19ce10f8c81181f69346fa92d9f0f56d8000000008a473044022052b445f705318979dcf6fe95ef1d5b576fb5d176fc963fe3658c952f834baac202202856755f204532535c4f16183eb25eb05623202db12bb4b4f98bf3b94634a91c014104b4650956fea316bed31e7cf66fec11768f8c14c1a1d904be54b9f80672c039e836a2aab554f428d84bdaa839e7c366bc09fc03ec0adda5d71272240359c79c96ffffffffa26d92bcabaa9ecb3bffb6c66f3cad7a7d7ec0f08560876dea7da43690aede9d010000008b483045022100a390353bc9972073feabb70b9bb5911e827b4f5f08d99457fb1896f9ee012ffe02206ea83c7f40038d55302f6e49e2d04ebfcc32762a977f39342fc9f0723b8161840141041fd9face2fe59141a4592837848559f2525d09d017f1d2d809ed725787e183984a3cafd8b24549cd63691d204b020b60cad943f429fe187420491e2f9b701a53ffffffff01781c1ba7030000001976a914507e3b3d4772036d6c028aafe4a0fdbb4fffee5a88ac000000000100000001bb28e4088a2194e4ecbc6397a881ac679d694e4c21ae69d5ddb530617a468035000000006c4930460221008f72bb3bc9d041018a955e66eb633c11ad7dc59463f90baaae7129a46dabc5e10221008aadeeb6ca8dcc7b42769c1605305a31fa9d99962f5f7d09bef9aa5606ec6d6e0121029dfce75cfc34ec6743ca16cf0d4f0ce60cc38e850dd543511d141aadf1b6fff4ffffffff66e06f0f00000000001976a914364a5d033c35b360c7fde9f2b24f63cef8b56f8888acc31c1700000000001976a91441429180273062370225f792de6c391f578d019c88acb8764c00000000001976a914e643967bf15455c3b9f1ec278c7d17e916a8753b88ac72928d01000000001976a914ce4f7bb532f1c5bf15f566569d53c9ee984b343488ac2f849a00000000001976a914c3fe8baa0f8191f9f221983aedb528bb0356f82488acbdc19d00000000001976a9148990301efd6dfd5a269c5a2007d8a17e07b72af588ac69930f00000000001976a914f16e7abe0c73dea87480c0cf94f6cf564627b50888acfb8f0f00000000001976a914fc6d01ca8b48dda4d38d60956221e0ddae319e7f88ac34f10f00000000001976a91455d6ebea9587cd750c8499bc33d392ae70a0b68188acacf94c00000000001976a9143ca9e94b3d1585d94c8e45e08a1158fc5319fd1c88acb4a20f00000000001976a9142939e68c1f84641334c83dae8f9625f990fe25a688acd0681e01000000001976a91496a03fdea518e0a039d43d7f3d4f78909608466d88ac76491200000000001976a914bd1c97fe3dc73ab6a8b267d7e7f96d1aba3a50c688acdb780f00000000001976a914c138c40b5652bb2f4f337ce6062b7ad6a5f8471188ac0fe91200000000001976a9142401bed67233ff49715bc98ce726a0f316695bd488ac72bc4420000000001976a91451b275fd6981c7b4eb9c77f5086e441006e6536f88accb760f00000000001976a91498de8fec46b50f8d4ce7eaeed1521fe96ece8d5188acb3570f00000000001976a914821048e23d69c2f9271eaf61ebbd92b6f797067988ac8f784e00000000001976a914a06413a9bd23963ce3b9fa8bc082d3af4107f52588acb7ae4c00000000001976a91490e5c956bc25445203ae3b516931bb633ea2ef7888ac01600f00000000001976a914543ee1392732a94ffcbe2620ffacbecb13a1dbb188ace8257302000000001976a914e4bd561c7915ac2682914529f4f05c3abe11af1988acc7636c00000000001976a91423e32de888e1b9b36fd96a5165646cd6f26ccd3f88ace9e45d00000000001976a9142e011e59894fc7574177b089031cfcca76353f5d88ac272f1600000000001976a914ddbadf032a636749963255d24e845488dd5af85688acceb11e00000000001976a9147e86657609351569c9df493600ff2dbbc91e07b188ac87041000000000001976a914d4d7ab957109639e0ed7834b3adb36c196ed266f88ac30d14d00000000001976a9145165caa6068ef9dc57ed44d27864990676bb9c9c88ac15159900000000001976a9145d84a7443fba85d03851f8d7ad4fd9a9038af8b788ac7eaa9800000000001976a9148dfd67b1d7ac048df49fec4df993ffa38b8d04c588acc0afa800000000001976a9140d57387e73965de41fe6907bb8627acc9cf230a188ac404d0f00000000001976a914e77a02471f71918af8176807ac400c00e7af3c5e88acf25a1400000000001976a914eef0c06809c50ce3d774cbabbda63cf61dbfb82588ac71477403000000001976a9143594bc81550a0a0f53d7e2ae1599c8c650327ff788aca671d301000000001976a91438dab28f3dd62d65bd03cd9c071da0e7b9499fd988ac2a4c1f01000000001976a9146c39e5da131031fdb4fd1dcbf32e25762a98940c88ac26eb4c00000000001976a9144409ffae7e30e66d835cd0000c731432f64b333288ac9ec48c01000000001976a914ddfd3522d53fc769059e2286f76d2cdf1eabdffb88ac66db9800000000001976a914b3b69e11b3c2fb8213fe5f27d74b7a33d491b2c688acbf52ff02000000001976a9144319714b89d2f6b6056e349f82299bc17a2e52f388ac4bd71000000000001976a914d6d2663465e8b302750bfa1f4ac9965cd762dbe188acc6460f00000000001976a91412a123b38aab75c096537a996f3ec0d864585cb888acfba9f200000000001976a914fe603f1855a6043e293c253f66e888c63a413e4a88aca4491100000000001976a914e98b64aed7a6cdd4bfee68725c04119defeeacb588acacef3c00000000001976a914d0ed21d83a5c221251d1e60737d80a2bb43eacec88ac7a754c00000000001976a9146256abc33847712c35bb6a6eddd0ad90028beeeb88accca4fd05000000001976a91442f6e6aa6d3b31eae36fab223c5925a7a5ae1fbb88aca3771000000000001976a914ac05ddece7a8e9acba1fd7fae0b76b26a7207e5588ac19784d00000000001976a914f9ffbf135258296267b4d800628988981109f91f88ac86549106000000001976a9145f383fae98b5683b669580606e933d04ce09e02d88ac07631200000000001976a914d5a48128be5f30f79c65dc1d687eff17340bc15688ac14952102000000001976a9147ad9a30592b34f4c35c690bbaac0fefa6bd9479388acb3680f00000000001976a914193dc516ee70d97f5e4b436724e6a2b77076d33988ac25f47c00000000001976a91483a50be3139b7bdb5ff2941b76fb463ebbd39ca888ac3f064d00000000001976a914ab9d4876cfbcb72816ca1d1d3f7ff8567e8951d288ac54a32600000000001976a914907c1996f4c775ee9e09a648e427cf2382d17e9d88acb5361000000000001976a914edc0421539b7936e45a3b133e69f0b888919c6ec88ace9ca5300000000001976a91413d0cb84c7e9d79da1b3602b087d52e5d6939d4888ac814a0f00000000001976a91455e79db66d313da2d3fb00a909e7ebd9e204e49088aca5430f00000000001976a9147bcb380ca0942a58a9067d0238df5394bc1e949d88ac9b32a600000000001976a91433464de18c04ecb460edfeb75326523d884e202b88aca89d7d01000000001976a914673db6af1e0dfbc0bb32d05aa6e6b3feb4a1080a88acb2a94c00000000001976a914bd2deaa89e01597daf021cb3ceef5e2cd3c29e6588ac4daa2006000000001976a914f5fe2040862b549cf4b82b8b3807db226ee28bad88accaa34c00000000001976a9148f41434f87da2ee08de5411a1feb65049520b33488ac3e149d06000000001976a91477c0d9d7a1851d53958fadf6755d83042b22c81f88ac08165505000000001976a914432d133b83dd9cbc9a904cb3482ad063222c869c88ac60b24e00000000001976a91445b31db096749def59c253b35ad0951741d5148d88ac991ce201000000001976a914b6d92b88fb0f35e21bbf3b5dc6ab2c73efe5031088acbaada700000000001976a914887ebceec763ec3336d69f6c46df4638a989efd088ac3e8e2307000000001976a9149ccbb003e427134ca29fe6c88e79caa5e372641e88acc0011000000000001976a9140a07602223c1a38542b0c821e1b8fefb4fed90ee88acadc81000000000001976a9142497792e008ea9321aea57d6e389f8773f77b28a88ac0f062700000000001976a914bf78ff62efe4359d251506d69efa2ecc5af0f96488acf660ef00000000001976a914b456e7ee45ce257dad303b9640f25ac5f1db592388acb449fb05000000001976a9144436b27fd96c9b3db71b6f3dfbd3c5d77c90a5a188acca332300000000001976a9142300717fe8cdb380c506fb09d0e5766ef46db76388acc00d1500000000001976a91450cbc1fa5c400baca761506659e7cdc44cbe731b88ac2aa01e00000000001976a91413dba81e844f4608bd5e48e50e7a1131a6c359c488ac9594430f000000001976a91452b35f109d65555e0f1cc316841b94913b6b111488ac00ec4c00000000001976a914d8614f0f059b5de5c856a6218659edf2d58704d888aca7039600000000001976a9146e03d03ffd304fa3d2c569e1c608c344612dc33c88ac077d0f00000000001976a914591f7f18638018d9b4a448bc39b2df4ad910ae3688ac66868101000000001976a914d55488d1664e82699de4359ff8072e21f77a8da188acdf09a100000000001976a91462cc3ea52fc84c53027aef6c609471b95787663c88ac7c631203000000001976a914c8cc3a9a2b52c60285baa432d8a4b038f569257888acae552a00000000001976a914262b3b41f168086e2c0398db9da54d3c248bf57c88acf6fa1100000000001976a9140c95a91dba0366866a3674edd549ed6e92acf64e88ac97578b00000000001976a914538a67323ad7846cc51137c6556a2ee93b2d011c88acfc681100000000001976a914ef7e8e1699059cf8ed96288c0e7b67aeeba6ee0a88ac2ee01b00000000001976a914683699685dfc77f24c26693d08b6fa05dfde2a1388ac0b329b00000000001976a914b23fcad941ba7a58436aeff0c07373351e22f87788ac30e01a06000000001976a91439bdb35e9627fea8801e607899eb2a5e2150e9e088aca8f49b06000000001976a9145e76ec86243f71221d2189c6b12ffa35db8db93088ac50a80f00000000001976a914078d950155d6bf557f0a76060334b28e517cc84088ac8c3c9a00000000001976a91453acbf24e27db4fba152ce8af2fc1477414a497988ac5c500f00000000001976a914b7a8c8a68c34ea3e67bb18468e8a81f39314f4b388ac1e0fa100000000001976a914a58fe421b268924738c5bc9c139a0c2bde398b4088ac3a650f00000000001976a9149cdce873c1779c9f4673c25a4f531e8377c36c8d88ac3ce29900000000001976a9142879424bd4b7fb41c477042e83e4bfbf268e790f88acaf5b2e00000000001976a9144c945c7940e2af295fc66d0d70f2df0a444e319088acd1590f00000000001976a9142891c0d25dc69a608af68c292dc634caec34816688ac0000000001000000013ca38560fe925aabfb51ba7f2ad63a883f5c2903fdf7df09661fe185304d86b0000000004847304402202b09ee726bba8bcf577b56117ad9d80c5f97e1f487f04352118f298a4901c3890220442366d103c1ba3222ec1ed8d410529cf3641c89a26b2c914ccdc4317a790af701ffffffff02ede5c56c00000000434104a39b9e4fbd213ef24bb9be69de4a118dd0644082e47c01fd9159d38637b83fbcdc115a5d6e970586a012d1cfe3e3a8b1a3d04e763bdc5a071c0e827c0bd834a5ac40420f00000000001976a914ec868b573eff869f9d7520bdcc96bd97bc126e6288ac000000000100000001dcfa896cc0c4d884b27072261d3fcc1e0eb61542d488c38fc55d459a05a068f7000000006b483045022100c0a177980448a80a4f4265a1c2887c150a7785a4ae5f7f2e2ae101700d286f720220523fe4aad11582d34334587e799ba51699ff89748380b8f7248850f759d6aff50121030b619529c1fd1e7f1ebaf720f6a4c5665db6733af781da4f7bcf43538ced986affffffff0100e1f505000000001976a914b049166fc91fb9fedb6e0dc9e3110a11619e3a5388ac0000000001000000021b0399cdd5f25d966626c823bc22858e99df57bf67d51e6e6f82bf2f8f1c43be010000006b483045022067905b449afd041c1d5ff39431ee3b341dbd5901307e45781c1988a0136ea2fd022100e16b0c9ef73e618ae9ac6f91ddedfd9baa8a283737281522119d118aa481e837012102ec0881ad8c06b83ecfa4cf4646dbd0d6435bbbee2397d20b03fafe31de6be7f0ffffffff8df9f79b3d1413dfae59db26001183b6016d5c8752e1af2cd3ebab5db47e61a5000000006a473044022037e673d0904863610e554525c650e9b9b4b07357a103013ee50ffed7aa0659ae02204e0075498f311d523c84b1cd8a04ea8d697bd5b73a25d891d4dfe74d17a72cf30121039516627347446666c6c49d3128f6d77a7f11b839edb008ef709eb9aa6a3df6dfffffffff0190ba890d000000001976a91420c9ebb50e7789ca71e284bcb9c5d3c155c0e1d688ac00000000010000002b8ec4b01b418e16625e82f53b47df4204bcadb749d0e7536247f40ebc44fb87111f0000008b483045022100f1159bd06b4afc6dc5dc4cd89422fb72bb2054e9712ac0bb05788315eb921f28022030c47c920cce324457e13bfc6f408e14e9ee4bf4b1a06b655c13076f93f305d40141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff9107d2c52dbd51df7d35acb9f35c28dc47222c903a2fc68c7bf08ba9e9e966942c0000008b48304502207a921b8b9626724fd8dbb336dd967c9c79583ae8efa86808c5fb1dcb2f7dba2d022100e7458f4d20cd166ed5b88ab98b62d9eb7306455f1745f66b470641ecb7e336530141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffe62ced3f2a6c66f08c48c53835265594968d4795a30e5dabd1aaca40170a4ef5200000008b4830450221008d28821a4e9ee149b1aa6159d81315e951f7626f3a5c1e0467834d198a39033b022047f418e280618060784ee9c45cc02d5a55b3b78992b3f74171a64a354b281a9e0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffe649e027e25b1784bf0b9f30e84f38935475853389e49ae64a1538296ceb4e052a0000008b483045022100ffe2caf8142066b5e1deff74bd0b713eec2510ad5b086a56194b273b730beb6802204feaa8bf62c434d0ada352071e74b49595a4684371d40db335079b5d389433850141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413fffffffff3fb2960260dff752abdfb7d13cb5fdb0494b00797fffb1ed9a641cc4a74633c310000008c493046022100a0597626ac5f79c1b127832ea6708b05b482f16869abf9b7670ffba8d2d12191022100b6a5135c5627fbf366a32ae6d2d1ef269a91eaf0c5d8b2a967603d69901023db0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413fffffffffae690fc8c5263039eaa398881871fe493abf38f0fcaff046dadc15f8499defb300000008b4830450220195e8745a09f0f5bdf87c08bc5376b346522723d0ff366f71c54ec555b192394022100eedfa8c16f061d5eabfa604cb772a60e2593782e9f4223a8af98b0243afb183e0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff051d7a8225d3440056944b519c63075adcca21c678b8af163eb10f6984a337dd270000008b4830450220697d1d1bb33ec063f03cef8b30cd1933eb3ffb18127d0f087159453f47716f7e022100fd51840f418ec7936fb04cd74204c2f55f294c6dd147a03cb666d1d92ad47a7b0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff166a6cfd823389f95905214bd8f5f30d44dc276d40afadc2a91e1a39e72122e8260000008c4930460221008951b92a4f9e32ba54de6d91f593540f7d432fa7d96e366eeeb687a78bdedd470221009afe32571d177e78670220975f42eacacfadc7a28523ad0ab51dc7eeb252142e0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff02955aa6678afe6866ce3123d4071411e6b58e8b9462f95f5c44cb78779e8353270000008b483045022001d703296a3ad7a222a93aee3c7ac6d6bac8a641fbcbe0d7bfbaa7f3e6e0bd6a022100cbfb7229c0ae8afad929f5f3f9bf67e4be597c1c8b226ae11ab049eee5b3b4ca0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff0e77c3687d1657e3a22dc71e6b95e3e64b6db80ca4b4e8b14a27c3aa8ba1518b290000008a4730440220287f6fb874320b2ab01797fbcaf84e2f9e6484a82a05570d8a1749f758db643602202d0b836d11893a86589c7334402d4d513647aaf63dca779e6bda86640290b46a0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff0ca50bad1a3de596166d902f7e7c36671171cd5052d8ef07a679309b7ffd3aba1d0000008c493046022100cc575871255feb5465c214b10359ed29a39a233957b0cb1b35025f7feb719740022100dc2e05417a499142b892a89a5607f90afc63375fbca6db86d69e4f6565d35c9c0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff060fce79462aac63edf9c58d05d2890a8e1e62f91b21ea2c69c8bef8a7a4f697140000008b48304502202663f4abf2333ecec985a6c00b45915b88a483525a5c5d0fc46ca59dfd2f2050022100886296c94739e4d8c27bea7e6c18c528e8cceff604da76e6aaff40ecc7f0ca540141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff4c1cc87a7c0ea8eed3b9d1d10a204e311b4bb65c1f760251ae5cf7d5826721261a0000008b483045022035801fc5b5c1a1cbefffd1f098846cf695676a8df3eba536a91323c07a77b368022100889385fdd781171b45dc009cb6f45cae320e16e29e189c51861185a9da1c69f80141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff7267619c48789002dbca3d6ef8692f62f4f1a74515da78a3e1821ba5613e12bb200000008c493046022100f9600de58bf979089f92c8b4a7c11d65defd82ab6d31177b2434f9ee92c1872e022100e5a244625444c8512549176cd889f913feafac33a09aa12d2790e6a7d79e82c80141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff78c332e062ccfe14990ace8cabeae19c95d40b1076a9701c02ead45f2a4a3a14000000008b483045022061e4efc6c2e0d6d810ab62af7880198de9403ca000e8e0b486a54d25cd03657f022100a5acc665d1177aeb723cedb1df952d58e7db84fe1a4c24f0d66d0ae78bbf89df0141047555a53848c05cc8c39d5f9c304ab24e09ef6b55d9ae61b6d80f3de4c347cb115a7798b04aa60ed192bf842472d8bc221777275d6b7715baf6608b278b137f5dffffffff7ff2f0cfa86152e8c177bc8e6238ff381a154a8febba6782a9577643a510c6f31e0000008b48304502210098dd6af275bd94ad1e2cab9d7a68b82176177ed838713f1fbd57ca812872fef8022036027fcef49b8b6169cafcbb7f4a0498a03310ac81c5aa843b4631c8e14a95e50141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff6e406691f7251ebe68c27bc796407a1df7809268c2e6d655c5d80034af5be70d300000008b483045022100cb430ebab8be0c4ab343819c24da6ad65a65a728cebbcdfc23fda9fbbc21668b02206008b647de68e4ac25cbbbd06185ba41c8cd7e11c04f44c4970351305ed58be10141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff61467da604007d6799c7672c104f6f2f3a7701bb83a7721d6fcd96defab975b6280000008b48304502204de113b4465f4018978c20b1f444c6638d437f62b5caddff863e035082706f6a022100bcd5361348f8b8b13202272bbf5cec11311f394886102d0b1fd4462625df52880141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff683ea4a67dbbc73bb81acbb6bec06f64804df81ea5c8669f135fa160eaa7a29a5f0000008b483045022100ce48264b5a5c707d83082ebff8968728955445fff9c8d490cfea8d715611fe4702207a0161107187a4c3663674d38cdc584e0ea4f33412a459ac026e7b3ed55738da0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff1e42c569b37fe47e1c97a7eb64b67c9de382f52d37c534fc40fb2485bdf412032a0000008b483045022100c9a86b582945a29c2d9287a4e6d1c2aaffe059ab64e044b70d0937dbcb42e0f00220462074a43000a353a5530933fd1bbfb2ec2bd8adf967516ed60a32275e961af90141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff348e36dcadc83c4b6b8680e10be8f5600f8b17f6a10ae4422742ac17dd190627290000008b4830450221009aee8721cafc3af9069e207956cf1039f6d24de7d3845004d1e780b11a0c486702201c13129bbd34d95544cd77205b810c4d474f0827e80c4efc05e81783613ae45c0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff35306edcb85796ff61727cefeb803190be9f7177230219bf896e30d14387f923260000008b483045022100e3aa865c439c68c2b3d02f509b30f562175042b922fb8f367836be23bbf556ca022040a200f313946ad864ead5fdf7c22f8cce096cb90d1afcfb0ca0b58f46459e430141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff34410ce9b37c3b01533da80056dc614305c26ee6de9f6315ca45cbdc70ad1c851e0000008b483045022100cace38c1d4c3b72ea1e98c83e758592b9957a475f7b23c696be4a23a4ea86b4802204a21283acf7eaf90251821b1524f161d52af3eeaeeb83d00112481b1e5c5b1b30141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff2b30295dc3475bc4a0fefba459cf74bf3b6d1c07e8460e07f9ee68571925482a240000008c493046022100baea2d733c41a9edd085631a7bba07451ebcf23523a01fe3f6b4af18790fc001022100fcf087df5106cb39368e732c7a8ec292a488092a8ca50af4295bbac4c5341f6e0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff3984cafcdf018f9e809e70aec51077776176d66f45f50c083f6702c10c6ba81f240000008c493046022100e3be315d3f4a335fbaf781704b517db58556effe2b2dfff8a2c5e343e6d883f40221008604a3d3d31ba296fcbb804451cadad557a3f47ed47d28a96794330e97688a710141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff311e6e975b20e7d81cf4bc03c920aae471e6d76b37dba6fc80c7c9a9e4c825cc1d0000008c493046022100c9483278092ad31a04ee1c55f777d493cdb8d89739720b1672da5002eb014da4022100aaea44deb8781c4693bfae6d89bd71234e285dd022218028e787528f01d986520141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff2ff81a51b5b792953fd07e8bc66e00b6f60349e56d9f10255657cdc33eb4d488230000008b483045022100bb5fea29a740e132c78c85c85c568c0741ac10fae463f65d633ef2815b9606a302203a356e6f81642b146d01254919fa2be2b12b07cd485d0341b66ce0ab0670be7d0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff40d0336a09821a0e7a3121435389ed56adb90bc7940fd49fc927178be50e2228000000008a4730440220239532d470dba1bf1969e3c41910720c052426f2091ba85295ca95bc24dfd75e02201301a683be624755e3c05045f97bbfbd67722e81b650103ee653aaef06c51c4e0141043b1c8fed78882e8c3f02aa94399441035f15eb48f17e76bd82d64f844baba4d5a26960b8f24ac55a8d19c670f9da93b22f118e5d43bd6d81cb15e3badf9579c2ffffffff2fe114a0f35d39810680ddcfa74d77f1b70ec56444946a35eabafbe700f28c1a1d0000008b483045022100ec9f343de087ad958c1271007c77757f07e705d0ca0c4b73f1d912a56a41d7ae02206de4610f56f789061b6b4d92068a7370084ae03f6b01cd153c6420884e3f42620141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff4485e8ff683a5954a271c82dc1bccd5079b4e261b033ef2d0f5f281d08b367272e0000008b4830450220112ef6631b8e7779acaaaae7b63baa7d4acd3677f3e2b929d7398d50a88738d9022100ea98a72e8142580a4425b7300070c10d2bffce7978cd62f2083779f4cbcd7ff40141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff9b8f25d8cf02dce0e0190f15b453b4ad96f4bc1b686b4c4f230236bf8987beac290000008a473044022060fa43b691f05f65f2eef9793af3250e1d432467e912fa2913e45744c636184202200a5108e89bc4a8bbc54f6a506b3f75918a5aab0bd426429345bb45d14ac0c7870141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffa081580becbef43d961aee1f08bfe35a05da13675bf4730cc34c838f59c6c5fe240000008b483045022100b65d7a18e1cb8821e70d3d8b08770c7f626a0b20f607a4757faa7f4085f86803022061955e7c449cac4558464a118ef2d197551bfa0490e32d2a0f90d0a02c6673140141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffa5099f8e1192e0517575937bb65b9020ffe3a0068ee84f7af20ee58cb2067b762a0000008b483045022046cf0322369844f8d76fae340c20676318e8c36d07cbcb411a0c66c36850665902210099859cd317a50048653a5feb5d53c0c0cd96abb4ceda6236c7e159d166198b270141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffa78f5c4535f4aed4adce054f22c5b05744a2cdae2efd0b9d476640eacae23c8c220000008c493046022100afa9ccf3be911ece3b593a29064ec4581d1fcf7898d82dfbe5b871eb78ec113c022100d0c1ef474bbd33c2a0ad1e02fef91b3df0b3652014f32ebe0c92d79d177674df0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffa9e6acbbd6b4b9b88d06edf16bd4c31e0b934d6580130aa3f0ce007e19cdb670010000008c493046022100de8153764192a1156d34a40e8fca24540962b56844fdae886bf0cd37d86ba367022100ad917bf31de803b4765ef60f85603c51740e68952df92f0b6d078a9ca159e664014104e3d619f5c69128522f8034c4223071896583f5bb0e78d9dfbd7875f716c245988f3b6bc25765a92fdcfbfcba85dca52dbf3946775874cbbbacd64818dd0c5408ffffffffaa77bce4f4d530d8f25844d3cdd27220dd67d8c4fe1e872632919d55a8965bff260000008b483045022062736d337b0fd024871d9abad1ed26dd8ff4fcdfe3cef57db60ea50359d44f6f022100f575dddc9d6edeb76be66b8a74cd7e6b79ba76b247ad51c5252be4f7e55881e40141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffac7e7e511b481f0ea45481cae74fae734d6f183cd68000260124e6ee9b6f0f4c220000008c493046022100b941dbe825a22c881e85f955d5f7a0f483a29c1f1b4822009baa151a43367b870221009d13ff015a229495926806a786b9f3189c9581a6ff66f8b5307282a4f5227df10141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffaff335111a3cb4be8482c9fca6e4b515ac3d6b76bdb227d4807132f6c5f1efc8230000008b483045022003fc9fd4920086543eca58fc3b39c6326c2266b66e57ba528800f1fa0e1a8a89022100c5dc3d2298334d92352d5c8a5297669ab869935caeba2d95c57efe5418d2a0fd0141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffb0183cf368fbd0dd17e5c54f3d3eb927e9f80740b187afdcad8203b00ef7bac5290000008b4830450220172be11a5b2dd1162088bab15daeffb9cf9de2bb30fed5f14db09d9c4e072d37022100e952a28416a2a3b1519118571bef957546086fe003d46db03130c2d0b9896e350141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffb5b5c7890b1b3fd735f2045bffd7a8ced9a7cbadd692d411fac05c4fb28a6486250000008a47304402205f5added2705875a982ae72797e97a605c57fd959ab070ef1a4c051b01ccb91c0220162c764781ae38f9ddfa500eb43c675bfcfa4eb3572210e6069b79d4fa8308700141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffbd0e202672f620f8ba39a10148946d81522c17246474c3cebb7b66aa7ea43a9c270000008b483045022100d1a5250e619d39678680745d2760b742f095d6cf42119eff09ba59b4a90f8b90022024bc4826f3359a042cf04f10b621f6d8161bc8faf776fa7c3b6d3ec89bce52d70141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffc695a5199e40947b6a0aa8aaf59099ddc1a25cb052d1185ebb465898cd3b28c4260000008a47304402206272e28732242ba6c643687ae3604ce3ac96b49c9d440000d4884c5ec6f3b2ea02203b1095e7ec5767797ad29b08288c30ed456c300652efaeb6866b050412fc9a680141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffffd7a933a2998c97a73428a48779f2114ad9649a6132bccbde357b2328ac6cec86040000008a4730440220324323bdc959f8bdf65021a3b6c449a2662f00d47bb72dafb442957e2eaef93702205b98f0a099ba2c89e07e207fd7b517694fef0cd3cd0dcf05031a9d845c0ccab80141048636d058e1822c762429563c7c5944f712bb9b9fb5a7311392ac2c02bf3a46567d47df56aa2c87026b186b80be00cf525b366f5025d39ce7b5f79dc7f4f2d413ffffffff0281831000000000001976a914ea719bbed904114f3a291af638126a9d4076ac9888ac804a5d05000000001976a914bfbb01296cd7175606181053362792b11491096f88ac000000000100000001887fb9da198400ef4da74ab9d831a2f15257c2849295232c443c18167d2e8ef0010000006a473044022071105984e568c78efce6c110d9ceb09f967a8e967fd61d19d26288eccc8365a9022075e32a470c2b6c00e258ce373103b0d8573b36faa11da0e92639fc670ec637ea012102da43192938f041c3ea2d7b06bc6f94f61198e412760bca674adea25fdf41dc73ffffffff0240420f00000000001976a914ef7015080b7a0f840585401b435b8177d34f0aad88acaaa18448000000001976a914390096d69e7e320a69719b3e0ca6cdb5fafe034588ac000000000100000001e99305617a883fb739faf5204b1296def363cb5c2569c4f2c41758a5df39984e000000004847304402202ee005329d86ff95105bcabb25e8b34d1ba9893ef444cfce9a9f39d9e5f48ec70220076d25aa0b4efa3d2d45e0769714823c19c9a19b1a482e8aaf7c5c2b483d8ba201ffffffff023b70739200000000434104a39b9e4fbd213ef24bb9be69de4a118dd0644082e47c01fd9159d38637b83fbcdc115a5d6e970586a012d1cfe3e3a8b1a3d04e763bdc5a071c0e827c0bd834a5ac80969800000000001976a914b50be4373f4125494c126ffb28ea0741a413e77888ac0000000001000000015050779156f520553205bd244d9e760ab4150f25ad20d8baa1951510ccaed8ab000000006c493046022100a12ae323ee9a0bdededaa8e06e12eebf89b1de6f60de732f2d8e9d1f072cc7d9022100b6895db1776259be2eaece2b8e5f2bce600b2aa98ce5f3becb9982ae83bf1a5a012103ca4b3b8c33c973747e0772d8e9986de3ecad8f1b9c1eaf016965a77d8d7e4b3effffffff02808d5b00000000001976a91406f1b66fd59a34755c37a8f701f43e937cdbeb1388acb0135604000000001976a91489bf6ffcc372c419d9b669db7a10b0db99dc532388ac000000000100000002fa614167053bc4240a825fb71b07c48ca3e2eb84287904205d4145ecdebdf341030000008b483045022100b79b3987b42d713edcc61d7b6e71d7fda57a1250c48b4771b14961224bcca8770220416af78a286861ab334cee4e7305d54087b902b5e4ee20cc6867d5a55e5e040b01410496700ea9647517d5e208c7693ad717370074b550b78fea5bed670f2028fe0ae418e57abda866b59a3b2ec7b74b7b248c698dab770aff2ad9f2a0dd31ed00e735ffffffffbad54ccaff7ba2c6c4eb48b8706fb1f0ecb052941a1344aca10f9c60122c753d010000008c493046022100d989550e73a64bd3cbc92a4e9df913941a28d306db4a16985027080767ecbbb6022100ca0aede45b5ba6598c7e209c3ecab291909f91ce993cc36250d576096607a922014104df62c2fb571ba442d09ebf526dc88800c64e73fb71f75f44e1f798cf5eb11e39b36e85f1f1f589d73a49fc500e23d69783fddcb2407143e1f96296d1e018fe29ffffffff02188a8504000000001976a914a9d74803360158798ca18a78f52631d1d07c9c0e88ac406a8b06000000001976a914591b396cda577aea97aa6a7feb7a0a8e670e8f9e88ac000000000100000001bb82001e1392216cc6c4a3cf1a27194b428a70d16caa02129315db5dfa16f5e6000000004a493046022100e70956360e17b1744b232ffdf03fe131c579f3b6dcadd73880f8b1a7fdf8e83f022100fcc63239970f55a916564c3a66efa1ec11309a881974b1b965f4cd5f21d8229701ffffffff02ca8a637800000000434104a39b9e4fbd213ef24bb9be69de4a118dd0644082e47c01fd9159d38637b83fbcdc115a5d6e970586a012d1cfe3e3a8b1a3d04e763bdc5a071c0e827c0bd834a5ac40420f00000000001976a9146f9bc3976b168d9c7fe86eec493488ab7ad3f7d088ac00000000010000000197f6583f60ddc1dde64cb340cbc1b9671169eeee6f91fc56e62707207b6b662a000000006b483045022048ed1aed7aa96e70f7730eb9c062f359c089bf76ef10a6582eeb6a6737d9521a0221008885540c193516566a5b271335d7df816f995be3a9d12e8d6d4f478c104a1353012102d1c3f37944951b2aba021b3c89054b2e482783b9ae5bd41be3b89b36d059162effffffff0200e28c13000000001976a914ac323d08b27e0968976f206e9800faf7e3cc0d7488aca07b50c0000000001976a914aecb69c0e859ad85262962722fc77f280b7747bd88ac0000000001000000018f7c8bb2ce27bb87336c9611a4c4ab085383a70c92b64056a18e97c02b8eb024010000006c493046022100e0cec834d66914aed4ca6036412576c92c378fe73acb24b6e187e9af3ea744ca022100b962d71acf54eacf51edc1d1a2e899957be8c8461bce3809fa4df2ab76ab5fb9012103c58cd00b7eec4c5609cc3cbb175db7b670517a5d880c9cf178d4fb5f4f4bcc7cffffffff02405e5d0e000000001976a9146526c18f8909877c13e8f2306e921a809711c4b788ac407cebb1000000001976a9149422de0f21af26135de5b300323d0db0887bc0de88ac00000000010000000197bb9e8bc82635fc4d9ef3039214bf2f93357abcdef6aa10bfc55a478bca142a010000008c4930460221009fa59f6e377785c76fac5addf08c08ee17e91a5406ac8689912649f9ddfe23d7022100ae5aab41529ef61dd831c1085057e15886bc19a9075aab914876a5c4ea5c001c01410446b2542db113d55e498d83e809a53d6751c47fae7630c79e806bef8fb798cf1e2754970bff7d41fca00bb2fb4f5d068542532ad6f473d67c87e0f47c6ed223ebffffffff01b01df505000000001976a91498537cbf67009041ab0e84d88e7f77593b1b74f188ac0000000001000000017daf06f43c7bad0359209d073f0fa6e4eeade5b16599a97ee8e22aa3ba337fdd010000006a47304402201fa6452b0e979bf97e87506410d4ff2ae8b6d464e23a21220ae01420284267b3022049c0ba3437435c9f4c7cb144d547fe855b855f21a1a29bcf9125e208379f493d01210272e5515e90178fa2667fc671857badc529621600c0b5a4e0d25ff6effa18473dffffffff02e0b9bc03000000001976a9149eee8d9a5e10a1e40c23f2b7304d98d9fae66f8e88ac80969800000000001976a91406f1b66fd59a34755c37a8f701f43e937cdbeb1388ac00000000010000000126883cce8352815fe5808268a690f852de19779027207532e32e3ac14bdbacaf010000006b483045022100b04b0720e79bfd785e0672ee5091173d0bf9bbdac2a1c771f2bea2af2547d0460220514d755c1a45ff5f4c075e4d8cc2f5520a9c2b9fd966d99292e56288f7c7842901210277e985f389e7778089465d46e4c7f62890077265186bffeaaf7fabd525b0fcbaffffffff0250802000000000001976a9146e98e5e7b53f7f88da37fa96c6b58686ab5b406688ac8ce64518000000001976a9148964f3acbd9bb341bcab7e86d7a9d4ae710dbf6488ac000000000100000001b6f4aeeb231c95ed35f6269eb6c7dd64d1ea0c386b738b41aece8c5c25fc1bb1010000006b48304502204dafc4045f4172b76de0d3d8e220ef198307a1c853ee3e7bb0c61c426ae2dffb022100c52b7e63be10bc9624d9b3e6d25d0767f744cd0c482f05e1328510b109c4260b012102a212d0bb54331e79c1af59cded8de444755905841cd2f84d5ad271aff79bf9d6ffffffff02808d5b00000000001976a914ac79235ea67e8363d8db52699188c3f0a4118c8b88aca0c44a00000000001976a91444c124314f9074ab4963f08c124dfec43856984d88ac000000000100000001c863d209d95ee67846606ca447b6eeae6ca71e3ab50cacb84bc2f1146e35b395010000006c493046022100f5bcf64e41a4863e38d4de620fce2a006f5f04c21dcf61fa203d9c67592480ff0221009b275fd0a5f56e6bd941817a3c860136b419900dbb4b3216f77e23552e7b93830121034dde6db6e135c57bf61815e0f6b2ff87871609e09ba4c0e1138d1c6530441a74ffffffff02aadf0000000000001976a914b481f68d70d44efe5db069eec7c4d3abde50ed1388ac6025e805000000001976a914812e95e7345b022e99837719d93b1e6b986478ce88ac000000000100000001136769a2cf8e562f22247fbaad9d258f3bc6b74f93f3d839d2afcb3b620fb287010000006c493046022100a98cb2233066a9f90ba9f33b6ce1405af5f99b71975c5a16b43c322a79fceaf2022100c3d6905354a7ec751c3b12bf98e906a2c7f52019660a9e7169e910b08ff8b446012102e829ebd95df516c1092be9874b231cce38328dc9872437acda55106fe4f59c84ffffffff0200127a00000000001976a91406f1b6703d3f56427bfcfd372f952d50d04b64bd88acf0f3b600000000001976a914509140632fb3779733506e6ca5f4d6b715b7a1a088ac000000000100000001b34daef0606be5649a4eea5871c646dbfe22126e89e7bfbb628cf74e6bad78f7010000008b4830450221008e63e8c5ce3c533f5ce655d2c133cb63fabcb683f54402e0940d89c74822e31d02203a6db5ea2148d3918f97e56b054c0c990270fb9bbee385823e7662fb995457d0014104e6bb34059b8637418ada23f6d0f15d8935eaf56e1ce489fb8b62d1fc2ed19e2c9fc7470b6c3c99125d07e1b3cec53b5ebab319c87874577f253bbd16087ffd9bffffffff02002d3101000000001976a91406f1b6703d3f56427bfcfd372f952d50d04b64bd88ac28785a00000000001976a914cea66fff9c6b94ed72b41033302e0433a6852a8688ac000000000100000001777f3863af9244d468d963cf064cd71688bf8c2b8ada5865c65600bb0b57ad35000000008b48304502204b72eee53b010d3460e2483bb75a3b2e42542bc101eab242dda740730f8b3847022100fa1f536d7db3d005364a55489a1f503c5a87a61afd7f3bce922498ae8c6fe6d6014104e6bb34059b8637418ada23f6d0f15d8935eaf56e1ce489fb8b62d1fc2ed19e2c9fc7470b6c3c99125d07e1b3cec53b5ebab319c87874577f253bbd16087ffd9bffffffff02002d3101000000001976a91406f1b6703d3f56427bfcfd372f952d50d04b64bd88ac504a2301000000001976a914cea66fff9c6b94ed72b41033302e0433a6852a8688ac000000000100000001008416a7f4f3b11c92858ec154203bc87f3957f50bd5691565e6b35f47ccd4c9000000008b483045022075f232115e17006915c47ed1d4bb1010e800bd389438ba5e3a0c74d95ecca8ef02210088574691d3678125bc6bec702d23569b3e019d05879678274bb21ac8c5a7379a014104606bf18bd8b5994b1e37ce13e7eed33e8508234a40d8795cfa6fe1f875c7e7eea13d31dc6fc77d2cc02880a9848d1573005d246a7a215b6b1ef48f7296a9d0b3ffffffff02a0860100000000001976a914cea66fff9c6b94ed72b41033302e0433a6852a8688acc01f2e01000000001976a91445d906fdc235c3ffb40acb4831a509e5bac4092c88ac000000000100000004f9ef1061d10834a5a00177e5c2098c2b7bb4585c0ba5ea5ff49a9e2e7d58a828010000008a473044022022cb21f7d0c6643f7e83d02c6b1d04e6b78649a520d9f5bc338c9414d7baf98602200c9d7f180bcc2a74872856c3cc706ca07577dfd8d2dab6643cda3cff57c225c9014104b1d702ecd1157a9ebd5194e39ee01d4bc317c1eec5aa49ca8457a08872cc34e0940404e5e17afcd970c324563f884c02c0bec6a4b840972cb17de6366da07917ffffffff92c5570403128fe36e18c788917a52991f07a0f2bbfe6e5ed42181a872c1b18c0100000089463043022041d66eb32b9610941306a2c522507f561711112dc448d0752b329146bb8b173a021f0c93793eb6b836a1b26bd82f525705d39504988c7695cb57a05ff62527c61a0141046368603cbdb7b86dcf7217391cb3e8c6d2d01542d9872af5f3f1c7d750d6074faba4360a6ecb21b9d467bfcd85013a7adb0168525d33f057bbc0258cdb11085fffffffffd8933c2e56b1ae36959320186ad254b81365f400d13beaa115a6056caaced5bb020000008b483045022100b9a8406ce913c80bcb82c1f9e3ac52c015a75af0a3337595ed08aa66d0a91a9c02200ee3c343273ee79e4778fad9edcd40e7981c475ad592f24555d4cf07f30d98fc014104147c9684f526d6061e5bbf815e49adf1f3ad625a88f8103e9a2b7c49965e2d7ffae8345f424f7fa73ed85203611da2bfdfc3a7203ca8abb2534773fcb2c0454effffffffce15409da1f1e628a6be36b31c6e246274b22a3d46c1891e219a8250559dbbf2010000008b483045022031eb018b41b37c5656198b99c8ce7b6c2016b6a9e92af3e61d919cc67c5be5c8022100ed54f4288a380c43fec57eba9be01e8f739db59742f71010b92ecb7f33a02d6f014104e4489e60dbd32409bfd13414a9d987d21642b8d6206e1122cbd582498b642d11bc553fd56a08641b3bac081644736ddf7e4150d214c45f990c5127834adf2a2fffffffff0200093d00000000001976a9145f35178c0eef183514cc3585716f18b2b754c8e188ac83fa3533000000001976a914b59cd5a89a456ac7dbcb3a4b54de1df958c3d41288ac00000000010000000215cd26ca742cfe53fa9ecdaeb19a96fb894fb7ded47d7337eadf2ab093c4a058010000008a473044022004884366494d64f88bd52406cb3ee0de4863810e11749c4991d74f14530487e302201efab9e588a0ebf2bdcc4d7c18a23b84d2f862287cce7a11141aab147a3df2ed0141047bb8e299ef838abc413907b129c500805cbf7f16447c0e65819f16e9b9d4b2ddacba547f15005f5ae2ce98a94c70188223270a86aadeabae3045093f7920fe4fffffffff43e6a2a27f63955e25df9d3e56f5e27e53c51fd18abd5608e6d3b49aa2225c47000000008b4830450220466c845ba54eef479d72c803b2a503c63c58ee1cd4a4bb675fe3f3162282b8c30221008c498eae3c4680a9594baaefef69c1c68aa67d22ca5a53f3385cc4dd8c8addbe0141047bb8e299ef838abc413907b129c500805cbf7f16447c0e65819f16e9b9d4b2ddacba547f15005f5ae2ce98a94c70188223270a86aadeabae3045093f7920fe4fffffffff0240aeeb02000000001976a914e529310084e57b3c8f9f5a03cc3eb55bf951e79f88ac0bed0e00000000001976a9143152e44f6c676fe7a0df2608bff9ec5f15cc979c88ac00000000";

        // Test standard parsing with byte arrays
        Block block = Block.parse(rawBlock);
        
        assertEquals(rawBlock, HEX.encode(block.bitcoinSerialize()));
        assertEquals(hash, block.getHash().toString());

        // Test standard parsing with stream
        Block block2 = Block.parse(new ByteArrayInputStream(HEX.decode(rawBlock)));

        assertEquals(rawBlock, HEX.encode(block2.bitcoinSerialize()));
        assertEquals(hash, block2.getHash().toString());


    }

}
