package com.zaneschepke.wireguardautotunnel.ui.state

import com.wireguard.config.Interface
import com.zaneschepke.wireguardautotunnel.util.extensions.ifNotBlank
import com.zaneschepke.wireguardautotunnel.util.extensions.joinAndTrim
import com.zaneschepke.wireguardautotunnel.util.extensions.toTrimmedList
import java.util.*

data class InterfaceProxy(
    val privateKey: String = "",
    val publicKey: String = "",
    val addresses: String = "",
    val dnsServers: String = "",
    val listenPort: String = "",
    val mtu: String = "",
    val includedApplications: Set<String> = emptySet(),
    val excludedApplications: Set<String> = emptySet(),
    val junkPacketCount: String = "",
    val junkPacketMinSize: String = "",
    val junkPacketMaxSize: String = "",
    val initPacketJunkSize: String = "",
    val responsePacketJunkSize: String = "",
    val initPacketMagicHeader: String = "",
    val responsePacketMagicHeader: String = "",
    val underloadPacketMagicHeader: String = "",
    val transportPacketMagicHeader: String = "",
    val i1: String = "",
    val i2: String = "",
    val i3: String = "",
    val i4: String = "",
    val i5: String = "",
    val j1: String = "",
    val j2: String = "",
    val j3: String = "",
    val itime: String = "",
    val preUp: String = "",
    val postUp: String = "",
    val preDown: String = "",
    val postDown: String = "",
) {

    fun toWgInterface(): Interface {
        return Interface.Builder()
            .apply {
                parseAddresses(addresses)
                parsePrivateKey(privateKey)
                dnsServers.ifNotBlank { parseDnsServers(it) }
                listenPort.ifNotBlank { parseListenPort(it) }
                mtu.ifNotBlank { parseMtu(it) }
                includeApplications(includedApplications)
                excludeApplications(excludedApplications)
                preUp.toTrimmedList().forEach { parsePreUp(it) }
                postUp.toTrimmedList().forEach { parsePostUp(it) }
                preDown.toTrimmedList().forEach { parsePreDown(it) }
                postDown.toTrimmedList().forEach { parsePostDown(it) }
            }
            .build()
    }

    fun isAmneziaEnabled(): Boolean {
        return listOf(
                junkPacketCount,
                junkPacketMinSize,
                junkPacketMaxSize,
                initPacketJunkSize,
                responsePacketJunkSize,
                initPacketMagicHeader,
                responsePacketMagicHeader,
                underloadPacketMagicHeader,
                transportPacketMagicHeader,
                i1,
                i2,
                i3,
                i4,
                i5,
                j1,
                j2,
                j3,
                itime,
            )
            .any { it.isNotBlank() }
    }

    fun toAmneziaCompatibilityConfig(): InterfaceProxy {
        return copy(
            junkPacketCount = "4",
            junkPacketMinSize = "40",
            junkPacketMaxSize = "70",
            initPacketJunkSize = "0",
            responsePacketJunkSize = "0",
            initPacketMagicHeader = "1",
            responsePacketMagicHeader = "2",
            underloadPacketMagicHeader = "3",
            transportPacketMagicHeader = "4",
            i1 = "",
            i2 = "",
            i3 = "",
            i4 = "",
            i5 = "",
            j1 = "",
            j2 = "",
            j3 = "",
            itime = "",
        )
    }

    fun resetAmneziaProperties(): InterfaceProxy {
        return copy(
            junkPacketCount = "",
            junkPacketMinSize = "",
            junkPacketMaxSize = "",
            initPacketJunkSize = "",
            responsePacketJunkSize = "",
            initPacketMagicHeader = "",
            responsePacketMagicHeader = "",
            underloadPacketMagicHeader = "",
            transportPacketMagicHeader = "",
            i1 = "",
            i2 = "",
            i3 = "",
            i4 = "",
            i5 = "",
            j1 = "",
            j2 = "",
            j3 = "",
            itime = "",
        )
    }

    // TODO fix this later when we get amnezia to properly return 0
    fun isAmneziaCompatibilityModeSet(): Boolean {
        return (initPacketJunkSize.toIntOrNull() ?: 0) == 0 &&
            (responsePacketJunkSize.toIntOrNull() ?: 0) == 0 &&
            initPacketMagicHeader.toLongOrNull() == 1L &&
            responsePacketMagicHeader.toLongOrNull() == 2L &&
            underloadPacketMagicHeader.toLongOrNull() == 3L &&
            transportPacketMagicHeader.toLongOrNull() == 4L
    }

    fun isCompatibleWithStandardWireGuard(): Boolean {
        return isAmneziaCompatibilityModeSet()
    }

    fun toAmInterface(): org.amnezia.awg.config.Interface {
        return org.amnezia.awg.config.Interface.Builder()
            .apply {
                parseAddresses(addresses)
                parsePrivateKey(privateKey)
                dnsServers.ifNotBlank { parseDnsServers(it) }
                listenPort.ifNotBlank { parseListenPort(it) }
                mtu.ifNotBlank { parseMtu(it) }
                includeApplications(includedApplications)
                excludeApplications(excludedApplications)
                preUp.toTrimmedList().forEach { parsePreUp(it) }
                postUp.toTrimmedList().forEach { parsePostUp(it) }
                preDown.toTrimmedList().forEach { parsePreDown(it) }
                postDown.toTrimmedList().forEach { parsePostDown(it) }
                junkPacketCount.ifNotBlank { parseJunkPacketCount(it) }
                junkPacketMinSize.ifNotBlank { parseJunkPacketMinSize(it) }
                junkPacketMaxSize.ifNotBlank { parseJunkPacketMaxSize(it) }
                initPacketJunkSize.ifNotBlank { parseInitPacketJunkSize(it) }
                responsePacketJunkSize.ifNotBlank { parseResponsePacketJunkSize(it) }
                initPacketMagicHeader.ifNotBlank { parseInitPacketMagicHeader(it) }
                responsePacketMagicHeader.ifNotBlank { parseResponsePacketMagicHeader(it) }
                underloadPacketMagicHeader.ifNotBlank { parseUnderloadPacketMagicHeader(it) }
                transportPacketMagicHeader.ifNotBlank { parseTransportPacketMagicHeader(it) }
                i1.ifNotBlank { parseI1(it) }
                i2.ifNotBlank { parseI2(it) }
                i3.ifNotBlank { parseI3(it) }
                i4.ifNotBlank { parseI4(it) }
                i5.ifNotBlank { parseI5(it) }
                j1.ifNotBlank { parseJ1(it) }
                j2.ifNotBlank { parseJ2(it) }
                j3.ifNotBlank { parseJ3(it) }
                itime.ifNotBlank { parseItime(it) }
            }
            .build()
    }

    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (privateKey.isBlank()) {
            errors.add("Private key is required")
        } else if (!isValidBase64(privateKey) || privateKey.length != 44) {
            errors.add("Invalid private key format (must be 44-character Base64)")
        }

        // Addresses validation (basic)
        if (addresses.isBlank()) {
            errors.add("Addresses are required")
        } // More detailed CIDR validation can be added if needed

        listenPort.ifNotBlank {
            val port = it.toIntOrNull()
            if (port == null) errors.add("Listen port must be an integer")
            else if (port !in 1..65535) errors.add("Listen port must be between 1 and 65535")
        }

        mtu.ifNotBlank {
            val mtuValue = it.toIntOrNull()
            if (mtuValue == null) errors.add("MTU must be an integer")
            else if (mtuValue !in 576..9200) errors.add("MTU should be between 576 and 9200")
        }

        junkPacketCount.ifNotBlank {
            val count = it.toIntOrNull()
            if (count == null) errors.add("Junk packet count must be an integer")
            else if (count !in 1..128) errors.add("Junk packet count must be between 0 and 128")
        }

        junkPacketMinSize.ifNotBlank {
            val min = it.toIntOrNull()
            if (min == null) errors.add("Junk packet min size must be an integer")
            else if (min !in 1..1279) errors.add("Junk packet min size must be between 1 and 1279")
        }

        junkPacketMaxSize.ifNotBlank {
            val max = it.toIntOrNull()
            if (max == null) errors.add("Junk packet max size must be an integer")
            else if (max !in 2..1280) errors.add("Junk packet max size must be between 2 and 1280")
        }

        if (junkPacketMinSize.isNotBlank() && junkPacketMaxSize.isNotBlank()) {
            val min = junkPacketMinSize.toIntOrNull() ?: 0
            val max = junkPacketMaxSize.toIntOrNull() ?: 0
            if (min >= max) errors.add("Junk packet min size must be less than max size")
        }

        initPacketJunkSize.ifNotBlank {
            val size = it.toIntOrNull()
            if (size == null) errors.add("Init packet junk size must be an integer")
            else if (size !in 0..64) errors.add("Init packet junk size must be between 0 and 64")
        }

        responsePacketJunkSize.ifNotBlank {
            val size = it.toIntOrNull()
            if (size == null) errors.add("Response packet junk size must be an integer")
            else if (size !in 0..64)
                errors.add("Response packet junk size must be between 0 and 64")
        }

        val h1 = initPacketMagicHeader.toIntOrNull()
        val h2 = responsePacketMagicHeader.toIntOrNull()
        val h3 = underloadPacketMagicHeader.toIntOrNull()
        val h4 = transportPacketMagicHeader.toIntOrNull()
        val headers = listOf(h1, h2, h3, h4)
        if (headers.any { it != null }) {
            if (headers.any { it == null })
                errors.add("All magic headers must be set if any is set")
            else {
                val hs = headers.filterNotNull()
                if (hs.any { it !in 1..4 }) errors.add("Magic headers must be between 1 and 4")
                if (hs.distinct().size != 4)
                    errors.add("Magic headers must be a unique permutation of 1-4")
            }
        }

        fun validateHexBlob(field: String, name: String) {
            if (field.isNotBlank() && !field.matches(Regex("^<b 0x[0-9a-fA-F]+>$"))) {
                errors.add("$name must be in format <b 0xHEXSTRING>")
            }
        }

        validateHexBlob(i1, "i1")
        validateHexBlob(i2, "i2")
        validateHexBlob(i3, "i3")
        validateHexBlob(i4, "i4")
        validateHexBlob(i5, "i5")
        validateHexBlob(j1, "j1")
        validateHexBlob(j2, "j2")
        validateHexBlob(j3, "j3")

        itime.ifNotBlank {
            val time = it.toIntOrNull()
            if (time == null) errors.add("itime must be an integer")
            else if (time < 0) errors.add("itime must be non-negative")
        }

        return errors
    }

    fun setQuicMimic(): InterfaceProxy {
        val result = MimicGenerator.generateQuicMimic(MimicSettings.defaultQuic())
        return applyMimicResult(result)
    }

    fun setDnsMimic(): InterfaceProxy {
        val result = MimicGenerator.generateDnsMimic(MimicSettings.defaultDns())
        return applyMimicResult(result)
    }

    fun setSipMimic(): InterfaceProxy {
        val result = MimicGenerator.generateSipMimic(MimicSettings.defaultSip())
        return applyMimicResult(result)
    }

    fun setMimicFromSettings(settings: MimicSettings): InterfaceProxy {
        val result = MimicGenerator.generate(settings)
        return applyMimicResult(result)
    }

    fun applyMimicResult(result: MimicResult): InterfaceProxy {
        return copy(
            i1 = result.i1,
            i2 = result.i2,
            i3 = result.i3,
            i4 = result.i4,
            i5 = result.i5,
            j1 = result.j1,
            j2 = result.j2,
            j3 = result.j3,
            itime = result.itime,
        )
    }

    private fun isValidBase64(str: String): Boolean {
        return try {
            Base64.getDecoder().decode(str)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun from(i: Interface): InterfaceProxy {
            val dnsString =
                listOf(
                        i.dnsServers.joinToString(", ").replace("/", "").trim(),
                        i.dnsSearchDomains.joinAndTrim(),
                    )
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() }
            return InterfaceProxy(
                publicKey = i.keyPair.publicKey.toBase64().trim(),
                privateKey = i.keyPair.privateKey.toBase64().trim(),
                addresses = i.addresses.joinToString(", ").trim(),
                dnsServers = dnsString ?: "",
                listenPort =
                    if (i.listenPort.isPresent) i.listenPort.get().toString().trim() else "",
                mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
                includedApplications = i.includedApplications.toMutableSet(),
                excludedApplications = i.excludedApplications.toMutableSet(),
                preUp = i.preUp.joinAndTrim(),
                postUp = i.postUp.joinAndTrim(),
                preDown = i.preDown.joinAndTrim(),
                postDown = i.postDown.joinAndTrim(),
            )
        }

        fun from(i: org.amnezia.awg.config.Interface): InterfaceProxy {
            val dnsString =
                (i.dnsServers + i.dnsSearchDomains)
                    .joinToString(", ")
                    .replace("/", "")
                    .trim()
                    .takeIf { it.isNotBlank() }
            return InterfaceProxy(
                publicKey = i.keyPair.publicKey.toBase64().trim(),
                privateKey = i.keyPair.privateKey.toBase64().trim(),
                addresses = i.addresses.joinToString(", ").trim(),
                dnsServers = dnsString ?: "",
                listenPort =
                    if (i.listenPort.isPresent) i.listenPort.get().toString().trim() else "",
                mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
                includedApplications = i.includedApplications.toMutableSet(),
                excludedApplications = i.excludedApplications.toMutableSet(),
                preUp = i.preUp.joinAndTrim(),
                postUp = i.postUp.joinAndTrim(),
                preDown = i.preDown.joinAndTrim(),
                postDown = i.postDown.joinAndTrim(),
                junkPacketCount =
                    if (i.junkPacketCount.isPresent) i.junkPacketCount.get().toString() else "",
                junkPacketMinSize =
                    if (i.junkPacketMinSize.isPresent) i.junkPacketMinSize.get().toString() else "",
                junkPacketMaxSize =
                    if (i.junkPacketMaxSize.isPresent) i.junkPacketMaxSize.get().toString() else "",
                initPacketJunkSize =
                    if (i.initPacketJunkSize.isPresent) i.initPacketJunkSize.get().toString()
                    else "",
                responsePacketJunkSize =
                    if (i.responsePacketJunkSize.isPresent)
                        i.responsePacketJunkSize.get().toString()
                    else "",
                initPacketMagicHeader =
                    if (i.initPacketMagicHeader.isPresent) i.initPacketMagicHeader.get().toString()
                    else "",
                responsePacketMagicHeader =
                    if (i.responsePacketMagicHeader.isPresent)
                        i.responsePacketMagicHeader.get().toString()
                    else "",
                underloadPacketMagicHeader =
                    if (i.underloadPacketMagicHeader.isPresent)
                        i.underloadPacketMagicHeader.get().toString()
                    else "",
                transportPacketMagicHeader =
                    if (i.transportPacketMagicHeader.isPresent)
                        i.transportPacketMagicHeader.get().toString()
                    else "",
                i1 = if (i.i1.isPresent) i.i1.get() else "",
                i2 = if (i.i2.isPresent) i.i2.get() else "",
                i3 = if (i.i3.isPresent) i.i3.get() else "",
                i4 = if (i.i4.isPresent) i.i4.get() else "",
                i5 = if (i.i5.isPresent) i.i5.get() else "",
                j1 = if (i.j1.isPresent) i.j1.get() else "",
                j2 = if (i.j2.isPresent) i.j2.get() else "",
                j3 = if (i.j3.isPresent) i.j3.get() else "",
                itime = if (i.itime.isPresent) i.itime.get().toString() else "",
            )
        }
    }
}
