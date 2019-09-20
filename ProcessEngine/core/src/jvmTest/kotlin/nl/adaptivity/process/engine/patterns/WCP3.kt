/*
 * Copyright (c) 2017.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine.patterns

import nl.adaptivity.process.engine.ModelData
import nl.adaptivity.process.engine.ModelSpek
import nl.adaptivity.process.engine.TestConfigurableModel
import nl.adaptivity.process.engine.trace
import nl.adaptivity.process.processModel.configurableModel.*

private const val WCP3_expectedJson = "{\"name\":\"WCP3\",\"owner\":\"pdvrieze\",\"roles\":null,\"uuid\":null,\"childModel\":[],\"import\":[],\"export\":[],\"nodes\":[" +
                                      "[\"nl.adaptivity.process.processModel.engine.XmlStartNode\",{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"start\",\"label\":null}]," +
                                      "[\"nl.adaptivity.process.processModel.engine.XmlSplit\",{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"split\",\"label\":null,\"min\":2,\"max\":2,\"predecessor\":\"start\"}]," +
                                      "[\"nl.adaptivity.process.processModel.engine.XmlActivity\",{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"ac1\",\"label\":null,\"predecessor\":\"split\",\"message\":null,\"childId\":null}]," +
                                      "[\"nl.adaptivity.process.processModel.engine.XmlActivity\",{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"ac2\",\"label\":null,\"predecessor\":\"split\",\"message\":null,\"childId\":null}]," +
                                      "[\"nl.adaptivity.process.processModel.engine.XmlJoin\",{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"join\",\"label\":null,\"min\":2,\"max\":2,\"isMultiMerge\":false,\"predecessors\":[{\"predecessor\":\"ac1\"},{\"predecessor\":\"ac2\"}]}]," +
                                      "[\"nl.adaptivity.process.processModel.engine.XmlEndNode\",{\"isMultiInstance\":false,\"x\":NaN,\"y\":NaN,\"define\":[],\"result\":[],\"id\":\"end\",\"label\":null,\"predecessor\":\"join\"}]]}"

class WCP3: ModelSpek(run{
  val model = object: TestConfigurableModel("WCP3") {
    val start by startNode
    val split by split(start) { min = 2; max = 2 }
    val ac1   by activity(split)
    val ac2   by activity(split)
    val join  by join(ac1, ac2){ min = 2; max = 2 }
    val end   by endNode(join)
  }
  val validTraces =  with(model) { trace {
    (start ..(ac1 % ac2)) * (split % join % end)
  } }

  val invalidTraces = with(model) { trace {
    join or end or split or
      (start .. (split or end or join or
        (ac1 or
          ac2) * (split or join or end)
                ))
  }}
  ModelData(model, validTraces, invalidTraces)
}, modelJson = WCP3_expectedJson)
