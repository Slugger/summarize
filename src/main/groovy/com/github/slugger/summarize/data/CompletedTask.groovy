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
package com.github.slugger.summarize.data

import groovy.sql.GroovyResultSet
import groovy.sql.GroovyRowResult
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import com.github.slugger.summarize.DataStore

@ToString
@EqualsAndHashCode(includes='id')
class CompletedTask {
	final Long id
	final LinkedTask linkedTask
	final Build build
	Date start
	Date finish
	Date updatedAt
	String updatedBy
	String state
	String url
	String description
	
	private CompletedTask(def input) {
		id = input.id
		linkedTask = DataStore.instance.getLinkedTaskById(input.does_task_id)
		build = DataStore.instance.getBuildById(input.build_id)
		start = input.start
		finish = input.finish
		updatedAt = input.updated
		updatedBy = input.updated_by
		state = input.state
		url = input.url
		description = input.brief_desc
	}
	
	CompletedTask(GroovyRowResult input) { this((Object)input) }
	CompletedTask(GroovyResultSet input) { this((Object)input) }
	CompletedTask(LinkedTask lt, Build b, String desc, String state = 'RUN', String url = null, String updatedBy = null, Date start = null, Date finish = null, Integer taskOrder = null) {
		id = null
		linkedTask = lt
		if(taskOrder != null)
			lt.order = taskOrder
		build = b
		description = desc
		this.state = state
		this.url = url
		this.updatedBy = updatedBy ?: 'automation'
		updatedAt = new Date()
		this.start = start ?: updatedAt
		this.finish = finish
	}
}
