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
package com.github.slugger.summarize

import groovy.util.logging.Log4j

import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

@Log4j
class InitDbListener implements ServletContextListener {

	static private initDone = false
	
	static private synchronized initDb() {
		if(!initDone) {
			try {
				DataStore.instance
				initDone = true
			} catch(Throwable t) {
				log.fatal('DB init error!', t)
			}
		}
	}
	
	@Override
	void contextInitialized(ServletContextEvent sce) { initDb() }

	@Override
	void contextDestroyed(ServletContextEvent sce) {}
}
