package com.zaneschepke.wireguardautotunnel.ui.state

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom

@Serializable
enum class MimicType {
    DNS,
    QUIC,
    SIP
}

@Serializable
data class MimicSettings(
    val type: MimicType = MimicType.DNS,
    val domain: String = "example.com",
    val sipFromUser: String = "alice",
    val sipToUser: String = "bob",
    val sipFromDomain: String = "atlanta.com",
    val sipToDomain: String = "biloxi.com",
    val quicVersion: String = "1",
    val itimeMin: Int = 120,
    val itimeMax: Int = 180,
    val regenerateIntervalSeconds: Int = 20,
) {
    fun toJson(): String = Json.encodeToString<MimicSettings>(this)

    companion object {
        const val ITIME_MIN_ALLOWED = 100
        const val ITIME_MAX_ALLOWED = 600
        const val REGENERATE_MIN = 20
        const val REGENERATE_MAX = 60
        const val DEFAULT_REGENERATE_INTERVAL = 20

        fun defaultDns() = MimicSettings(type = MimicType.DNS)
        fun defaultQuic() = MimicSettings(type = MimicType.QUIC)
        fun defaultSip() = MimicSettings(type = MimicType.SIP)

        fun fromJson(json: String): MimicSettings? {
            return try {
                Json.decodeFromString<MimicSettings>(json)
            } catch (e: Exception) {
                null
            }
        }

        val Saver: Saver<MimicSettings, *> = listSaver(
            save = { listOf(
                it.type.ordinal,
                it.domain,
                it.sipFromUser,
                it.sipToUser,
                it.sipFromDomain,
                it.sipToDomain,
                it.quicVersion,
                it.itimeMin,
                it.itimeMax,
                it.regenerateIntervalSeconds
            ) },
            restore = {
                MimicSettings(
                    type = MimicType.entries[it[0] as Int],
                    domain = it[1] as String,
                    sipFromUser = it[2] as String,
                    sipToUser = it[3] as String,
                    sipFromDomain = it[4] as String,
                    sipToDomain = it[5] as String,
                    quicVersion = it[6] as String,
                    itimeMin = it[7] as Int,
                    itimeMax = it[8] as Int,
                    regenerateIntervalSeconds = it[9] as Int
                )
            }
        )
    }
}

class MimicDomainRequiredException : IllegalArgumentException("Domain is required for DNS mimic")

object MimicGenerator {

    private val random = SecureRandom()

    private fun <T> List<T>.secureRandom(): T = this[random.nextInt(size)]

    private fun <T> List<T>.secureShuffled(): List<T> {
        val result = this.toMutableList()
        for (i in result.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }
        return result
    }

    private const val DNS_TYPE_A = "0001"
    private const val DNS_TYPE_AAAA = "001c"
    private const val DNS_TYPE_HTTPS = "0041"

    fun generateDnsMimic(settings: MimicSettings): MimicResult {
        if (settings.domain.isBlank()) {
            throw MimicDomainRequiredException()
        }

        val transactionId1 = random.nextInt(0x0001, 0xFFFF)
        val transactionId2 = random.nextInt(0x0001, 0xFFFF)
        val transactionId3 = random.nextInt(0x0001, 0xFFFF)
        val domain = settings.domain

        val dnsQueryA = buildDnsQueryWithEdns(domain, transactionId1, DNS_TYPE_A)
        val dnsQueryAAAA = buildDnsQueryWithEdns(domain, transactionId2, DNS_TYPE_AAAA)
        val dnsResponse = buildDnsResponse(domain, transactionId1)

        val extraQuery = if (random.nextBoolean()) {
            buildDnsQueryWithEdns(domain, transactionId3, DNS_TYPE_HTTPS)
        } else {
            buildDnsQueryWithEdns("www.$domain", transactionId3, DNS_TYPE_A)
        }

        val itime = random.nextInt(settings.itimeMin, settings.itimeMax + 1)

        return MimicResult(
            i1 = "<b 0x${dnsQueryA}>",
            i2 = "<b 0x${dnsQueryAAAA}>",
            i3 = "<b 0x${dnsResponse}>",
            i4 = "<b 0x${extraQuery}>",
            i5 = "<b 0x${buildDnsResponse(domain, transactionId2)}>",
            j1 = "<b 0x${generateRandomHex(random.nextInt(8, 24))}>",
            j2 = "<b 0x${generateRandomHex(random.nextInt(4, 16))}>",
            j3 = "",
            itime = itime.toString()
        )
    }

