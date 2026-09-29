package lamz.netblocker.firewall

import android.net.VpnService
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Loopback-only SOCKS5 endpoint for tun2socks. Outbound sockets are protected
 * from the VPN, preventing the forwarding path from recursively entering TUN.
 */
class LocalSocks5Proxy(
    private val vpnService: VpnService,
    private val onBlockedDomain: (String, String) -> Unit,
) {
    private val workers = Executors.newCachedThreadPool()
    private var server: ServerSocket? = null

    val port: Int get() = requireNotNull(server) { "Proxy has not started" }.localPort

    fun start() {
        check(server == null) { "Proxy already started" }
        server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        workers.execute {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val client = requireNotNull(server).accept()
                    workers.execute { handleClient(client) }
                } catch (_: Exception) {
                    return@execute
                }
            }
        }
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
        workers.shutdownNow()
    }

    private fun handleClient(client: Socket) = client.use { socket ->
        val input = BufferedInputStream(socket.getInputStream())
        val output = BufferedOutputStream(socket.getOutputStream())
        if (input.read() != 5) return
        repeat(input.read()) { input.read() }
        output.write(byteArrayOf(5, 0)); output.flush()
        if (input.read() != 5) return
        val command = input.read(); input.read()
        val host = readHost(input) ?: return
        val port = readU16(input)
        when (command) {
            1 -> connectTcp(input, output, host, port)
            3 -> associateUdp(output)
            else -> output.write(byteArrayOf(5, 7, 0, 1, 0, 0, 0, 0, 0, 0))
        }
        output.flush()
    }

    private fun connectTcp(input: BufferedInputStream, output: BufferedOutputStream, host: String, port: Int) {
        val remote = Socket()
        vpnService.protect(remote)
        remote.connect(java.net.InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        output.write(byteArrayOf(5, 0, 0, 1, 0, 0, 0, 0, 0, 0)); output.flush()
        workers.execute { remote.use { it.getInputStream().copyTo(output); runCatching { output.flush() } } }
        input.copyTo(remote.getOutputStream())
    }

    private fun associateUdp(output: BufferedOutputStream) {
        val relay = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
        vpnService.protect(relay)
        val relayPort = relay.localPort
        output.write(byteArrayOf(5, 0, 0, 1, 127, 0, 0, 1, (relayPort shr 8).toByte(), relayPort.toByte()))
        output.flush()
        workers.execute { receiveUdp(relay) }
    }

    private fun receiveUdp(relay: DatagramSocket) = relay.use { socket ->
        val packet = DatagramPacket(ByteArray(MAX_UDP_PACKET), MAX_UDP_PACKET)
        while (!Thread.currentThread().isInterrupted) {
            try {
                packet.length = MAX_UDP_PACKET; socket.receive(packet)
                val request = packet.data.copyOf(packet.length)
                val parsed = parseUdpRequest(request) ?: continue
                if (parsed.port == DNS_PORT) {
                    val domain = dnsQuestionName(parsed.payload) ?: continue
                    val category = DomainBlocklist.categoryFor(domain)
                    if (category != null) {
                        onBlockedDomain(domain, category)
                        val response = makeNxDomain(parsed.payload)
                        socket.send(DatagramPacket(wrapUdpResponse(parsed, response), wrapUdpResponse(parsed, response).size, packet.address, packet.port))
                        continue
                    }
                }
                forwardUdp(socket, packet.address, packet.port, parsed)
            } catch (_: Exception) { return@use }
        }
    }

    private fun forwardUdp(relay: DatagramSocket, client: InetAddress, clientPort: Int, request: UdpRequest) {
        workers.execute {
            val upstream = DatagramSocket()
            try {
                vpnService.protect(upstream)
                upstream.soTimeout = UDP_TIMEOUT_MS
                upstream.send(DatagramPacket(request.payload, request.payload.size, InetAddress.getByName(request.host), request.port))
                val response = DatagramPacket(ByteArray(MAX_UDP_PACKET), MAX_UDP_PACKET)
                upstream.receive(response)
                val body = response.data.copyOf(response.length)
                val wrapped = wrapUdpResponse(request.copy(host = response.address.hostAddress, port = response.port), body)
                relay.send(DatagramPacket(wrapped, wrapped.size, client, clientPort))
            } catch (_: Exception) { } finally { upstream.close() }
        }
    }

    private fun parseUdpRequest(bytes: ByteArray): UdpRequest? {
        if (bytes.size < 7 || bytes[2].toInt() != 0) return null
        var index = 3
        val host = when (bytes[index++].toInt()) {
            1 -> bytes.copyOfRange(index, index + 4).also { index += 4 }.joinToString(".") { (it.toInt() and 0xff).toString() }
            3 -> bytes[index++].toInt().and(0xff).let { size -> String(bytes, index, size).also { index += size } }
            else -> return null
        }
        val port = ((bytes[index++].toInt() and 0xff) shl 8) or (bytes[index++].toInt() and 0xff)
        return UdpRequest(host, port, bytes.copyOfRange(index, bytes.size))
    }

    private fun wrapUdpResponse(request: UdpRequest, payload: ByteArray): ByteArray {
        val address = InetAddress.getByName(request.host).address
        return byteArrayOf(0, 0, 0, 1) + address + byteArrayOf((request.port shr 8).toByte(), request.port.toByte()) + payload
    }

    private fun dnsQuestionName(message: ByteArray): String? {
        if (message.size < 13) return null
        var index = 12; val labels = mutableListOf<String>()
        while (index < message.size) { val size = message[index++].toInt() and 0xff; if (size == 0) break; if (size > 63 || index + size > message.size) return null; labels += String(message, index, size); index += size }
        return labels.takeIf { it.isNotEmpty() }?.joinToString(".")
    }

    private fun makeNxDomain(query: ByteArray): ByteArray = query.copyOf().also { response ->
        if (response.size >= 12) { response[2] = 0x81.toByte(); response[3] = 0x83.toByte(); for (index in 6..11) response[index] = 0 }
    }

    private fun readHost(input: BufferedInputStream): String? = when (input.read()) {
        1 -> ByteArray(4).also { input.readNBytes(it, 0, 4) }.joinToString(".") { (it.toInt() and 0xff).toString() }
        3 -> input.read().takeIf { it >= 0 }?.let { String(input.readNBytes(it)) }
        else -> null
    }
    private fun readU16(input: BufferedInputStream) = (input.read() shl 8) or input.read()
    private data class UdpRequest(val host: String, val port: Int, val payload: ByteArray)
    private companion object { const val DNS_PORT = 53; const val CONNECT_TIMEOUT_MS = 10_000; const val UDP_TIMEOUT_MS = 10_000; const val MAX_UDP_PACKET = 65_535 }
}
