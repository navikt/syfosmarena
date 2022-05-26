package no.nav.syfo

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.rules.Description
import no.nav.syfo.rules.Rule
import no.nav.syfo.rules.ValidationRuleChain
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KClass

class RuleChainDocGenSpek : FunSpec({
    fun <T : Annotation> Any.enumAnnotationValue(type: KClass<out T>, enumName: String): T? =
        if (javaClass.getField(enumName)?.isAnnotationPresent(type.java) == true) {
            javaClass.getField(enumName).getAnnotation(type.java)
        } else {
            null
        }

    context("Generate docs for rule chains") {
        test("Generates a CSV file with rule chain") {
            val basePath = Paths.get("build", "reports")
            Files.createDirectories(basePath)
            val ruleCSV = arrayOf("Regel navn;Regel ID;Beskrivelse")
                .union(
                    listOf<List<Rule<*>>>(ValidationRuleChain.values().toList()).flatten()
                        .map { rule ->
                            "${rule.name};${rule.ruleId
                                ?: ""};${rule.enumAnnotationValue(Description::class, rule.name)?.description ?: ""}"
                        }
                )
            val csvFile = basePath.resolve("rules.csv")
            Files.write(csvFile, ruleCSV, Charsets.UTF_8)
        }
    }
})
