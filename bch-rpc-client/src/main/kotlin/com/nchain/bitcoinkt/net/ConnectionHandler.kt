/*
 * Copyright 2013 Google Inc.
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

package com.nchain.bitcoinkt.net

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.base.Preconditions.checkState
import com.google.common.base.Throwables
import com.nchain.bitcoinkt.core.Message
import com.nchain.bitcoinkt.utils.Threading
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.CancelledKeyException
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.*
import javax.annotation.concurrent.GuardedBy

// TODO: The locking in all this class is horrible and not really necessary. We should just run all network stuff on one thread.

/**
 * A simple NIO MessageWriteTarget which handles all the business logic of a connection (reading+writing bytes).
 * Used only by the NioClient and NioServer classes
 */
internal class ConnectionHandler private constructor(@field:GuardedBy("lock") var connection: StreamConnection?, @field:GuardedBy("lock") private val key: SelectionKey) : MessageWriteTarget {

    // We lock when touching local flags and when writing data, but NEVER when calling any methods which leave this
    // class into non-Java classes.
    private val lock = Threading.lock("nioConnectionHandler")
    @GuardedBy("lock")
    private var readBuff: ByteBuffer?
    @GuardedBy("lock")
    private val channel: SocketChannel
    @GuardedBy("lock")
    private var closeCalled = false

    @GuardedBy("lock")
    private var bytesToWriteRemaining: Long = 0
    @GuardedBy("lock")
    private val bytesToWrite = LinkedList<ByteBuffer>()

    private var connectedHandlers: MutableSet<ConnectionHandler>? = null

    @Throws(IOException::class)
    constructor(connectionFactory: StreamConnectionFactory, key: SelectionKey) : this(connectionFactory.getNewConnection((key.channel() as SocketChannel).socket().inetAddress, (key.channel() as SocketChannel).socket().port), key) {
        if (connection == null)
            throw IOException("Parser factory.getNewConnection returned null")
    }

    init {
        this.channel = checkNotNull(key.channel() as SocketChannel)
        if (connection == null) {
            readBuff = null
        }
        else {
            readBuff = ByteBuffer.allocateDirect(Math.min(Math.max(connection!!.maxMessageSize, BUFFER_SIZE_LOWER_BOUND), BUFFER_SIZE_UPPER_BOUND))
            connection!!.writeTarget = this // May callback into us (eg closeConnection() now)
            connectedHandlers = null
        }
    }

    constructor(connection: StreamConnection, key: SelectionKey, connectedHandlers: MutableSet<ConnectionHandler>) : this(checkNotNull<StreamConnection>(connection), key) {

        // closeConnection() may have already happened because we invoked the other c'tor above, which called
        // connection.setWriteTarget which might have re-entered already. In this case we shouldn't add ourselves
        // to the connectedHandlers set.
        lock.lock()
        try {
            this.connectedHandlers = connectedHandlers
            if (!closeCalled)
                checkState(this.connectedHandlers!!.add(this))
        } finally {
            lock.unlock()
        }
    }

    @GuardedBy("lock")
    private fun setWriteOps() {
        // Make sure we are registered to get updated when writing is available again
        key.interestOps(key.interestOps() or SelectionKey.OP_WRITE)
        // Refresh the selector to make sure it gets the new interestOps
        key.selector().wakeup()
    }

    // Tries to write any outstanding write bytes, runs in any thread (possibly unlocked)
    @Throws(IOException::class)
    private fun tryWriteBytes() {
        lock.lock()
        try {
            // Iterate through the outbound ByteBuff queue, pushing as much as possible into the OS' network buffer.
            val bytesIterator = bytesToWrite.iterator()
            while (bytesIterator.hasNext()) {
                val buff = bytesIterator.next()
                bytesToWriteRemaining -= channel.write(buff).toLong()
                if (!buff.hasRemaining())
                    bytesIterator.remove()
                else {
                    setWriteOps()
                    break
                }
            }
            // If we are done writing, clear the OP_WRITE interestOps
            if (bytesToWrite.isEmpty())
                key.interestOps(key.interestOps() and SelectionKey.OP_WRITE.inv())
            // Don't bother waking up the selector here, since we're just removing an op, not adding
        } finally {
            lock.unlock()
        }
    }

