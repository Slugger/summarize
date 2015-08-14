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

@ToString
@EqualsAndHashCode(includes='id')
class Comment {
	final long id
	final String author
	String comment
	final Date created
	Date updatedAt
	String updatedBy
	
	private Comment(def input) {
		id = input.id
		author = input.author
		comment = input.comment
		created = input.created
		updatedAt = input.updated
		//updatedBy = input.updated_by
	}
	
	Comment(GroovyRowResult input) { this((Object)input) }
	Comment(GroovyResultSet input) { this((Object)input) }
}
