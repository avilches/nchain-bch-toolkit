package com.nchain.bitcoinkt.core.listeners

import com.nchain.bitcoinkt.core.EmptyMessage
import com.nchain.params.NetworkParameters

/**
 * Created by HashEngineering on 8/11/2017.
 */
class FeeFilterMessage(params: NetworkParameters) : EmptyMessage(params)
