/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package uk.ac.bournemouth.darwin.architecture

import com.structurizr.Workspace
import com.structurizr.api.StructurizrClient
import com.structurizr.io.json.JsonWriter
import com.structurizr.model.Model
import com.structurizr.model.SoftwareSystem
import com.structurizr.model.Tags
import com.structurizr.view.Shape
import com.structurizr.view.Styles
import com.structurizr.view.ViewSet
import java.io.FileWriter

/*
 * Created by pdvrieze on 10/04/16.
 */

fun main(args:Array<String>) {
  val workspaceId = if(args.size>=3) args[0].toLong() else 1
  val apiKey = if (args.size >= 3) args[1] else null
  val apiSecret = if (args.size >= 3) args[2] else null
  System.err.println("Hello ${apiKey ?: "<missing key>"}, ${apiSecret ?: "<missing secret>"}")

  val workspace = createWorkspace()

  // create your model, views and styles

  outputWorkspaceToFile(workspace)
  if (!(apiKey.isNullOrBlank() || apiSecret.isNullOrBlank())) {
    uploadWorkspaceToStructurizr(workspace, workspaceId, apiKey!!, apiSecret!!)
  }

}

class Entities(model: Model) {

  val user = model.addPerson("user", "A regular user of the system")
  val administrator = model.addPerson("administrator", "An administrator of the system")

}

class DarwinSystem(val system: SoftwareSystem) {
  val processEngine = system.addContainer("ProcessEngine", "The process engine", "Java War")
  val taskManager = system.addContainer("PEUserMessageHandler", "Task handler", "Java War")
  val accountmgr = system.addContainer("accountmgr", "Account management interface", "Java War")
  val darwinServives = system.addContainer("darwinServices", "Miscelaneous services", "Java War")
  val messenger = system.addContainer("DarwinMessenger", "Messenger that provides messenging for the other components.", "Java War")
  val containerApi = system.addContainer("ContainerAPI", "Code available in the container classpath to facilitate other parts", "Jar on container classpath")
  val darwinRealm = system.addContainer("DarwinRealm", "Authentication of users and provision of roles", "Tomcat/7 Jar")
  val darwinGenerators = system.addContainer("DarwinGenerators", "Code generator for api access", "Standalone Jar Application")
  val androidAuth = system.addContainer("android-auth", "Android account provider", "Android APK")
  val PMEditor = system.addContainer("PMEditor", "Android interface", "Android APK")
}

class Systems(model:Model) {
  val darwinpmi = DarwinSystem(model.addSoftwareSystem("Darwin PMI", "The process management interface"))
  val soapServices = model.addSoftwareSystem("External SOAP Services", "Soap based external services")
  val restServices = model.addSoftwareSystem("External REST Services", "Rest based external services")
  val email = model.addSoftwareSystem("External Email Server", "Email server")
}

class Views(val viewSet:ViewSet, darwinPmi:DarwinSystem, entities: Entities) {
  val overallSystemView = viewSet.createContextView(darwinPmi.system, "The overall system").apply {
    addAllPeople()
    addAllSoftwareSystems()
  }
  val containerView = viewSet.createContainerView(darwinPmi.system, "The various bits making up darwinPMI"). apply {
    addAllContainers()
  }
}

class Containers(systems: Systems) {
}

private fun createWorkspace(): Workspace {
  val workspace = Workspace("Darwin PMI", "Darwin PMI is a collection of software for lightweight business process management")
  val model = workspace.getModel()
  val entities = Entities(model)
  val systems = Systems(model)
  val containers = Containers(systems)

  setSytemRelations(entities, systems)

  val views = Views(workspace.getViews(), systems.darwinpmi, entities)



  val styles = views.viewSet.getConfiguration().getStyles()
//  styles.addElementStyle(Tags.PERSON).shape(Shape.Person)
  return workspace
}

private fun setSytemRelations(entities: Entities,
                              systems: Systems) {
  systems.darwinpmi.system.uses(systems.email, "(Optionally) sends messages through email")
  systems.darwinpmi.system.uses(systems.soapServices, "The process models invoke soap services")
  systems.darwinpmi.system.uses(systems.restServices, "The process models invoke rest services")
  entities.user.uses(systems.darwinpmi.system, "Uses")
  entities.administrator.uses(systems.darwinpmi.system, "Administrates")
}

fun outputWorkspaceToFile(workspace: Workspace) {
  FileWriter("darwinpmi.json").use { fileWriter ->
    val jsonWriter = JsonWriter(true)
    jsonWriter.write(workspace, fileWriter)
  }
}

fun uploadWorkspaceToStructurizr(workspace: Workspace, workspaceId:Long, apiKey: String, apiSecret: String) {
  val structurizrClient = StructurizrClient(apiKey, apiSecret)
  structurizrClient.workspaceArchiveLocation=null // Don't keep the file around. We save it here
  structurizrClient.mergeWorkspace(workspaceId, workspace)
}
