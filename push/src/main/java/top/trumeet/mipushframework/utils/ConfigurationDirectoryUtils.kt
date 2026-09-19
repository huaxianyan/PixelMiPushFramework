package top.trumeet.mipushframework.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import com.xiaomi.xmsf.R

/**
 * Turns a SAF tree uri into something a human can read.
 *
 * A stored uri looks like
 * `content://com.android.externalstorage.documents/tree/primary%3ASyncthing%2FConfig`,
 * the readable form of it is `Internal storage/Syncthing/Config`.
 *
 * The separator is a bare slash with no padding: this is a path being shown, not prose, so the
 * spacing rules of Chinese typography do not apply to it.
 */
object ConfigurationDirectoryUtils {

    private const val PRIMARY_VOLUME_ID = "primary"
    private const val SEPARATOR = "/"

    fun displayName(context: Context, uri: Uri?): String {
        if (uri == null) {
            return context.getString(R.string.settings_configuration_directory_not_set)
        }

        val documentId = try {
            DocumentsContract.getTreeDocumentId(uri)
        } catch (e: Exception) {
            return uri.toString()
        }
        val segments = documentId?.split('/')?.filter { it.isNotEmpty() }
        if (segments.isNullOrEmpty()) {
            return uri.toString()
        }

        // The first segment carries the volume, not the whole segment: a tree
        // document id looks like `primary:Syncthing/Config`, where `primary` names
        // the volume and `Syncthing` is the first directory below it.
        val head = segments.first()
        val colon = head.indexOf(':')
        val volumeId = if (colon >= 0) head.substring(0, colon) else head
        val headPath = if (colon >= 0) head.substring(colon + 1) else null

        val parts = mutableListOf(volumeName(context, volumeId))
        if (!headPath.isNullOrEmpty()) {
            parts.add(headPath)
        }
        parts.addAll(segments.drop(1))
        return parts.joinToString(SEPARATOR)
    }

    private fun volumeName(context: Context, volumeId: String): String {
        if (volumeId == PRIMARY_VOLUME_ID) {
            return context.getString(R.string.settings_volume_internal_storage)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return volumeId
        }
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            ?: return volumeId
        return storageManager.storageVolumes
            .firstOrNull { it.uuid?.equals(volumeId, ignoreCase = true) == true }
            ?.getDescription(context)
            ?.takeIf { it.isNotEmpty() }
            ?: volumeId
    }
}
