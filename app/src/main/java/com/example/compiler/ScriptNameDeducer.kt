package com.example.compiler

/**
 * Deduce de manera inteligente el nombre del archivo de script (.csa o .csi).
 *
 * Prioridades:
 * 1. Comentario explícito del modder: // NAME: mi_mod o ; NAME: mi_mod o #NAME mi_mod
 * 2. Cabecera CLEO: {$CLEO .csa} // mi_mod
 * 3. Declaración de thread: 03A4: name_thread 'MYNAME' o name_thread 'MYNAME' o thread 'MYNAME'
 * 4. Inferencia semántica según opcodes y palabras clave (dinero, armas, vida, vehículos, touch, etc.)
 * 5. Generación de identificador único (evitando que scripts diferentes se sobrescriban en la carpeta del juego).
 */
object ScriptNameDeducer {

  fun inferScriptName(sourceCode: String, formatExtension: String): String {
    // 1. Comentario explícito del modder
    val nameRegex = Regex("(?im)^\\s*(?://|;|#)\\s*(?:NAME|SCRIPT|MOD|FILE|TITULO|TITLE)\\s*[:=]?\\s*([a-zA-Z0-9_-]+)")
    val match = nameRegex.find(sourceCode)
    if (match != null) {
      val found = sanitizeName(match.groupValues[1])
      if (found.isNotEmpty() && !isGenericPlaceholder(found)) return found
    }

    // 2. Cabecera CLEO estilo Sanny Builder: {$CLEO .csa} // mi_mod o {$CLEO .csi} mi_mod
    val cleoHeaderRegex = Regex("(?im)^\\s*\\{\\s*\\\$CLEO\\s+\\.(?:csa|csi|cs)\\s*\\}\\s*(?://|;)?\\s*([a-zA-Z0-9_-]+)")
    val cleoMatch = cleoHeaderRegex.find(sourceCode)
    if (cleoMatch != null) {
      val found = sanitizeName(cleoMatch.groupValues[1])
      if (found.isNotEmpty() && !isGenericPlaceholder(found)) return found
    }

    // 3. Declaración de thread (03A4: name_thread 'NOMBRE' o name_thread 'NOMBRE')
    val threadRegex = Regex("(?im)(?:03A4:\\s*)?(?:name_thread|thread)\\s*['\"]([a-zA-Z0-9_-]+)['\"]")
    val threadMatch = threadRegex.find(sourceCode)
    if (threadMatch != null) {
      val found = sanitizeName(threadMatch.groupValues[1])
      if (found.isNotEmpty() && !isGenericPlaceholder(found)) return found
    }

    // 4. Inferencia semántica por contenido y opcodes
    val lower = sourceCode.lowercase()
    val isCsi = formatExtension.lowercase().contains("csi")

    // Dinero / Cash / Money
    if (lower.contains("0109") || lower.contains("010a") || lower.contains("010b") || lower.contains("032b") ||
        lower.contains("add_money") || lower.contains("set_money") || lower.contains("money") ||
        lower.contains("dinero") || lower.contains("cash")) {
      return if (lower.contains("999999") || lower.contains("infinite")) "infinite_money" else "money_boost"
    }

    // Armas / Munición / Weapons
    if (lower.contains("01b2") || lower.contains("01b4") || lower.contains("0555") || lower.contains("05e2") ||
        lower.contains("give_weapon") || lower.contains("weapon") || lower.contains("arma") ||
        lower.contains("ammo") || lower.contains("gun") || lower.contains("minigun")) {
      return "weapons_spawner"
    }

    // Salud / Inmortalidad / Godmode / Blindaje
    if (lower.contains("0223") || lower.contains("01b9") || lower.contains("02e0") || lower.contains("02ab") ||
        lower.contains("godmode") || lower.contains("inmortal") || lower.contains("health") ||
        lower.contains("armour") || lower.contains("vida") || lower.contains("blindaje")) {
      return "godmode_health"
    }

    // Vehículos / Spawner de autos
    if (lower.contains("00a5") || lower.contains("00a6") || lower.contains("0407") || lower.contains("create_car") ||
        lower.contains("vehicle") || lower.contains("car_spawn") || lower.contains("auto") || lower.contains("coche")) {
      return "vehicle_spawner"
    }

    // Menú táctil / Gestos de CLEO Android
    if (lower.contains("0dd0") || lower.contains("0dd1") || lower.contains("0d4") ||
        lower.contains("touch") || lower.contains("menu") || lower.contains("swipe") ||
        lower.contains("gesto") || lower.contains("tactil")) {
      return if (lower.contains("menu")) "touch_menu" else "touch_controls"
    }

    // Animaciones
    if (lower.contains("0605") || lower.contains("038b") || lower.contains("anim") ||
        lower.contains("dance") || lower.contains("baile") || lower.contains("walk_style")) {
      return if (isCsi) "anim_menu" else "anim_player"
    }

    // Teleport / Coordenadas
    if (lower.contains("00a1") || lower.contains("00ab") || lower.contains("teleport") ||
        lower.contains("coords") || lower.contains("tp")) {
      return "teleport_mod"
    }

    // Clima / Tiempo
    if (lower.contains("01b6") || lower.contains("00c0") || lower.contains("weather") || lower.contains("clima")) {
      return "weather_control"
    }

    // Nivel de búsqueda policial (Wanted level)
    if (lower.contains("010d") || lower.contains("0110") || lower.contains("01f0") ||
        lower.contains("wanted") || lower.contains("police") || lower.contains("policia")) {
      return "never_wanted"
    }

    // Si es formato CSI (scripts invocados táctiles por menú)
    if (isCsi) {
      return "cleo_action"
    }

    // 5. Nombre temático con hash de contenido para garantizar unicidad y evitar sobreescritura en el juego
    val hash = kotlin.math.abs(sourceCode.trim().hashCode()).toString(16).take(4)
    return "cleo_mod_$hash"
  }

  private fun isGenericPlaceholder(name: String): Boolean {
    val low = name.lowercase()
    return low == "script" || low == "noname" || low == "main" || low == "cleo" || low == "untitled" || low.isBlank()
  }

  fun sanitizeName(raw: String): String {
    val clean = raw.trim()
      .replace(Regex("[^a-zA-Z0-9_]"), "_")
      .trim('_')
      .lowercase()
    return clean.take(24)
  }
}
