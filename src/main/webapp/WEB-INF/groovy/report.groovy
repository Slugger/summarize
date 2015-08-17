/*
 Copyright 2015 Battams, Derek
 
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
 
		http://www.apache.org/licenses/LICENSE-2.0
 
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import com.github.slugger.summarize.DataStore
import com.github.slugger.summarize.data.CompletedTask


try {
	def json = new JsonSlurper().parse(request.inputStream)

	def ds = DataStore.instance
	def prod = ds.getProduct(json.product, json.version)
	def task = ds.getTaskByName(json.task)
	def cTasks = ds.getBuildsForProduct(prod)
	def bld = cTasks.keySet().find { it.build == json.build } ?: ds.registerBuild(prod.id, json.build)
	def cTask = cTasks[bld]?.find { it.linkedTask.task.name == json.task }
	if(cTask) {
		cTask.start = new Date()
		cTask.finish = json.finish ? new Date(json.finish.toLong()) : null
		cTask.updatedAt = cTask.start
		cTask.updatedBy = json.author ?: 'automation'
		cTask.state = json.state
		cTask.url = json.url
		cTask.description = json.status
	} else {
		cTask = new CompletedTask(ds.getLinkedTasksForProduct(prod).find { it.task.name == task.name }, bld, json.status, json.state, json.url, json.author, new Date(), null)
	}
	ds.updateBuild(cTask)
	response.contentType = 'application/json'
	out << JsonOutput.toJson([result: 'OK'])
} catch(Throwable t) {
	def w = new CharArrayWriter()
	t.printStackTrace(new PrintWriter(w))
	out << JsonOutput.toJson([result: 'ERROR', error: w.toString()])
}