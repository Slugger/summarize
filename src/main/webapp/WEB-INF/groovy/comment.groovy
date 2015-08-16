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

def data = DataStore.instance
if(request.method == 'POST')
	data.addComment(params.ctid.toLong(), params.author, params.comment)
def ct = data.getCompletedTaskById(params.ctid.toLong())
def html = new MarkupBuilder(out)
out << '<!DOCTYPE html>\n'
html.html {
	head {
		title("Summarize: Comments for $ct.linkedTask.product.name/$ct.linkedTask.product.version/$ct.build.build/$ct.linkedTask.task.name")
		script(src: '/static/jquery/jquery.min.js', '')
		script(src: '/static/js/kickstart.js', '')
		link(rel: 'stylesheet', href: '/static/css/kickstart.css')
		link(rel: 'stylesheet', href: '/static/css/summarize.css')
	}
	body {
		div('class': 'col_4') {
			div {
				p('Add comment for:')
				ul {
					li("Product: $ct.linkedTask.product.name/$ct.linkedTask.product.version")
					li("Build: $ct.build.build")
					li("Task: $ct.linkedTask.task.name")
					li {
						span("State: ")
						span('class': "summarize-state-${ct.state.toLowerCase()}", ct.state)
					}
				}
			}
			div {
				form(method: 'POST', action: request.requestURI) {
					input(type: 'hidden', name: 'ctid', value: params.ctid)
					div {
						label('for': 'author', 'Email:')
						input(type: 'text', name: 'author')
					}
					div {
						label('for': 'comment', 'Comment:')
						textarea(name: 'comment', '')
					}
					input(type: 'submit', name: 'submit', value: 'Add')
				}
			}
		}
		div('class': 'col_8') {
			div {
				h3('Notes')
				div {
					a('class': 'button small', href: 'status.groovy') {
						i('class': 'fa fa-arrow-left', '')
						span(style: 'padding-left: 3px;', 'Return to Status')
					}
				}
				data.getComments(ct).each { cmnt ->
					div("$cmnt.author wrote on $cmnt.created")
					div {
						pre(cmnt.comment)
					}
					hr()
				}
			}
		}
	}
}