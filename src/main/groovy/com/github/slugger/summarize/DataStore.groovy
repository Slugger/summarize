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

import groovy.sql.Sql
import groovy.util.logging.Log4j

import java.sql.SQLException

@Log4j
class DataStore {
	static private final String JDBC_DRIVER = 'org.apache.derby.jdbc.ClientDriver'
	
	static private DataStore INSTANCE = null
	synchronized static DataStore getInstance() {
		if(!INSTANCE)
			INSTANCE = new DataStore()
		return INSTANCE
	}

	private final Sql sql = null
	
	private DataStore() {
		def jdbcStr = 'jdbc:derby://db:1530/summarize' //;create=true'
		log.info "Connecting to database: $jdbcStr"
		try {
			sql = Sql.newInstance(jdbcStr, JDBC_DRIVER)
			log.info 'Connected to existing database'
		} catch(SQLException e) {
			if(e.SQLState == '08004' && e.message.toLowerCase().contains('not found')) {
				jdbcStr += ';create=true'
				sql = Sql.newInstance(jdbcStr, JDBC_DRIVER)
				createTables()
				setDbVersion()
				log.info 'New database created'
			} else
				throw e
		}
	}

	void updateBuild(long bldId, long taskLinkId, String state, String desc, String url, Date start = null, Date finish = null, String author = null)	{
		def now = new Date()
		sql.withTransaction {
			sql.execute("DELETE FROM completed_task WHERE build_id = $bldId AND does_task_id = $taskLinkId")
			sql.execute("INSERT INTO completed_task (build_id, does_task_id, start, finish, updated, updated_by, url, brief_desc, state) VALUES ($bldId, $taskLinkId, ${start ?: now}, $finish, $now, ${author ?: 'automation'}, $url, $desc, $state)")
		}
	}
	
	Long getTaskLinkId(long taskId, long prodId) {
		sql.firstRow("SELECT id FROM does_task WHERE task_id = $taskId AND prod_id = $prodId")?.id
	}
	
	long registerBuild(long prodId, String build) {
		def row = sql.firstRow("SELECT id FROM prod_build WHERE prod_id = $prodId AND build = $build") 
		if(!row)
			sql.executeInsert("INSERT INTO prod_build (prod_id, build) VALUES ($prodId, $build)")[0][0]
		else
			row.id
	}
	
	Long getProductId(String name, String version) {
		sql.firstRow("SELECT id FROM product WHERE name = $name AND version = $version")?.id
	}
	
	Long getTaskId(String name) {
		sql.firstRow("SELECT id FROM task WHERE name = $name")?.id
	}
	
	void addProduct(String name, String version, String desc) {
		sql.execute("INSERT INTO product (name, version, description) VALUES ($name, $version, $desc)")
	}
	
	void addTask(String name, String desc) {
		sql.execute("INSERT INTO task (name, description) VALUES ($name, $desc)")
	}
	
	Map getProducts() {
		def map = [:]
		sql.eachRow("SELECT id, name FROM product") {
			map[it['id']] = it['name']
		}
		map
	}
	
	Map getTasks() {
		def map = [:]
		sql.eachRow("SELECT id, name FROM task") {
			map[it['id']] = it['name']
		}
		map
	}
	