    fun generateQuicMimic(settings: MimicSettings): MimicResult {
        val connectionId = generateRandomHex(8)
        val packetNumber = random.nextInt(0, 0xFFFFFF)
        val quicInitial = buildQuicInitialPacket(connectionId, packetNumber, settings.quicVersion)
        val quicFollowUp = buildQuicFollowUpPacket()
        val junkData = generateRandomHex(8)
        val itime = random.nextInt(settings.itimeMin, settings.itimeMax + 1)

        return MimicResult(
            i1 = "<b 0x${quicInitial}>",
            i2 = "<b 0x${quicFollowUp}>",
            i3 = "",
            i4 = "",
            i5 = "",
            j1 = "<b 0x${junkData}>",
            j2 = "",
            j3 = "",
            itime = itime.toString()
        )
    }

    fun generateSipMimic(settings: MimicSettings): MimicResult {
        val callId = generateRandomHex(16)
        val branch = "z9hG4bK${generateRandomHex(8)}"
<<<<<<< HEAD
<<<<<<< HEAD
        val fromTag = random.nextInt(100000000, 999999999).toString()
        val toTag = random.nextInt(100000000, 999999999).toString()
        val cseq = random.nextInt(1, 999999)
        val userAgent = SIP_USER_AGENTS.secureRandom()
        val clientIp = generateRandomPrivateIp()
        val serverIp = generateRandomPrivateIp()
        val clientPort = random.nextInt(10000, 65000)
        val serverPort = listOf(5060, 5060, 5060, 5061).secureRandom()
        val rtpPort = (random.nextInt(8000, 30000) / 2) * 2
        val sessionId = random.nextLong(1000000000L, 9999999999L).toString()
=======
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        val fromTag = Random.nextInt(100000000, 999999999).toString()
        val toTag = Random.nextInt(100000000, 999999999).toString()
        val cseq = Random.nextInt(1, 999999)
        val userAgent = SIP_USER_AGENTS.random()
        val clientIp = generateRandomPrivateIp()
        val serverIp = generateRandomPrivateIp()
        val clientPort = Random.nextInt(10000, 65000)
        val serverPort = listOf(5060, 5060, 5060, 5061).random()
        val rtpPort = (Random.nextInt(8000, 30000) / 2) * 2
        val sessionId = Random.nextLong(1000000000L, 9999999999L).toString()
<<<<<<< HEAD
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1

        val sipInvite = buildSipInvite(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = branch,
            fromTag = fromTag,
            cseq = cseq,
            userAgent = userAgent,
            clientIp = clientIp,
            clientPort = clientPort,
            rtpPort = rtpPort,
            sessionId = sessionId
        )

        val sipTrying = buildSipTrying(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = branch,
            fromTag = fromTag,
            cseq = cseq,
            clientIp = clientIp,
            clientPort = clientPort
<<<<<<< HEAD
<<<<<<< HEAD
        )

        val sipRinging = buildSipRinging(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = branch,
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq,
            clientIp = clientIp,
            clientPort = clientPort
        )

        val sipOk = buildSip200Ok(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = branch,
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq,
            userAgent = userAgent,
            clientIp = clientIp,
            clientPort = clientPort,
            serverIp = serverIp,
            serverPort = serverPort,
            rtpPort = rtpPort + 2,
            sessionId = sessionId
        )

        val sipAck = buildSipAck(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq,
            userAgent = userAgent,
            clientIp = clientIp
        )

        val sipBye = buildSipBye(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq + 1,
            userAgent = userAgent,
            clientIp = clientIp
        )

        val sipByeOk = buildSipByeOk(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = "z9hG4bK${generateRandomHex(8)}",
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq + 1,
            clientIp = clientIp,
            clientPort = clientPort
        )

        val sipOptions = buildSipOptions(
            fromUser = settings.sipFromUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            userAgent = userAgent,
            clientIp = clientIp,
            clientPort = clientPort
        )

        val itime = random.nextInt(settings.itimeMin, settings.itimeMax + 1)
=======
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        )

        val sipRinging = buildSipRinging(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = branch,
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq,
            clientIp = clientIp,
            clientPort = clientPort
        )

        val sipOk = buildSip200Ok(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = branch,
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq,
            userAgent = userAgent,
            clientIp = clientIp,
            clientPort = clientPort,
            serverIp = serverIp,
            serverPort = serverPort,
            rtpPort = rtpPort + 2,
            sessionId = sessionId
        )