    @Throws(IOException::class)
    override fun writeBytes(message: ByteArray) {
        var andUnlock = true
        lock.lock()
        try {
            // Network buffers are not unlimited (and are often smaller than some messages we may wish to send), and
            // thus we have to buffer outbound messages sometimes. To do this, we use a queue of ByteBuffers and just
            // append to it when we want to send a message. We then let tryWriteBytes() either send the message or
            // register our SelectionKey to wakeup when we have free outbound buffer space available.

            if (bytesToWriteRemaining + message.size > OUTBOUND_BUFFER_BYTE_COUNT)
                throw IOException("Outbound buffer overflowed")
            // Just dump the message onto the write buffer and call tryWriteBytes
            // TODO: Kill the needless message duplication when the write completes right away
            bytesToWrite.offer(ByteBuffer.wrap(Arrays.copyOf(message, message.size)))
            bytesToWriteRemaining += message.size.toLong()
            setWriteOps()
        } catch (e: IOException) {
            lock.unlock()
            andUnlock = false
            log.warn("Error writing message to connection, closing connection", e)
            closeConnection()
            throw e
        } catch (e: CancelledKeyException) {
            lock.unlock()
            andUnlock = false
            log.warn("Error writing message to connection, closing connection", e)
            closeConnection()
            throw IOException(e)
        } finally {
            if (andUnlock)
                lock.unlock()
        }
    }

    // May NOT be called with lock held
    override fun closeConnection() {
        checkState(!lock.isHeldByCurrentThread)
        try {
            channel.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        connectionClosed()
    }

    private fun connectionClosed() {
        var callClosed = false
        lock.lock()
        try {
            callClosed = !closeCalled
            closeCalled = true
        } finally {
            lock.unlock()
        }
        if (callClosed) {
            checkState(connectedHandlers == null || connectedHandlers!!.remove(this))
            connection!!.connectionClosed()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ConnectionHandler::class.java)

        private val BUFFER_SIZE_LOWER_BOUND = 4096
        private val BUFFER_SIZE_UPPER_BOUND = 65536

        private val OUTBOUND_BUFFER_BYTE_COUNT = Message.MAX_SIZE + 24 // 24 byte message header

        // Handle a SelectionKey which was selected
        // Runs unlocked as the caller is single-threaded (or if not, should enforce that handleKey is only called
        // atomically for a given ConnectionHandler)
        fun handleKey(key: SelectionKey) {
            val handler = key.attachment() as ConnectionHandler?
            try {
                if (handler == null)
                    return
                if (!key.isValid) {
                    handler.closeConnection() // Key has been cancelled, make sure the socket gets closed
                    return
                }
                if (key.isReadable) {
                    // Do a socket read and invoke the connection's receiveBytes message
                    val read = handler.channel.read(handler.readBuff)
                    if (read == 0)
                        return  // Was probably waiting on a write
                    else if (read == -1) { // Socket was closed
                        key.cancel()
                        handler.closeConnection()
                        return
                    }
                    // "flip" the buffer - setting the limit to the current position and setting position to 0
                    handler.readBuff!!.flip()
                    // Use connection.receiveBytes's return value as a check that it stopped reading at the right location
                    val bytesConsumed = checkNotNull<StreamConnection>(handler.connection).receiveBytes(handler.readBuff!!)
                    checkState(handler.readBuff!!.position() == bytesConsumed)
                    // Now drop the bytes which were read by compacting readBuff (resetting limit and keeping relative
                    // position)
                    handler.readBuff!!.compact()
                }
                if (key.isWritable)
                    handler.tryWriteBytes()
            } catch (e: Exception) {
                // This can happen eg if the channel closes while the thread is about to get killed
                // (ClosedByInterruptException), or if handler.connection.receiveBytes throws something
                val t = Throwables.getRootCause(e)
                log.warn("Error handling SelectionKey: {} {}", t.javaClass.name, if (t.message != null) t.message else "", e)
                handler?.closeConnection()
            }

        }
    }
}
