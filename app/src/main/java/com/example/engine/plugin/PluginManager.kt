package com.example.engine.plugin

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.domain.plugin.InstalledPlugin
import com.example.domain.plugin.PluginCategory
import com.example.domain.plugin.PluginItemManifest
import com.example.domain.plugin.PluginLifecycleState
import com.example.domain.plugin.PluginManifest
import com.example.domain.plugin.PluginModule
import com.example.domain.plugin.PluginValidationResult
import com.example.domain.plugin.StandardPluginModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object PluginManager {
  private const val TAG = "PluginManager"
  private const val REGISTRY_FILE_NAME = "plugins_registry.json"

  private val _installedPlugins = MutableStateFlow<List<InstalledPlugin>>(emptyList())
  val installedPlugins: StateFlow<List<InstalledPlugin>> = _installedPlugins.asStateFlow()

  private val activeModules = mutableMapOf<String, PluginModule>()
  private var isInitialized = false

  fun initialize(context: Context) {
    if (isInitialized) return
    isInitialized = true
    loadRegistry(context)
    
    // Initialize loaded modules
    for (plugin in _installedPlugins.value) {
      val module = StandardPluginModule(plugin)
      module.onInitialize(context)
      if (plugin.isEnabled) {
        module.onEnable()
      } else {
        module.onDisable()
      }
      activeModules[plugin.manifest.id] = module
    }
  }

  /**
   * Registers a custom or dynamically loaded PluginModule.
   */
  fun registerModule(context: Context, module: PluginModule) {
    module.onInitialize(context)
    if (module.state == PluginLifecycleState.ENABLED) {
      module.onEnable()
    }
    activeModules[module.id] = module
    Log.d(TAG, "Registered PluginModule: ${module.id} (${module.name})")
  }

  /**
   * Unregisters and unloads a PluginModule.
   */
  fun unregisterModule(pluginId: String) {
    activeModules[pluginId]?.let { module ->
      module.onDisable()
      module.onUnload()
      activeModules.remove(pluginId)
      Log.d(TAG, "Unregistered PluginModule: $pluginId")
    }
  }

  /**
   * Returns all currently active loaded PluginModules.
   */
  fun getLoadedModules(): List<PluginModule> {
    return activeModules.values.toList()
  }

  /**
   * Retrieves specific PluginModule by ID.
   */
  fun getModule(pluginId: String): PluginModule? {
    return activeModules[pluginId]
  }

  /**
   * Installs a plugin from a Content Uri (e.g. selected via system File Picker).
   */
  fun installPluginFromUri(context: Context, uri: Uri): PluginValidationResult {
    return try {
      val contentResolver = context.contentResolver
      val inputStream = contentResolver.openInputStream(uri)
        ?: return PluginValidationResult.Error("Could not open input stream from file URI.")
      val tempZipFile = File(context.cacheDir, "plugin_upload_${System.currentTimeMillis()}.zip")
      
      FileOutputStream(tempZipFile).use { output ->
        inputStream.copyTo(output)
      }

      val result = installPluginFromZipFile(context, tempZipFile)
      tempZipFile.delete()
      result
    } catch (e: Exception) {
      Log.e(TAG, "Error installing plugin from Uri", e)
      PluginValidationResult.Error("Failed to read plugin ZIP: ${e.localizedMessage}")
    }
  }

  /**
   * Installs or updates a plugin from a local ZIP File.
   * Performs path traversal security checks, extracts package contents,
   * validates manifest structure, and registers the plugin.
   */
  fun installPluginFromZipFile(context: Context, zipFile: File): PluginValidationResult {
    if (!zipFile.exists() || zipFile.length() == 0L) {
      return PluginValidationResult.Error("ZIP file does not exist or is empty.")
    }

    val pluginsBaseDir = File(context.filesDir, "plugins")
    if (!pluginsBaseDir.exists()) {
      pluginsBaseDir.mkdirs()
    }

    val tempExtractDir = File(context.cacheDir, "temp_extract_${System.currentTimeMillis()}")
    if (!tempExtractDir.mkdirs()) {
      return PluginValidationResult.Error("Failed to create temporary extraction directory.")
    }

    try {
      // 1. Safe ZIP Extraction with Path Traversal Protection
      ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
        var entry: ZipEntry? = zipIn.nextEntry
        while (entry != null) {
          val destinationFile = File(tempExtractDir, entry.name)
          
          // SECURITY: Prevent Zip Slip path traversal vulnerability
          val canonicalDest = destinationFile.canonicalPath
          val canonicalBase = tempExtractDir.canonicalPath
          if (!canonicalDest.startsWith(canonicalBase + File.separator) && canonicalDest != canonicalBase) {
            tempExtractDir.deleteRecursively()
            return PluginValidationResult.Error("Security Error: ZIP entry attempted path traversal (${entry.name})")
          }

          if (entry.isDirectory) {
            destinationFile.mkdirs()
          } else {
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { out ->
              zipIn.copyTo(out)
            }
          }
          zipIn.closeEntry()
          entry = zipIn.nextEntry
        }
      }

      // 2. Locate Manifest File (plugin.json or manifest.json)
      var manifestFile = File(tempExtractDir, "plugin.json")
      if (!manifestFile.exists()) {
        manifestFile = File(tempExtractDir, "manifest.json")
      }

      // Check if ZIP extracted into a single wrapper folder
      if (!manifestFile.exists()) {
        val subDirs = tempExtractDir.listFiles { f -> f.isDirectory }
        if (subDirs != null && subDirs.isNotEmpty()) {
          for (dir in subDirs) {
            val nested = File(dir, "plugin.json")
            if (nested.exists()) {
              manifestFile = nested
              break
            }
          }
        }
      }

      if (!manifestFile.exists()) {
        tempExtractDir.deleteRecursively()
        return PluginValidationResult.Error("Invalid Plugin ZIP: Missing 'plugin.json' manifest in root of package.")
      }

      // 3. Parse and Validate Manifest
      val jsonContent = manifestFile.readText()
      val manifest = parseManifestJson(jsonContent)
        ?: run {
          tempExtractDir.deleteRecursively()
          return PluginValidationResult.Error("Invalid Plugin Manifest: Failed to parse 'plugin.json'. Check JSON syntax.")
        }

      if (manifest.id.isBlank()) {
        tempExtractDir.deleteRecursively()
        return PluginValidationResult.Error("Invalid Plugin Manifest: Missing required field 'id'.")
      }

      if (manifest.name.isBlank()) {
        tempExtractDir.deleteRecursively()
        return PluginValidationResult.Error("Invalid Plugin Manifest: Missing required field 'name'.")
      }

      val sourceDir = manifestFile.parentFile ?: tempExtractDir

      // 4. Copy to permanent Plugin Directory (/data/data/com.example/files/plugins/<plugin_id>/)
      val targetPluginDir = File(pluginsBaseDir, manifest.id)
      if (targetPluginDir.exists()) {
        targetPluginDir.deleteRecursively()
      }
      sourceDir.copyRecursively(targetPluginDir, overwrite = true)
      tempExtractDir.deleteRecursively()

      // 5. Register Plugin
      val installedPlugin = InstalledPlugin(
        manifest = manifest,
        installDirAbsolutePath = targetPluginDir.absolutePath,
        isEnabled = true,
        installedTimestamp = System.currentTimeMillis()
      )

      val currentList = _installedPlugins.value.toMutableList()
      currentList.removeAll { it.manifest.id == manifest.id }
      currentList.add(installedPlugin)
      _installedPlugins.value = currentList

      val module = StandardPluginModule(installedPlugin)
      registerModule(context, module)

      saveRegistry(context)

      Log.d(TAG, "Successfully installed plugin '${manifest.name}' v${manifest.version} with ${manifest.items.size} assets.")
      return PluginValidationResult.Success(
        installedPlugin = installedPlugin,
        message = "Plugin '${manifest.name}' v${manifest.version} installed successfully with ${manifest.items.size} items!"
      )

    } catch (e: Exception) {
      Log.e(TAG, "Error unzipping and installing plugin package", e)
      tempExtractDir.deleteRecursively()
      return PluginValidationResult.Error("Failed to extract plugin package: ${e.localizedMessage}")
    }
  }

  fun uninstallPlugin(context: Context, pluginId: String): Boolean {
    val plugin = _installedPlugins.value.find { it.manifest.id == pluginId } ?: return false
    try {
      unregisterModule(pluginId)
      val dir = File(plugin.installDirAbsolutePath)
      if (dir.exists()) {
        dir.deleteRecursively()
      }
      val newList = _installedPlugins.value.filter { it.manifest.id != pluginId }
      _installedPlugins.value = newList
      saveRegistry(context)
      Log.d(TAG, "Uninstalled plugin: $pluginId")
      return true
    } catch (e: Exception) {
      Log.e(TAG, "Error uninstalling plugin $pluginId", e)
      return false
    }
  }

  fun togglePluginEnabled(context: Context, pluginId: String, enabled: Boolean) {
    val newList = _installedPlugins.value.map {
      if (it.manifest.id == pluginId) it.copy(isEnabled = enabled) else it
    }
    _installedPlugins.value = newList
    
    activeModules[pluginId]?.let { module ->
      if (enabled) module.onEnable() else module.onDisable()
    }
    
    saveRegistry(context)
  }

  fun getEnabledItemsForCategory(category: PluginCategory): List<Pair<InstalledPlugin, PluginItemManifest>> {
    val result = mutableListOf<Pair<InstalledPlugin, PluginItemManifest>>()
    for (plugin in _installedPlugins.value) {
      if (plugin.isEnabled && plugin.manifest.category == category) {
        for (item in plugin.manifest.items) {
          result.add(Pair(plugin, item))
        }
      }
    }
    return result
  }

  fun getEnabledItemsForCategoryKey(categoryKey: String): List<Pair<InstalledPlugin, PluginItemManifest>> {
    return getEnabledItemsForCategory(PluginCategory.fromKey(categoryKey))
  }

  private fun parseManifestJson(jsonStr: String): PluginManifest? {
    return try {
      val obj = JSONObject(jsonStr)
      val id = obj.optString("id", "")
      val name = obj.optString("name", "")
      val version = obj.optString("version", "1.0.0")
      val author = obj.optString("author", "Unknown Creator")
      val description = obj.optString("description", "")
      val type = obj.optString("type", obj.optString("category", "other"))
      val minVersion = obj.optString("minimumAppVersion", "1.0.0")
      val icon = obj.optString("icon", "")

      val itemsList = mutableListOf<PluginItemManifest>()
      val itemsArray = obj.optJSONArray("items") ?: obj.optJSONArray("assets")
      if (itemsArray != null) {
        for (i in 0 until itemsArray.length()) {
          val itemObj = itemsArray.optJSONObject(i) ?: continue
          val itemId = itemObj.optString("id", "${id}_item_$i")
          val itemName = itemObj.optString("name", "Asset $i")
          val itemDesc = itemObj.optString("description", "")
          val itemPreview = itemObj.optString("preview", "")
          val itemFile = itemObj.optString("file", itemObj.optString("path", ""))
          val itemEmoji = itemObj.optString("emoji", "🎬")

          val paramsMap = mutableMapOf<String, Any>()
          val paramsObj = itemObj.optJSONObject("parameters")
          if (paramsObj != null) {
            val keys = paramsObj.keys()
            while (keys.hasNext()) {
              val k = keys.next()
              val v = paramsObj.get(k)
              if (v is JSONArray) {
                val list = mutableListOf<Any>()
                for (j in 0 until v.length()) {
                  list.add(v.get(j))
                }
                paramsMap[k] = list
              } else {
                paramsMap[k] = v
              }
            }
          }

          itemsList.add(
            PluginItemManifest(
              id = itemId,
              name = itemName,
              description = itemDesc,
              preview = itemPreview,
              file = itemFile,
              emoji = itemEmoji,
              parameters = paramsMap
            )
          )
        }
      }

      PluginManifest(
        id = id,
        name = name,
        version = version,
        author = author,
        description = description,
        type = type,
        minimumAppVersion = minVersion,
        icon = icon,
        items = itemsList
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing plugin manifest JSON", e)
      null
    }
  }

  private fun saveRegistry(context: Context) {
    try {
      val jsonArray = JSONArray()
      for (plugin in _installedPlugins.value) {
        val obj = JSONObject().apply {
          put("id", plugin.manifest.id)
          put("name", plugin.manifest.name)
          put("version", plugin.manifest.version)
          put("author", plugin.manifest.author)
          put("description", plugin.manifest.description)
          put("type", plugin.manifest.type)
          put("minimumAppVersion", plugin.manifest.minimumAppVersion)
          put("icon", plugin.manifest.icon)
          put("installDirAbsolutePath", plugin.installDirAbsolutePath)
          put("isEnabled", plugin.isEnabled)
          put("installedTimestamp", plugin.installedTimestamp)

          val itemsArray = JSONArray()
          for (item in plugin.manifest.items) {
            val itemObj = JSONObject().apply {
              put("id", item.id)
              put("name", item.name)
              put("description", item.description)
              put("preview", item.preview)
              put("file", item.file)
              put("emoji", item.emoji)
              
              if (item.parameters.isNotEmpty()) {
                val pObj = JSONObject()
                for ((k, v) in item.parameters) {
                  if (v is List<*>) {
                    val arr = JSONArray()
                    v.forEach { elem -> arr.put(elem) }
                    pObj.put(k, arr)
                  } else {
                    pObj.put(k, v)
                  }
                }
                put("parameters", pObj)
              }
            }
            itemsArray.put(itemObj)
          }
          put("items", itemsArray)
        }
        jsonArray.put(obj)
      }

      val registryFile = File(context.filesDir, REGISTRY_FILE_NAME)
      registryFile.writeText(jsonArray.toString(2))
      Log.d(TAG, "Saved plugin registry with ${_installedPlugins.value.size} plugins.")
    } catch (e: Exception) {
      Log.e(TAG, "Error saving plugin registry", e)
    }
  }

  private fun loadRegistry(context: Context) {
    try {
      val registryFile = File(context.filesDir, REGISTRY_FILE_NAME)
      if (!registryFile.exists()) return

      val jsonStr = registryFile.readText()
      if (jsonStr.isBlank()) return

      val jsonArray = JSONArray(jsonStr)
      val loadedList = mutableListOf<InstalledPlugin>()

      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val installDir = obj.optString("installDirAbsolutePath")
        val dirFile = File(installDir)
        if (!dirFile.exists()) continue // Skipped deleted plugins

        val manifest = parseManifestJson(obj.toString()) ?: continue
        val isEnabled = obj.optBoolean("isEnabled", true)
        val timestamp = obj.optLong("installedTimestamp", System.currentTimeMillis())

        loadedList.add(
          InstalledPlugin(
            manifest = manifest,
            installDirAbsolutePath = installDir,
            isEnabled = isEnabled,
            installedTimestamp = timestamp
          )
        )
      }

      _installedPlugins.value = loadedList
      Log.d(TAG, "Loaded ${loadedList.size} installed plugins from registry.")
    } catch (e: Exception) {
      Log.e(TAG, "Error loading plugin registry", e)
    }
  }
}