        val sipAck = buildSipAck(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq,
            userAgent = userAgent,
            clientIp = clientIp
        )

        val sipBye = buildSipBye(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq + 1,
            userAgent = userAgent,
            clientIp = clientIp
        )

        val sipByeOk = buildSipByeOk(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = "z9hG4bK${generateRandomHex(8)}",
            fromTag = fromTag,
            toTag = toTag,
            cseq = cseq + 1,
            clientIp = clientIp,
            clientPort = clientPort
        )

        val sipOptions = buildSipOptions(
            fromUser = settings.sipFromUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            userAgent = userAgent,
            clientIp = clientIp,
            clientPort = clientPort
        )

        val itime = Random.nextInt(settings.itimeMin, settings.itimeMax + 1)
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1

        return MimicResult(
            i1 = "<b 0x${sipInvite}>",
            i2 = "<b 0x${sipTrying}>",
            i3 = "<b 0x${sipRinging}>",
            i4 = "<b 0x${sipOk}>",
            i5 = "<b 0x${sipAck}>",
            j1 = "<b 0x${sipBye}>",
            j2 = "<b 0x${sipByeOk}>",
            j3 = "<b 0x${sipOptions}>",
            itime = itime.toString()
        )
    }

    fun generate(settings: MimicSettings): MimicResult {
        return when (settings.type) {
            MimicType.DNS -> generateDnsMimic(settings)
            MimicType.QUIC -> generateQuicMimic(settings)
            MimicType.SIP -> generateSipMimic(settings)
        }
    }

    private fun buildDnsQueryWithEdns(domain: String, transactionId: Int, queryType: String): String {
        val sb = StringBuilder()

        sb.append(String.format("%04x", transactionId))
        val flags = listOf("0100", "0120", "0100", "0110").secureRandom()
        sb.append(flags)
        sb.append("0001")
        sb.append("0000")
        sb.append("0000")
        sb.append("0001")

        encodeDomainName(sb, domain)
        sb.append(queryType)
        sb.append("0001")

        sb.append("00")
        sb.append("0029")
        sb.append("1000")
        sb.append("0000")
        sb.append("8000")

        val ednsOptions = mutableListOf<String>()

        val clientCookie = generateRandomHex(8)
        val cookieOption = "000a0008$clientCookie"
        ednsOptions.add(cookieOption)

        if (random.nextBoolean()) {
            val paddingLen = random.nextInt(12, 64)
            val paddingOption = "000c${String.format("%04x", paddingLen)}${"00".repeat(paddingLen)}"
            ednsOptions.add(paddingOption)
        }

        if (random.nextBoolean()) {
            val subnetOption = "0008000701${generateRandomHex(3)}00"
            ednsOptions.add(subnetOption)
        }

        ednsOptions.shuffle()
        val rdataContent = ednsOptions.joinToString("")
        sb.append(String.format("%04x", rdataContent.length / 2))
        sb.append(rdataContent)

        return sb.toString()
    }

    private fun buildDnsResponse(domain: String, transactionId: Int): String {
        val sb = StringBuilder()

        sb.append(String.format("%04x", transactionId))
        val responseFlags = listOf("8180", "8580", "8180").secureRandom()
        sb.append(responseFlags)
        sb.append("0001")
        val answerCount = random.nextInt(1, 4)
        sb.append(String.format("%04x", answerCount))
        sb.append("0000")
        sb.append("0001")

        encodeDomainName(sb, domain)
        sb.append(DNS_TYPE_A)
        sb.append("0001")

        for (i in 0 until answerCount) {
            sb.append("c00c")
            sb.append(DNS_TYPE_A)
            sb.append("0001")
            sb.append(String.format("%08x", random.nextInt(60, 7200)))
            sb.append("0004")
            sb.append(String.format("%02x", random.nextInt(1, 255)))
            sb.append(String.format("%02x", random.nextInt(0, 255)))
            sb.append(String.format("%02x", random.nextInt(0, 255)))
            sb.append(String.format("%02x", random.nextInt(1, 255)))
        }

        sb.append("00")
        sb.append("0029")
        sb.append("1000")
        sb.append("0000")
        sb.append("8000")

        val responseEdns = mutableListOf<String>()
        val serverCookie = generateRandomHex(8) + generateRandomHex(random.nextInt(8, 16))
        val cookieOption = "000a${String.format("%04x", serverCookie.length / 2)}$serverCookie"
        responseEdns.add(cookieOption)

        if (random.nextBoolean()) {
            val paddingLen = random.nextInt(8, 32)
            val paddingOption = "000c${String.format("%04x", paddingLen)}${"00".repeat(paddingLen)}"
            responseEdns.add(paddingOption)
        }

        val rdataContent = responseEdns.joinToString("")
        sb.append(String.format("%04x", rdataContent.length / 2))
        sb.append(rdataContent)

        return sb.toString()
    }

    private fun encodeDomainName(sb: StringBuilder, domain: String) {
        val parts = domain.split(".")
        for (part in parts) {
            sb.append(String.format("%02x", part.length))
            for (c in part) {
                sb.append(String.format("%02x", c.code))
            }
        }
        sb.append("00")
    }

    private fun buildQuicInitialPacket(connectionId: String, packetNumber: Int, version: String): String {
        val sb = StringBuilder()
        sb.append("c1")
        when (version) {
            "1" -> sb.append("00000001")
            "2" -> sb.append("6b3343cf")
            else -> sb.append("ff000020")
        }
        sb.append(String.format("%02x", connectionId.length / 2))
        sb.append(connectionId)
        sb.append("00")
        val tokenLength = random.nextInt(0, 16)
        sb.append(String.format("%02x", tokenLength))
        sb.append(generateRandomHex(tokenLength))
        val payloadLength = random.nextInt(200, 400)
        sb.append(String.format("%04x", payloadLength or 0x4000))
        sb.append(String.format("%02x", packetNumber and 0xFF))
        sb.append("060040")
        sb.append(String.format("%02x", random.nextInt(50, 100)))
        sb.append("0303")
        sb.append(generateRandomHex(32))
        sb.append("20")
        sb.append(generateRandomHex(32))
        sb.append("1301")
        sb.append("0100")
        val extensionsLen = random.nextInt(100, 200)
        sb.append(String.format("%04x", extensionsLen))
        sb.append(generateRandomHex(extensionsLen))
        val currentLen = sb.length / 2
        val paddingNeeded = maxOf(0, payloadLength - currentLen + 10)
        sb.append("00".repeat(paddingNeeded))
        return sb.toString()
    }

    private fun buildQuicFollowUpPacket(): String {
        val sb = StringBuilder()
        sb.append("00")
        sb.append("00000001")
        sb.append("00")
        sb.append(generateRandomHex(24))
        return sb.toString()
    }

    private val SIP_USER_AGENTS = listOf(
        "Ooma/1.0",
        "Ooma/2.0.1",
        "Ooma/3.1.0",
        "Grandstream GXP2170 1.0.11.23",
        "Grandstream GXP1625 1.0.4.128",
        "Grandstream GXV3370 1.0.1.58",
        "Grandstream HT802 1.0.29.8",
        "Yealink SIP-T46S 66.86.0.15",
        "Yealink SIP-T54W 96.86.0.80",
        "Yealink SIP-T58A 58.86.0.20",
        "Yealink W60B 77.86.0.15",
        "Cisco-SIPGateway/IOS-12.x",
        "Cisco-SIPGateway/IOS-15.x",
        "Cisco/7841-3PCC-11.3.7",
        "Cisco/8845-3PCC-12.0.1",
        "Polycom/VVX-VVX_501-UA/6.3.1.8427",
        "Polycom/SoundPoint-IP_550-UA/3.3.5.0247",
        "Polycom/VVX-VVX_411-UA/6.4.0.9774",
        "Linphone/4.5.0 (belle-sip/4.5.0)",
        "Linphone/5.1.0 (belle-sip/5.2.0)",
        "Linphone Desktop/4.4.0",
        "Zoiper rv2.10.18.4",
        "Zoiper/5.5.14",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
        "Ooma/1.0",
    )

    private val SDP_CODECS = listOf(
        "0" to "PCMU/8000",
        "8" to "PCMA/8000",
        "18" to "G729/8000",
        "4" to "G723/8000",
        "9" to "G722/8000",
        "3" to "GSM/8000",
        "101" to "telephone-event/8000",
        "96" to "opus/48000/2",
        "97" to "iLBC/8000"
    )

    private val SDP_SESSION_NAMES = listOf(
        "SIP Call",
        "VoIP Session",
        "Phone Call",
        "Audio Session",
        "-",
        "SIP Media",
        "Call"
    )

    private fun generateRandomPrivateIp(): String {
<<<<<<< HEAD
<<<<<<< HEAD
        return when (random.nextInt(3)) {
            0 -> "192.168.${random.nextInt(0, 256)}.${random.nextInt(1, 255)}"
            1 -> "10.${random.nextInt(0, 256)}.${random.nextInt(0, 256)}.${random.nextInt(1, 255)}"
            else -> "172.${random.nextInt(16, 32)}.${random.nextInt(0, 256)}.${random.nextInt(1, 255)}"
=======
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        return when (Random.nextInt(3)) {
            0 -> "192.168.${Random.nextInt(0, 256)}.${Random.nextInt(1, 255)}"
            1 -> "10.${Random.nextInt(0, 256)}.${Random.nextInt(0, 256)}.${Random.nextInt(1, 255)}"
            else -> "172.${Random.nextInt(16, 32)}.${Random.nextInt(0, 256)}.${Random.nextInt(1, 255)}"
<<<<<<< HEAD
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        }
    }

    private fun buildSdpBody(
        user: String,
        domain: String,
        ip: String,
        rtpPort: Int,
        sessionId: String
    ): String {
<<<<<<< HEAD
<<<<<<< HEAD
        val codecs = SDP_CODECS.secureShuffled().take(random.nextInt(2, 6))
        val codecIds = codecs.joinToString(" ") { it.first }
        val rtpmaps = codecs.joinToString("") { "a=rtpmap:${it.first} ${it.second}\r\n" }
        val sessionName = SDP_SESSION_NAMES.secureRandom()
        val sessionVersion = random.nextLong(1L, 9999999999L).toString()
        val originHost = if (random.nextBoolean()) ip else domain
=======
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        val codecs = SDP_CODECS.shuffled().take(Random.nextInt(2, 6))
        val codecIds = codecs.joinToString(" ") { it.first }
        val rtpmaps = codecs.joinToString("") { "a=rtpmap:${it.first} ${it.second}\r\n" }
        val sessionName = SDP_SESSION_NAMES.random()
        val sessionVersion = Random.nextLong(1L, 9999999999L).toString()
        val originHost = if (Random.nextBoolean()) ip else domain
<<<<<<< HEAD
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1

        return buildString {
            append("v=0\r\n")
            append("o=$user $sessionId $sessionVersion IN IP4 $originHost\r\n")
            append("s=$sessionName\r\n")
            append("c=IN IP4 $ip\r\n")
            append("t=0 0\r\n")
            append("m=audio $rtpPort RTP/AVP $codecIds\r\n")
            append(rtpmaps)
<<<<<<< HEAD
<<<<<<< HEAD
            if (random.nextBoolean()) {
                append("a=fmtp:101 0-16\r\n")
            }
            append("a=sendrecv\r\n")
            if (random.nextBoolean()) {
                append("a=ptime:${listOf(10, 20, 30, 40).secureRandom()}\r\n")
            }
            if (random.nextInt(3) == 0) {
                append("a=maxptime:150\r\n")
            }
            if (random.nextInt(4) == 0) {
=======
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
            if (Random.nextBoolean()) {
                append("a=fmtp:101 0-16\r\n")
            }
            append("a=sendrecv\r\n")
            if (Random.nextBoolean()) {
                append("a=ptime:${listOf(10, 20, 30, 40).random()}\r\n")
            }
            if (Random.nextInt(3) == 0) {
                append("a=maxptime:150\r\n")
            }
            if (Random.nextInt(4) == 0) {
<<<<<<< HEAD
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
                append("a=rtcp:${rtpPort + 1}\r\n")
            }
        }
    }

    private val SIP_ALLOW_METHODS = listOf(
        "INVITE, ACK, CANCEL, BYE, OPTIONS, INFO, REFER, NOTIFY",
        "INVITE, ACK, CANCEL, BYE, OPTIONS, NOTIFY, REFER, SUBSCRIBE, INFO, MESSAGE",
        "INVITE, ACK, BYE, CANCEL, OPTIONS, NOTIFY, REFER",
        "INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, SUBSCRIBE, NOTIFY, INFO, MESSAGE, PRACK, UPDATE",
        "INVITE, ACK, BYE, CANCEL, OPTIONS, INFO, SUBSCRIBE, NOTIFY, REFER, MESSAGE"
    )

    private val SIP_SUPPORTED = listOf(
        "replaces, timer",
        "replaces, 100rel, timer",
        "replaces, norefersub, timer",
        "100rel, replaces, timer, norefersub",
        "replaces",
        "timer, replaces, path, gruu"
    )

    private fun buildSipInvite(
        fromUser: String,
        toUser: String,
        fromDomain: String,
        toDomain: String,
        callId: String,
        branch: String,
        fromTag: String,
        cseq: Int,
        userAgent: String,
        clientIp: String,
        clientPort: Int,
        rtpPort: Int,
        sessionId: String
    ): String {
        val sdpBody = buildSdpBody(fromUser, fromDomain, clientIp, rtpPort, sessionId)
        val headers = mutableListOf<String>()
        headers.add("INVITE sip:$toUser@$toDomain SIP/2.0")
        headers.add("Via: SIP/2.0/UDP $clientIp:$clientPort;branch=$branch;rport")
<<<<<<< HEAD
<<<<<<< HEAD
        headers.add("Max-Forwards: ${random.nextInt(68, 71)}")
=======
        headers.add("Max-Forwards: ${Random.nextInt(68, 71)}")
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
        headers.add("Max-Forwards: ${Random.nextInt(68, 71)}")
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        headers.add("From: \"$fromUser\" <sip:$fromUser@$fromDomain>;tag=$fromTag")
        headers.add("To: <sip:$toUser@$toDomain>")
        headers.add("Call-ID: $callId@$fromDomain")
        headers.add("CSeq: $cseq INVITE")
        headers.add("Contact: <sip:$fromUser@$clientIp:$clientPort>")
        headers.add("User-Agent: $userAgent")
<<<<<<< HEAD
<<<<<<< HEAD
        headers.add("Allow: ${SIP_ALLOW_METHODS.secureRandom()}")
        headers.add("Supported: ${SIP_SUPPORTED.secureRandom()}")
        if (random.nextInt(3) == 0) {
            headers.add("Session-Expires: ${random.nextInt(1800, 3600)};refresher=uac")
        }
        if (random.nextInt(4) == 0) {
=======
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        headers.add("Allow: ${SIP_ALLOW_METHODS.random()}")
        headers.add("Supported: ${SIP_SUPPORTED.random()}")
        if (Random.nextInt(3) == 0) {
            headers.add("Session-Expires: ${Random.nextInt(1800, 3600)};refresher=uac")
        }
        if (Random.nextInt(4) == 0) {
<<<<<<< HEAD
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
            headers.add("Min-SE: 90")
        }
        headers.add("Content-Type: application/sdp")
        headers.add("Content-Length: ${sdpBody.length}")

        return (headers.joinToString("\r\n") + "\r\n\r\n" + sdpBody).toHex()
    }

    private fun buildSipTrying(
        fromUser: String,
        toUser: String,
        fromDomain: String,
        toDomain: String,
        callId: String,
        branch: String,
        fromTag: String,
        cseq: Int,
        clientIp: String,
        clientPort: Int
    ): String {
        val headers = mutableListOf<String>()
        headers.add("SIP/2.0 100 Trying")
        headers.add("Via: SIP/2.0/UDP $clientIp:$clientPort;branch=$branch;received=$clientIp;rport=$clientPort")
        headers.add("From: \"$fromUser\" <sip:$fromUser@$fromDomain>;tag=$fromTag")
        headers.add("To: <sip:$toUser@$toDomain>")
        headers.add("Call-ID: $callId@$fromDomain")
        headers.add("CSeq: $cseq INVITE")
        headers.add("Content-Length: 0")

        return (headers.joinToString("\r\n") + "\r\n\r\n").toHex()
    }

    private fun buildSipRinging(
        fromUser: String,
        toUser: String,
        fromDomain: String,
        toDomain: String,
        callId: String,
        branch: String,
        fromTag: String,
        toTag: String,
        cseq: Int,
        clientIp: String,
        clientPort: Int
    ): String {
        val headers = mutableListOf<String>()
        headers.add("SIP/2.0 180 Ringing")
        headers.add("Via: SIP/2.0/UDP $clientIp:$clientPort;branch=$branch;received=$clientIp;rport=$clientPort")
        headers.add("From: \"$fromUser\" <sip:$fromUser@$fromDomain>;tag=$fromTag")
        headers.add("To: <sip:$toUser@$toDomain>;tag=$toTag")
        headers.add("Call-ID: $callId@$fromDomain")
        headers.add("CSeq: $cseq INVITE")
        headers.add("Contact: <sip:$toUser@$toDomain>")
<<<<<<< HEAD
<<<<<<< HEAD
        if (random.nextInt(4) == 0) {
=======
        if (Random.nextInt(4) == 0) {
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
        if (Random.nextInt(4) == 0) {
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
            headers.add("Require: 100rel")
        }
        headers.add("Content-Length: 0")

        return (headers.joinToString("\r\n") + "\r\n\r\n").toHex()
    }

    private fun buildSip200Ok(
        fromUser: String,
        toUser: String,
        fromDomain: String,
        toDomain: String,
        callId: String,
        branch: String,
        fromTag: String,
        toTag: String,
        cseq: Int,
        userAgent: String,
        clientIp: String,
        clientPort: Int,
        serverIp: String,
        serverPort: Int,
        rtpPort: Int,
        sessionId: String
    ): String {
        val sdpBody = buildSdpBody(toUser, toDomain, serverIp, rtpPort, sessionId)
        val headers = mutableListOf<String>()
        headers.add("SIP/2.0 200 OK")
        headers.add("Via: SIP/2.0/UDP $clientIp:$clientPort;branch=$branch;received=$clientIp;rport=$clientPort")
        headers.add("From: \"$fromUser\" <sip:$fromUser@$fromDomain>;tag=$fromTag")
        headers.add("To: <sip:$toUser@$toDomain>;tag=$toTag")
        headers.add("Call-ID: $callId@$fromDomain")
        headers.add("CSeq: $cseq INVITE")
        headers.add("Contact: <sip:$toUser@$serverIp:$serverPort>")
        headers.add("User-Agent: $userAgent")
<<<<<<< HEAD
<<<<<<< HEAD
        headers.add("Allow: ${SIP_ALLOW_METHODS.secureRandom()}")
        headers.add("Supported: ${SIP_SUPPORTED.secureRandom()}")
        if (random.nextInt(3) == 0) {
            headers.add("Session-Expires: ${random.nextInt(1800, 3600)};refresher=uas")
        }
        if (random.nextInt(5) == 0) {
=======
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        headers.add("Allow: ${SIP_ALLOW_METHODS.random()}")
        headers.add("Supported: ${SIP_SUPPORTED.random()}")
        if (Random.nextInt(3) == 0) {
            headers.add("Session-Expires: ${Random.nextInt(1800, 3600)};refresher=uas")
        }
        if (Random.nextInt(5) == 0) {
<<<<<<< HEAD
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
            headers.add("Server: $userAgent")
        }
        headers.add("Content-Type: application/sdp")
        headers.add("Content-Length: ${sdpBody.length}")

        return (headers.joinToString("\r\n") + "\r\n\r\n" + sdpBody).toHex()
    }

    private fun buildSipAck(
        fromUser: String,
        toUser: String,
        fromDomain: String,
        toDomain: String,
        callId: String,
        fromTag: String,
        toTag: String,
        cseq: Int,
        userAgent: String,
        clientIp: String
    ): String {
        val branch = "z9hG4bK${generateRandomHex(8)}"
        val headers = mutableListOf<String>()
        headers.add("ACK sip:$toUser@$toDomain SIP/2.0")
        headers.add("Via: SIP/2.0/UDP $clientIp;branch=$branch;rport")
<<<<<<< HEAD
<<<<<<< HEAD
        headers.add("Max-Forwards: ${random.nextInt(68, 71)}")
=======
        headers.add("Max-Forwards: ${Random.nextInt(68, 71)}")
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
        headers.add("Max-Forwards: ${Random.nextInt(68, 71)}")
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        headers.add("From: \"$fromUser\" <sip:$fromUser@$fromDomain>;tag=$fromTag")
        headers.add("To: <sip:$toUser@$toDomain>;tag=$toTag")
        headers.add("Call-ID: $callId@$fromDomain")
        headers.add("CSeq: $cseq ACK")
        headers.add("User-Agent: $userAgent")
        headers.add("Content-Length: 0")

        return (headers.joinToString("\r\n") + "\r\n\r\n").toHex()
    }

    private fun buildSipBye(
        fromUser: String,
        toUser: String,
        fromDomain: String,
        toDomain: String,
        callId: String,
        fromTag: String,
        toTag: String,
        cseq: Int,
        userAgent: String,
        clientIp: String
    ): String {
        val branch = "z9hG4bK${generateRandomHex(8)}"
        val headers = mutableListOf<String>()
        headers.add("BYE sip:$toUser@$toDomain SIP/2.0")
        headers.add("Via: SIP/2.0/UDP $clientIp;branch=$branch;rport")
<<<<<<< HEAD
<<<<<<< HEAD
        headers.add("Max-Forwards: ${random.nextInt(68, 71)}")
=======
        headers.add("Max-Forwards: ${Random.nextInt(68, 71)}")
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
        headers.add("Max-Forwards: ${Random.nextInt(68, 71)}")
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        headers.add("From: \"$fromUser\" <sip:$fromUser@$fromDomain>;tag=$fromTag")
        headers.add("To: <sip:$toUser@$toDomain>;tag=$toTag")
        headers.add("Call-ID: $callId@$fromDomain")
        headers.add("CSeq: $cseq BYE")
        headers.add("User-Agent: $userAgent")
<<<<<<< HEAD
<<<<<<< HEAD
        if (random.nextInt(3) == 0) {
=======
        if (Random.nextInt(3) == 0) {
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
        if (Random.nextInt(3) == 0) {
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
            headers.add("Reason: Q.850;cause=16;text=\"Normal call clearing\"")
        }
        headers.add("Content-Length: 0")

        return (headers.joinToString("\r\n") + "\r\n\r\n").toHex()
    }

    private fun buildSipByeOk(
        fromUser: String,
        toUser: String,
        fromDomain: String,
        toDomain: String,
        callId: String,
        branch: String,
        fromTag: String,
        toTag: String,
        cseq: Int,
        clientIp: String,
        clientPort: Int
    ): String {
        val headers = mutableListOf<String>()
        headers.add("SIP/2.0 200 OK")
        headers.add("Via: SIP/2.0/UDP $clientIp:$clientPort;branch=$branch;received=$clientIp;rport=$clientPort")
        headers.add("From: \"$fromUser\" <sip:$fromUser@$fromDomain>;tag=$fromTag")
        headers.add("To: <sip:$toUser@$toDomain>;tag=$toTag")
        headers.add("Call-ID: $callId@$fromDomain")
        headers.add("CSeq: $cseq BYE")
        headers.add("Content-Length: 0")

        return (headers.joinToString("\r\n") + "\r\n\r\n").toHex()
    }

    private fun buildSipOptions(
        fromUser: String,
        fromDomain: String,
        toDomain: String,
        userAgent: String,
        clientIp: String,
        clientPort: Int
    ): String {
        val branch = "z9hG4bK${generateRandomHex(8)}"
<<<<<<< HEAD
<<<<<<< HEAD
        val tag = random.nextInt(100000000, 999999999).toString()
        val callId = generateRandomHex(16)
        val cseq = random.nextInt(1, 999999)
=======
        val tag = Random.nextInt(100000000, 999999999).toString()
        val callId = generateRandomHex(16)
        val cseq = Random.nextInt(1, 999999)
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
        val tag = Random.nextInt(100000000, 999999999).toString()
        val callId = generateRandomHex(16)
        val cseq = Random.nextInt(1, 999999)
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1

        val headers = mutableListOf<String>()
        headers.add("OPTIONS sip:$toDomain SIP/2.0")
        headers.add("Via: SIP/2.0/UDP $clientIp:$clientPort;branch=$branch;rport")
<<<<<<< HEAD
<<<<<<< HEAD
        headers.add("Max-Forwards: ${random.nextInt(68, 71)}")
=======
        headers.add("Max-Forwards: ${Random.nextInt(68, 71)}")
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
=======
        headers.add("Max-Forwards: ${Random.nextInt(68, 71)}")
>>>>>>> e7670f2817fc4e06e914b6d572cec218d6e90ce1
        headers.add("From: <sip:$fromUser@$fromDomain>;tag=$tag")
        headers.add("To: <sip:$toDomain>")
        headers.add("Call-ID: $callId@$fromDomain")
        headers.add("CSeq: $cseq OPTIONS")
        headers.add("Contact: <sip:$fromUser@$clientIp:$clientPort>")
        headers.add("User-Agent: $userAgent")
        headers.add("Accept: application/sdp")
        headers.add("Content-Length: 0")

        return (headers.joinToString("\r\n") + "\r\n\r\n").toHex()
    }

    private fun generateRandomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        random.nextBytes(bytes)
        return bytes.joinToString("") { String.format("%02x", it) }
    }

    private fun String.toHex(): String {
        return this.toByteArray(Charsets.UTF_8).joinToString("") { String.format("%02x", it) }
    }
}

data class MimicResult(
    val i1: String,
    val i2: String,
    val i3: String,
    val i4: String,
    val i5: String,
    val j1: String,
    val j2: String,
    val j3: String,
    val itime: String
)
