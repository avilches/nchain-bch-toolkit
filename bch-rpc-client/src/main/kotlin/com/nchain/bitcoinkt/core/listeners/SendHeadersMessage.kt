package com.nchain.bitcoinkt.core.listeners

import com.nchain.bitcoinkt.core.EmptyMessage
import com.nchain.bitcoinkt.params.NetworkParameters

/**
 * Created by HashEngineering on 8/11/2017.
 */
class SendHeadersMessage(params: NetworkParameters) : EmptyMessage(params)
