/*
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

package com.nchain.tools

import com.nchain.bitcoinkt.utils.Utils

object DRMWorkaround {
//    private val log = LoggerFactory.getLogger(DRMWorkaround::class.java)

    private var done = false

    fun maybeDisableExportControls() {
        // This sorry story is documented in https://bugs.openjdk.java.net/browse/JDK-7024850
        // Oracle received permission to ship AES-256 by default in 2011, but didn't get around to it for Java 8
        // even though that shipped in 2014! That's dumb. So we disable the ridiculous US government mandated DRM
        // for AES-256 here, as Tor/BIP38 requires it.

        if (done) return
        done = true

        if (Utils.isAndroidRuntime)
            return
        try {
            val gate = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted")
            gate.isAccessible = true
            gate.setBoolean(null, false)
            val allPerm = Class.forName("javax.crypto.CryptoAllPermission").getDeclaredField("INSTANCE")
            allPerm.isAccessible = true
            val accessAllAreasCard = allPerm.get(null)
            val constructor = Class.forName("javax.crypto.CryptoPermissions").getDeclaredConstructor()
            constructor.isAccessible = true
            val coll = constructor.newInstance()
            val addPerm = Class.forName("javax.crypto.CryptoPermissions").getDeclaredMethod("add", java.security.Permission::class.java)
            addPerm.isAccessible = true
            addPerm.invoke(coll, accessAllAreasCard)
            val defaultPolicy = Class.forName("javax.crypto.JceSecurity").getDeclaredField("defaultPolicy")
            defaultPolicy.isAccessible = true
            defaultPolicy.set(null, coll)
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO vilches
//            log.warn("Failed to deactivate AES-256 barrier logic, Tor mode/BIP38 decryption may crash if this JVM requires it: " + e.message)
        }

    }
}
