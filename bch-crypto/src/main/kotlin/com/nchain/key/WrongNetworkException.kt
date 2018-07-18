/*
 * Copyright 2012 Google Inc.
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

package com.nchain.key

import com.nchain.address.AddressFormatException
import java.util.*

/**
 * This exception is thrown by the Address class when you try and decode an address with a version code that isn't
 * used by that network. You shouldn't allow the user to proceed in this case as they are trying to send money across
 * different chains, an operation that is guaranteed to destroy the money.
 */
class WrongNetworkException(
        /** The version code that was provided in the address.  */
        var verCode: Int,
        /** The list of acceptable versions that were expected given the addresses network parameters.  */
        var acceptableVersions: IntArray?) : AddressFormatException("Version code of address did not match acceptable versions for network: " + verCode + " not in " +
        Arrays.toString(acceptableVersions))
