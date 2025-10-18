package com.ix7.tracker.protocol

import android.util.Log

class FrameAssembler {
    private val buffer = mutableListOf<Byte>()
    private val TAG = "FrameAssembler"

    fun addData(chunk: ByteArray): List<ByteArray> {
        buffer.addAll(chunk.toList())

        val completeFrames = mutableListOf<ByteArray>()

        while (buffer.size >= 4) {
            // Chercher le premier header 0x61 0x9E
            var headerIdx = findNextHeader(0)
            if (headerIdx == -1) {
                buffer.clear()
                break
            }

            // Supprimer avant le header
            if (headerIdx > 0) {
                repeat(headerIdx) { buffer.removeAt(0) }
            }

            // Chercher le PROCHAIN header (ou la fin si CA trouvé avant)
            var frameEndIdx = -1

            // Stratégie: chercher soit le prochain 61 9E, soit un CA
            for (i in 2 until buffer.size) {
                if (buffer[i] == 0xCA.toByte()) {
                    // Trouvé un CA, c'est probablement la fin
                    frameEndIdx = i + 1

                    // Vérifier s'il y a un 61 9E avant ce CA
                    if (i + 1 < buffer.size &&
                        i + 2 < buffer.size &&
                        buffer[i + 1] == 0x61.toByte() &&
                        buffer[i + 2] == 0x9E.toByte()) {
                        // Oui, donc c'est le bon CA
                        break
                    } else if (i == buffer.size - 1) {
                        // CA est le dernier byte du buffer, probablement la fin
                        break
                    }
                    frameEndIdx = -1  // Réinitialiser et continuer
                }

                // Chercher le prochain header
                if (i + 1 < buffer.size &&
                    buffer[i] == 0x61.toByte() &&
                    buffer[i + 1] == 0x9E.toByte()) {
                    frameEndIdx = i
                    break
                }
            }

            if (frameEndIdx == -1) {
                // Pas de fin de frame trouvée
                break
            }

            // Extraire la frame
            val frame = buffer.subList(0, frameEndIdx).toByteArray()
            Log.d(TAG, "✅ Frame de ${frame.size} bytes: ${frame.joinToString(" ") { "%02X".format(it) }}")
            completeFrames.add(frame)

            // Supprimer du buffer
            repeat(frameEndIdx) { buffer.removeAt(0) }
        }

        return completeFrames
    }

    private fun findNextHeader(startIdx: Int): Int {
        for (i in startIdx until buffer.size - 1) {
            if (buffer[i] == 0x61.toByte() && buffer[i + 1] == 0x9E.toByte()) {
                return i
            }
        }
        return -1
    }
}