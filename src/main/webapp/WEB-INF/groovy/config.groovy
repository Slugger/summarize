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
import groovy.xml.MarkupBuilder

import com.github.slugger.summarize.DataStore

def html = new MarkupBuilder(out)
html.html {
	head {
		title('Summarize Configuration')
	}
	body {
		h1('Add Product')
		form(method: 'POST', action: 'config/prod.groovy') {
			div {
				label('for': 'name', 'Name:')
				input(type: 'text', name: 'name')
			}
			div {
				label('for': 'version', 'Version:')
				input(type: 'text', name: 'version')
			}
			div {
				label('for': 'desc', 'Description:')
				input(type: 'text', name: 'desc')
			}
			div {
				input(type: 'submit', name: 'task', value: 'Add')
			}
		}
		
		h1('Add Task')
		form(method: 'POST', action: 'config/task.groovy') {
			div {
				label('for': 'name', 'Name:')
				input(type: 'text', name: 'name')
			}
			div {
				label('for': 'desc', 'Description:')
				input(type: 'text', name: 'desc')
			}
			div {
				input(type: 'submit', name: 'task', value: 'Add')
			}
		}
		
		h1('Link Tasks')
		form(method: 'POST', action: 'config/task.groovy') {
			div {
				label('for': 'prod', 'Product:')
				select(name: 'prod') {
					option(value: '', '-- Select --')
					DataStore.instance.products.each { k, v ->
						option(value: k, v)
					}
				}
			}
			div {
				label('for': 'tasks', 'Tasks:')
				select(name: 'tasks', multiple: 'multiple') {
					DataStore.instance.tasks.each { k, v ->
						option(value: k, v)
					}
				}
			}
			div {
				input(type: 'submit', name: 'task', value: 'Link')
			}
		}
	}
}