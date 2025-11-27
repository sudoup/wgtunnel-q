package com.zaneschepke.wireguardautotunnel.ui.state

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

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

object MimicGenerator {

    fun generateDnsMimic(settings: MimicSettings): MimicResult {
        val transactionId = Random.nextInt(0x0001, 0xFFFF)
        val dnsQuery = buildDnsQuery(settings.domain, transactionId)
        val itime = Random.nextInt(settings.itimeMin, settings.itimeMax + 1)

        return MimicResult(
            i1 = "<b 0x${dnsQuery}>",
            i2 = "",
            i3 = "",
            i4 = "",
            i5 = "",
            j1 = "",
            j2 = "",
            j3 = "",
            itime = itime.toString()
        )
    }

    fun generateQuicMimic(settings: MimicSettings): MimicResult {
        val connectionId = generateRandomHex(8)
        val packetNumber = Random.nextInt(0, 0xFFFFFF)
        val quicInitial = buildQuicInitialPacket(connectionId, packetNumber, settings.quicVersion)
        val quicFollowUp = buildQuicFollowUpPacket()
        val junkData = generateRandomHex(8)
        val itime = Random.nextInt(settings.itimeMin, settings.itimeMax + 1)

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
        val tag = Random.nextInt(100000000, 999999999).toString()
        val cseq = Random.nextInt(1, 999999)

        val sipInvite = buildSipInvite(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = branch,
            tag = tag,
            cseq = cseq
        )

        val sipTrying = buildSipTrying(
            fromUser = settings.sipFromUser,
            toUser = settings.sipToUser,
            fromDomain = settings.sipFromDomain,
            toDomain = settings.sipToDomain,
            callId = callId,
            branch = branch,
            tag = tag,
            cseq = cseq
        )

        val junkData = generateRandomHex(8)
        val itime = Random.nextInt(settings.itimeMin, settings.itimeMax + 1)

        return MimicResult(
            i1 = "<b 0x${sipInvite}>",
            i2 = "<b 0x${sipTrying}>",
            i3 = "",
            i4 = "",
            i5 = "",
            j1 = "<b 0x${junkData}>",
            j2 = "",
            j3 = "",
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

    private fun buildDnsQuery(domain: String, transactionId: Int): String {
        val sb = StringBuilder()
        sb.append(String.format("%04x", transactionId))
        sb.append("01000001000000000000")
        val parts = domain.split(".")
        for (part in parts) {
            sb.append(String.format("%02x", part.length))
            for (c in part) {
                sb.append(String.format("%02x", c.code))
            }
        }
        sb.append("00")
        sb.append("0001")
        sb.append("0001")
        return sb.toString()
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
        val tokenLength = Random.nextInt(0, 16)
        sb.append(String.format("%02x", tokenLength))
        sb.append(generateRandomHex(tokenLength))
        val payloadLength = Random.nextInt(200, 400)
        sb.append(String.format("%04x", payloadLength or 0x4000))
        sb.append(String.format("%02x", packetNumber and 0xFF))
        sb.append("060040")
        sb.append(String.format("%02x", Random.nextInt(50, 100)))
        sb.append("0303")
        sb.append(generateRandomHex(32))
        sb.append("20")
        sb.append(generateRandomHex(32))
        sb.append("1301")
        sb.append("0100")
        val extensionsLen = Random.nextInt(100, 200)
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

    private fun buildSipInvite(
        fromUser: String,
        toUser: String,
        fromDomain: String,
        toDomain: String,
        callId: String,
        branch: String,
        tag: String,
        cseq: Int
    ): String {
        val sipMessage = StringBuilder()
        sipMessage.append("INVITE sip:$toUser@$toDomain SIP/2.0\r\n")
        sipMessage.append("Via: SIP/2.0/UDP $fromDomain;branch=$branch\r\n")
        sipMessage.append("Max-Forwards: 70\r\n")
        sipMessage.append("To: <sip:$toUser@$toDomain>\r\n")
        sipMessage.append("From: <sip:$fromUser@$fromDomain>;tag=$tag\r\n")
        sipMessage.append("Call-ID: $callId@$fromDomain\r\n")
        sipMessage.append("CSeq: $cseq INVITE\r\n")
        sipMessage.append("Contact: <sip:$fromUser@$fromDomain>\r\n")
        sipMessage.append("Content-Type: application/sdp\r\n")
        sipMessage.append("Content-Length: 0\r\n")
        sipMessage.append("\r\n")
        return sipMessage.toString().toHex()
    }

    private fun buildSipTrying(
        fromUser: String,
        toUser: String,
        fromDomain: String,
        toDomain: String,
        callId: String,
        branch: String,
        tag: String,
        cseq: Int
    ): String {
        val sipMessage = StringBuilder()
        sipMessage.append("SIP/2.0 100 Trying\r\n")
        sipMessage.append("Via: SIP/2.0/UDP $fromDomain;branch=$branch\r\n")
        sipMessage.append("To: <sip:$toUser@$toDomain>\r\n")
        sipMessage.append("From: <sip:$fromUser@$fromDomain>;tag=$tag\r\n")
        sipMessage.append("Call-ID: $callId@$fromDomain\r\n")
        sipMessage.append("CSeq: $cseq INVITE\r\n")
        sipMessage.append("Content-Length: 0\r\n")
        sipMessage.append("\r\n")
        return sipMessage.toString().toHex()
    }

    private fun generateRandomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        Random.nextBytes(bytes)
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
