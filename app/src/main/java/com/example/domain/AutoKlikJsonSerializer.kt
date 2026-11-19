package com.example.domain

import org.json.JSONArray
import org.json.JSONObject

object AutoKlikJsonSerializer {

  fun buildFullExportJson(
    accounts: List<AccountContainerConfig>,
    globalConfig: GlobalRuntimeConfig,
  ): String {
    val root = JSONObject()
    root.put("schema", "AutoKlikPro")
    root.put("version", "3.2")
    root.put("extensionName", "AutoKlik-v3.2-Mandiri-Extension")
    root.put("compatibleWith", "Ferizy Multi-Container Electron (Fase 2)")
    root.put("targetOrigin", "https://trip.ferizy.com")
    root.put("ntfyTopic", globalConfig.ntfyTopic.ifBlank { "ferizy_Transsamudra05" })
    root.put("ntfyServer", "https://ntfy.sh")
    root.put("hardCapTabsPerAccount", globalConfig.hardCapPerAccount)
    root.put("lazyAntiRevert", globalConfig.lazyAntiRevertEnabled)
    root.put("winIsolationMode", if (globalConfig.winIsolationPerTab) "per_tab" else "per_account")

    val containersObj = JSONObject()
    accounts.forEach { acc ->
      containersObj.put(acc.partition, accountToJson(acc, globalConfig))
    }
    root.put("containers", containersObj)

    val primaryPoints = accounts.firstOrNull()?.points.orEmpty()
    root.put("points", pointsToJsonArray(primaryPoints))
    return root.toString(2)
  }

  fun buildSingleAccountExportJson(
    account: AccountContainerConfig,
    globalConfig: GlobalRuntimeConfig,
  ): String {
    val obj = accountToJson(account, globalConfig)
    obj.put("schema", "AutoKlikPro")
    obj.put("version", "3.2")
    obj.put("extensionName", "AutoKlik-v3.2-Mandiri-Extension")
    return obj.toString(2)
  }

  private fun accountToJson(
    account: AccountContainerConfig,
    globalConfig: GlobalRuntimeConfig,
  ): JSONObject {
    val obj = JSONObject()
    obj.put("accountId", account.accountId)
    obj.put("partition", account.partition)
    obj.put("label", account.name)
    obj.put("ntfyTopic", globalConfig.ntfyTopic.ifBlank { "ferizy_Transsamudra05" })
    obj.put(
      "extensionSettings",
      JSONObject().apply {
        put("enabled", account.extensionSettings.enabled)
        put("loopEnabled", account.extensionSettings.loopEnabled)
        put("maxLoops", account.extensionSettings.maxLoops)
        put("loopDelayMs", account.extensionSettings.loopDelayMs)
        put("radiusPx", account.extensionSettings.radiusPx)
        put("smartMatch", account.extensionSettings.smartMatch)
        put("antiRevert", account.extensionSettings.antiRevert)
        put("markersHidden", account.extensionSettings.markersHidden)
        put("pointsLocked", account.extensionSettings.pointsLocked)
      },
    )
    obj.put(
      "geolocation",
      JSONObject().apply {
        put("mode", account.geoMode.id)
        put("preset", account.geoPreset.id)
        put("latitude", account.effectiveLat)
        put("longitude", account.effectiveLng)
      },
    )
    obj.put("points", pointsToJsonArray(account.points))
    return obj
  }

  private fun pointsToJsonArray(points: List<AutoKlikPoint>): JSONArray {
    val array = JSONArray()
    points
      .sortedBy { it.order }
      .forEachIndexed { idx, pt ->
        val obj = JSONObject()
        obj.put("id", pt.id)
        obj.put("order", idx + 1)
        obj.put("type", pt.type.jsonType)
        obj.put("label", pt.label)
        obj.put("enabled", pt.enabled)
        obj.put("x", pt.x)
        obj.put("y", pt.y)
        obj.put("radiusPx", pt.radiusPx)
        if (pt.type == AutoKlikPointType.SWIPE) {
          obj.put("endX", pt.endX ?: pt.x)
          obj.put("endY", pt.endY ?: (pt.y - 250))
          obj.put("durationMs", pt.durationMs)
        }
        if (pt.selector.isNotBlank()) {
          obj.put("selector", pt.selector)
        }
        if (pt.type == AutoKlikPointType.INPUT) {
          obj.put("inputValue", pt.inputValue)
          obj.put("text", pt.inputValue)
          obj.put("typingSpeedMs", pt.typingSpeedMs)
          obj.put("popupDelayMs", pt.popupDelayMs)
          obj.put("antiRevert", pt.antiRevert)
        }
        if (pt.type == AutoKlikPointType.POPUP_KEY) {
          obj.put("popupKey", pt.popupKey)
          obj.put("key", pt.popupKey)
        }
        obj.put("delayAfterMs", pt.delayAfterMs)
        array.put(obj)
      }
    return array
  }