	Map getLatestComment(long bldId) {
		def qry = "SELECT author, comment, updated FROM notes WHERE build_id = $bldId ORDER BY updated DESC"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(qry)
			def qryStr = sql.asSql(qry, params)
			log.trace "$qryStr $params"
		}
		def comment = sql.firstRow(qry)
		if(comment) {
			comment = [
				author: comment.author,
				note: comment.comment,
				date: comment.updated
			]
		}
		comment
	}
	
	List getTasks(String product, String version) {
		def qry = "SELECT name FROM tasks_for_prod WHERE prod_name = $product AND prod_ver = $version"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(qry)
			def qryStr = sql.asSql(qry, params)
			log.trace "$qryStr $params"
		}
		def tasks = []
		sql.eachRow(qry) { tasks << it[0] }
		tasks
	}
	
	Map getBuilds(String product, String version) {
		def qry = "SELECT * FROM tasks_completed WHERE prod = $product AND version = $version"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(qry)
			def qryStr = sql.asSql(qry, params)
			log.trace "$qryStr $params"
		}
		def builds = [:]
		sql.eachRow(qry) {
			def bld = builds[it['build']]
			if(bld == null) {
				bld = [:]
				builds[it['build']] = bld
			}
			def task = bld[it['task_name']]
			if(!task || task['finish'].before(it['finish'])) {
				task = [
					buildId: it['build_id'],
					start: it['start'],
					finish: it['finish'],
					updated: it['updated'],
					updatedBy: it['updated_by'],
					state: it['state'],
					url: it['url'],
					stateDesc: it['brief_desc']
				]
				bld[it['task_name']] = task
			}
		}
		builds
	}
	
	String getSetting(String name, String defaultValue = null) {
		def qry = "SELECT value FROM settings WHERE name = $name"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(qry)
			def qryStr = sql.asSql(qry, params)
			log.trace "$qryStr $params"
		}
		return sql.firstRow(qry)?.value ?: defaultValue
	}

	void setSetting(String name, String value) {
		def delQry = "DELETE FROM settings WHERE name = $name"
		def insQry = "INSERT INTO settings (name, value) VALUES ($name, $value)"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(delQry)
			def qryStr = sql.asSql(delQry, params)
			log.trace "$qryStr $params"
			params = sql.getParameters(insQry)
			qryStr = sql.asSql(insQry, params)
			log.trace "$qryStr $params"
		}
		sql.withTransaction {
			sql.execute delQry
			if(sql.executeUpdate(insQry) != 1)
				throw new RuntimeException('DBError writing setting value')
		}
	}
	
	void link(long prodId, long[] taskIds) {
		def delQry = "DELETE FROM does_task WHERE prod_id = $prodId"
		if(log.isTraceEnabled()) {
			def params = sql.getParameters(delQry)
			def qryStr = sql.asSql(delQry, params)
			log.trace "$qryStr $params"
		}
		sql.withTransaction {
			sql.execute delQry
			taskIds.each {
				def insQry = "INSERT INTO does_task (prod_id, task_id, ordering) VALUES ($prodId, $it, 1)"
				if(log.isTraceEnabled()) {
					def params = sql.getParameters(insQry)
					def qryStr = sql.asSql(insQry, params)
					log.trace "$qryStr $params"
				}
				sql.execute insQry
			}
		}
	}
		
	private void setDbVersion() {
		def qry = "INSERT INTO settings (name, value) VALUES ('dbVersion', '0')"
		if(log.isTraceEnabled())
			log.trace qry
		sql.execute qry
	}
	
	private void createTables() {
		sql.withTransaction {
			sql.execute '''
				CREATE TABLE product (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					name VARCHAR(128) NOT NULL,
					version VARCHAR(128) NOT NULL,
					description VARCHAR(255),
					CONSTRAINT unique_name_ver UNIQUE (name, version)
				)
			'''
			
			sql.execute '''
				CREATE TABLE prod_build (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					prod_id BIGINT NOT NULL,
					build VARCHAR(64) NOT NULL,
					CONSTRAINT prod_build_prod_id_ref FOREIGN KEY (prod_id) REFERENCES product(id) ON DELETE CASCADE,
					CONSTRAINT unique_prod_bld UNIQUE (prod_id, build)
				)
			'''
			
			sql.execute '''
				CREATE TABLE task (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					name VARCHAR(64) NOT NULL CONSTRAINT unique_name UNIQUE,
					description VARCHAR(255)
				)
			'''
			
			sql.execute '''
				CREATE TABLE does_task (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					task_id BIGINT NOT NULL,
					prod_id BIGINT NOT NULL,
					ordering INT NOT NULL,
					CONSTRAINT does_task_task_id_ref FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
					CONSTRAINT does_task_prod_id_ref FOREIGN KEY (prod_id) REFERENCES product(id) ON DELETE CASCADE,
					CONSTRAINT unique_task_prod UNIQUE (task_id, prod_id)
				)
			'''
			
			sql.execute '''
				CREATE TABLE completed_task (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					does_task_id BIGINT NOT NULL,
					build_id BIGINT NOT NULL,
					start TIMESTAMP NOT NULL,
					finish TIMESTAMP,
					updated TIMESTAMP NOT NULL,
					updated_by VARCHAR(128) NOT NULL,
					state CHAR(4),
					url LONG VARCHAR,
					brief_desc VARCHAR(32) NOT NULL,
					CONSTRAINT completed_task_does_task_id_ref FOREIGN KEY (does_task_id) REFERENCES does_task(id) ON DELETE CASCADE,
					CONSTRAINT completed_task_build_id_ref FOREIGN KEY (build_id) REFERENCES prod_build(id) ON DELETE CASCADE,
					CONSTRAINT unique_does_bld UNIQUE (does_task_id, build_id),
					CONSTRAINT valid_states CHECK (state IN ('RUN', 'PASS', 'FAIL', 'WARN'))
				)
			'''
						
			sql.execute '''
				CREATE TABLE notes (
					id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1) PRIMARY KEY,
					c_task_id BIGINT NOT NULL,
					author VARCHAR(128) NOT NULL,
					comment LONG VARCHAR NOT NULL,
					created TIMESTAMP NOT NULL,
					updated TIMESTAMP NOT NULL,
					CONSTRAINT notes_c_task_id_ref FOREIGN KEY (c_task_id) REFERENCES completed_task(id) ON DELETE CASCADE
				)
			'''
			
			sql.execute '''
				CREATE TABLE settings (
					name VARCHAR(64) NOT NULL PRIMARY KEY,
					value VARCHAR(128)
				)
			'''
			
			sql.execute '''
				CREATE VIEW tasks_completed AS
					SELECT ct.id AS id, p.name AS prod, p.version AS version, p.description AS prod_desc, pb.build AS build, 
						ct.start AS start, ct.finish AS finish, ct.updated AS updated, ct.updated_by as updated_by,
						ct.state AS state, ct.url AS URL, ct.brief_desc AS brief_desc, t.name AS task_name,
						t.description AS task_desc, pb.id AS build_id FROM
							app.product AS p LEFT OUTER JOIN app.prod_build AS pb ON (p.id = pb.prod_id)
							LEFT OUTER JOIN app.completed_task AS ct ON (ct.build_id = pb.id)
							LEFT OUTER JOIN app.does_task AS dt ON (dt.prod_id = p.id AND dt.id = ct.does_task_id)
							LEFT OUTER JOIN app.task AS t ON (t.id = dt.id)
						WHERE ct.id IS NOT NULL
			'''
			
			sql.execute '''
				CREATE VIEW tasks_for_prod AS
					SELECT t.id AS id, t.name AS name, t.description AS task_desc, p.name AS prod_name,
						p.version AS prod_ver, p.description AS prod_desc FROM
							app.task AS t LEFT OUTER JOIN app.does_task AS dt ON (t.id = dt.task_id)
							LEFT OUTER JOIN app.product AS p ON (dt.prod_id = p.id) ORDER BY ordering ASC
			'''
		}
	}
}
