package no.gruppe7.amate.feature.achievements.domain

// Hva et merke måles på. Nye typer legges til her, og evaluatoren får én linje til.
enum class AchievementType {
    WATER_TOTAL_ML,      // alt vann noensinne, i ml
    WATER_GOAL_DAYS,     // antall dager vannmålet er nådd
    STEPS_BEST_DAY,      // flest skritt på én dag
    STEPS_TOTAL,         // alle skritt noensinne
    TASKS_COMPLETED,     // gjøremål fullført totalt
}

// Ett merke. Koden er stabil og lagres i databasen; tittel og tekst ligger i achievements.xml.
// Terskelen ligger her, i kode, ikke i databasen. Da kan vi justere den uten migrering.
data class AchievementDefinition(
    val code: String,
    val type: AchievementType,
    val threshold: Long,
)

// Katalogen over alle merkene i appen. Rekkefølgen her er rekkefølgen på skjermen.
object AchievementCatalog {
    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition("WATER_FIRST", AchievementType.WATER_TOTAL_ML, 1),
        AchievementDefinition("WATER_10L", AchievementType.WATER_TOTAL_ML, 10_000),
        AchievementDefinition("WATER_GOAL_3", AchievementType.WATER_GOAL_DAYS, 3),
        AchievementDefinition("WATER_GOAL_7", AchievementType.WATER_GOAL_DAYS, 7),
        AchievementDefinition("STEPS_10K_DAY", AchievementType.STEPS_BEST_DAY, 10_000),
        AchievementDefinition("STEPS_100K", AchievementType.STEPS_TOTAL, 100_000),
        AchievementDefinition("TASKS_FIRST", AchievementType.TASKS_COMPLETED, 1),
        AchievementDefinition("TASKS_25", AchievementType.TASKS_COMPLETED, 25),
    )

    fun byCode(code: String): AchievementDefinition? = all.firstOrNull { it.code == code }
}

// Tallene evaluatoren trenger. ViewModel regner dem ut fra vann, skritt og gjøremål.
// null betyr «vet ikke ennå» (laster eller feilet), ikke 0. Da deler vi ikke ut merker på feil grunnlag.
data class AchievementStats(
    val waterTotalMl: Long? = null,
    val waterGoalDays: Int? = null,
    val stepsBestDay: Long? = null,
    val stepsTotal: Long? = null,
    val tasksCompleted: Int? = null,
) {
    // Verdien som hører til en type. Samler oppslaget ett sted så evaluatoren blir enkel.
    fun valueFor(type: AchievementType): Long? = when (type) {
        AchievementType.WATER_TOTAL_ML -> waterTotalMl
        AchievementType.WATER_GOAL_DAYS -> waterGoalDays?.toLong()
        AchievementType.STEPS_BEST_DAY -> stepsBestDay
        AchievementType.STEPS_TOTAL -> stepsTotal
        AchievementType.TASKS_COMPLETED -> tasksCompleted?.toLong()
    }
}

// Avgjør hvilke merker som er oppnådd. Ren funksjon: samme tall inn gir alltid samme svar.
class AchievementEvaluator(
    private val catalog: List<AchievementDefinition> = AchievementCatalog.all,
) {
    // Kodene til alle merker der tallet er kjent og har nådd terskelen.
    // Ukjente tall (null) gir aldri et merke, så en lesefeil kan ikke dele ut noe feilaktig.
    fun earned(stats: AchievementStats): Set<String> =
        catalog.filter { definition ->
            val value = stats.valueFor(definition.type)
            value != null && value >= definition.threshold
        }.map { it.code }.toSet()

    // Hvor langt brukeren er fra et merke, mellom 0 og 1. null når tallet er ukjent.
    fun progress(definition: AchievementDefinition, stats: AchievementStats): Float? =
        stats.valueFor(definition.type)?.let { value ->
            (value.toFloat() / definition.threshold).coerceIn(0f, 1f)
        }
}