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
import com.github.slugger.summarize.DataStore

switch(params?.task.toLowerCase()) {
	case 'add':
		DataStore.instance.addTask(params.name, params.desc); break
	case 'link':
		DataStore.instance.link(params.prod.toLong(), request.getParameterValues('tasks').collect { it.toLong() } as long[]); break
	case 'reorder':
		def keys = params.keySet().findAll { it.startsWith('lt_') }
		keys.each {
			def id = it.split('_', 2)[1].toLong()
			def lt = DataStore.instance.getLinkedTaskById(id)
			lt.order = params[it].toInteger()
			DataStore.instance.updateLinkedTask(lt)
		}
		break
	default: throw new RuntimeException("Unsupported task: $params.task")
}
response.sendRedirect('../config.groovy')