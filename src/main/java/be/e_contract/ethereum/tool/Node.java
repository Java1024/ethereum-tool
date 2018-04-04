/*
 * Ethereum Tool project.
 * Copyright (C) 2018 e-Contract.be BVBA.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */
package be.e_contract.ethereum.tool;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;
import picocli.CommandLine;

@CommandLine.Command(name = "node", description = "display node information", separator = " ")
public class Node implements Callable<Void> {

    private static final int CHAIN_ID_INC = 35;
    private static final int LOWER_REAL_V = 27;

    @CommandLine.Option(names = {"-l", "--location"}, required = true, description = "the location of the client node")
    private Web3j web3;

    @Override
    public Void call() throws Exception {
        boolean mining = this.web3.ethMining().send().isMining();
        System.out.println("Mining: " + mining);
        if (mining) {
            String coinbase = this.web3.ethCoinbase().send().getAddress();
            System.out.println("Coinbase address: " + coinbase);
            BigInteger hashRate = this.web3.ethHashrate().send().getHashrate();
            System.out.println("Hash rate: " + hashRate);
        }
        String protocolVersion = this.web3.ethProtocolVersion().send().getProtocolVersion();
        System.out.println("protocol version: " + protocolVersion);
        boolean syncing = this.web3.ethSyncing().send().isSyncing();
        System.out.println("Syncing: " + syncing);
        String version = this.web3.netVersion().send().getNetVersion();
        System.out.println("network version: " + version);
        BigInteger peerCount = this.web3.netPeerCount().send().getQuantity();
        System.out.println("Peer count: " + peerCount);
        Integer chainId = getChainId();
        if (null != chainId) {
            System.out.println("chain id: " + chainId);
        }
        return null;
    }

    private Integer getChainId() throws IOException {
        BigInteger blockNumber = this.web3.ethBlockNumber().send().getBlockNumber();
        while (!blockNumber.equals(BigInteger.ZERO)) {
            EthBlock.Block block = this.web3.ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), true).send().getBlock();
            List<EthBlock.TransactionResult> transactions = block.getTransactions();
            if (!transactions.isEmpty()) {
                EthBlock.TransactionResult transactionResult = transactions.get(0);
                EthBlock.TransactionObject transactionObject = (EthBlock.TransactionObject) transactionResult;
                Transaction transaction = transactionObject.get();
                int v = transaction.getV();
                if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) {
                    return null;
                }
                Integer chainId = (v - CHAIN_ID_INC) / 2;
                return chainId;
            }
            blockNumber = blockNumber.subtract(BigInteger.ONE);
        }
        Output.error("could not determine chain id");
        return null;
    }
}
