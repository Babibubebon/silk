/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.workspace

import java.io._
import java.util.logging.Logger

import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceLoader}
import org.silkframework.util.Identifier

class Workspace(val provider: WorkspaceProvider) {

  private val logger = Logger.getLogger(classOf[Workspace].getName)

  private var cachedProjects = loadProjects()

  def projects: Seq[Project] = cachedProjects

  /**
   * Retrieves a project by name.
   *
   * @throws java.util.NoSuchElementException If no project with the given name has been found
   */
  def project(name: Identifier): Project = {
    projects.find(_.name == name).getOrElse(throw new ProjectNotFoundException(name))
  }

  def createProject(name: Identifier) = {
    require(!cachedProjects.exists(_.name == name), "A project with the name '" + name + "' already exists")

    val projectConfig = ProjectConfig(name)
    provider.putProject(projectConfig)
    val newProject = new Project(projectConfig, provider)
    cachedProjects :+= newProject
    newProject
  }

  def removeProject(name: Identifier) = {
    provider.deleteProject(name)
    cachedProjects = cachedProjects.filterNot(_.name == name)
  }

  def exportProject(name: Identifier, outputStream: OutputStream, marshaller: ProjectMarshallingTrait): String = {
    provider.exportProject(name, outputStream, marshaller)
  }

  def importProject(name: Identifier,
                    inputStream: InputStream,
                    marshaller: ProjectMarshallingTrait) {
    provider.importProjectMarshaled(name, inputStream, marshaller)
    reload()
  }

  def reload() {
    cachedProjects = loadProjects()
  }

  private def loadProjects(): Seq[Project] = {
    for(projectConfig <- provider.readProjects()) yield {
      logger.info("Loading project: " + projectConfig.id)
      new Project(projectConfig, provider)
    }
  }
}