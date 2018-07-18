/*
 * Copyright 2014 Giannis Dzegoutanis
 * Copyright 2018 nChain Ltd.
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
 *
 * This file has been modified for the bitcoinkt project.
 * The original file was from the bitcoinj-cash project (https://github.com/bitcoinj-cash/bitcoinj).
 */

package com.nchain.params

import com.nchain.bitcoinkt.params.MainNetParams
import com.nchain.bitcoinkt.params.NetworkParameters
import com.nchain.bitcoinkt.params.TestNet3Params

/**
 * Utility class that holds all the registered NetworkParameters types used for Address auto discovery.
 * By default only MainNetParams and TestNet3Params are used. If you want to use TestNet2, RegTestParams or
 * UnitTestParams use the register and unregister the TestNet3Params as they don't have their own address
 * version/type code.
 */
object Networks {
    /** Registered networks  */
    private var networks: Set<NetworkParameters> = setOf(TestNet3Params, MainNetParams)

    fun get(): Set<NetworkParameters> {
        return networks
    }

/*
    fun register(network: NetworkParameters) {
        register(Lists.newArrayList(network))
    }

    fun register(networks: Collection<NetworkParameters>) {
        val builder = ImmutableSet.builder<NetworkParameters>()
        builder.addAll(Networks.networks)
        builder.addAll(networks)
        Networks.networks = builder.build()
    }

    fun unregister(network: NetworkParameters) {
        if (networks.contains(network)) {
            val builder = ImmutableSet.builder<NetworkParameters>()
            for (parameters in networks) {
                if (parameters == network)
                    continue
                builder.add(parameters)
            }
            networks = builder.build()
        }
    }
*/
}
