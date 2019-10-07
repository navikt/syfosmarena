package no.nav.syfo

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KClass
import no.nav.syfo.rules.Description
import no.nav.syfo.rules.Rule
import no.nav.syfo.rules.ValidationRuleChain
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object RuleChainDocGenSpek : Spek({
    fun <T : Annotation> Any.enumAnnotationValue(type: KClass<out T>, enumName: String): T? =
            if (javaClass.getField(enumName)?.isAnnotationPresent(type.java) == true) {
            javaClass.getField(enumName).getAnnotation(type.java)
        } else {
            null
        }

    describe("Generate docs for rule chains") {
        it("Generates a CSV file with rule chain") {
            val basePath = Paths.get("build", "reports")
            Files.createDirectories(basePath)
            val ruleCSV = arrayOf("Regel navn;Regel ID;Beskrivelse")
                    .union(listOf<List<Rule<*>>>(ValidationRuleChain.values().toList()).flatten()
                    .map { rule ->
                        "${rule.name};${rule.ruleId
                                ?: ""};${rule.enumAnnotationValue(Description::class, rule.name)?.description ?: ""}"
                    })
            val csvFile = basePath.resolve("rules.csv")
            Files.write(csvFile, ruleCSV, Charsets.UTF_8)
        }
    }
})
