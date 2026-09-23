package com.example.compiler

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Tipo de parámetro para las instrucciones CLEO de GTA San Andreas.
 */
enum class ParamType {
  INTEGER,
  FLOAT,
  STRING_SHORT,
  STRING_LONG,
  LABEL,
  VARIABLE
}

/**
 * Definición de un Opcode de GTA SA / CLEO.
 */
data class OpcodeDef(
  val opcode: Int,
  val hexString: String,
  val commandName: String,
  val description: String,
  val minParams: Int,
  val maxParams: Int,
  val expectedTypes: List<ParamType> = emptyList(),
  val example: String,
  val category: String = "General",
  val isCustom: Boolean = false
)

/**
 * Base de datos de opcodes.
 * Mantiene separados los opcodes oficiales de los opcodes personalizados del usuario.
 */
object CleoOpcodeDatabase {

  private val officialOpcodes = mutableMapOf<Int, OpcodeDef>()
  private val customOpcodes = mutableMapOf<Int, OpcodeDef>()

  init {
    loadEmbeddedOpcodes()
    tryAutoLoadJson()
  }

  fun registerOfficial(def: OpcodeDef) {
    officialOpcodes[def.opcode] = def.copy(isCustom = false)
  }

  fun registerCustom(def: OpcodeDef) {
    customOpcodes[def.opcode] = def.copy(
      isCustom = true,
      category = if (def.category.isNotBlank() && def.category != "General") def.category else "Personalizado"
    )
  }

  fun getCategories(): List<String> {
    val cats = linkedSetOf<String>()
    officialOpcodes.values.forEach { cats.add(it.category) }
    if (customOpcodes.isNotEmpty()) {
      cats.add("Personalizado")
    }
    return cats.toList()
  }

  fun count(): Int = officialOpcodes.size + customOpcodes.size
  fun officialCount(): Int = officialOpcodes.size
  fun customCount(): Int = customOpcodes.size

