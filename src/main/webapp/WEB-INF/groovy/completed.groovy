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
def tasks = data.getTasks('SIQ4L', '2.0.0')
def html = new MarkupBuilder(out)
html.html {
	head {
		body {
			table(border: '1') {
				tr {
					th('Build')
					tasks.each { th(it) }
					th('Latest Comment')
				}
				data.getBuilds('SIQ4L', '2.0.0').each { bld, cTasks ->
					def bldId = null
					tr {
						td(bld)
						tasks.each {
							def result = cTasks.find { k, v -> k == it }?.value
							td {
								if(result) {
									div("$result.stateDesc ($result.state)")
									div(result.finish.format('M/d HH:mm'))
									bldId = result.buildId
								} else
									span('N/A')
							}
						}
						def note = bldId != null ? data.getLatestComment(bldId) : null
						td(note ? "[By $note.author on $note.date] $note.note" : '[No comments]')
					}
				}
			}
		}
	}
}
out << "<!--\n${data.getBuilds('SIQ4L', '2.0.0')}\n-->"