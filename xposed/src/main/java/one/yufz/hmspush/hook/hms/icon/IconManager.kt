package one.yufz.hmspush.hook.hms.icon

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.yufz.hmspush.common.IconData
import one.yufz.hmspush.common.IconData.Companion.scaleForNotification
import one.yufz.hmspush.common.model.IconModel
import one.yufz.hmspush.hook.hms.StorageContext
import java.io.File

object IconManager {
    private val iconDir = File(StorageContext.get().filesDir, "hms_push/icons")

    private val cache = LruCache<String, IconData>(20)

    suspend fun saveToLocal(packageName: String, jsonString: String) {
        withContext(Dispatchers.IO) {
            if (!iconDir.exists()) {
                iconDir.mkdirs()
            }

            File(iconDir, packageName).apply {
                if (exists()) {
                    delete()
                }
                createNewFile()

                writeText(jsonString)
            }
        }
    }

    fun getNotificationIconData(context: Context, packageName: String): IconData? {
        val cacheIconData = cache.get(packageName)
        if (cacheIconData != null) return cacheIconData

        val iconDataFile = File(iconDir, packageName)

        if (!iconDataFile.exists()) return null

        val iconData = IconData.fromJson(iconDataFile.readText())
            .scaleForNotification(context)

        cache.put(packageName, iconData)

        return iconData
    }

    suspend fun getAllIconModel(): List<IconModel> {
        return withContext(Dispatchers.IO) {
            iconDir.listFiles()
                ?.map { IconModel(it.name, dataFD = ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY)) }
                ?: emptyList()
        }
    }

    suspend fun deleteIcon(packages: Array<String>?) {
        return withContext(Dispatchers.IO) {
            val size = iconDir.listFiles()?.size ?: 0
            if (packages.isNullOrEmpty()) {
                if (size != 0) {
                    iconDir.deleteRecursively()
                    cache.evictAll()
                }
            } else {
                packages.onEach {
                    File(iconDir, it).delete()
                    cache.remove(it)
                }
            }
        }
    }
}