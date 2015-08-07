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
import groovy.json.JsonSlurper

import com.github.slugger.summarize.DataStore

def json = new JsonSlurper().parse(request.inputStream)

def ds = DataStore.instance
def prodId = ds.getProductId(json.product, '2.0.0')
def bldId = ds.registerBuild(prodId, json.build)
def taskId = ds.getTaskId(json.task)
ds.updateBuild(bldId, ds.getTaskLinkId(taskId, prodId), json.state.toUpperCase(), json.status, json.url, null, null, json.author ?: 'unknown')