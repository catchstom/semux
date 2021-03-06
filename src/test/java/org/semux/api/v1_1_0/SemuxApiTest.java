/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
/**
 * Semux
 * Semux is an experimental high-performance blockchain platform that powers decentralized application.
 *
 * OpenAPI spec version: 1.0.2
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semux.api.v1_1_0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.semux.TestUtils.createBlock;
import static org.semux.TestUtils.createTransaction;
import static org.semux.core.Amount.Unit.NANO_SEM;
import static org.semux.core.Amount.Unit.SEM;
import static org.semux.core.TransactionType.TRANSFER;
import static org.semux.core.TransactionType.UNVOTE;
import static org.semux.core.TransactionType.VOTE;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.semux.api.v1_1_0.model.AddNodeResponse;
import org.semux.api.v1_1_0.model.BlockType;
import org.semux.api.v1_1_0.model.CreateAccountResponse;
import org.semux.api.v1_1_0.model.DelegateType;
import org.semux.api.v1_1_0.model.DoTransactionResponse;
import org.semux.api.v1_1_0.model.GetAccountResponse;
import org.semux.api.v1_1_0.model.GetAccountTransactionsResponse;
import org.semux.api.v1_1_0.model.GetBlockResponse;
import org.semux.api.v1_1_0.model.GetDelegateResponse;
import org.semux.api.v1_1_0.model.GetDelegatesResponse;
import org.semux.api.v1_1_0.model.GetInfoResponse;
import org.semux.api.v1_1_0.model.GetLatestBlockNumberResponse;
import org.semux.api.v1_1_0.model.GetLatestBlockResponse;
import org.semux.api.v1_1_0.model.GetPeersResponse;
import org.semux.api.v1_1_0.model.GetPendingTransactionsResponse;
import org.semux.api.v1_1_0.model.GetRootResponse;
import org.semux.api.v1_1_0.model.GetTransactionLimitsResponse;
import org.semux.api.v1_1_0.model.GetTransactionResponse;
import org.semux.api.v1_1_0.model.GetValidatorsResponse;
import org.semux.api.v1_1_0.model.GetVoteResponse;
import org.semux.api.v1_1_0.model.GetVotesResponse;
import org.semux.api.v1_1_0.model.ListAccountsResponse;
import org.semux.api.v1_1_0.model.PeerType;
import org.semux.api.v1_1_0.model.SendTransactionResponse;
import org.semux.api.v1_1_0.model.SignMessageResponse;
import org.semux.api.v1_1_0.model.TransactionType;
import org.semux.api.v1_1_0.model.VerifyMessageResponse;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.Genesis;
import org.semux.core.PendingManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.net.ChannelManager;
import org.semux.net.Peer;
import org.semux.net.filter.FilterRule;
import org.semux.net.filter.SemuxIpFilter;
import org.semux.util.Bytes;

import io.netty.handler.ipfilter.IpFilterRuleType;

/**
 * API tests for {@link org.semux.api.v1_1_0.impl.SemuxApiServiceImpl}
 */
public class SemuxApiTest extends SemuxApiTestBase {

