/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.semux.core.Amount.ZERO;
import static org.semux.core.Amount.Unit.SEM;

import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;
import org.semux.Network;
import org.semux.core.Amount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainnetConfigTest {

    private Logger logger = LoggerFactory.getLogger(MainnetConfigTest.class);

    private MainnetConfig config;

    @Before
    public void testLoad() {
        config = new MainnetConfig(Constants.DEFAULT_DATA_DIR);
        assertEquals(Network.MAINNET, config.network());
    }

    @Test
    public void testBlockReward() {
        Amount total = LongStream
                .rangeClosed(1, 100_000_000)
                .mapToObj(config::getBlockReward)
                .reduce(ZERO, Amount::sum);

        assertEquals(SEM.of(75_000_000), total);
    }

    @Test
    public void testNumberOfValidators() {
        int last = 0;
        for (int i = 0; i < 60 * Constants.BLOCKS_PER_DAY; i++) {
            int n = config.getNumberOfValidators(i);
            if (n != last) {
                assertTrue(n > last && (n - last == 1 || last == 0));
                logger.info("block # = {}, validators = {}", i, n);
                last = n;
            }
        }

        assertEquals(100, last);
    }
}