  data class ParsedAccountProfile(
    val extensionSettings: AutoKlikExtensionSettings?,
    val points: List<AutoKlikPoint>,
  )

  fun parseAccountProfileJson(rawJson: String, targetAccountId: Int): Result<ParsedAccountProfile> {
    return runCatching {
      val trimmed = rawJson.trim()
      require(trimmed.isNotEmpty()) { "File JSON kosong" }
      val sourceObj: JSONObject?
      val pointsArray: JSONArray
      if (trimmed.startsWith("[")) {
        sourceObj = null
        pointsArray = JSONArray(trimmed)
      } else {
        val root = JSONObject(trimmed)
        val containers = root.optJSONObject("containers")
        val targetKey = "persist:akun_$targetAccountId"
        val resolved =
          when {
            containers != null && containers.has(targetKey) -> containers.getJSONObject(targetKey)
            containers != null && containers.length() > 0 -> containers.getJSONObject(containers.keys().next())
            else -> root
          }
        sourceObj = resolved
        pointsArray =
          resolved.optJSONArray("points")
            ?: root.optJSONArray("points")
            ?: throw IllegalArgumentException("JSON tidak memiliki daftar points")
      }

      val parsedExt =
        sourceObj?.optJSONObject("extensionSettings")?.let { ext ->
          AutoKlikExtensionSettings(
            enabled = ext.optBoolean("enabled", true),
            loopEnabled = ext.optBoolean("loopEnabled", true),
            maxLoops = ext.optInt("maxLoops", 0).coerceIn(0, 999),
            loopDelayMs = ext.optInt("loopDelayMs", 100).coerceIn(20, 60000),
            radiusPx = ext.optInt("radiusPx", 28).coerceIn(5, 200),
            smartMatch = ext.optBoolean("smartMatch", true),
            antiRevert = ext.optBoolean("antiRevert", true),
            markersHidden = ext.optBoolean("markersHidden", false),
            pointsLocked = ext.optBoolean("pointsLocked", false),
          )
        }

      val points = buildList {
        for (i in 0 until pointsArray.length()) {
          val p = pointsArray.getJSONObject(i)
          val typeStr = p.optString("type", "tap")
          val ptType = AutoKlikPointType.entries.find { it.jsonType == typeStr } ?: AutoKlikPointType.TAP
          add(
            AutoKlikPoint(
              id = "pt_${targetAccountId}_${i + 1}",
              order = i + 1,
              type = ptType,
              label = p.optString("label", "Titik ${i + 1}"),
              x = p.optInt("x", 540),
              y = p.optInt("y", 420),
              endX = if (p.has("endX")) p.optInt("endX") else null,
              endY = if (p.has("endY")) p.optInt("endY") else null,
              durationMs = p.optInt("durationMs", 180),
              selector = p.optString("selector", ""),
              inputValue = p.optString("inputValue", p.optString("text", "")),
              typingSpeedMs = p.optInt("typingSpeedMs", 30).coerceIn(1, 1000),
              popupKey = p.optString("popupKey", p.optString("key", "Enter")),
              popupDelayMs = p.optInt("popupDelayMs", 300).coerceIn(20, 10000),
              delayAfterMs = p.optInt("delayAfterMs", 500).coerceAtLeast(15),
              radiusPx = p.optInt("radiusPx", 28).coerceIn(5, 200),
              antiRevert = p.optBoolean("antiRevert", false),
              enabled = p.optBoolean("enabled", true),
            )
          )
        }
      }
      ParsedAccountProfile(extensionSettings = parsedExt, points = points)
    }
  }
}