    @Test
    public void addNodeTest() {
        String node = "127.0.0.1:5162";
        AddNodeResponse response = api.addNode(node);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, nodeMgr.queueSize());
    }

    @Test
    public void addToBlacklistTest() throws UnknownHostException {
        ChannelManager channelManagerSpy = spy(kernelRule.getKernel().getChannelManager());
        kernelRule.getKernel().setChannelManager(channelManagerSpy);

        // blacklist 8.8.8.8
        assertTrue(api.addToBlacklist("8.8.8.8").isSuccess());
        verify(channelManagerSpy).removeBlacklistedChannels();

        // assert that 8.8.8.8 is no longer acceptable
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));
        assertFalse(channelMgr.isAcceptable(inetSocketAddress));

        // assert that ipfilter.json is persisted
        File ipfilterJson = new File(config.configDir(), SemuxIpFilter.CONFIG_FILE);
        assertTrue(ipfilterJson.exists());
    }

    @Test
    public void addToWhitelistTest() throws UnknownHostException {
        // reject all connections
        channelMgr.getIpFilter().appendRule(new FilterRule("0.0.0.0/0", IpFilterRuleType.REJECT));

        // whitelist 8.8.8.8
        assertTrue(api.addToWhitelist("8.8.8.8").isSuccess());

        // assert that 8.8.8.8 is acceptable
        InetSocketAddress inetSocketAddress = mock(InetSocketAddress.class);
        when(inetSocketAddress.getAddress()).thenReturn(InetAddress.getByName("8.8.8.8"));
        assertTrue(channelMgr.isAcceptable(inetSocketAddress));

        // assert that ipfilter.json is persisted
        File ipfilterJson = new File(config.configDir(), SemuxIpFilter.CONFIG_FILE);
        assertTrue(ipfilterJson.exists());
    }

    @Test
    public void createAccountTest() {
        int size = wallet.getAccounts().size();
        CreateAccountResponse response = api.createAccount(null);
        assertTrue(response.isSuccess());
        assertEquals(size + 1, wallet.getAccounts().size());
    }

    @Test
    public void getAccountTest() {
        // create an account
        Key key = new Key();
        accountState.adjustAvailable(key.toAddress(), SEM.of(1000));
        chain.addBlock(createBlock(
                chain.getLatestBlockNumber() + 1,
                Collections.singletonList(createTransaction(config, key, key, Amount.ZERO)),
                Collections.singletonList(new TransactionResult(true))));

        // request api endpoint
        GetAccountResponse response = api.getAccount(key.toAddressString());
        assertTrue(response.isSuccess());
        assertEquals(SEM.of(1000).getNano(), Long.parseLong(response.getResult().getAvailable()));
        assertEquals(Integer.valueOf(1), response.getResult().getTransactionCount());
    }

    @Test
    public void getAccountTransactionsTest() {
        Transaction tx = createTransaction(config);
        TransactionResult res = new TransactionResult(true);
        Block block = createBlock(chain.getLatestBlockNumber() + 1, Collections.singletonList(tx),
                Collections.singletonList(res));
        chain.addBlock(block);

        GetAccountTransactionsResponse response = api.getAccountTransactions(Hex.encode(tx.getFrom()), "0", "1024");
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());
        for (TransactionType txType : response.getResult()) {
            assertEquals(block.getNumber(), Long.parseLong(txType.getBlockNumber()));
        }
    }

    @Test
    public void getBlockByHashTest() {
        Genesis gen = chain.getGenesis();
        GetBlockResponse response = api.getBlockByHash(Hex.encode0x(gen.getHash()));
        assertTrue(response.isSuccess());
        assertEquals(Hex.encode0x(gen.getHash()), response.getResult().getHash());
        assertNotNull(response.getResult().getTransactions());
    }

    @Test
    public void getBlockByNumberTest() {
        Genesis gen = chain.getGenesis();
        GetBlockResponse response = api.getBlockByNumber(String.valueOf(gen.getNumber()));
        assertTrue(response.isSuccess());
        assertEquals(Hex.encode0x(gen.getHash()), response.getResult().getHash());
        assertNotNull(response.getResult().getTransactions());
    }

    @Test
    public void getDelegateTest() {
        Genesis gen = chain.getGenesis();
        for (Map.Entry<String, byte[]> entry : gen.getDelegates().entrySet()) {
            GetDelegateResponse response = api.getDelegate(Hex.encode0x(entry.getValue()));
            assertTrue(response.isSuccess());
            assertEquals(entry.getKey(), response.getResult().getName());
        }
    }

    @Test
    public void getDelegatesTest() {
        Genesis gen = chain.getGenesis();
        GetDelegatesResponse response = api.getDelegates();
        assertEquals(gen.getDelegates().size(), response.getResult().size());
        assertThat(gen.getDelegates().entrySet().stream().map(e -> Hex.encode0x(e.getValue())).sorted()
                .collect(Collectors.toList()))
                        .isEqualTo(response.getResult().stream().map(DelegateType::getAddress).sorted()
                                .collect(Collectors.toList()));
    }

    @Test
    public void getInfoTest() {
        GetInfoResponse response = api.getInfo();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());
        assertEquals("0", response.getResult().getLatestBlockNumber());
        assertEquals(Hex.encode0x(chain.getLatestBlock().getHash()), response.getResult().getLatestBlockHash());
        assertEquals(Integer.valueOf(0), response.getResult().getActivePeers());
        assertEquals(Integer.valueOf(0), response.getResult().getPendingTransactions());
        assertEquals(config.getClientId(), response.getResult().getClientId());
        assertEquals(Hex.encode0x(wallet.getAccount(0).toAddress()), response.getResult().getCoinbase());
    }

    @Test
    public void getLatestBlockTest() {
        Genesis genesisBlock = chain.getGenesis();
        GetLatestBlockResponse response = api.getLatestBlock();
        assertNotNull(response);
        assertTrue(response.isSuccess());

        BlockType blockJson = response.getResult();
        assertEquals(Hex.encode0x(genesisBlock.getHash()), blockJson.getHash());
        assertEquals(genesisBlock.getNumber(), Long.parseLong(blockJson.getNumber()));
        assertEquals(Hex.encode0x(genesisBlock.getCoinbase()), blockJson.getCoinbase());
        assertEquals(Hex.encode0x(genesisBlock.getParentHash()), blockJson.getParentHash());
        assertEquals(genesisBlock.getTimestamp(), Long.parseLong(blockJson.getTimestamp()));
        assertEquals(Hex.encode0x(genesisBlock.getTransactionsRoot()), blockJson.getTransactionsRoot());
        assertEquals(Hex.encode0x(genesisBlock.getData()), blockJson.getData());
    }

    @Test
    public void getLatestBlockNumberTest() {
        GetLatestBlockNumberResponse response = api.getLatestBlockNumber();
        assertNotNull(response);
        assertEquals(chain.getLatestBlock().getNumber(), Long.parseLong(response.getResult()));
    }

    @Test
    public void getPeersTest() {
        channelMgr = spy(kernelRule.getKernel().getChannelManager());
        List<Peer> peers = Arrays.asList(
                new Peer("1.2.3.4", 5161, (short) 1, "client1", "peer1", 1, config.capabilitySet()),
                new Peer("2.3.4.5", 5171, (short) 2, "client2", "peer2", 2, config.capabilitySet()));
        when(channelMgr.getActivePeers()).thenReturn(peers);
        kernelRule.getKernel().setChannelManager(channelMgr);

        GetPeersResponse response = api.getPeers();
        assertTrue(response.isSuccess());
        List<PeerType> result = response.getResult();

        assertNotNull(result);
        assertEquals(peers.size(), result.size());
        for (int i = 0; i < peers.size(); i++) {
            PeerType peerJson = result.get(i);
            Peer peer = peers.get(i);
            assertEquals(peer.getIp(), peerJson.getIp());
            assertEquals(peer.getPort(), peerJson.getPort().intValue());
            assertEquals(peer.getNetworkVersion(), peerJson.getNetworkVersion().shortValue());
            assertEquals(peer.getClientId(), peerJson.getClientId());
            assertEquals(Hex.PREF + peer.getPeerId(), peerJson.getPeerId());
            assertEquals(peer.getLatestBlockNumber(), Long.parseLong(peerJson.getLatestBlockNumber()));
            assertEquals(peer.getLatency(), Long.parseLong(peerJson.getLatency()));
            assertEquals(peer.getCapabilities().toList(), peerJson.getCapabilities());
        }
    }

    @Test
    public void getPendingTransactionsTest() {
        Transaction tx = createTransaction(config);
        TransactionResult result = new TransactionResult(true);
        PendingManager pendingManager = spy(kernelRule.getKernel().getPendingManager());
        when(pendingManager.getPendingTransactions()).thenReturn(
                Collections.singletonList(new PendingManager.PendingTransaction(tx, result)));
        kernelRule.getKernel().setPendingManager(pendingManager);

        GetPendingTransactionsResponse response = api.getPendingTransactions();
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());
        assertThat(response.getResult()).hasSize(1);
    }

    @Test
    public void getRootTest() {
        GetRootResponse response = api.getRoot();
        assertNotNull(response);
    }

    @Test
    public void getTransactionTest() {
        Key from = new Key(), to = new Key();
        Transaction tx = createTransaction(config, from, to, Amount.Unit.SEM.of(1));
        TransactionResult res = new TransactionResult(true);
        Block block = createBlock(chain.getLatestBlockNumber() + 1, Collections.singletonList(tx),
                Collections.singletonList(res));
        chain.addBlock(block);

        GetTransactionResponse response = api.getTransaction(Hex.encode(tx.getHash()));
        assertTrue(response.isSuccess());
        assertEquals(Hex.encode0x(to.toAddress()), response.getResult().getTo());
        assertEquals(block.getNumber(), Long.parseLong(response.getResult().getBlockNumber()));
        assertEquals(Hex.encode0x(tx.getHash()), response.getResult().getHash());
        assertEquals(Hex.encode0x(Bytes.EMPTY_BYTES), response.getResult().getData());
        assertEquals(tx.getFee().getNano(), Long.parseLong(response.getResult().getFee()));
        assertEquals(Hex.encode0x(tx.getFrom()), response.getResult().getFrom());
        assertEquals(tx.getNonce(), Long.parseLong(response.getResult().getNonce()));
        assertEquals(tx.getTimestamp(), Long.parseLong(response.getResult().getTimestamp()));
        assertEquals(tx.getType().toString(), response.getResult().getType());
        assertEquals(tx.getValue().getNano(), Long.parseLong(response.getResult().getValue()));
    }

    @Test
    public void getTransactionLimitsTest() {
        for (org.semux.core.TransactionType type : org.semux.core.TransactionType.values()) {
            GetTransactionLimitsResponse response = api.getTransactionLimits(type.toString());
            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(config.maxTransactionDataSize(type),
                    response.getResult().getMaxTransactionDataSize().intValue());
            assertEquals(config.minTransactionFee().getNano(),
                    Long.parseLong(response.getResult().getMinTransactionFee()));

            if (type.equals(org.semux.core.TransactionType.DELEGATE)) {
                assertEquals(config.minDelegateBurnAmount().getNano(),
                        Long.parseLong(response.getResult().getMinDelegateBurnAmount()));
            } else {
                assertNull(response.getResult().getMinDelegateBurnAmount());
            }
        }
    }

    @Test
    public void getValidatorsTest() {
        Genesis gen = chain.getGenesis();
        GetValidatorsResponse response = api.getValidators();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(gen.getDelegates().size(), response.getResult().size());
        assertThat(gen.getDelegates().entrySet().stream().map(e -> Hex.encode0x(e.getValue())).sorted()
                .collect(Collectors.toList()))
                        .isEqualTo(response.getResult().stream().sorted().collect(Collectors.toList()));
    }

    @Test
    public void getVoteTest() {
        Key key = new Key();
        Key key2 = new Key();
        DelegateState ds = chain.getDelegateState();
        ds.register(key2.toAddress(), Bytes.of("test"));
        ds.vote(key.toAddress(), key2.toAddress(), NANO_SEM.of(200));

        GetVoteResponse response = api.getVote(key2.toAddressString(), key.toAddressString());
        assertTrue(response.isSuccess());
        assertEquals(200L, Long.parseLong(response.getResult()));
    }

    @Test
    public void getVotesTest() {
        Key voterKey = new Key();
        Key delegateKey = new Key();
        DelegateState ds = chain.getDelegateState();
        assertTrue(ds.register(delegateKey.toAddress(), Bytes.of("test")));
        assertTrue(ds.vote(voterKey.toAddress(), delegateKey.toAddress(), NANO_SEM.of(200)));
        ds.commit();

        GetVotesResponse response = api.getVotes(delegateKey.toAddressString());
        assertTrue(response.isSuccess());
        assertEquals(200L, Long.parseLong(response.getResult().get(Hex.PREF + voterKey.toAddressString())));
    }

    @Test
    public void listAccountsTest() {
        ListAccountsResponse response = api.listAccounts();
        assertNotNull(response);
        assertThat(response.getResult())
                .hasSize(wallet.size())
                .isEqualTo(wallet.getAccounts().parallelStream()
                        .map(acc -> Hex.PREF + acc.toAddressString())
                        .collect(Collectors.toList()));
    }

    @Test
    public void registerDelegateTest() throws InterruptedException {
        String from = wallet.getAccount(0).toAddressString();
        String fee = String.valueOf(config.minTransactionFee().getNano());
        String data = Hex.encode(Bytes.of("test_delegate"));
        DoTransactionResponse response = api.registerDelegate(from, fee, data);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.getResult()));
        assertEquals(list.get(list.size() - 1).transaction.getType(), org.semux.core.TransactionType.DELEGATE);
    }

    @Test
    public void sendTransactionTest() throws InterruptedException {
        Transaction tx = createTransaction(config);

        SendTransactionResponse response = api.sendTransaction(Hex.encode(tx.toBytes()));
        assertTrue(response.isSuccess());

        Thread.sleep(200);
        List<Transaction> list = pendingMgr.getQueue();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).getHash(), tx.getHash());
    }

    @Test
    public void signMessageTest() {
        String address = wallet.getAccount(0).toAddressString();
        String addressOther = wallet.getAccount(1).toAddressString();

        String message = "helloworld";
        SignMessageResponse response = api.signMessage(address, message);
        assertTrue(response.isSuccess());
        String signature = response.getResult();
        VerifyMessageResponse verifyMessageResponse = api.verifyMessage(address, message, signature);
        assertTrue(verifyMessageResponse.isSuccess());
        assertTrue(verifyMessageResponse.isValidSignature());

        // verify no messing with fromaddress
        verifyMessageResponse = api.verifyMessage(addressOther, message, signature);
        assertTrue(verifyMessageResponse.isSuccess());
        assertFalse(verifyMessageResponse.isValidSignature());

        // verify no messing with message
        verifyMessageResponse = api.verifyMessage(address, message + "other", signature);
        assertTrue(verifyMessageResponse.isSuccess());
        assertFalse(verifyMessageResponse.isValidSignature());
    }

    @Test
    public void transferTest() throws InterruptedException {
        Key key = new Key();
        String value = "1000000000";
        String from = wallet.getAccount(0).toAddressString();
        String to = key.toAddressString();
        String fee = String.valueOf(config.minTransactionFee().getNano());
        String data = Hex.encode(Bytes.of("test_transfer"));

        DoTransactionResponse response = api.transfer(from, to, value, fee, data);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.getResult()));
        assertEquals(TRANSFER, list.get(list.size() - 1).transaction.getType());
    }

    @Test
    public void unvoteTest() throws InterruptedException {
        Key delegate = new Key();
        delegateState.register(delegate.toAddress(), Bytes.of("test_unvote"));

        Amount amount = NANO_SEM.of(1000000000);
        byte[] voter = wallet.getAccounts().get(0).toAddress();
        accountState.adjustLocked(voter, amount);
        delegateState.vote(voter, delegate.toAddress(), amount);

        String from = wallet.getAccount(0).toAddressString();
        String to = delegate.toAddressString();
        String value = String.valueOf(amount.getNano());
        String fee = "50000000";

        DoTransactionResponse response = api.unvote(from, to, value, fee);
        assertNotNull(response);

        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.getResult()));
        assertEquals(UNVOTE, list.get(list.size() - 1).transaction.getType());
    }

    @Test
    public void voteTest() throws InterruptedException {
        Key delegate = new Key();
        delegateState.register(delegate.toAddress(), Bytes.of("test_unvote"));

        Amount amount = NANO_SEM.of(1000000000);
        byte[] voter = wallet.getAccounts().get(0).toAddress();
        accountState.adjustLocked(voter, amount);
        delegateState.vote(voter, delegate.toAddress(), amount);

        String from = wallet.getAccount(0).toAddressString();
        String to = delegate.toAddressString();
        String value = String.valueOf(amount.getNano());
        String fee = String.valueOf(config.minTransactionFee().getNano());

        DoTransactionResponse response = api.vote(from, to, value, fee);
        assertTrue(response.isSuccess());
        assertNotNull(response.getResult());

        Thread.sleep(200);

        List<PendingManager.PendingTransaction> list = pendingMgr.getPendingTransactions();
        assertFalse(list.isEmpty());
        assertArrayEquals(list.get(list.size() - 1).transaction.getHash(), Hex.decode0x(response.getResult()));
        assertEquals(VOTE, list.get(list.size() - 1).transaction.getType());
    }

}