  /**
   * Carga los opcodes oficiales desde una cadena JSON válida.
   */
  fun loadFromJsonString(json: String): Int {
    return try {
      var loaded = 0
      val array = JSONArray(json)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val hex = obj.getString("opcode").trim().uppercase()
        val intVal = hex.toIntOrNull(16) ?: continue
        val name = obj.optString("name", "unknown")
        val desc = obj.optString("desc", "")
        val minParams = obj.optInt("min_params", 0)
        val maxParams = obj.optInt("max_params", minParams)
        val example = obj.optString("example", "$hex: $name")
        val category = obj.optString("category", "General")

        registerOfficial(
          OpcodeDef(
            opcode = intVal,
            hexString = hex,
            commandName = name,
            description = desc,
            minParams = minParams,
            maxParams = maxParams,
            example = example,
            category = category
          )
        )
        loaded++
      }
      loaded
    } catch (e: Exception) {
      0
    }
  }

  private fun tryAutoLoadJson() {
    try {
      val candidateFiles = listOf(
        File("src/main/assets/opcodes_database.json"),
        File("app/src/main/assets/opcodes_database.json"),
        File("../app/src/main/assets/opcodes_database.json")
      )
      for (file in candidateFiles) {
        if (file.exists() && file.length() > 0) {
          loadFromJsonString(file.readText(Charsets.UTF_8))
          return
        }
      }
      val stream = javaClass.classLoader?.getResourceAsStream("opcodes_database.json")
        ?: Thread.currentThread().contextClassLoader?.getResourceAsStream("opcodes_database.json")
      stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
        loadFromJsonString(reader.readText())
      }
    } catch (_: Throwable) {
      // Ignorar excepciones en entornos que no admitan lectura directa de archivos
    }
  }

  /**
   * Carga los opcodes oficiales desde el archivo JSON de assets.
   */
  fun loadFromAssets(context: Context, fileName: String = "opcodes_database.json"): Int {
    return try {
      val json = context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
      loadFromJsonString(json)
    } catch (e: Exception) {
      0
    }
  }

  /**
   * Sincroniza y garantiza la presencia de opcodes_database.json en Android/data
   * de forma silenciosa en segundo plano sin interfaces innecesarias.
   */
  fun initializeAndSyncDatabase(context: Context): Int {
    try {
      val extDir = context.getExternalFilesDir(null)
      val extFile = extDir?.let { File(it, "opcodes_database.json") }
      val intFile = File(context.filesDir, "opcodes_database.json")

      // 1. Extraer a Android/data si no existe
      if (extFile != null && (!extFile.exists() || extFile.length() < 100)) {
        try {
          context.assets.open("opcodes_database.json").use { input ->
            extFile.outputStream().use { output ->
              input.copyTo(output)
            }
          }
        } catch (_: Exception) {}
      }

      // 2. Extraer a almacenamiento interno como respaldo
      if (!intFile.exists() || intFile.length() < 100) {
        try {
          if (extFile != null && extFile.exists() && extFile.length() > 100) {
            extFile.copyTo(intFile, overwrite = true)
          } else {
            context.assets.open("opcodes_database.json").use { input ->
              intFile.outputStream().use { output ->
                input.copyTo(output)
              }
            }
          }
        } catch (_: Exception) {}
      }

      // 3. Cargar en memoria: primero desde Android/data si existe
      var loaded = 0
      if (extFile != null && extFile.exists() && extFile.length() > 100) {
        try {
          loaded = loadFromJsonString(extFile.readText(Charsets.UTF_8))
        } catch (_: Exception) {}
      }
      if (loaded == 0 && intFile.exists() && intFile.length() > 100) {
        try {
          loaded = loadFromJsonString(intFile.readText(Charsets.UTF_8))
        } catch (_: Exception) {}
      }
      if (loaded == 0) {
        loaded = loadFromAssets(context)
      }

      loadCustomFromStorage(context)
      return loaded
    } catch (e: Exception) {
      e.printStackTrace()
      loadFromAssets(context)
      loadCustomFromStorage(context)
      return officialOpcodes.size
    }
  }

  /**
   * Carga los opcodes personalizados guardados por el usuario.
   * Revisa primero Android/data y luego el almacenamiento interno.
   */
  fun loadCustomFromStorage(context: Context): Int {
    customOpcodes.clear()
    val extFile = context.getExternalFilesDir(null)?.let { File(it, "custom_opcodes.json") }
    val intFile = File(context.filesDir, "custom_opcodes.json")
    val fileToRead = if (extFile != null && extFile.exists() && extFile.length() > 5) extFile else intFile

    if (!fileToRead.exists()) return 0

    return try {
      val content = fileToRead.readText(Charsets.UTF_8)
      val array = JSONArray(content)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val hex = obj.getString("opcode").trim().uppercase()
        val intVal = hex.toIntOrNull(16) ?: continue
        val name = obj.optString("name", "custom_cmd")
        val desc = obj.optString("desc", "Opcode personalizado del usuario")
        val minParams = obj.optInt("min_params", 0)
        val maxParams = obj.optInt("max_params", minParams)
        val example = obj.optString("example", "$hex: $name")

        registerCustom(
          OpcodeDef(
            opcode = intVal,
            hexString = hex,
            commandName = name,
            description = desc,
            minParams = minParams,
            maxParams = maxParams,
            example = example,
            isCustom = true
          )
        )
      }
      customOpcodes.size
    } catch (e: Exception) {
      e.printStackTrace()
      0
    }
  }

  /**
   * Obtiene el texto crudo del archivo custom_opcodes.json o una plantilla si está vacío.
   */
  fun getCustomOpcodesRawText(context: Context): String {
    val extFile = context.getExternalFilesDir(null)?.let { File(it, "custom_opcodes.json") }
    val intFile = File(context.filesDir, "custom_opcodes.json")
    val file = if (extFile != null && extFile.exists() && extFile.length() > 5) extFile else intFile

    if (file.exists() && file.length() > 0) {
      return file.readText(Charsets.UTF_8)
    }
    // Plantilla inicial en formato JSON limpio y editable
    return """[
  {
    "opcode": "0A90",
    "name": "call_function",
    "desc": "Llama a una función interna de memoria",
    "min_params": 1
  }
]""".trimIndent()
  }

  /**
   * Guarda y analiza los opcodes personalizados del usuario en JSON.
   * Guarda en simultáneo tanto en Android/data como en almacenamiento interno.
   */
  fun saveAndExecuteCustomOpcodes(context: Context, rawInput: String): Pair<Boolean, String> {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) {
      return Pair(false, "El editor está vacío. Escribe al menos un opcode.")
    }

    try {
      val finalJsonArray = JSONArray()

      if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
        // Formato JSON directo
        if (trimmed.startsWith("{")) {
          finalJsonArray.put(JSONObject(trimmed))
        } else {
          val parsedArray = JSONArray(trimmed)
          for (i in 0 until parsedArray.length()) {
            finalJsonArray.put(parsedArray.getJSONObject(i))
          }
        }
      } else {
        // Formato simple por líneas: "0A90: call_function descripción"
        val lines = trimmed.lines()
        for (line in lines) {
          val cleanLine = line.trim()
          if (cleanLine.isBlank() || cleanLine.startsWith("//") || cleanLine.startsWith(";")) continue

          val parts = cleanLine.split(":", limit = 2)
          if (parts.size >= 2) {
            val hex = parts[0].trim().uppercase()
            val rest = parts[1].trim()
            val tokens = rest.split("\\s+".toRegex())
            val name = if (tokens.isNotEmpty()) tokens[0] else "custom_cmd"
            val desc = if (tokens.size > 1) tokens.drop(1).joinToString(" ") else "Opcode personalizado"

            val item = JSONObject().apply {
              put("opcode", hex)
              put("name", name)
              put("desc", desc)
              put("min_params", 0)
            }
            finalJsonArray.put(item)
          }
        }
      }

      if (finalJsonArray.length() == 0) {
        return Pair(false, "No se encontraron opcodes válidos en el texto ingresado.")
      }

      val jsonString = finalJsonArray.toString(2)

      // Guardar en almacenamiento interno
      val file = File(context.filesDir, "custom_opcodes.json")
      file.writeText(jsonString, Charsets.UTF_8)

      // Guardar también en Android/data para visibilidad en exploradores
      val extDir = context.getExternalFilesDir(null)
      if (extDir != null) {
        try {
          val extCustom = File(extDir, "custom_opcodes.json")
          extCustom.writeText(jsonString, Charsets.UTF_8)
        } catch (_: Exception) {}
      }

      // Recargar en memoria
      val loaded = loadCustomFromStorage(context)
      return Pair(true, "$loaded opcode(s) personalizados guardados y ejecutados con éxito.")
    } catch (e: Exception) {
      return Pair(false, "Error en el formato: ${e.message}")
    }
  }

  /**
   * Búsqueda prioritaria:
   * 1. Busca primero en los opcodes oficiales.
   * 2. Si no lo encuentra, busca en los personalizados del usuario.
   */
  fun findByHex(hex: String): OpcodeDef? {
    val clean = hex.trim().removePrefix("0x").removeSuffix(":").uppercase()
    val intVal = clean.toIntOrNull(16) ?: return null

    if (intVal == 0x0000) {
      return officialOpcodes[0] ?: OpcodeDef(0x0000, "0000", "nop", "No operation (NOP)", 0, 0, emptyList(), "0000: NOP")
    }

    officialOpcodes[intVal]?.let { return it }
    customOpcodes[intVal]?.let { return it }

    // Si es una condición negada (0x8000 o superior, ej. 80DF -> condición invertida de 00DF)
    if (intVal >= 0x8000) {
      val baseInt = intVal and 0x7FFF
      val baseDef = officialOpcodes[baseInt] ?: customOpcodes[baseInt]
      if (baseDef != null) {
        return baseDef.copy(
          opcode = intVal,
          hexString = "%04X".format(intVal),
          commandName = "not_${baseDef.commandName}",
          description = "Condición negada: ${baseDef.description}"
        )
      }
    }

    return null
  }

  fun findByName(name: String): OpcodeDef? {
    val clean = name.trim().lowercase()
    return officialOpcodes.values.firstOrNull { it.commandName.lowercase() == clean }
      ?: customOpcodes.values.firstOrNull { it.commandName.lowercase() == clean }
  }

  fun isSupported(opcodeInt: Int): Boolean =
    officialOpcodes.containsKey(opcodeInt) || customOpcodes.containsKey(opcodeInt)

  fun getAll(): List<OpcodeDef> = (officialOpcodes.values + customOpcodes.values).toList()
  fun getCustomList(): List<OpcodeDef> = customOpcodes.values.toList()

  private fun loadEmbeddedOpcodes() {
    val defaultList = listOf(
      OpcodeDef(0x0000, "0000", "nop", "No operation (NOP)", 0, 0, emptyList(), "0000: NOP"),
      OpcodeDef(0x0001, "0001", "wait", "Pausa la ejecución del script por X milisegundos", 1, 1, listOf(ParamType.INTEGER), "0001: wait 0 ms"),
      OpcodeDef(0x0002, "0002", "jump", "Salto incondicional a una etiqueta", 1, 1, listOf(ParamType.LABEL), "0002: jump @MAIN_LOOP"),
      OpcodeDef(0x004D, "004D", "jump_if_false", "Salto condicional si la última condición fue falsa (jf)", 1, 1, listOf(ParamType.LABEL), "004D: jump_if_false @END"),
      OpcodeDef(0x004E, "004E", "end_thread", "Termina el hilo del script actual", 0, 0, emptyList(), "004E: end_thread"),
      OpcodeDef(0x0050, "0050", "gosub", "Llama a una subrutina", 1, 1, listOf(ParamType.LABEL), "0050: gosub @SUB_ROUTINE"),
      OpcodeDef(0x0051, "0051", "return", "Regresa de una subrutina", 0, 0, emptyList(), "0051: return"),
      OpcodeDef(0x0053, "0053", "create_player", "Crea el objeto de jugador en coordenadas", 4, 4, emptyList(), "0053: create_player 0 2488.0 -1666.0 13.0"),
      OpcodeDef(0x009A, "009A", "create_actor", "Crea un personaje (ped/actor)", 5, 5, emptyList(), "009A: create_actor 4 105 0.0 0.0 0.0 to \$ACTOR"),
      OpcodeDef(0x009B, "009B", "destroy_actor", "Destruye un personaje creado", 1, 1, emptyList(), "009B: destroy_actor \$ACTOR"),
      OpcodeDef(0x00A0, "00A0", "get_actor_coordinates", "Obtiene las coordenadas X Y Z de un personaje", 4, 4, emptyList(), "00A0: get_actor \$ACTOR coordinates 0@ 1@ 2@"),
      OpcodeDef(0x00A1, "00A1", "set_actor_coordinates", "Teletransporta o coloca al personaje en X Y Z", 4, 4, emptyList(), "00A1: set_actor \$ACTOR coordinates 0.0 0.0 10.0"),
      OpcodeDef(0x00A5, "00A5", "create_car", "Crea un vehículo de modelo específico", 5, 5, emptyList(), "00A5: create_car 411 0.0 0.0 0.0 to \$CAR"),
      OpcodeDef(0x00A6, "00A6", "destroy_car", "Destruye y borra un vehículo", 1, 1, emptyList(), "00A6: destroy_car \$CAR"),
      OpcodeDef(0x00AA, "00AA", "get_car_coordinates", "Obtiene las coordenadas de un vehículo", 4, 4, emptyList(), "00AA: get_car \$CAR coordinates 0@ 1@ 2@"),
      OpcodeDef(0x00AB, "00AB", "set_car_coordinates", "Posiciona un vehículo en coordenadas X Y Z", 4, 4, emptyList(), "00AB: set_car \$CAR coordinates 0.0 0.0 5.0"),
      OpcodeDef(0x00BA, "00BA", "show_styled_text", "Muestra texto estilizado en pantalla", 3, 3, listOf(ParamType.STRING_SHORT, ParamType.INTEGER, ParamType.INTEGER), "00BA: show_styled_text 'MOD_ON' 1000 1"),
      OpcodeDef(0x00BC, "00BC", "print_text", "Muestra un texto de subtítulo", 2, 2, emptyList(), "00BC: print_text 'INFO' 2000"),
      OpcodeDef(0x00BD, "00BD", "print_text_now", "Muestra un texto de subtítulo inmediatamente", 2, 2, emptyList(), "00BD: print_text_now 'ALERTA' 1500"),
      OpcodeDef(0x00BE, "00BE", "clear_prints", "Limpia subtítulos de pantalla", 0, 0, emptyList(), "00BE: clear_prints"),
      OpcodeDef(0x00BF, "00BF", "text_clear_all", "Limpia todos los textos en pantalla", 0, 0, emptyList(), "00BF: text_clear_all"),
      OpcodeDef(0x00C0, "00C0", "set_time_of_day", "Establece la hora y minuto del mundo de juego", 2, 2, emptyList(), "00C0: set_time_of_day 12 0"),
      OpcodeDef(0x00C1, "00C1", "get_time_of_day", "Obtiene la hora y minuto actuales del juego", 2, 2, emptyList(), "00C1: get_time_of_day 0@ 1@"),
      OpcodeDef(0x00D6, "00D6", "if", "Inicia bloque condicional con N condiciones", 1, 1, listOf(ParamType.INTEGER), "00D6: if 0"),
      OpcodeDef(0x00DF, "00DF", "is_char_in_any_car", "Comprueba si el personaje está dentro de algún coche", 1, 1, emptyList(), "00DF: is_char_in_any_car \$PLAYER_ACTOR"),
      OpcodeDef(0x00E1, "00E1", "is_key_pressed", "Comprueba si se presiona una tecla o botón", 2, 2, emptyList(), "00E1: key_pressed 0 15"),
      OpcodeDef(0x0109, "0109", "player_add_money", "Añade dinero al jugador", 2, 2, listOf(ParamType.VARIABLE, ParamType.INTEGER), "0109: player \$PLAYER_CHAR add_money 500"),
      OpcodeDef(0x010A, "010A", "player_remove_money", "Resta dinero al jugador", 2, 2, emptyList(), "010A: player \$PLAYER_CHAR remove_money 200"),
      OpcodeDef(0x010B, "010B", "player_set_money", "Establece el dinero exacto del jugador", 2, 2, emptyList(), "010B: player \$PLAYER_CHAR set_money 999999"),
      OpcodeDef(0x010E, "010E", "player_get_money", "Almacena el dinero del jugador en variable", 2, 2, emptyList(), "010E: player \$PLAYER_CHAR get_money 0@"),
      OpcodeDef(0x010F, "010F", "player_defined", "Comprueba si el jugador está instanciado", 1, 1, emptyList(), "010F: player \$PLAYER_CHAR defined"),
      OpcodeDef(0x0118, "0118", "is_actor_dead", "Comprueba si un personaje ha muerto", 1, 1, emptyList(), "0118: actor \$ACTOR dead"),
      OpcodeDef(0x0119, "0119", "is_car_dead", "Comprueba si un coche está destruido", 1, 1, emptyList(), "0119: car \$CAR dead"),
      OpcodeDef(0x014B, "014B", "set_actor_health", "Establece la vida del personaje", 2, 2, emptyList(), "014B: set_actor \$ACTOR health 100"),
      OpcodeDef(0x014C, "014C", "get_actor_health", "Obtiene la vida del personaje en variable", 2, 2, emptyList(), "014C: get_actor \$ACTOR health 0@"),
      OpcodeDef(0x014D, "014D", "set_actor_armour", "Establece el blindaje del personaje", 2, 2, emptyList(), "014D: set_actor \$ACTOR armour 100"),
      OpcodeDef(0x014E, "014E", "get_actor_armour", "Obtiene el blindaje del personaje", 2, 2, emptyList(), "014E: get_actor \$ACTOR armour 0@"),
      OpcodeDef(0x015D, "015D", "set_camera_position", "Coloca la cámara en posición X Y Z", 6, 6, emptyList(), "015D: set_camera_position 0.0 0.0 10.0 0.0 0.0 0.0"),
      OpcodeDef(0x015F, "015F", "restore_camera", "Restaura la cámara detrás del jugador", 0, 0, emptyList(), "015F: restore_camera"),
      OpcodeDef(0x0164, "0164", "camera_point_at_actor", "Apunta la cámara a un personaje", 3, 3, emptyList(), "0164: camera_point_at_actor \$ACTOR 2 1"),
      OpcodeDef(0x0169, "0169", "fade", "Inicia transición de fundido de pantalla", 2, 2, emptyList(), "0169: fade 0 500"),
      OpcodeDef(0x016A, "016A", "fade_screen", "Comprueba si el fundido de pantalla terminó", 0, 0, emptyList(), "016A: fade_screen"),
      OpcodeDef(0x0180, "0180", "set_actor_heading", "Ajusta la orientación/ángulo del personaje", 2, 2, emptyList(), "0180: set_actor \$ACTOR heading 90.0"),
      OpcodeDef(0x0184, "0184", "set_car_health", "Establece los puntos de salud de un vehículo", 2, 2, emptyList(), "0184: set_car \$CAR health 1000"),
      OpcodeDef(0x0185, "0185", "get_car_health", "Obtiene los puntos de salud de un vehículo", 2, 2, emptyList(), "0185: get_car \$CAR health 0@"),
      OpcodeDef(0x01B2, "01B2", "give_actor_weapon", "Entrega arma y munición al actor", 3, 3, emptyList(), "01B2: give_actor \$ACTOR weapon 24 ammo 100"),
      OpcodeDef(0x01B4, "01B4", "set_player_can_move", "Habilita o congela el movimiento del jugador", 2, 2, listOf(ParamType.VARIABLE, ParamType.INTEGER), "01B4: set_player \$PLAYER_CHAR can_move 1"),
      OpcodeDef(0x01B6, "01B6", "set_weather", "Establece el clima del juego (lluvia, soleado, etc.)", 1, 1, emptyList(), "01B6: set_weather 1"),
      OpcodeDef(0x01B7, "01B7", "set_player_control", "Activa o desactiva los controles del jugador", 2, 2, emptyList(), "01B7: set_player \$PLAYER_CHAR control 1"),
      OpcodeDef(0x01C2, "01C2", "remove_references_to_actor", "Libera la variable de un personaje", 1, 1, emptyList(), "01C2: remove_references_to_actor \$ACTOR"),
      OpcodeDef(0x01C3, "01C3", "remove_references_to_car", "Libera la variable de un vehículo", 1, 1, emptyList(), "01C3: remove_references_to_car \$CAR"),
      OpcodeDef(0x01E3, "01E3", "show_text_1number", "Muestra texto con un valor numérico", 4, 4, emptyList(), "01E3: show_text_1number 'SCORE' 100 2000 1"),
      OpcodeDef(0x01E4, "01E4", "show_text_1number_styled", "Muestra texto estilizado con un número", 4, 4, emptyList(), "01E4: show_text_1number_styled 'VAL' 50 1500 2"),
      OpcodeDef(0x01E5, "01E5", "show_text_2numbers", "Muestra texto con 2 números enteros", 5, 5, emptyList(), "01E5: show_text_2numbers 'HP' 100 100 2000 1"),
      OpcodeDef(0x0208, "0208", "is_actor_in_car", "Comprueba si el actor está dentro de un coche", 2, 2, emptyList(), "0208: actor \$ACTOR in_car \$CAR"),
      OpcodeDef(0x0209, "0209", "is_actor_in_any_car", "Comprueba si el actor está en cualquier coche", 1, 1, emptyList(), "0209: actor \$ACTOR in_any_car"),
      OpcodeDef(0x0223, "0223", "set_actor_health", "Ajusta la salud del personaje a valor entero", 2, 2, emptyList(), "0223: set_actor \$ACTOR health 100"),
      OpcodeDef(0x0226, "0226", "get_actor_health", "Lee la salud del personaje", 2, 2, emptyList(), "0226: get_actor \$ACTOR health 0@"),
      OpcodeDef(0x0229, "0229", "set_car_health", "Ajusta la salud de un vehículo", 2, 2, emptyList(), "0229: set_car \$CAR health 1000"),
      OpcodeDef(0x022C, "022C", "get_car_health", "Lee la salud de un vehículo", 2, 2, emptyList(), "022C: get_car \$CAR health 0@"),
      OpcodeDef(0x0247, "0247", "request_model", "Solicita cargar un modelo de vehículo o ped", 1, 1, emptyList(), "0247: request_model 411"),
      OpcodeDef(0x0248, "0248", "is_model_available", "Comprueba si el modelo solicitado terminó de cargar", 1, 1, emptyList(), "0248: model 411 available"),
      OpcodeDef(0x0249, "0249", "release_model", "Descarga un modelo cargado de la memoria", 1, 1, emptyList(), "0249: release_model 411"),
      OpcodeDef(0x02A3, "02A3", "toggle_player_infinite_run", "Activa o desactiva correr infinito para el jugador", 2, 2, emptyList(), "02A3: toggle_player \$PLAYER_CHAR infinite_run 1"),
      OpcodeDef(0x02AB, "02AB", "set_actor_immunities", "Configura inmunidades del actor (modo Dios)", 6, 6, emptyList(), "02AB: set_actor \$PLAYER_ACTOR immunities 1 1 1 1 1"),
      OpcodeDef(0x02AC, "02AC", "set_car_immunities", "Configura inmunidades completas del vehículo", 6, 6, emptyList(), "02AC: set_car \$CAR immunities 1 1 1 1 1"),
      OpcodeDef(0x02BF, "02BF", "create_pickup", "Crea un pickup (arma, objeto, salud)", 6, 6, emptyList(), "02BF: create_pickup 370 15 0.0 0.0 5.0 to \$PICKUP"),
      OpcodeDef(0x02C0, "02C0", "destroy_pickup", "Elimina un pickup del mapa", 1, 1, emptyList(), "02C0: destroy_pickup \$PICKUP"),
      OpcodeDef(0x02E0, "02E0", "get_actor_z_angle", "Obtiene el ángulo Z del personaje", 2, 2, emptyList(), "02E0: get_actor \$ACTOR z_angle 0@"),
      OpcodeDef(0x02E1, "02E1", "set_actor_z_angle", "Establece el ángulo Z de orientación del personaje", 2, 2, emptyList(), "02E1: set_actor \$ACTOR z_angle 180.0"),
      OpcodeDef(0x02E2, "02E2", "set_actor_heading", "Ajusta la rotación frontal del personaje", 2, 2, emptyList(), "02E2: set_actor \$ACTOR heading 0.0"),
      OpcodeDef(0x02E3, "02E3", "get_actor_heading", "Lee la rotación frontal del personaje", 2, 2, emptyList(), "02E3: get_actor \$ACTOR heading 0@"),
      OpcodeDef(0x0337, "0337", "set_actor_visible", "Hace visible o invisible a un personaje", 2, 2, emptyList(), "0337: set_actor \$ACTOR visible 1"),
      OpcodeDef(0x0362, "0362", "remove_actor", "Elimina inmediatamente un actor", 1, 1, emptyList(), "0362: remove_actor \$ACTOR"),
      OpcodeDef(0x036A, "036A", "car_engine_on", "Enciende o apaga el motor de un coche", 2, 2, emptyList(), "036A: set_car \$CAR engine 1"),
      OpcodeDef(0x038B, "038B", "load_requested_models", "Fuerza la carga de modelos solicitados en memoria", 0, 0, emptyList(), "038B: load_requested_models"),
      OpcodeDef(0x0395, "0395", "clear_actor_objective", "Cancela cualquier acción u objetivo actual del actor", 1, 1, emptyList(), "0395: clear_actor \$ACTOR objective"),
      OpcodeDef(0x03A4, "03A4", "name_thread", "Asigna un nombre al hilo del script", 1, 1, listOf(ParamType.STRING_SHORT), "03A4: name_thread 'MYMOD'"),
      OpcodeDef(0x03AB, "03AB", "set_car_color", "Cambia los colores primario y secundario de un coche", 3, 3, emptyList(), "03AB: set_car \$CAR color 1 1"),
      OpcodeDef(0x03AC, "03AC", "set_car_numberplate", "Asigna un texto a la matrícula de un vehículo", 2, 2, emptyList(), "03AC: set_car \$CAR numberplate 'SAN_AND'"),
      OpcodeDef(0x03C0, "03C0", "car_door_lock", "Bloquea o desbloquea las puertas de un coche", 2, 2, emptyList(), "03C0: car \$CAR door_lock 2"),
      OpcodeDef(0x03D0, "03D0", "warp_actor_into_car", "Teletransporta a un actor al asiento de conductor", 2, 2, emptyList(), "03D0: warp_actor \$ACTOR into_car \$CAR"),
      OpcodeDef(0x03F0, "03F0", "make_actor_leave_car", "Hace que el actor baje del coche", 1, 1, emptyList(), "03F0: make_actor \$ACTOR leave_car"),
      OpcodeDef(0x0407, "0407", "create_car_generator", "Crea un punto de generación permanente de coches", 9, 9, emptyList(), "0407: create_car_generator 0.0 0.0 5.0 90.0 411 1 1 0 0 to \$GEN"),
      OpcodeDef(0x0430, "0430", "set_car_speed", "Establece la velocidad de avance de un vehículo", 2, 2, emptyList(), "0430: set_car \$CAR speed 40.0"),
      OpcodeDef(0x0441, "0441", "get_car_speed", "Obtiene la velocidad de un vehículo en variable", 2, 2, emptyList(), "0441: get_car \$CAR speed 0@"),
      OpcodeDef(0x0446, "0446", "is_actor_shooting", "Comprueba si el personaje está disparando", 1, 1, emptyList(), "0446: actor \$ACTOR shooting"),
      OpcodeDef(0x0470, "0470", "actor_task_walk_to_coords", "Ordena al actor caminar hacia coordenadas X Y Z", 5, 5, emptyList(), "0470: actor \$ACTOR walk_to 0.0 0.0 5.0 1 -1"),
      OpcodeDef(0x04C4, "04C4", "store_actor_coords_to", "Guarda la posición X Y Z del actor en variables", 4, 4, emptyList(), "04C4: store_actor \$ACTOR coords_to 0@ 1@ 2@"),
      OpcodeDef(0x04E4, "04E4", "refresh_streaming", "Refresca el streaming del mapa", 2, 2, emptyList(), "04E4: refresh_streaming 0.0 0.0"),
      OpcodeDef(0x051E, "051E", "set_actor_stay_in_car", "Impide que el actor sea expulsado del coche", 2, 2, emptyList(), "051E: set_actor \$ACTOR stay_in_car 1"),
      OpcodeDef(0x054C, "054C", "is_actor_touching_vehicle", "Comprueba si el actor toca el vehículo", 2, 2, emptyList(), "054C: actor \$ACTOR touching_car \$CAR"),
      OpcodeDef(0x0555, "0555", "remove_weapon_from_actor", "Quita un arma específica al actor", 2, 2, emptyList(), "0555: remove_weapon 22 from_actor \$PLAYER_ACTOR"),
      OpcodeDef(0x05E2, "05E2", "give_actor_weapon", "Entrega arma y munición personalizada", 3, 3, emptyList(), "05E2: give_actor \$ACTOR weapon 31 ammo 500"),
      OpcodeDef(0x0605, "0605", "set_actor_walk_style", "Cambia el estilo de animación de caminado del actor", 2, 2, emptyList(), "0605: set_actor \$ACTOR walk_style 'SWAGGER'"),
      OpcodeDef(0x06A8, "06A8", "actor_driveby_car", "Ordena disparar desde el coche en movimiento", 7, 7, emptyList(), "06A8: actor \$ACTOR driveby \$TARGET 0 0.0 0.0 0.0 100.0 4"),
      OpcodeDef(0x0A96, "0A96", "get_ped_pointer", "CLEO: Obtiene el puntero de memoria del actor", 2, 2, emptyList(), "0A96: 0@ = actor \$PLAYER_ACTOR pointer"),
      OpcodeDef(0x0A97, "0A97", "get_vehicle_pointer", "CLEO: Obtiene el puntero de memoria de un vehículo", 2, 2, emptyList(), "0A97: 0@ = car \$CAR pointer"),
      OpcodeDef(0x0ACA, "0ACA", "show_text_box", "CLEO: Muestra una caja de texto de ayuda", 1, 1, listOf(ParamType.STRING_LONG), "0ACA: show_text_box \"Script activado\""),
      OpcodeDef(0x0ACB, "0ACB", "show_styled_text", "CLEO: Muestra texto estilizado personalizado", 3, 3, listOf(ParamType.STRING_LONG, ParamType.INTEGER, ParamType.INTEGER), "0ACB: show_styled_text \"MOD ON\" 2000 1"),
      OpcodeDef(0x0DD2, "0DD2", "read_memory_float", "CLEO: Lee un número flotante de la memoria", 4, 4, emptyList(), "0DD2: read_memory 0xBAA420 size 4 vp 0 to 0@"),
      OpcodeDef(0x0DD4, "0DD4", "read_memory_int", "CLEO: Lee un entero de una dirección de memoria", 4, 4, emptyList(), "0DD4: read_memory 0xBAA420 size 4 vp 0 to 0@")
    )
    defaultList.forEach { registerOfficial(it) }
  }
}
