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

def verInput = params.prod?.split('\\/', 2)
def data = DataStore.instance
def prod = verInput ? data.products.find { it.name == verInput[0] && it.version == verInput[1] } : data.products[0]
def tasks = data.getTasks(prod)
def html = new MarkupBuilder(out)
out << '<!DOCTYPE html>\n'
html.html {
	head {
		title('Summarize: Deliverable Status')
		script(src: '/static/jquery/jquery.min.js', '')
		script(src: '/static/js/kickstart.js', '')
		link(rel: 'stylesheet', href: '/static/css/kickstart.css')
		link(rel: 'stylesheet', href: '/static/css/summarize.css')
	}
	body {
		div('class': 'col_12', style: 'padding: 75px;') {
			div {
				form(method: 'GET', action: request.requestURI) {
					select(name: 'prod') {
						option(value: '', '----- Select -----')
						data.products.each {
							option(value: "$it.name/$it.version", "$it.name/$it.version")
						}
					}
					input(type: 'submit', name: 'submit', value: 'Go')
				}
			}
			if(prod) {
				h3("$prod.name/$prod.version")
				div {
					small {
						i("All dates are ${TimeZone.default.displayName}")
					}
				}
				table('class': 'sortable') {
					thead {
						tr {
							th('Build')
							tasks.each { th(it.name) }
						}
					}
					tbody {
						data.getBuildsForProduct(prod).each { bld, cTasks ->
							def bldId = null
							tr {
								td(bld.build)
								tasks.each {
									def result = cTasks.find { ct -> ct.linkedTask.task.name == it.name }
									def state = result?.state
									td(value: state ?: '-1', 'class': "summarize-state-${state?.toLowerCase() ?: 'none'}") {
										if(result) {
											div('class': 'center', result.description)
											def date = result.finish ?: result.start
											div('class': 'center', 'style': 'font-size: 10px;', date.format('M/d HH:mm'))
											bldId = result.build.id
											div {
												if(result.url)
													a('class': 'fa fa-external-link', href: result.url, target: '_blank', '')
												a('class': 'fa fa-comment', href: "${request.contextPath}/comment.groovy?ctid=${result.id}", data.getComments(result).size() ?: '')
													
											}
										} else
											div('class': 'center', 'N/A')
									}
								}
							}
						}	
					}
				}
			}
		}
	}
}