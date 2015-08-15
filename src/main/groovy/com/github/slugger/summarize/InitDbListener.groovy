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

import org.apache.log4j.Level
import org.apache.log4j.Logger

@Log4j
class InitDbListener implements ServletContextListener {

	static private initDone = false
	static private shutdownDone = false
	
	static private synchronized initDb() {
		if(!initDone) {
			try {
				DataStore.instance
				Logger.rootLogger.level = Level.toLevel(DataStore.instance.getSetting(LogSettings.LOG_SETTING_VAR, LogSettings.LOG_SETTING_DEFAULT))
				initDone = true
			} catch(Throwable t) {
				log.fatal('DB init error!', t)
			}
		}
	}
	
	static private synchronized shutdownDb() {
		if(!shutdownDone) {
			try {
				DataStore.instance.shutdown()
			} catch(Throwable t) {
				log.error('DB shutdown failed!', t)
			} finally {
				shutdownDone = true			
			}
		}
	}
	
	@Override
	void contextInitialized(ServletContextEvent sce) { initDb() }

	@Override
	void contextDestroyed(ServletContextEvent sce) { shutdownDb() }
}
