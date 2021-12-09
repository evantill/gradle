/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.configurationcache

import org.gradle.integtests.fixtures.configurationcache.ConfigurationCacheFixture

class ConfigurationCacheIncompatibleTasksIntegrationTest extends AbstractConfigurationCacheIntegrationTest {
    ConfigurationCacheFixture fixture = new ConfigurationCacheFixture(this)

    def "reports incompatible task serialization and execution problems and discards cache entry when task is scheduled"() {
        addTasksWithProblems()

        when:
        configurationCacheRun("declared")

        then:
        result.assertTasksExecuted(":declared")
        fixture.assertStateStoredAndDiscarded {
            serializationProblem("Task `:declared` of type `Broken`: cannot serialize object of type 'org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer', a subtype of 'org.gradle.api.artifacts.ConfigurationContainer', as these are not supported with the configuration cache.")
        }

        when:
        configurationCacheRun("declared")

        then:
        result.assertTasksExecuted(":declared")
        fixture.assertStateStoredAndDiscarded {
            serializationProblem("Task `:declared` of type `Broken`: cannot serialize object of type 'org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer', a subtype of 'org.gradle.api.artifacts.ConfigurationContainer', as these are not supported with the configuration cache.")
        }
    }

    def "problems in tasks that are not marked incompatible are treated as failures when incompatible tasks are also scheduled"() {
        addTasksWithProblems()

        when:
        configurationCacheFails("declared", "notDeclared")

        then:
        result.assertTasksExecuted(":declared", ":notDeclared")
        fixture.assertStateStoredAndDiscarded {
            serializationProblem("Task `:declared` of type `Broken`: cannot serialize object of type 'org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer', a subtype of 'org.gradle.api.artifacts.ConfigurationContainer', as these are not supported with the configuration cache.")
            serializationProblem("Task `:notDeclared` of type `Broken`: cannot serialize object of type 'org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer', a subtype of 'org.gradle.api.artifacts.ConfigurationContainer', as these are not supported with the configuration cache.")
        }

        when:
        configurationCacheFails("declared", "notDeclared")

        then:
        result.assertTasksExecuted(":declared", ":notDeclared")
        fixture.assertStateStoredAndDiscarded {
            serializationProblem("Task `:declared` of type `Broken`: cannot serialize object of type 'org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer', a subtype of 'org.gradle.api.artifacts.ConfigurationContainer', as these are not supported with the configuration cache.")
            serializationProblem("Task `:notDeclared` of type `Broken`: cannot serialize object of type 'org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer', a subtype of 'org.gradle.api.artifacts.ConfigurationContainer', as these are not supported with the configuration cache.")
        }
    }

    private addTasksWithProblems() {
        buildFile("""
            class Broken extends DefaultTask {
                private final configurations = project.configurations

                @TaskAction
                void execute() {
//                    project.configurations
                }
            }

            tasks.register("declared", Broken) {
                notCompatibleWithConfigurationCache("retains configuration container")
            }

            tasks.register("notDeclared", Broken) {
            }

            tasks.register("ok") {
                doLast { }
            }
        """)
    }
}
