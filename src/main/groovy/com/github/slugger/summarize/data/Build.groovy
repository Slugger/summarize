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
class Build {
	final long id
	final Product product
	final String build
	
	private Build(def input) {
		id = input.id
		build = input.build
		product = DataStore.instance.getProductById(input.prod_id)
	}
	
	Build(GroovyRowResult input) { this((Object)input) }
	Build(GroovyResultSet input) { this((Object)input) }
}
