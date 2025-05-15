package org.torproject.android.service.db

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import android.util.Log
import org.torproject.android.service.util.Utils
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import androidx.core.net.toUri

object OnionServiceColumns : BaseColumns {
    private const val NAME: String = "name"
    private const val PORT: String = "port"
    private const val ONION_PORT: String = "onion_port"
    private const val DOMAIN: String = "domain"
    private const val ENABLED: String = "enabled"
    private const val PATH: String = "filepath"

    private val V3_ONION_SERVICE_PROJECTION: Array<String> =
        arrayOf(BaseColumns._ID, NAME, DOMAIN, PORT, ONION_PORT, ENABLED, PATH)

    @JvmStatic
    fun addV3OnionServicesToTorrc(
        torrc: StringBuffer,
        context: Context,
        v3OnionBasePath: File
    ) {
        try {
            val contentResolver = context.applicationContext.contentResolver
            val uri = getContentUri(context)
            val onionServices = contentResolver.query(
                uri, V3_ONION_SERVICE_PROJECTION,
                "$ENABLED=1", null, null
            )
            if (onionServices == null) return
            while (onionServices.moveToNext()) {
                val id_index = onionServices.getColumnIndex(BaseColumns._ID)
                val port_index = onionServices.getColumnIndex(PORT)
                val onion_port_index = onionServices.getColumnIndex(ONION_PORT)
                val path_index = onionServices.getColumnIndex(PATH)
                val domain_index = onionServices.getColumnIndex(DOMAIN)
                // Ensure that we have all the indexes before trying to use them
                if (id_index < 0 || port_index < 0 || onion_port_index < 0 || path_index < 0 || domain_index < 0) continue

                val id = onionServices.getInt(id_index)
                val localPort = onionServices.getInt(port_index)
                val onionPort = onionServices.getInt(onion_port_index)
                var path = onionServices.getString(path_index)
                val domain = onionServices.getString(domain_index)
                if (path == null) {
                    path = "v3"
                    if (domain == null) path += UUID.randomUUID().toString()
                    else path += localPort
                    val cv = ContentValues()
                    cv.put(PATH, path)
                    contentResolver.update(
                        uri,
                        cv,
                        BaseColumns._ID + "=" + id,
                        null
                    )
                }
                val v3DirPath: String =
                    File(v3OnionBasePath.absolutePath, path).getCanonicalPath()
                torrc.append("HiddenServiceDir ").append(v3DirPath).append("\n")
                    .append("HiddenServiceVersion 3\n")
                    .append("HiddenServicePort ").append(onionPort).append(" 127.0.0.1:")
                    .append(localPort).append("\n")
            }
            onionServices.close()
        } catch (e: Exception) {
            Log.e("OnionServiceColums", "$e")
        }
    }

    @JvmStatic
    fun updateV3OnionNames(context: Context, v3OnionBasePath: File) {
        val uri = getContentUri(context)
        val contentResolver = context.applicationContext.contentResolver
        val onionServices = contentResolver.query(uri, null, null, null, null) ?: return
        try {
            while (onionServices.moveToNext()) {
                val domain_index = onionServices.getColumnIndex(DOMAIN)
                val path_index = onionServices.getColumnIndex(PATH)
                val id_index = onionServices.getColumnIndex(BaseColumns._ID)
                if (domain_index < 0 || path_index < 0 || id_index < 0) continue
                var domain = onionServices.getString(domain_index)
                if (domain == null || TextUtils.isEmpty(domain)) {
                    val path = onionServices.getString(path_index)
                    val v3OnionDirPath: String =
                        File(v3OnionBasePath.absolutePath, path).getCanonicalPath()
                    val hostname = File(v3OnionDirPath, "hostname")
                    if (hostname.exists()) {
                        val id = onionServices.getInt(id_index);
                        domain = Utils.readInputStreamAsString( FileInputStream(hostname)).trim();
                        val fields = ContentValues();
                        fields.put(DOMAIN, domain);
                        contentResolver.update(uri, fields, BaseColumns._ID + "=" + id, null);
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OnionServiceColumns", "$e")
        } finally {
            onionServices.close()
        }
    }

    private fun getContentUri(context: Context) : Uri {
        return "content://${context.applicationContext.packageName}.ui.v3onionservice/v3".toUri()
    }
}
